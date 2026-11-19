package ast;

import java.util.Objects;
import visitor.Visitor;

/**
 * CP5: funcao ou procedimento declarado dentro de um bloco (funcao aninhada).
 * Envolve o mesmo FunctionDeclNode/ProcedureDeclNode usado no nivel global,
 * como LocalVarStmtNode envolve VarDeclNode.
 */
public final class LocalSubprogramStmtNode extends StmtNode {
    public final DeclNode declaration;

    public LocalSubprogramStmtNode(int linha, int coluna, DeclNode declaration) {
        super(linha, coluna);
        this.declaration = Objects.requireNonNull(declaration, "declaration");
    }

    @Override public <T> T accept(Visitor<T> v) {
        return v.visitLocalSubprogram(this);
    }
}
