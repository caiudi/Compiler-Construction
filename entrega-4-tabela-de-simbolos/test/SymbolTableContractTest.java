import ast.IdNode;
import symbol.*;

public final class SymbolTableContractTest {
    private static int checks = 0;
    private static void ok(boolean b, String message) {
        checks++;
        if (!b) throw new AssertionError(message);
    }
    private static Symbol sym(String name, int line, int level) {
        return new Symbol(name, SymbolCategory.VARIABLE, new IdNode(line,1,name), level);
    }
    public static void main(String[] args) {
        SymbolTable t = new StackSymbolTable();
        ok(t.level()==0,"global nivel 0");
        Symbol global=sym("x",1,0);
        ok(t.declare(global),"declara global");
        ok(t.lookup("x")==global,"busca global");
        ok(t.lookupLocal("x")==global,"busca local global");
        ok(!t.declare(sym("x",2,0)),"redeclara devolve false");
        ok(t.lookup("x")==global,"preserva primeiro");
        ok(t.lookup("naoExiste")==null,"lookup inexistente");
        ok(t.lookupLocal("naoExiste")==null,"lookupLocal inexistente");
        t.openScope();
        ok(t.level()==1,"nivel 1");
        ok(t.lookupLocal("x")==null,"local nao enxerga externo");
        ok(t.lookup("x")==global,"lookup enxerga externo");
        Symbol inner=sym("x",3,1);
        ok(t.declare(inner),"sombreamento permitido");
        ok(t.lookup("x")==inner,"lookup escolhe interno");
        t.openScope();
        ok(t.level()==2,"nivel 2");
        ok(t.lookup("x")==inner,"lookup atravessa escopos");
        t.closeScope();
        ok(t.level()==1,"restaura nivel 1");
        t.closeScope();
        ok(t.level()==0,"restaura nivel global");
        ok(t.lookup("x")==global,"restaura simbolo global");
        boolean caught=false;
        try {t.closeScope();} catch (IllegalStateException e) {caught=e.getMessage()!=null&&!e.getMessage().isBlank();}
        ok(caught,"fechamento global tem excecao explicativa");
        ok(t.lookup("x")==global,"global intacto apos fechamento invalido");
        System.out.println("OK: tabela isolada, " + checks + " assercoes");
    }
}
