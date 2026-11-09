package ast;
import visitor.Visitor;

public final class CharLiteralNode extends ExprNode {
    public final char value;
    public CharLiteralNode(int linha, int coluna, char value) {
        super(linha, coluna);
        this.value = value;
    }
    @Override public <T> T accept(Visitor<T> v) {
        return v.visitCharLiteral(this);
    }
}
