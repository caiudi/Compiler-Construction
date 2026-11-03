package ast;
import visitor.Visitor;

public final class BoolLiteralNode extends ExprNode {
    public final boolean value;
    public BoolLiteralNode(int linha, int coluna, boolean value) {
        super(linha, coluna);
        this.value = value;
    }
    @Override public <T> T accept(Visitor<T> v) {
        return v.visitBoolLiteral(this);
    }
}
