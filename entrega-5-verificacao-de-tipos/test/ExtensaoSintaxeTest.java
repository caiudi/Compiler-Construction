import ast.*;
import visitor.*;
import java.nio.file.*;
import java.util.List;

/** CP5: extensoes da linguagem (print, read, subprogramas aninhados) no parser e nos impressores. */
public final class ExtensaoSintaxeTest {
    private static int total = 0;

    private static void check(boolean ok, String description) {
        total++;
        if (!ok) throw new AssertionError(description);
        System.out.println("OK " + description);
    }

    private static void roundTrip(String name, String source) {
        Parser.ParseResult r = new Parser(source).parse();
        check(r.valido(), name + " aceito " + r.errors);
        String tree = new AstPrinter().print(r.ast);
        String once = new SourcePrinter().print(r.ast);
        Parser.ParseResult again = new Parser(once).parse();
        check(again.valido(), name + " reimpresso e aceito " + again.errors);
        check(once.equals(new SourcePrinter().print(again.ast)), name + " fonte estavel");
        check(tree.equals(new AstPrinter().print(again.ast)), name + " AST estavel");
    }

    private static void rejected(String description, String source) {
        Parser.ParseResult r = new Parser(source).parse();
        check(!r.valido() && r.ast == null && !r.errors.isEmpty(), "rejeita " + description);
    }

    public static void main(String[] args) throws Exception {
        roundTrip("valido_completo", Files.readString(Path.of("test/programas/valido_completo.txt")));
        roundTrip("aninhada", Files.readString(Path.of("test/programas/aninhada.txt")));

        String tree = new AstPrinter().print(new Parser(
                "void f(){ int y = 1; int g(int x){ return x + y; } print(g(2)); read(y); }").parse().ast);
        check(tree.equals(String.join("\n",
                "ProgramNode",
                "  ProcedureDeclNode",
                "    IdNode(name=f)",
                "    BlockNode",
                "      LocalVarStmtNode",
                "        VarDeclNode",
                "          TypeNode(kind=INT)",
                "          IdNode(name=y)",
                "          IntLiteralNode(lexeme=1)",
                "      LocalSubprogramStmtNode",
                "        FunctionDeclNode",
                "          TypeNode(kind=INT)",
                "          IdNode(name=g)",
                "          ParamNode",
                "            TypeNode(kind=INT)",
                "            IdNode(name=x)",
                "          BlockNode",
                "            ReturnNode",
                "              BinaryExprNode(operator=MAIS)",
                "                IdNode(name=x)",
                "                IdNode(name=y)",
                "      PrintNode",
                "        CallExprNode",
                "          IdNode(name=g)",
                "          IntLiteralNode(lexeme=2)",
                "      ReadNode",
                "        IdNode(name=y)",
                "")), "arvore com funcao aninhada, print e read");

        rejected("print sem parenteses", "void f(){ print 1; }");
        rejected("read de expressao", "void f(){ read(1 + 2); }");
        rejected("read sem argumento", "void f(){ read(); }");
        rejected("print sem ;", "void f(){ print(1) }");
        rejected("funcao aninhada sem corpo", "void f(){ int g(); }");
        Parser.ParseResult panic = new Parser("void f(){ print(; read(); int x = 1; }").parse();
        check(!panic.valido() && panic.errors.size() >= 2, "modo panico com print e read: " + panic.errors.size() + " erros");
        check(List.of("print", "read").stream().allMatch(w ->
                new Scanner(w).nextToken().getTipo() != TokenType.IDENTIFICADOR), "print e read sao palavras reservadas");

        System.out.println("TOTAL: " + total + " verificacoes das extensoes sintaticas");
    }
}
