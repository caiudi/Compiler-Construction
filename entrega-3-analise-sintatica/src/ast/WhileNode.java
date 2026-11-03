package ast;
import java.util.Objects;
import visitor.Visitor;

public final class WhileNode extends StmtNode {
    public final ExprNode condition;
    public final BlockNode body;
    public WhileNode(int linha, int coluna, ExprNode condition, BlockNode body) {
        super(linha, coluna);
        this.condition = Objects.requireNonNull(condition, "condition");
        this.body = Objects.requireNonNull(body, "body");
    }
    @Override public <T> T accept(Visitor<T> v) {
        return v.visitWhile(this);
    }
}
