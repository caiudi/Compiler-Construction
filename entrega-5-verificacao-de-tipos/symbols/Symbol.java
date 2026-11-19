package symbols;

import ast.Node;
import java.util.Objects;

/** Entrada de uma declaracao na tabela de simbolos. */
public final class Symbol {
    public final String name;
    public final SymbolCategory category;
    public final Node declaration;
    public final int level;
    /** CP5: tipo da variavel/parametro, ou tipo de retorno da funcao (VOID para procedimento). */
    public final types.Type type;
    /** CP5: assinatura de funcao/procedimento; null para variaveis e parametros. */
    public final types.Signature signature;
    // O deslocamento sera acrescentado na geracao de codigo.

    /** Construtor do CP4 (sem tipo), mantido para os testes da tabela isolada. */
    public Symbol(String name, SymbolCategory category, Node declaration, int level) {
        this(name, category, declaration, level, null, null);
    }

    public Symbol(String name, SymbolCategory category, Node declaration, int level,
                  types.Type type, types.Signature signature) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Nome do simbolo nao pode ser vazio");
        if (level < 0)
            throw new IllegalArgumentException("Nivel nao pode ser negativo");
        this.name = name;
        this.category = Objects.requireNonNull(category, "category");
        this.declaration = Objects.requireNonNull(declaration, "declaration");
        this.level = level;
        this.type = type;
        this.signature = signature;
    }

    public boolean isSubprogram() {
        return category == SymbolCategory.FUNCTION || category == SymbolCategory.PROCEDURE;
    }

    @Override
    public String toString() {
        return name + "@" + declaration.linha + " ["
                + category + ", nivel=" + level
                + (signature != null ? ", " + signature : type != null ? ", " + type : "") + "]";
    }
}
