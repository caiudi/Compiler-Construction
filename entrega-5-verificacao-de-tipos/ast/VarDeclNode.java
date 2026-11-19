package ast;
import java.util.Objects;
import visitor.Visitor;

public final class VarDeclNode extends DeclNode {
    public final TypeNode type;
    public final IdNode name;
    public final ExprNode initializer;
    public VarDeclNode(int linha, int coluna, TypeNode type, IdNode name, ExprNode initializer) {
        super(linha, coluna);
        this.type = Objects.requireNonNull(type, "type");
        this.name = Objects.requireNonNull(name, "name");
        this.initializer = initializer;
    }
    @Override public <T> T accept(Visitor<T> v) {
        return v.visitVarDecl(this);
    }
}
