package visitor;

import ast.*;
import symbols.*;
import types.Signature;
import types.Type;
import java.util.*;

/**
 * CP4: identificacao de nomes, sem checagem de tipos.
 * Erros sao ordenados por linha, coluna e ordem de registro.
 * O corpo externo de uma funcao compartilha escopo com seus parametros.
 *
 * CP5:
 * - cada Symbol recebe o tipo da declaracao (e a assinatura, para funcoes e
 *   procedimentos), que o verificador de tipos le em IdNode.decl;
 * - funcoes/procedimentos declarados dentro de um bloco sao pre-declarados no
 *   escopo desse bloco antes dos comandos (mesma regra do nivel global);
 * - o inicializador de uma variavel e resolvido ANTES de o nome ser declarado:
 *   em "int x = x;" o x da direita nao e o x que esta sendo declarado;
 * - print e read percorrem a expressao / o identificador.
 */
public final class IdentificationVisitor implements Visitor<Void> {
    private final SymbolTable table;
    private final List<Diagnostic> diagnostics = new ArrayList<>();
    private int sequence;
    /** Variaveis globais pre-registradas (para detectar redeclaracao) mas ainda nao alcancadas na ordem textual. */
    private final Set<Node> pendingGlobals = Collections.newSetFromMap(new IdentityHashMap<>());

    private static final class Diagnostic {
        final int line, column, order;
        final String message;
        Diagnostic(int line, int column, int order, String message) {
            this.line=line; this.column=column; this.order=order; this.message=message;
        }
    }

    public IdentificationVisitor(SymbolTable table) {
        this.table = Objects.requireNonNull(table, "table");
        if (table.level() != 0) throw new IllegalArgumentException("Tabela deve iniciar no nivel global");
    }

    public List<String> getErrors() {
        List<Diagnostic> sorted = new ArrayList<>(diagnostics);
        sorted.sort(Comparator.comparingInt((Diagnostic d)->d.line)
                .thenComparingInt(d->d.column).thenComparingInt(d->d.order));
        List<String> result = new ArrayList<>();
        for (Diagnostic d : sorted) result.add(d.message);
        return Collections.unmodifiableList(result);
    }

    private void error(int line, int col, String message) {
        diagnostics.add(new Diagnostic(line,col,sequence++, "linha " + line + ": " + message));
    }

    private void declare(IdNode id, Node owner, SymbolCategory category, Type type, Signature signature) {
        if (id == null || owner == null) return;
        Symbol previous = table.lookupLocal(id.name);
        Symbol symbol = new Symbol(id.name, category, owner, table.level(), type, signature);
        if (!table.declare(symbol)) {
            int firstLine = previous == null ? owner.linha : previous.declaration.linha;
            error(id.linha, id.coluna, "'" + id.name
                    + "' já declarado neste escopo (linha " + firstLine + ")");
        }
    }

    private static Signature signature(List<ParamNode> params, Type result) {
        List<Type> types = new ArrayList<>();
        for (ParamNode p : params) types.add(Type.of(p.type.kind));
        return new Signature(types, result);
    }

    private void declareFunction(FunctionDeclNode n) {
        Type result = Type.of(n.returnType.kind);
        declare(n.name, n, SymbolCategory.FUNCTION, result, signature(n.parameters, result));
    }

    private void declareProcedure(ProcedureDeclNode n) {
        declare(n.name, n, SymbolCategory.PROCEDURE, Type.VOID, signature(n.parameters, Type.VOID));
    }

    private void declareVariable(VarDeclNode n) {
        declare(n.name, n, SymbolCategory.VARIABLE, Type.of(n.type.kind), null);
    }

    /** Comandos de um bloco: primeiro pre-declara os subprogramas locais, depois visita na ordem. */
    private void children(BlockNode block) {
        if (block == null) return;
        for (StmtNode statement : block.statements) statement.accept(predeclareLocal);
        for (StmtNode statement : block.statements) visit(statement);
    }

    private void visit(Node node) {
        if (node != null) node.accept(this);
    }

    private void functionBody(List<ParamNode> params, BlockNode body) {
        table.openScope();
        try {
            for (ParamNode p : params) visit(p);
            children(body); // nao abre novo escopo para o corpo externo
        } finally {
            table.closeScope();
        }
    }

    /** Pre-declaracao global: funcoes, procedimentos e variaveis, na ordem do texto. */
    private final Visitor<Void> predeclare = new VisitorBase<Void>() {
        @Override public Void visitFunctionDecl(FunctionDeclNode n) { declareFunction(n); return null; }
        @Override public Void visitProcedureDecl(ProcedureDeclNode n) { declareProcedure(n); return null; }
        @Override public Void visitVarDecl(VarDeclNode n) {
            declareVariable(n);
            pendingGlobals.add(n); // visivel apenas a partir da sua posicao textual
            return null;
        }
    };

    /** CP5: pre-declaracao dos subprogramas declarados dentro de um bloco. */
    private final Visitor<Void> predeclareLocal = new VisitorBase<Void>() {
        @Override public Void visitLocalSubprogram(LocalSubprogramStmtNode n) {
            n.declaration.accept(subprogramHeader); return null;
        }
    };
    private final Visitor<Void> subprogramHeader = new VisitorBase<Void>() {
        @Override public Void visitFunctionDecl(FunctionDeclNode n) { declareFunction(n); return null; }
        @Override public Void visitProcedureDecl(ProcedureDeclNode n) { declareProcedure(n); return null; }
    };

    /** Segunda visita global: corpos e inicializadores, na ordem do texto. */
    private final Visitor<Void> globalBodies = new VisitorBase<Void>() {
        @Override public Void visitVarDecl(VarDeclNode n) {
            visit(n.initializer);     // o inicializador nao enxerga a propria variavel
            pendingGlobals.remove(n);
            return null;
        }
        @Override public Void visitFunctionDecl(FunctionDeclNode n) { return IdentificationVisitor.this.visitFunctionDecl(n); }
        @Override public Void visitProcedureDecl(ProcedureDeclNode n) { return IdentificationVisitor.this.visitProcedureDecl(n); }
    };

    @Override public Void visitProgram(ProgramNode n) {
        // Pre-registra todas as declaracoes globais (recursao mutua e redeclaracao na ordem do texto).
        for (DeclNode d : n.declarations) d.accept(predeclare);
        for (DeclNode d : n.declarations) d.accept(globalBodies);
        return null;
    }

    @Override public Void visitFunctionDecl(FunctionDeclNode n) {
        functionBody(n.parameters, n.body);
        return null;
    }
    @Override public Void visitProcedureDecl(ProcedureDeclNode n) {
        functionBody(n.parameters, n.body);
        return null;
    }
    /** Variavel local: o inicializador e resolvido antes de declarar o nome. */
    @Override public Void visitVarDecl(VarDeclNode n) {
        visit(n.initializer);
        declareVariable(n);
        return null;
    }
    @Override public Void visitLocalVarStmt(LocalVarStmtNode n) {
        visit(n.declaration);
        return null;
    }
    /** CP5: o nome ja foi pre-declarado em children(); aqui so o corpo e visitado. */
    @Override public Void visitLocalSubprogram(LocalSubprogramStmtNode n) {
        visit(n.declaration);
        return null;
    }
    @Override public Void visitParam(ParamNode n) {
        declare(n.name, n, SymbolCategory.PARAMETER, Type.of(n.type.kind), null);
        return null;
    }
    @Override public Void visitType(TypeNode n) { return null; }
    @Override public Void visitBlock(BlockNode n) {
        table.openScope();
        try { children(n); }
        finally { table.closeScope(); }
        return null;
    }
    @Override public Void visitAssign(AssignNode n) {
        visit(n.target); visit(n.value); return null;
    }
    @Override public Void visitIf(IfNode n) {
        visit(n.condition); visit(n.thenBlock); visit(n.elseBlock); return null;
    }
    @Override public Void visitWhile(WhileNode n) {
        visit(n.condition); visit(n.body); return null;
    }
    @Override public Void visitCallStmt(CallStmtNode n) {
        visit(n.call); return null;
    }
    @Override public Void visitReturn(ReturnNode n) {
        visit(n.value); return null;
    }
    @Override public Void visitPrint(PrintNode n) {
        visit(n.value); return null;
    }
    @Override public Void visitRead(ReadNode n) {
        visit(n.target); return null;
    }
    @Override public Void visitId(IdNode n) {
        n.decl = table.lookup(n.name);
        if (n.decl != null && pendingGlobals.contains(n.decl.declaration)) n.decl = null;
        if (n.decl == null) error(n.linha, n.coluna, "'" + n.name + "' não declarado");
        return null;
    }
    @Override public Void visitBinaryExpr(BinaryExprNode n) {
        visit(n.left); visit(n.right); return null;
    }
    @Override public Void visitUnaryExpr(UnaryExprNode n) {
        visit(n.operand); return null;
    }
    @Override public Void visitCallExpr(CallExprNode n) {
        visit(n.callee);
        for (ExprNode arg : n.arguments) visit(arg);
        return null;
    }
    @Override public Void visitIntLiteral(IntLiteralNode n) { return null; }
    @Override public Void visitDoubleLiteral(DoubleLiteralNode n) { return null; }
    @Override public Void visitBoolLiteral(BoolLiteralNode n) { return null; }
    @Override public Void visitStringLiteral(StringLiteralNode n) { return null; }
    @Override public Void visitCharLiteral(CharLiteralNode n) { return null; }
}
