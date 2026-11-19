package ast;
import java.util.Objects;
import visitor.Visitor;

public final class CallStmtNode extends StmtNode {
    public final CallExprNode call;
    public CallStmtNode(int linha, int coluna, CallExprNode call) {
        super(linha, coluna);
        this.call = Objects.requireNonNull(call, "call");
    }
    @Override public <T> T accept(Visitor<T> v) {
        return v.visitCallStmt(this);
    }
}
