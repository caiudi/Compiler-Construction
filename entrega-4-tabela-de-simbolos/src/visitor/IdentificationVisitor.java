package visitor;

import ast.*;
import symbol.*;
import java.util.*;

/**
 * CP4: identificacao de nomes, sem checagem de tipos.
 * Erros sao ordenados por linha, coluna e ordem de registro.
 * O corpo externo de uma funcao compartilha escopo com seus parametros.
 */
public final class IdentificationVisitor implements Visitor<Void> {
    private final SymbolTable table;
    private final List<Diagnostic> diagnostics = new ArrayList<>();
    private int sequence;

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

    private void declare(IdNode id, Node owner, SymbolCategory category) {
        if (id == null || owner == null) return;
        Symbol previous = table.lookupLocal(id.name);
        Symbol symbol = new Symbol(id.name, category, owner, table.level());
        if (!table.declare(symbol)) {
            int firstLine = previous == null ? owner.linha : previous.declaration.linha;
            error(id.linha, id.coluna, "'" + id.name
                    + "' já declarado neste escopo (linha " + firstLine + ")");
        }
    }

    private void children(BlockNode block) {
        if (block != null) for (StmtNode statement : block.statements) visit(statement);
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

    // Visitor separado para predeclaracao: dispensa instanceof e switch.
    private final Visitor<Void> predeclare = new PredeclareVisitor();
    private final class PredeclareVisitor implements Visitor<Void> {
        @Override public Void visitFunctionDecl(FunctionDeclNode n) {
            declare(n.name, n, SymbolCategory.FUNCTION); return null;
        }
        @Override public Void visitProcedureDecl(ProcedureDeclNode n) {
            declare(n.name, n, SymbolCategory.PROCEDURE); return null;
        }
        @Override public Void visitProgram(ProgramNode n) { return null; }
        @Override public Void visitVarDecl(VarDeclNode n) {
            declare(n.name, n, SymbolCategory.VARIABLE); return null;
        }
        @Override public Void visitLocalVarStmt(LocalVarStmtNode n) { return null; }
        @Override public Void visitParam(ParamNode n) { return null; }
        @Override public Void visitType(TypeNode n) { return null; }
        @Override public Void visitBlock(BlockNode n) { return null; }
        @Override public Void visitAssign(AssignNode n) { return null; }
        @Override public Void visitIf(IfNode n) { return null; }
        @Override public Void visitWhile(WhileNode n) { return null; }
        @Override public Void visitCallStmt(CallStmtNode n) { return null; }
        @Override public Void visitReturn(ReturnNode n) { return null; }
        @Override public Void visitIntLiteral(IntLiteralNode n) { return null; }
        @Override public Void visitDoubleLiteral(DoubleLiteralNode n) { return null; }
        @Override public Void visitBoolLiteral(BoolLiteralNode n) { return null; }
        @Override public Void visitStringLiteral(StringLiteralNode n) { return null; }
        @Override public Void visitCharLiteral(CharLiteralNode n) { return null; }
        @Override public Void visitId(IdNode n) { return null; }
        @Override public Void visitBinaryExpr(BinaryExprNode n) { return null; }
        @Override public Void visitUnaryExpr(UnaryExprNode n) { return null; }
        @Override public Void visitCallExpr(CallExprNode n) { return null; }
    }

    private final Visitor<Void> globalBodies = new GlobalBodiesVisitor();
    private final class GlobalBodiesVisitor implements Visitor<Void> {
        @Override public Void visitVarDecl(VarDeclNode n) { visit(n.initializer); return null; }
        @Override public Void visitFunctionDecl(FunctionDeclNode n) { return IdentificationVisitor.this.visitFunctionDecl(n); }
        @Override public Void visitProcedureDecl(ProcedureDeclNode n) { return IdentificationVisitor.this.visitProcedureDecl(n); }
        @Override public Void visitProgram(ProgramNode n) { return null; }
        @Override public Void visitLocalVarStmt(LocalVarStmtNode n) { return null; }
        @Override public Void visitParam(ParamNode n) { return null; }
        @Override public Void visitType(TypeNode n) { return null; }
        @Override public Void visitBlock(BlockNode n) { return null; }
        @Override public Void visitAssign(AssignNode n) { return null; }
        @Override public Void visitIf(IfNode n) { return null; }
        @Override public Void visitWhile(WhileNode n) { return null; }
        @Override public Void visitCallStmt(CallStmtNode n) { return null; }
        @Override public Void visitReturn(ReturnNode n) { return null; }
        @Override public Void visitIntLiteral(IntLiteralNode n) { return null; }
        @Override public Void visitDoubleLiteral(DoubleLiteralNode n) { return null; }
        @Override public Void visitBoolLiteral(BoolLiteralNode n) { return null; }
        @Override public Void visitStringLiteral(StringLiteralNode n) { return null; }
        @Override public Void visitCharLiteral(CharLiteralNode n) { return null; }
        @Override public Void visitId(IdNode n) { return null; }
        @Override public Void visitBinaryExpr(BinaryExprNode n) { return null; }
        @Override public Void visitUnaryExpr(UnaryExprNode n) { return null; }
        @Override public Void visitCallExpr(CallExprNode n) { return null; }
    }


    @Override public Void visitProgram(ProgramNode n) {
        // Pre-registra todas as funcoes/procedimentos globais para recursao mutua.
        // A AST atual nao permite funcoes aninhadas.
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
    @Override public Void visitVarDecl(VarDeclNode n) {
        declare(n.name, n, SymbolCategory.VARIABLE);
        visit(n.initializer);
        return null;
    }
    @Override public Void visitLocalVarStmt(LocalVarStmtNode n) {
        visit(n.declaration);
        return null;
    }
    @Override public Void visitParam(ParamNode n) {
        declare(n.name, n, SymbolCategory.PARAMETER);
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
    @Override public Void visitId(IdNode n) {
        n.decl = table.lookup(n.name);
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
