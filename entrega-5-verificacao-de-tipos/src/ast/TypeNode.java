package ast;
import java.util.Objects;
import visitor.Visitor;

public final class TypeNode extends Node {
    public final BasicType kind;
    public TypeNode(int linha, int coluna, BasicType kind) {
        super(linha, coluna);
        this.kind = Objects.requireNonNull(kind, "kind");
    }
    @Override public <T> T accept(Visitor<T> v) {
        return v.visitType(this);
    }
}
