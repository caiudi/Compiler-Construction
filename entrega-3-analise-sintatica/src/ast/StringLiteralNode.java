package ast;
import java.util.Objects;
import visitor.Visitor;

public final class StringLiteralNode extends ExprNode {
    public final String lexeme;
    public StringLiteralNode(int linha, int coluna, String lexeme) {
        super(linha, coluna);
        this.lexeme = Objects.requireNonNull(lexeme, "lexeme");
    }
    @Override public <T> T accept(Visitor<T> v) {
        return v.visitStringLiteral(this);
    }
}
