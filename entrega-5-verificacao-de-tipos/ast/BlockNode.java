package ast;
import java.util.List;
import java.util.Objects;
import visitor.Visitor;

public final class BlockNode extends StmtNode {
    public final List<StmtNode> statements;
    public BlockNode(int linha, int coluna, List<StmtNode> statements) {
        super(linha, coluna);
        this.statements = List.copyOf(Objects.requireNonNull(statements, "statements"));
    }
    @Override public <T> T accept(Visitor<T> v) {
        return v.visitBlock(this);
    }
}
