import ast.*;
import symbol.*;
import visitor.*;
import java.nio.file.*;
import java.util.*;
import java.io.IOException;

/** Compara arquivos escritos manualmente antes da execucao da identificacao. */
public final class IdentificationSuiteTest {
    private static int passed = 0;
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static List<String> expected(Path path) throws IOException {
        return Files.readAllLines(path);
    }
    private static void caseFile(Path file) throws IOException {
        String name=file.getFileName().toString();
        String source=Files.readString(file);
        Parser.ParseResult result=new Parser(source).parse();
        check(result.valido(),name+": sintaxe invalida: "+result.errors);
        SymbolTable table=new StackSymbolTable();
        IdentificationVisitor visitor=new IdentificationVisitor(table);
        result.ast.accept(visitor);
        List<String> actual=visitor.getErrors();
        List<String> want=expected(Path.of("test/esperado",name));
        check(actual.equals(want),name+"\nEsperado: "+want+"\nObtido: "+actual);
        check(table.level()==0,name+": escopos nao restaurados");
        passed++;
        System.out.println("OK " + name + " | " + actual.size() + " erro(s)");
        if (name.equals("01_valido_sombreamento.txt")) verifyLinks(result.ast);
        if (name.equals("12_desenho_parte1.txt")) verifyClosedBlock(result.ast);
    }
    private static void verifyLinks(ProgramNode program) {
        VarDeclNode global=(VarDeclNode)program.declarations.get(0);
        FunctionDeclNode fn=(FunctionDeclNode)program.declarations.get(1);
        BlockNode body=fn.body;
        AssignNode first=(AssignNode)body.statements.get(1);
        check(first.target.decl.declaration==global,"x global no inicio");
        check(((IdNode)first.value).decl.declaration==fn.parameters.get(0),"p parametro");
        IfNode conditional=(IfNode)body.statements.get(2);
        LocalVarStmtNode local=(LocalVarStmtNode)conditional.thenBlock.statements.get(0);
        AssignNode inside=(AssignNode)conditional.thenBlock.statements.get(1);
        check(inside.target.decl.declaration==local.declaration,"x sombreado dentro do if");
        check(((IdNode)inside.value).decl.declaration==fn.parameters.get(0),"p visivel dentro do if");
        WhileNode loop=(WhileNode)conditional.thenBlock.statements.get(2);
        LocalVarStmtNode z=(LocalVarStmtNode)loop.body.statements.get(0);
        AssignNode useZ=(AssignNode)loop.body.statements.get(1);
        check(((IdNode)useZ.value).decl.declaration==local.declaration,"x dentro do while");
        check(useZ.target.decl.declaration==z.declaration,"z dentro do while");
        AssignNode after=(AssignNode)body.statements.get(3);
        check(((IdNode)after.value).decl.declaration==global,"x restaurado depois do if");
        ReturnNode ret=(ReturnNode)body.statements.get(4);
        LocalVarStmtNode y=(LocalVarStmtNode)body.statements.get(0);
        check(((IdNode)ret.value).decl.declaration==y.declaration,"y retorna declaracao local");
        System.out.println("OK vinculos: usos apontam para declaracoes corretas");
    }
    private static void verifyClosedBlock(ProgramNode program) {
        FunctionDeclNode fn=(FunctionDeclNode)program.declarations.get(1);
        IfNode conditional=(IfNode)fn.body.statements.get(2);
        WhileNode loop=(WhileNode)conditional.thenBlock.statements.get(2);
        AssignNode invalid=(AssignNode)conditional.thenBlock.statements.get(3);
        check(((IdNode)invalid.value).decl==null,"z fora do while deve ficar null");
        check(loop.body.statements.size()==2,"while interno existe");
        System.out.println("OK vinculos: uso apos fechamento fica decl=null");
    }
    public static void main(String[] args) throws Exception {
        List<Path> paths;
        try (var stream=Files.list(Path.of("test/programas"))) {
            paths=stream.filter(p->p.toString().endsWith(".txt")).sorted().toList();
        }
        check(paths.size()>=11,"casos obrigatorios ausentes");
        for (Path path:paths) caseFile(path);
        System.out.println("TOTAL: " + passed + " programas comparados com test/esperado/");
    }
}
