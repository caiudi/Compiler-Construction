package ast;
import java.util.Objects;
import visitor.Visitor;

public final class LocalVarStmtNode extends StmtNode {
    public final VarDeclNode declaration;
    public LocalVarStmtNode(int linha, int coluna, VarDeclNode declaration) {
        super(linha, coluna);
        this.declaration = Objects.requireNonNull(declaration, "declaration");
    }
    @Override public <T> T accept(Visitor<T> v) {
        return v.visitLocalVarStmt(this);
    }
}
