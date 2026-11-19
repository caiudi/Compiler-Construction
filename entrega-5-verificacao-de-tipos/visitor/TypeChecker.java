package visitor;

import ast.*;
import symbols.Symbol;
import symbols.SymbolCategory;
import types.Signature;
import types.Type;
import java.util.*;

/**
 * CP5: verificador de tipos. Roda sobre a AST decorada pela identificacao (CP4):
 * cada IdNode de uso aponta para o seu Symbol, que guarda tipo e assinatura.
 *
 * - Cada expressao recebe o seu tipo no campo ExprNode.type.
 * - Os erros sao acumulados; depois do primeiro erro numa expressao, o tipo ERROR
 *   sobe e e compativel com tudo, sem nova mensagem (sem cascata).
 * - Nao declara nomes nem abre/fecha escopos: so le IdNode.decl.
 * - Variavel local usada antes de receber valor gera AVISO (analise conservadora).
 *
 * Onde fica cada regra (ver regras-de-tipos-rascunho.md):
 *   [LIT-*]            visitIntLiteral ... visitCharLiteral
 *   [ID], [ID-SUBPROG] visitId
 *   [ARIT-*], [CONCAT], [REL], [IGUAL], [LOG]   visitBinaryExpr / binaryResult
 *   [NEG], [NAO]       visitUnaryExpr / unaryResult
 *   [ATRIB], [COMPAT]  visitAssign / assignable
 *   [DECL]             visitVarDecl
 *   [IF], [WHILE]      visitIf / visitWhile / condition
 *   [RETURN-*]         visitReturn
 *   [CAMINHOS]         visitFunctionDecl / ReturnPaths
 *   [CHAMADA], [CHAMADA-CMD]  call
 *   [PRINT], [READ]    visitPrint / visitRead
 */
public final class TypeChecker implements Visitor<Type> {

    // ------------------------------------------------------------------ mensagens

    private static final class Diagnostic {
        final int line, column, order;
        final String message;
        Diagnostic(int line, int column, int order, String message) {
            this.line = line; this.column = column; this.order = order; this.message = message;
        }
    }

    private final List<Diagnostic> errors = new ArrayList<>();
    private final List<Diagnostic> warnings = new ArrayList<>();
    private int sequence;

    private void error(Node at, String message) {
        errors.add(new Diagnostic(at.linha, at.coluna, sequence++, "linha " + at.linha + ": " + message));
    }

    private void warning(Node at, String message) {
        warnings.add(new Diagnostic(at.linha, at.coluna, sequence++, "linha " + at.linha + ": aviso: " + message));
    }

    private static List<String> sorted(List<Diagnostic> list) {
        List<Diagnostic> copy = new ArrayList<>(list);
        copy.sort(Comparator.comparingInt((Diagnostic d) -> d.line)
                .thenComparingInt(d -> d.column).thenComparingInt(d -> d.order));
        List<String> result = new ArrayList<>();
        for (Diagnostic d : copy) result.add(d.message);
        return Collections.unmodifiableList(result);
    }

    /** Erros de tipo, em ordem de linha e coluna. */
    public List<String> getErrors() { return sorted(errors); }

    /** Avisos (variavel possivelmente sem valor); nao impedem a geracao de codigo. */
    public List<String> getWarnings() { return sorted(warnings); }

    // ------------------------------------------------------------------ contexto

    /** Subprograma corrente: confere o return. */
    private static final class Subprogram {
        final String name;
        final Type result;   // VOID para procedimento
        Subprogram(String name, Type result) { this.name = name; this.result = result; }
    }

    private final Deque<Subprogram> subprograms = new ArrayDeque<>();
    /** Lacos abertos. A linguagem nao tem break; o contador fica para quando houver. */
    private int openLoops;

    // ------------------------------------------------------------------ fluxo (avisos)

    /** Variaveis que certamente ja receberam valor no ponto atual; dead = depois de um return. */
    private static final class Flow {
        final Set<Node> assigned = Collections.newSetFromMap(new IdentityHashMap<>());
        boolean dead;
        Flow copy() { Flow f = new Flow(); f.assigned.addAll(assigned); f.dead = dead; return f; }
        static Flow merge(Flow a, Flow b) {
            if (a.dead) return b;
            if (b.dead) return a;
            Flow f = a.copy();
            f.assigned.retainAll(b.assigned);
            return f;
        }
    }

    private Flow flow = new Flow();
    /** Variaveis locais declaradas sem inicializador: as unicas acompanhadas pela analise. */
    private final Set<Node> tracked = Collections.newSetFromMap(new IdentityHashMap<>());
    /** Um aviso por variavel. */
    private final Set<Node> warned = Collections.newSetFromMap(new IdentityHashMap<>());

    // ------------------------------------------------------------------ entrada

    public void check(ProgramNode program) {
        Objects.requireNonNull(program, "program").accept(this);
    }

    /** Calcula o tipo de uma expressao isolada (usado pelos testes das regras, sem parser). */
    public Type typeOf(ExprNode expression) {
        return type(expression);
    }

    private Type type(ExprNode e) {
        if (e == null) return Type.ERROR;
        Type t = e.accept(this);
        if (t == null) t = Type.ERROR;
        e.type = t;
        return t;
    }

    private void visit(Node n) {
        if (n != null) n.accept(this);
    }

    // ------------------------------------------------------------------ compatibilidade

    /**
     * [COMPAT] Um valor do tipo v pode ser guardado onde se espera t?
     * Igual; ou alargamento char -> int -> double (decisoes 1, 2 e 8).
     * double -> int e int -> char sao recusados. ERROR e compativel com tudo.
     */
    public static boolean assignable(Type t, Type v) {
        if (t.isError() || v.isError()) return true;
        if (t == v) return true;
        if (t == Type.DOUBLE) return v == Type.INT || v == Type.CHAR;
        if (t == Type.INT) return v == Type.CHAR;
        return false;
    }

    // ------------------------------------------------------------------ declaracoes

    @Override public Type visitProgram(ProgramNode n) {
        for (DeclNode d : n.declarations) d.accept(this);
        return null;
    }

    /** [DECL] T x = e;  e : V  e  V compativel com T. O nome so existe depois (ver identificacao). */
    @Override public Type visitVarDecl(VarDeclNode n) {
        Type declared = Type.of(n.type.kind);
        if (n.initializer != null) {
            Type value = type(n.initializer);
            if (!assignable(declared, value))
                error(n.name, "inicialização de '" + n.name.name + "' espera " + declared + ", mas recebeu " + value);
            flow.assigned.add(n);
        }
        return null;
    }

    @Override public Type visitLocalVarStmt(LocalVarStmtNode n) {
        visit(n.declaration);
        if (n.declaration.initializer == null) tracked.add(n.declaration);
        return null;
    }

    @Override public Type visitFunctionDecl(FunctionDeclNode n) {
        Type result = Type.of(n.returnType.kind);
        subprogramBody(n.name.name, result, n.body);
        // [CAMINHOS] decisao 9: todo caminho de uma funcao termina em return.
        if (!n.body.accept(RETURN_PATHS))
            error(n, "função '" + n.name.name + "' pode terminar sem return");
        return null;
    }

    @Override public Type visitProcedureDecl(ProcedureDeclNode n) {
        subprogramBody(n.name.name, Type.VOID, n.body);
        return null;
    }

    @Override public Type visitLocalSubprogram(LocalSubprogramStmtNode n) {
        visit(n.declaration);
        return null;
    }

    private void subprogramBody(String name, Type result, BlockNode body) {
        Flow outer = flow;
        int outerLoops = openLoops;
        flow = outer.copy();          // variaveis do nivel de cima: o que ja tinha valor na declaracao
        flow.dead = false;
        openLoops = 0;                // um laco de fora nao vale dentro do subprograma
        subprograms.push(new Subprogram(name, result));
        try {
            visit(body);
        } finally {
            subprograms.pop();
            flow = outer;
            openLoops = outerLoops;
        }
    }

    @Override public Type visitParam(ParamNode n) { return null; }
    @Override public Type visitType(TypeNode n) { return null; }

    // ------------------------------------------------------------------ comandos

    @Override public Type visitBlock(BlockNode n) {
        for (StmtNode s : n.statements) visit(s);
        return null;
    }

    /** Tipo do alvo de atribuicao/read: precisa ser variavel ou parametro. */
    private Type target(IdNode id) {
        Symbol s = id.decl;
        Type t;
        if (s == null) {
            t = Type.ERROR;                       // ja reportado pela identificacao
        } else if (s.isSubprogram()) {
            error(id, "'" + id.name + "' não é variável");
            t = Type.ERROR;
        } else {
            t = s.type == null ? Type.ERROR : s.type;
            flow.assigned.add(s.declaration);
        }
        id.type = t;
        return t;
    }

    /** [ATRIB] x : T   e : V   V compativel com T  =>  x = e; ok */
    @Override public Type visitAssign(AssignNode n) {
        Type value = type(n.value);
        Type declared = target(n.target);
        if (!assignable(declared, value))
            error(n, "atribuição a '" + n.target.name + "' espera " + declared + ", mas recebeu " + value);
        return null;
    }

    private void condition(Node statement, String keyword, ExprNode condition) {
        Type c = type(condition);
        if (!c.isError() && c != Type.BOOL)
            error(statement, "condição de '" + keyword + "' deve ser bool, mas é " + c);
    }

    /** [IF] e : bool  =>  if (e) S1 else S2 ok   (decisao 3: int nao e condicao) */
    @Override public Type visitIf(IfNode n) {
        condition(n, "if", n.condition);
        Flow before = flow.copy();
        visit(n.thenBlock);
        Flow afterThen = flow;
        flow = before;
        visit(n.elseBlock);
        flow = Flow.merge(afterThen, flow);
        return null;
    }

    /** [WHILE] e : bool  =>  while (e) S ok */
    @Override public Type visitWhile(WhileNode n) {
        condition(n, "while", n.condition);
        Flow before = flow.copy();
        openLoops++;
        try {
            visit(n.body);
        } finally {
            openLoops--;
        }
        flow = before;                // o corpo pode nao executar
        return null;
    }

    /** [CHAMADA-CMD] decisao 6: funcao usada como comando e permitida; o valor e descartado. */
    @Override public Type visitCallStmt(CallStmtNode n) {
        call(n.call, true);
        return null;
    }

    /** [RETURN-*] confere o return com o subprograma corrente. */
    @Override public Type visitReturn(ReturnNode n) {
        Subprogram current = subprograms.peek();
        Type value = n.value == null ? null : type(n.value);
        if (current != null) {
            if (current.result == Type.VOID) {
                if (n.value != null)
                    error(n, "procedimento '" + current.name + "' não pode retornar valor");
            } else if (n.value == null) {
                error(n, "return sem valor em '" + current.name + "', que retorna " + current.result);
            } else if (!assignable(current.result, value)) {
                error(n, "'" + current.name + "' retorna " + current.result + ", mas o return devolve " + value);
            }
        }
        flow.dead = true;
        return null;
    }

    /** [PRINT] e : T, T qualquer tipo basico  =>  print(e) ok */
    @Override public Type visitPrint(PrintNode n) {
        type(n.value);
        return null;
    }

    /** [READ] x variavel de tipo basico  =>  read(x) ok */
    @Override public Type visitRead(ReadNode n) {
        target(n.target);
        return null;
    }

    // ------------------------------------------------------------------ expressoes

    @Override public Type visitIntLiteral(IntLiteralNode n) { return Type.INT; }
    @Override public Type visitDoubleLiteral(DoubleLiteralNode n) { return Type.DOUBLE; }
    @Override public Type visitBoolLiteral(BoolLiteralNode n) { return Type.BOOL; }
    @Override public Type visitStringLiteral(StringLiteralNode n) { return Type.STRING; }
    @Override public Type visitCharLiteral(CharLiteralNode n) { return Type.CHAR; }

    /** [ID] x declarado com tipo T => x : T.  [ID-SUBPROG] decisao 11: nome de funcao sem () e erro. */
    @Override public Type visitId(IdNode n) {
        Symbol s = n.decl;
        if (s == null) return Type.ERROR;          // identificacao ja reportou
        if (s.isSubprogram()) {
            error(n, (s.category == SymbolCategory.FUNCTION ? "função '" + n.name + "' usada"
                    : "procedimento '" + n.name + "' usado") + " sem parênteses");
            return Type.ERROR;
        }
        if (tracked.contains(s.declaration) && !flow.assigned.contains(s.declaration)
                && !flow.dead && warned.add(s.declaration))
            warning(n, "variável '" + n.name + "' pode ser usada sem valor");
        return s.type == null ? Type.ERROR : s.type;
    }

    @Override public Type visitBinaryExpr(BinaryExprNode n) {
        Type l = type(n.left);
        Type r = type(n.right);
        if (l.isError() || r.isError()) return Type.ERROR;  // sem cascata
        Type result = binaryResult(n.operator, l, r);
        if (result == null) {
            error(n, "operador '" + symbol(n.operator) + "' não se aplica a " + l + " e " + r);
            return Type.ERROR;
        }
        return result;
    }

    /**
     * Tabela de operadores binarios. Devolve null quando o par de tipos e recusado.
     * (switch sobre o enum do operador, nao sobre o tipo do no)
     */
    public static Type binaryResult(Operator op, Type l, Type r) {
        switch (op) {
            case MAIS:
                // [CONCAT] decisao 4: + de string so com string
                if (l == Type.STRING && r == Type.STRING) return Type.STRING;
                return arithmetic(l, r);
            case MENOS: case MULTIPLICACAO: case DIVISAO:
                return arithmetic(l, r);
            case MENOR: case MENOR_IGUAL: case MAIOR: case MAIOR_IGUAL:
                // [REL] so numericos (decisao 4: string nao tem <)
                return l.isNumeric() && r.isNumeric() ? Type.BOOL : null;
            case IGUAL: case DIFERENTE:
                // [IGUAL] decisao 5: numericos entre si, ou bool/string com o mesmo tipo
                if (l.isNumeric() && r.isNumeric()) return Type.BOOL;
                return l == r && (l == Type.BOOL || l == Type.STRING) ? Type.BOOL : null;
            case E_LOGICO: case OU_LOGICO:
                // [LOG]
                return l == Type.BOOL && r == Type.BOOL ? Type.BOOL : null;
            default:
                return null;
        }
    }

    /** [ARIT-INT] / [ARIT-DOUBLE]: char vale como int (decisao 2); havendo double, o resultado e double (decisao 1). */
    private static Type arithmetic(Type l, Type r) {
        if (!l.isNumeric() || !r.isNumeric()) return null;   // decisao 3: bool fora da aritmetica
        return l == Type.DOUBLE || r == Type.DOUBLE ? Type.DOUBLE : Type.INT;
    }

    @Override public Type visitUnaryExpr(UnaryExprNode n) {
        Type t = type(n.operand);
        if (t.isError()) return Type.ERROR;
        Type result = unaryResult(n.operator, t);
        if (result == null) {
            error(n, "operador '" + symbol(n.operator) + "' não se aplica a " + t);
            return Type.ERROR;
        }
        return result;
    }

    /** [NEG] -e: int/char -> int, double -> double.  [NAO] !e: bool -> bool. */
    public static Type unaryResult(Operator op, Type t) {
        switch (op) {
            case MENOS:
                if (t == Type.DOUBLE) return Type.DOUBLE;
                return t.isNumeric() ? Type.INT : null;
            case NEGACAO:
                return t == Type.BOOL ? Type.BOOL : null;
            default:
                return null;
        }
    }

    @Override public Type visitCallExpr(CallExprNode n) {
        return call(n, false);
    }

    /**
     * [CHAMADA] f : (T1..Tn) -> R,  ei : Vi,  Vi compativel com Ti  =>  f(e1..en) : R
     * Confere: e funcao/procedimento; procedimento so como comando; aridade; cada argumento.
     */
    private Type call(CallExprNode n, boolean asStatement) {
        Symbol s = n.callee.decl;
        Type result;
        if (s == null) {
            typeAll(n.arguments);                          // identificacao ja reportou
            result = Type.ERROR;
        } else if (!s.isSubprogram() || s.signature == null) {
            error(n, "'" + n.callee.name + "' não é função nem procedimento");
            typeAll(n.arguments);
            result = Type.ERROR;
        } else {
            Signature sig = s.signature;
            boolean procedureInExpression = s.category == SymbolCategory.PROCEDURE && !asStatement;
            if (procedureInExpression)
                error(n, "procedimento '" + n.callee.name + "' usado em expressão");
            if (n.arguments.size() != sig.parameters.size()) {
                error(n, "'" + n.callee.name + "' espera " + sig.parameters.size()
                        + " argumento(s), recebeu " + n.arguments.size());
                typeAll(n.arguments);
            } else {
                for (int i = 0; i < n.arguments.size(); i++) {
                    ExprNode arg = n.arguments.get(i);
                    Type got = type(arg);
                    Type expected = sig.parameters.get(i);
                    // decisao 8: argumento aceita o mesmo alargamento da atribuicao (int -> double)
                    if (!assignable(expected, got))
                        error(arg, "argumento " + (i + 1) + " de '" + n.callee.name
                                + "' esperado " + expected + ", veio " + got);
                }
            }
            result = procedureInExpression ? Type.ERROR : sig.result;
        }
        n.type = result;
        return result;
    }

    private void typeAll(List<ExprNode> expressions) {
        for (ExprNode e : expressions) type(e);
    }

    // ------------------------------------------------------------------ auxiliares

    static String symbol(Operator op) {
        switch (op) {
            case OU_LOGICO: return "||";
            case E_LOGICO: return "&&";
            case IGUAL: return "==";
            case DIFERENTE: return "!=";
            case MENOR: return "<";
            case MENOR_IGUAL: return "<=";
            case MAIOR: return ">";
            case MAIOR_IGUAL: return ">=";
            case MAIS: return "+";
            case MENOS: return "-";
            case MULTIPLICACAO: return "*";
            case DIVISAO: return "/";
            case NEGACAO: return "!";
            default: return op.name();
        }
    }

    /**
     * [CAMINHOS] "todo caminho termina em return":
     *   return              -> sim
     *   bloco               -> algum comando do bloco garante return
     *   if com else         -> os dois ramos garantem
     *   if sem else, while  -> nao (o ramo/corpo pode nao executar)
     */
    private static final Visitor<Boolean> RETURN_PATHS = new VisitorBase<Boolean>() {
        @Override protected Boolean padrao() { return false; }
        @Override public Boolean visitReturn(ReturnNode n) { return true; }
        @Override public Boolean visitBlock(BlockNode n) {
            for (StmtNode s : n.statements) if (s.accept(this)) return true;
            return false;
        }
        @Override public Boolean visitIf(IfNode n) {
            return n.elseBlock != null && n.thenBlock.accept(this) && n.elseBlock.accept(this);
        }
    };
}
