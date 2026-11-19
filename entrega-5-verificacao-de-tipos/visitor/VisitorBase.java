package visitor;

import ast.*;

/**
 * Visitor que nao faz nada (devolve o valor padrao em todo no).
 * Os visitors auxiliares que so reagem a poucos tipos de no estendem esta classe
 * e sobrescrevem apenas esses metodos; a escolha continua sendo por double dispatch,
 * sem instanceof nem switch sobre o tipo do no.
 */
public abstract class VisitorBase<T> implements Visitor<T> {
    protected T padrao() { return null; }

    @Override public T visitProgram(ProgramNode n) { return padrao(); }
    @Override public T visitVarDecl(VarDeclNode n) { return padrao(); }
    @Override public T visitLocalVarStmt(LocalVarStmtNode n) { return padrao(); }
    @Override public T visitFunctionDecl(FunctionDeclNode n) { return padrao(); }
    @Override public T visitProcedureDecl(ProcedureDeclNode n) { return padrao(); }
    @Override public T visitParam(ParamNode n) { return padrao(); }
    @Override public T visitType(TypeNode n) { return padrao(); }
    @Override public T visitBlock(BlockNode n) { return padrao(); }
    @Override public T visitAssign(AssignNode n) { return padrao(); }
    @Override public T visitIf(IfNode n) { return padrao(); }
    @Override public T visitWhile(WhileNode n) { return padrao(); }
    @Override public T visitCallStmt(CallStmtNode n) { return padrao(); }
    @Override public T visitReturn(ReturnNode n) { return padrao(); }
    @Override public T visitIntLiteral(IntLiteralNode n) { return padrao(); }
    @Override public T visitDoubleLiteral(DoubleLiteralNode n) { return padrao(); }
    @Override public T visitBoolLiteral(BoolLiteralNode n) { return padrao(); }
    @Override public T visitStringLiteral(StringLiteralNode n) { return padrao(); }
    @Override public T visitCharLiteral(CharLiteralNode n) { return padrao(); }
    @Override public T visitId(IdNode n) { return padrao(); }
    @Override public T visitBinaryExpr(BinaryExprNode n) { return padrao(); }
    @Override public T visitUnaryExpr(UnaryExprNode n) { return padrao(); }
    @Override public T visitCallExpr(CallExprNode n) { return padrao(); }
    @Override public T visitPrint(PrintNode n) { return padrao(); }
    @Override public T visitRead(ReadNode n) { return padrao(); }
    @Override public T visitLocalSubprogram(LocalSubprogramStmtNode n) { return padrao(); }
}
