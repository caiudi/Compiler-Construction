package ast;

import java.util.Objects;
import visitor.Visitor;

/** CP5: comando print(expressao); */
public final class PrintNode extends StmtNode {
    public final ExprNode value;

    public PrintNode(int linha, int coluna, ExprNode value) {
        super(linha, coluna);
        this.value = Objects.requireNonNull(value, "value");
    }

    @Override public <T> T accept(Visitor<T> v) {
        return v.visitPrint(this);
    }
}
