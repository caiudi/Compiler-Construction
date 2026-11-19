package ast;
import java.util.Objects;
import visitor.Visitor;

public final class UnaryExprNode extends ExprNode {
    public final Operator operator;
    public final ExprNode operand;
    public UnaryExprNode(int linha, int coluna, Operator operator, ExprNode operand) {
        super(linha, coluna);
        this.operator = Objects.requireNonNull(operator, "operator");
        this.operand = Objects.requireNonNull(operand, "operand");
    }
    @Override public <T> T accept(Visitor<T> v) {
        return v.visitUnaryExpr(this);
    }
}
