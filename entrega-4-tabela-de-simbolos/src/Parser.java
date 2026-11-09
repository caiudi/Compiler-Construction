import ast.*;
import java.util.*;

/** Parser LL(1) do CP3: a AST so e valida quando nao ha erros. */
public final class Parser {
    private static final EnumSet<TokenType> TIPOS = EnumSet.of(TokenType.INT,TokenType.DOUBLE,TokenType.BOOL,TokenType.CHAR,TokenType.STRING);
    private static final EnumSet<TokenType> INICIO_DECL = EnumSet.of(TokenType.INT,TokenType.DOUBLE,TokenType.BOOL,TokenType.CHAR,TokenType.STRING,TokenType.VOID);
    private static final EnumSet<TokenType> INICIO_COMANDO = EnumSet.of(TokenType.INT,TokenType.DOUBLE,TokenType.BOOL,TokenType.CHAR,TokenType.STRING,TokenType.IDENTIFICADOR,TokenType.RETURN,TokenType.WHILE,TokenType.IF,TokenType.ABRE_CHAVES);
    private static final EnumSet<TokenType> INICIO_EXP = EnumSet.of(TokenType.NUMERO,TokenType.STRING_LITERAL,TokenType.CHAR_LITERAL,TokenType.TRUE,TokenType.FALSE,TokenType.IDENTIFICADOR,TokenType.ABRE_PARENTESES,TokenType.NEGACAO,TokenType.MENOS);
    private static final EnumSet<TokenType> FOLLOW_EXP = EnumSet.of(TokenType.PONTO_VIRGULA,TokenType.FECHA_PARENTESES,TokenType.VIRGULA);
    private static final EnumSet<TokenType> REL = EnumSet.of(TokenType.MENOR,TokenType.MENOR_IGUAL,TokenType.MAIOR,TokenType.MAIOR_IGUAL);
    private static EnumSet<TokenType> union(EnumSet<TokenType> s, TokenType... t) { EnumSet<TokenType> r = EnumSet.copyOf(s); Collections.addAll(r,t); return r; }
    private static final EnumSet<TokenType> FOLLOW_OU = EnumSet.copyOf(FOLLOW_EXP);
    private static final EnumSet<TokenType> FOLLOW_E = union(FOLLOW_OU,TokenType.OU_LOGICO);
    private static final EnumSet<TokenType> FOLLOW_IGUAL = union(FOLLOW_E,TokenType.E_LOGICO);
    private static final EnumSet<TokenType> FOLLOW_REL = union(FOLLOW_IGUAL,TokenType.IGUAL,TokenType.DIFERENTE);
    private static final EnumSet<TokenType> FOLLOW_ADITIVA = union(FOLLOW_REL,TokenType.MENOR,TokenType.MENOR_IGUAL,TokenType.MAIOR,TokenType.MAIOR_IGUAL);
    private static final EnumSet<TokenType> FOLLOW_MULT = union(FOLLOW_ADITIVA,TokenType.MAIS,TokenType.MENOS);
    private static final EnumSet<TokenType> FOLLOW_PRIMARIA = union(FOLLOW_MULT,TokenType.MULTIPLICACAO,TokenType.DIVISAO);
    public static final class ParseResult {
        public final ProgramNode ast;
        public final List<String> errors;
        public ParseResult(ProgramNode ast, List<String> errors) { this.ast=ast;this.errors=List.copyOf(errors); }
        public boolean valido() { return ast!=null && errors.isEmpty(); }
    }
    private final Scanner scanner;
    private Token lookahead;
    private final List<String> erros = new ArrayList<>();
    private ParseResult cached;
    public Parser(String codigoFonte) { scanner=new Scanner(codigoFonte);lookahead=scanner.nextToken(); }
    public List<String> getErros() { return Collections.unmodifiableList(erros); }
    public boolean analisar() { return parse().valido(); }
    public ParseResult parse() { if(cached!=null)return cached; ProgramNode p=parsePrograma(); cached=new ParseResult(erros.isEmpty()?p:null,erros);return cached; }
    private TokenType atual() { return lookahead.getTipo(); }
    private boolean is(TokenType t) { return atual()==t; }
    private boolean in(EnumSet<TokenType> s) { return s.contains(atual()); }
    private Token consume() {Token t=lookahead;lookahead=scanner.nextToken();return t;}
    private static final class ErroSintatico extends RuntimeException {}
    private void erro(String esperado) { erros.add("Erro sintatico na linha "+lookahead.getLinha()+", coluna "+lookahead.getColuna()+": esperado "+esperado+", encontrado "+atual()+" ('"+lookahead.getLexema()+"')");throw new ErroSintatico(); }
    private Token match(TokenType t) {if(!is(t))erro(t.name());return consume();}
    private void vazio(EnumSet<TokenType> follow,String ctx) {if(!in(follow))erro("continuação de "+ctx+" ou "+follow);}
    private void sincGlobal(){while(!is(TokenType.EOF)&&!in(INICIO_DECL))consume();}
    private void sincComando(){while(!is(TokenType.EOF)&&!is(TokenType.PONTO_VIRGULA)&&!is(TokenType.FECHA_CHAVES))consume();if(is(TokenType.PONTO_VIRGULA))consume();}
    private static int ln(Token t){return t.getLinha();} private static int col(Token t){return t.getColuna();}
    private IdNode id(){Token t=match(TokenType.IDENTIFICADOR);return new IdNode(ln(t),col(t),t.getLexema());}
    private ProgramNode parsePrograma(){Token t=lookahead;List<DeclNode> d=new ArrayList<>();while(true){if(in(INICIO_DECL)){try{d.add(global());}catch(ErroSintatico ex){sincGlobal();}}else if(is(TokenType.EOF))break;else{try{erro("declaração global ou EOF");}catch(ErroSintatico ex){consume();sincGlobal();}}}return new ProgramNode(ln(t),col(t),d);}
    private TypeNode tipo(){if(!in(TIPOS))erro("tipo int/double/bool/char/string");Token t=consume();return new TypeNode(ln(t),col(t),BasicType.valueOf(t.getTipo().name()));}
    private DeclNode global(){if(in(TIPOS)){TypeNode ty=tipo();IdNode name=id();if(is(TokenType.ATRIBUICAO)||is(TokenType.PONTO_VIRGULA)){ExprNode v=inicializacao();match(TokenType.PONTO_VIRGULA);return new VarDeclNode(ty.linha,ty.coluna,ty,name,v);}if(is(TokenType.ABRE_PARENTESES)){consume();List<ParamNode> ps=parametros();match(TokenType.FECHA_PARENTESES);BlockNode b=bloco();return new FunctionDeclNode(ty.linha,ty.coluna,ty,name,ps,b);}erro("=, ; ou (");}if(is(TokenType.VOID)){Token v=consume();IdNode name=id();match(TokenType.ABRE_PARENTESES);List<ParamNode> ps=parametros();match(TokenType.FECHA_PARENTESES);BlockNode b=bloco();return new ProcedureDeclNode(ln(v),col(v),name,ps,b);}erro("declaração global");return null;}
    private ExprNode inicializacao(){if(is(TokenType.ATRIBUICAO)){consume();return expressao();}vazio(EnumSet.of(TokenType.PONTO_VIRGULA),"Inicializacao");return null;}
    private List<ParamNode> parametros(){List<ParamNode> r=new ArrayList<>();if(in(TIPOS)){r.add(parametro());while(is(TokenType.VIRGULA)){consume();r.add(parametro());}vazio(EnumSet.of(TokenType.FECHA_PARENTESES),"ListaParametros");}else vazio(EnumSet.of(TokenType.FECHA_PARENTESES),"Parametros");return r;}
    private ParamNode parametro(){TypeNode t=tipo();IdNode i=id();return new ParamNode(t.linha,t.coluna,t,i);}
    private BlockNode bloco(){Token open=match(TokenType.ABRE_CHAVES);List<StmtNode> r=new ArrayList<>();while(true){if(in(INICIO_COMANDO)){try{r.add(comando());}catch(ErroSintatico ex){sincComando();}}else if(is(TokenType.FECHA_CHAVES))break;else if(is(TokenType.EOF))erro("comando ou }");else{try{erro("comando ou }");}catch(ErroSintatico ex){sincComando();}}}match(TokenType.FECHA_CHAVES);return new BlockNode(ln(open),col(open),r);}
    private StmtNode comando(){if(in(TIPOS)){TypeNode ty=tipo();IdNode n=id();ExprNode e=inicializacao();match(TokenType.PONTO_VIRGULA);VarDeclNode d=new VarDeclNode(ty.linha,ty.coluna,ty,n,e);return new LocalVarStmtNode(ty.linha,ty.coluna,d);}if(is(TokenType.IDENTIFICADOR)){IdNode n=id();if(is(TokenType.ATRIBUICAO)){consume();ExprNode v=expressao();match(TokenType.PONTO_VIRGULA);return new AssignNode(n.linha,n.coluna,n,v);}if(is(TokenType.ABRE_PARENTESES)){consume();List<ExprNode> a=argumentos();match(TokenType.FECHA_PARENTESES);match(TokenType.PONTO_VIRGULA);return new CallStmtNode(n.linha,n.coluna,new CallExprNode(n.linha,n.coluna,n,a));}erro("= ou (");}if(is(TokenType.RETURN)){Token t=consume();ExprNode v=null;if(in(INICIO_EXP))v=expressao();else vazio(EnumSet.of(TokenType.PONTO_VIRGULA),"ValorRetorno");match(TokenType.PONTO_VIRGULA);return new ReturnNode(ln(t),col(t),v);}if(is(TokenType.WHILE)){Token t=consume();match(TokenType.ABRE_PARENTESES);ExprNode e=expressao();match(TokenType.FECHA_PARENTESES);return new WhileNode(ln(t),col(t),e,bloco());}if(is(TokenType.IF)){Token t=consume();match(TokenType.ABRE_PARENTESES);ExprNode e=expressao();match(TokenType.FECHA_PARENTESES);BlockNode b=bloco(),other=null;if(is(TokenType.ELSE)){consume();other=bloco();}else vazio(union(INICIO_COMANDO,TokenType.FECHA_CHAVES),"OpcaoElse");return new IfNode(ln(t),col(t),e,b,other);}if(is(TokenType.ABRE_CHAVES))return bloco();erro("comando");return null;}
    private List<ExprNode> argumentos(){List<ExprNode> r=new ArrayList<>();if(in(INICIO_EXP)){r.add(expressao());while(is(TokenType.VIRGULA)){consume();r.add(expressao());}vazio(EnumSet.of(TokenType.FECHA_PARENTESES),"ListaArgumentos");}else vazio(EnumSet.of(TokenType.FECHA_PARENTESES),"Argumentos");return r;}
    private ExprNode expressao(){return ou();}
    private ExprNode bin(ExprNode l,Token op,ExprNode r){return new BinaryExprNode(l.linha,l.coluna,Operator.valueOf(op.getTipo().name()),l,r);}
    private ExprNode ou(){ExprNode l=e();while(is(TokenType.OU_LOGICO)){Token t=consume();l=bin(l,t,e());}vazio(FOLLOW_OU,"OuLogicoLinha");return l;}
    private ExprNode e(){ExprNode l=igual();while(is(TokenType.E_LOGICO)){Token t=consume();l=bin(l,t,igual());}vazio(FOLLOW_E,"ELogicoLinha");return l;}
    private ExprNode igual(){ExprNode l=rel();while(is(TokenType.IGUAL)||is(TokenType.DIFERENTE)){Token t=consume();l=bin(l,t,rel());}vazio(FOLLOW_IGUAL,"IgualdadeLinha");return l;}
    private ExprNode rel(){ExprNode l=aditiva();while(in(REL)){Token t=consume();l=bin(l,t,aditiva());}vazio(FOLLOW_REL,"RelacionalLinha");return l;}
    private ExprNode aditiva(){ExprNode l=mult();while(is(TokenType.MAIS)||is(TokenType.MENOS)){Token t=consume();l=bin(l,t,mult());}vazio(FOLLOW_ADITIVA,"AditivaLinha");return l;}
    private ExprNode mult(){ExprNode l=unaria();while(is(TokenType.MULTIPLICACAO)||is(TokenType.DIVISAO)){Token t=consume();l=bin(l,t,unaria());}vazio(FOLLOW_MULT,"MultiplicativaLinha");return l;}
    private ExprNode unaria(){if(is(TokenType.NEGACAO)||is(TokenType.MENOS)){Token t=consume();return new UnaryExprNode(ln(t),col(t),Operator.valueOf(t.getTipo().name()),unaria());}return primaria();}
    private static char decodeChar(String lexeme) {
        if (lexeme.length()==3) return lexeme.charAt(1);
        char escaped=lexeme.charAt(2);
        return switch (escaped) {case 'n' -> '\n'; case 't' -> '\t'; case 'r' -> '\r';
            case '0' -> '\0'; case '\\' -> '\\'; case '\'' -> '\''; case '"' -> '"';
            default -> throw new IllegalArgumentException("Escape de char invalido: "+lexeme);};
    }
    private ExprNode primaria(){if(is(TokenType.CHAR_LITERAL)){Token t=consume();return new CharLiteralNode(ln(t),col(t),decodeChar(t.getLexema()));}if(is(TokenType.NUMERO)){Token t=consume();String s=t.getLexema();return (s.contains(".")||s.contains("e")||s.contains("E"))?new DoubleLiteralNode(ln(t),col(t),s):new IntLiteralNode(ln(t),col(t),s);}if(is(TokenType.STRING_LITERAL)){Token t=consume();return new StringLiteralNode(ln(t),col(t),t.getLexema());}if(is(TokenType.TRUE)||is(TokenType.FALSE)){Token t=consume();return new BoolLiteralNode(ln(t),col(t),t.getTipo()==TokenType.TRUE);}if(is(TokenType.IDENTIFICADOR)){IdNode n=id();if(is(TokenType.ABRE_PARENTESES)){consume();List<ExprNode> a=argumentos();match(TokenType.FECHA_PARENTESES);return new CallExprNode(n.linha,n.coluna,n,a);}vazio(FOLLOW_PRIMARIA,"SufixoIdentificador");return n;}if(is(TokenType.ABRE_PARENTESES)){consume();ExprNode v=expressao();match(TokenType.FECHA_PARENTESES);return v;}erro("literal, identificador ou (");return null;}
}
