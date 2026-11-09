package ast;
import java.util.Objects;
import visitor.Visitor;

public final class IfNode extends StmtNode {
    public final ExprNode condition;
    public final BlockNode thenBlock;
    public final BlockNode elseBlock;
    public IfNode(int linha, int coluna, ExprNode condition, BlockNode thenBlock, BlockNode elseBlock) {
        super(linha, coluna);
        this.condition = Objects.requireNonNull(condition, "condition");
        this.thenBlock = Objects.requireNonNull(thenBlock, "thenBlock");
        this.elseBlock = elseBlock;
    }
    @Override public <T> T accept(Visitor<T> v) {
        return v.visitIf(this);
    }
}
