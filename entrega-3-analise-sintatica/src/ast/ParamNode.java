package ast;
import java.util.Objects;
import visitor.Visitor;

public final class ParamNode extends Node {
    public final TypeNode type;
    public final IdNode name;
    public ParamNode(int linha, int coluna, TypeNode type, IdNode name) {
        super(linha, coluna);
        this.type = Objects.requireNonNull(type, "type");
        this.name = Objects.requireNonNull(name, "name");
    }
    @Override public <T> T accept(Visitor<T> v) {
        return v.visitParam(this);
    }
}
