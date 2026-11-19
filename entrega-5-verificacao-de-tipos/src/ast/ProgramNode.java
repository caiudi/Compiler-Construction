package ast;
import java.util.List;
import java.util.Objects;
import visitor.Visitor;

public final class ProgramNode extends Node {
    public final List<DeclNode> declarations;
    public ProgramNode(int linha, int coluna, List<DeclNode> declarations) {
        super(linha, coluna);
        this.declarations = List.copyOf(Objects.requireNonNull(declarations, "declarations"));
    }
    @Override public <T> T accept(Visitor<T> v) {
        return v.visitProgram(this);
    }
}
