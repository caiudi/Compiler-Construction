package symbol;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Pilha de mapas; escopo global (nivel 0) criado no construtor. */
public final class StackSymbolTable implements SymbolTable {
    private final Deque<Map<String, Symbol>> scopes = new ArrayDeque<>();

    public StackSymbolTable() {
        scopes.push(new LinkedHashMap<>());
    }

    @Override
    public void openScope() {
        scopes.push(new LinkedHashMap<>());
    }

    @Override
    public void closeScope() {
        if (scopes.size() == 1)
            throw new IllegalStateException("Nao e permitido fechar o escopo global");
        scopes.pop();
    }

    @Override
    public boolean declare(Symbol s) {
        Objects.requireNonNull(s, "symbol");
        if (s.level != level())
            throw new IllegalArgumentException(
                "Nivel do simbolo (" + s.level + ") difere do nivel atual (" + level() + ")");
        Map<String, Symbol> current = scopes.peek();
        if (current.containsKey(s.name)) return false;
        current.put(s.name, s);
        return true;
    }

    @Override
    public Symbol lookupLocal(String name) {
        return name == null ? null : scopes.peek().get(name);
    }

    @Override
    public Symbol lookup(String name) {
        if (name == null) return null;
        for (Map<String, Symbol> scope : scopes) {
            Symbol result = scope.get(name);
            if (result != null) return result;
        }
        return null;
    }

    @Override
    public int level() {
        return scopes.size() - 1;
    }
}
