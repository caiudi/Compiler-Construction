package visitor;
import ast.*;

public interface Visitor<T> {
    T visitProgram(ProgramNode node);
    T visitVarDecl(VarDeclNode node);
    T visitLocalVarStmt(LocalVarStmtNode node);
    T visitFunctionDecl(FunctionDeclNode node);
    T visitProcedureDecl(ProcedureDeclNode node);
    T visitParam(ParamNode node);
    T visitType(TypeNode node);
    T visitBlock(BlockNode node);
    T visitAssign(AssignNode node);
    T visitIf(IfNode node);
    T visitWhile(WhileNode node);
    T visitCallStmt(CallStmtNode node);
    T visitReturn(ReturnNode node);
    T visitIntLiteral(IntLiteralNode node);
    T visitDoubleLiteral(DoubleLiteralNode node);
    T visitBoolLiteral(BoolLiteralNode node);
    T visitStringLiteral(StringLiteralNode node);
    T visitCharLiteral(CharLiteralNode node);
    T visitId(IdNode node);
    T visitBinaryExpr(BinaryExprNode node);
    T visitUnaryExpr(UnaryExprNode node);
    T visitCallExpr(CallExprNode node);
}
