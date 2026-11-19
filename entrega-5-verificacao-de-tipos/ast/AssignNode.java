package ast;
import java.util.Objects;
import visitor.Visitor;

public final class AssignNode extends StmtNode {
    public final IdNode target;
    public final ExprNode value;
    public AssignNode(int linha, int coluna, IdNode target, ExprNode value) {
        super(linha, coluna);
        this.target = Objects.requireNonNull(target, "target");
        this.value = Objects.requireNonNull(value, "value");
    }
    @Override public <T> T accept(Visitor<T> v) {
        return v.visitAssign(this);
    }
}
