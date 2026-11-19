package ast;
import java.util.List;
import java.util.Objects;
import visitor.Visitor;

public final class CallExprNode extends ExprNode {
    public final IdNode callee;
    public final List<ExprNode> arguments;
    public CallExprNode(int linha, int coluna, IdNode callee, List<ExprNode> arguments) {
        super(linha, coluna);
        this.callee = Objects.requireNonNull(callee, "callee");
        this.arguments = List.copyOf(Objects.requireNonNull(arguments, "arguments"));
    }
    @Override public <T> T accept(Visitor<T> v) {
        return v.visitCallExpr(this);
    }
}
