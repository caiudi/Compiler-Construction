package visitor;
import ast.*;
import java.util.stream.Collectors;

/** Reimprime programa canonico; expressoes binarias sempre parentetizadas. */
public final class SourcePrinter implements Visitor<String> {
    public String print(Node n) { return n.accept(this); }
    private String expr(Node n) { return n == null ? "" : n.accept(this); }
    private String params(java.util.List<ParamNode> p) { return p.stream().map(this::expr).collect(Collectors.joining(", ")); }
    private String args(java.util.List<ExprNode> a) { return a.stream().map(this::expr).collect(Collectors.joining(", ")); }
    private String op(Operator o) { return switch(o) {
        case OU_LOGICO -> "||"; case E_LOGICO -> "&&"; case IGUAL -> "=="; case DIFERENTE -> "!=";
        case MENOR -> "<"; case MENOR_IGUAL -> "<="; case MAIOR -> ">"; case MAIOR_IGUAL -> ">=";
        case MAIS -> "+"; case MENOS -> "-"; case MULTIPLICACAO -> "*"; case DIVISAO -> "/"; case NEGACAO -> "!"; }; }
    @Override public String visitProgram(ProgramNode n) { return n.declarations.stream().map(this::expr).collect(Collectors.joining("\n")); }
    @Override public String visitVarDecl(VarDeclNode n) { return expr(n.type)+" "+expr(n.name)+(n.initializer==null?"":" = "+expr(n.initializer))+";"; }
    @Override public String visitLocalVarStmt(LocalVarStmtNode n) { return expr(n.declaration); }
    @Override public String visitFunctionDecl(FunctionDeclNode n) { return expr(n.returnType)+" "+expr(n.name)+"("+params(n.parameters)+") "+expr(n.body); }
    @Override public String visitProcedureDecl(ProcedureDeclNode n) { return "void "+expr(n.name)+"("+params(n.parameters)+") "+expr(n.body); }
    @Override public String visitParam(ParamNode n) { return expr(n.type)+" "+expr(n.name); }
    @Override public String visitType(TypeNode n) { return n.kind.name().toLowerCase(java.util.Locale.ROOT); }
    @Override public String visitBlock(BlockNode n) { return "{\n"+n.statements.stream().map(s -> "  "+expr(s).replace("\n","\n  ")).collect(Collectors.joining("\n"))+(n.statements.isEmpty()?"":"\n")+"}"; }
    @Override public String visitAssign(AssignNode n) { return expr(n.target)+" = "+expr(n.value)+";"; }
    @Override public String visitIf(IfNode n) { return "if ("+expr(n.condition)+") "+expr(n.thenBlock)+(n.elseBlock==null?"":" else "+expr(n.elseBlock)); }
    @Override public String visitWhile(WhileNode n) { return "while ("+expr(n.condition)+") "+expr(n.body); }
    @Override public String visitCallStmt(CallStmtNode n) { return expr(n.call)+";"; }
    @Override public String visitReturn(ReturnNode n) { return "return"+(n.value==null?"":" "+expr(n.value))+";"; }
    @Override public String visitIntLiteral(IntLiteralNode n) { return n.lexeme; }
    @Override public String visitDoubleLiteral(DoubleLiteralNode n) { return n.lexeme; }
    @Override public String visitBoolLiteral(BoolLiteralNode n) { return Boolean.toString(n.value); }
    @Override public String visitStringLiteral(StringLiteralNode n) { return n.lexeme; }
    @Override public String visitCharLiteral(CharLiteralNode n) { return "'"+(n.value=='\n'?"\\n":n.value=='\t'?"\\t":n.value=='\r'?"\\r":n.value=='\0'?"\\0":n.value=='\''?"\\'":n.value=='\\'?"\\\\":String.valueOf(n.value))+"'"; }
    @Override public String visitId(IdNode n) { return n.name; }
    @Override public String visitBinaryExpr(BinaryExprNode n) { return "("+expr(n.left)+" "+op(n.operator)+" "+expr(n.right)+")"; }
    @Override public String visitUnaryExpr(UnaryExprNode n) { return "("+op(n.operator)+expr(n.operand)+")"; }
    @Override public String visitCallExpr(CallExprNode n) { return expr(n.callee)+"("+args(n.arguments)+")"; }
}
