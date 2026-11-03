package visitor;
import ast.*;

/** Impressao deterministica: dois espacos por nivel e um no por linha. */
public final class AstPrinter implements Visitor<Void> {
    private final StringBuilder out = new StringBuilder();
    private int depth;
    public String print(Node root) { out.setLength(0); depth=0; root.accept(this); return out.toString(); }
    private void line(String s) { out.append("  ".repeat(depth)).append(s).append('\n'); }
    private void child(Node n) { if (n != null) { depth++; n.accept(this); depth--; } }
    @Override public Void visitProgram(ProgramNode n) { line("ProgramNode"); n.declarations.forEach(this::child); return null; }
    @Override public Void visitVarDecl(VarDeclNode n) { line("VarDeclNode"); child(n.type);child(n.name);child(n.initializer);return null; }
    @Override public Void visitLocalVarStmt(LocalVarStmtNode n) { line("LocalVarStmtNode");child(n.declaration);return null; }
    @Override public Void visitFunctionDecl(FunctionDeclNode n) { line("FunctionDeclNode");child(n.returnType);child(n.name);n.parameters.forEach(this::child);child(n.body);return null; }
    @Override public Void visitProcedureDecl(ProcedureDeclNode n) { line("ProcedureDeclNode");child(n.name);n.parameters.forEach(this::child);child(n.body);return null; }
    @Override public Void visitParam(ParamNode n) { line("ParamNode");child(n.type);child(n.name);return null; }
    @Override public Void visitType(TypeNode n) { line("TypeNode(kind="+n.kind+")");return null; }
    @Override public Void visitBlock(BlockNode n) { line("BlockNode");n.statements.forEach(this::child);return null; }
    @Override public Void visitAssign(AssignNode n) { line("AssignNode");child(n.target);child(n.value);return null; }
    @Override public Void visitIf(IfNode n) { line("IfNode");child(n.condition);child(n.thenBlock);child(n.elseBlock);return null; }
    @Override public Void visitWhile(WhileNode n) { line("WhileNode");child(n.condition);child(n.body);return null; }
    @Override public Void visitCallStmt(CallStmtNode n) { line("CallStmtNode");child(n.call);return null; }
    @Override public Void visitReturn(ReturnNode n) { line("ReturnNode");child(n.value);return null; }
    @Override public Void visitIntLiteral(IntLiteralNode n) { line("IntLiteralNode(lexeme="+n.lexeme+")");return null; }
    @Override public Void visitDoubleLiteral(DoubleLiteralNode n) { line("DoubleLiteralNode(lexeme="+n.lexeme+")");return null; }
    @Override public Void visitBoolLiteral(BoolLiteralNode n) { line("BoolLiteralNode(value="+n.value+")");return null; }
    @Override public Void visitStringLiteral(StringLiteralNode n) { line("StringLiteralNode(lexeme="+n.lexeme.replace("\\","\\\\").replace("\n","\\n")+")");return null; }
    @Override public Void visitCharLiteral(CharLiteralNode n) { line("CharLiteralNode(value="+(int)n.value+")");return null; }
    @Override public Void visitId(IdNode n) { line("IdNode(name="+n.name+")");return null; }
    @Override public Void visitBinaryExpr(BinaryExprNode n) { line("BinaryExprNode(operator="+n.operator+")");child(n.left);child(n.right);return null; }
    @Override public Void visitUnaryExpr(UnaryExprNode n) { line("UnaryExprNode(operator="+n.operator+")");child(n.operand);return null; }
    @Override public Void visitCallExpr(CallExprNode n) { line("CallExprNode");child(n.callee);n.arguments.forEach(this::child);return null; }
}
