package ast;

import java.util.Objects;
import visitor.Visitor;

/** CP5: comando read(variavel); */
public final class ReadNode extends StmtNode {
    public final IdNode target;

    public ReadNode(int linha, int coluna, IdNode target) {
        super(linha, coluna);
        this.target = Objects.requireNonNull(target, "target");
    }

    @Override public <T> T accept(Visitor<T> v) {
        return v.visitRead(this);
    }
}
