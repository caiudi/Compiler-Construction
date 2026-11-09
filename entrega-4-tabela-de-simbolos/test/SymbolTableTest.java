
import ast.IdNode;
import symbol.*;

public final class SymbolTableTest {
    private static void check(boolean condition, String msg) {
        if (!condition) throw new AssertionError(msg);
    }
    private static Symbol sym(String name, int line, int level) {
        return new Symbol(name, SymbolCategory.VARIABLE, new IdNode(line, 1, name), level);
    }
    public static void main(String[] args) {
        SymbolTable table = new StackSymbolTable();
        check(table.level() == 0, "global deve ser nivel 0");
        Symbol first = sym("x", 1, 0);
        check(table.declare(first), "primeira declaracao");
        check(!table.declare(sym("x", 2, 0)), "redeclaracao deve falhar");
        check(table.lookup("x") == first, "primeira declaracao preservada");
        check(table.lookupLocal("x") == first, "busca local global");
        check(table.lookup("inexistente") == null, "lookup inexistente");
        check(table.lookupLocal("inexistente") == null, "lookupLocal inexistente");
        table.openScope();
        check(table.level() == 1, "nivel interno");
        check(table.lookupLocal("x") == null, "lookupLocal nao busca externo");
        check(table.lookup("x") == first, "lookup encontra externo");
        Symbol shadow = sym("x", 3, 1);
        check(table.declare(shadow), "sombreamento permitido");
        check(table.lookup("x") == shadow, "interno oculta externo");
        table.closeScope();
        check(table.level() == 0, "retorno ao global");
        check(table.lookup("x") == first, "restauracao do global");
        boolean thrown = false;
        try { table.closeScope(); } catch (IllegalStateException ex) { thrown = true; }
        check(thrown, "fechamento do global deve ter erro explicativo");
        check(table.lookup("x") == first, "global permanece apos erro");
        System.out.println("OK: 14 verificacoes da tabela de simbolos");
    }
}
