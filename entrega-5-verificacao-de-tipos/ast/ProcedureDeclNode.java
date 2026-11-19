package ast;
import java.util.List;
import java.util.Objects;
import visitor.Visitor;

public final class ProcedureDeclNode extends DeclNode {
    public final IdNode name;
    public final List<ParamNode> parameters;
    public final BlockNode body;
    public ProcedureDeclNode(int linha, int coluna, IdNode name, List<ParamNode> parameters, BlockNode body) {
        super(linha, coluna);
        this.name = Objects.requireNonNull(name, "name");
        this.parameters = List.copyOf(Objects.requireNonNull(parameters, "parameters"));
        this.body = Objects.requireNonNull(body, "body");
    }
    @Override public <T> T accept(Visitor<T> v) {
        return v.visitProcedureDecl(this);
    }
}
