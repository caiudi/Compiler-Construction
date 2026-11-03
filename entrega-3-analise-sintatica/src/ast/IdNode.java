package ast;
import java.util.Objects;
import visitor.Visitor;

public final class IdNode extends ExprNode {
    public final String name;
    public IdNode(int linha, int coluna, String name) {
        super(linha, coluna);
        this.name = Objects.requireNonNull(name, "name");
    }
    @Override public <T> T accept(Visitor<T> v) {
        return v.visitId(this);
    }
}
