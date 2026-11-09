package symbol;

import ast.Node;
import java.util.Objects;

/** Entrada de uma declaracao na tabela de simbolos. */
public final class Symbol {
    public final String name;
    public final SymbolCategory category;
    public final Node declaration;
    public final int level;

    // Tipo semantico e deslocamento serao acrescentados em etapas posteriores.
    public Symbol(String name, SymbolCategory category, Node declaration, int level) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Nome do simbolo nao pode ser vazio");
        if (level < 0)
            throw new IllegalArgumentException("Nivel nao pode ser negativo");
        this.name = name;
        this.category = Objects.requireNonNull(category, "category");
        this.declaration = Objects.requireNonNull(declaration, "declaration");
        this.level = level;
    }

    @Override
    public String toString() {
        return name + "@" + declaration.linha + " ["
                + category + ", nivel=" + level + "]";
    }
}
