import ast.*;
import visitor.*;
import symbol.*;
import java.util.*;

public final class IdentificationTest {
    static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    static ProgramNode parse(String src) {
        Parser.ParseResult result = new Parser(src).parse();
        check(result.valido(), "Erro sintatico: " + result.errors);
        return result.ast;
    }
    static IdentificationVisitor run(ProgramNode ast) {
        IdentificationVisitor v = new IdentificationVisitor(new StackSymbolTable());
        ast.accept(v);
        return v;
    }
    public static void main(String[] args) {
        String source = String.join("\n",
          "int x = 10;",
          "int calcular(int p) {",
          "  int y = p;",
          "  {",
          "    int x = 20;",
          "    y = x;",
          "    {",
          "      int z = x;",
          "      y = z;",
          "    }",
          "    y = x;",
          "  }",
          "  y = x;",
          "  y = z;",
          "  return y;",
          "}");
        ProgramNode ast = parse(source);
        IdentificationVisitor v = run(ast);
        check(v.getErrors().equals(List.of("linha 14: 'z' não declarado")), "Erro esperado: "+v.getErrors());
        FunctionDeclNode fn = (FunctionDeclNode) ast.declarations.get(1);
        BlockNode body = fn.body;
        LocalVarStmtNode y = (LocalVarStmtNode) body.statements.get(0);
        check(((IdNode)y.declaration.initializer).decl.declaration == fn.parameters.get(0), "parametro p");
        BlockNode outer = (BlockNode)body.statements.get(1);
        LocalVarStmtNode shadow = (LocalVarStmtNode)outer.statements.get(0);
        AssignNode inside = (AssignNode)outer.statements.get(1);
        check(((IdNode)inside.value).decl.declaration == shadow.declaration, "x sombreado");
        AssignNode outside = (AssignNode)body.statements.get(2);
        check(((IdNode)outside.value).decl.declaration == ast.declarations.get(0), "x global restaurado");
        AssignNode invalid = (AssignNode)body.statements.get(3);
        check(((IdNode)invalid.value).decl == null, "z fora de escopo");
        check(run(parse("int f(int a) { int a = 1; return a; }")).getErrors()
            .equals(List.of("linha 1: 'a' já declarado neste escopo (linha 1)")), "parametro redeclarado");
        check(run(parse("int f() { a = 1; b = 2; return c; }")).getErrors().size()==3, "tres erros");
        check(run(parse("int par(int n) { return impar(n); } int impar(int n) { return par(n); }"))
            .getErrors().isEmpty(), "recursao mutua");
        check(run(parse("void repetir() { repetir(); return; }")).getErrors().isEmpty(), "recursao direta");
        System.out.println("OK: identificacao, escopos, redeclaracao, 3 erros e recursao");
    }
}
