package ast;
import java.util.Objects;
import visitor.Visitor;

public final class BinaryExprNode extends ExprNode {
    public final Operator operator;
    public final ExprNode left;
    public final ExprNode right;
    public BinaryExprNode(int linha, int coluna, Operator operator, ExprNode left, ExprNode right) {
        super(linha, coluna);
        this.operator = Objects.requireNonNull(operator, "operator");
        this.left = Objects.requireNonNull(left, "left");
        this.right = Objects.requireNonNull(right, "right");
    }
    @Override public <T> T accept(Visitor<T> v) {
        return v.visitBinaryExpr(this);
    }
}
