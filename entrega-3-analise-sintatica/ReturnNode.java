package ast;
import java.util.Objects;
import visitor.Visitor;

public final class ReturnNode extends StmtNode {
    public final ExprNode value;
    public ReturnNode(int linha, int coluna, ExprNode value) {
        super(linha, coluna);
        this.value = value;
    }
    @Override public <T> T accept(Visitor<T> v) {
        return v.visitReturn(this);
    }
}
