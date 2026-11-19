import ast.*;
import visitor.VisitorBase;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * CP5: roda o analisador (sintaxe + identificacao + tipos) sobre cada programa de
 * test/programas/ e compara as mensagens com test/esperado/ (escrito a mao, a partir das regras).
 * Tambem confere que, num programa sem erros, toda expressao ficou com tipo gravado.
 */
public final class TypeSuiteTest {
    private static int passed = 0;

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static List<String> expected(Path path) throws IOException {
        return Files.readAllLines(path).stream().filter(l -> !l.isBlank()).toList();
    }

    /** Percorre a arvore inteira e conta expressoes sem tipo (o nome chamado em f(...) nao e expressao de valor). */
    private static final class Untyped extends VisitorBase<Void> {
        final List<String> missing = new ArrayList<>();
        private void expr(ExprNode e) {
            if (e == null) return;
            if (e.type == null) missing.add("linha " + e.linha + ": " + e.getClass().getSimpleName());
            e.accept(this);
        }
        private void node(Node n) { if (n != null) n.accept(this); }
        @Override public Void visitProgram(ProgramNode n) { n.declarations.forEach(this::node); return null; }
        @Override public Void visitVarDecl(VarDeclNode n) { expr(n.initializer); return null; }
        @Override public Void visitLocalVarStmt(LocalVarStmtNode n) { node(n.declaration); return null; }
        @Override public Void visitFunctionDecl(FunctionDeclNode n) { node(n.body); return null; }
        @Override public Void visitProcedureDecl(ProcedureDeclNode n) { node(n.body); return null; }
        @Override public Void visitLocalSubprogram(LocalSubprogramStmtNode n) { node(n.declaration); return null; }
        @Override public Void visitBlock(BlockNode n) { n.statements.forEach(this::node); return null; }
        @Override public Void visitAssign(AssignNode n) { expr(n.target); expr(n.value); return null; }
        @Override public Void visitIf(IfNode n) { expr(n.condition); node(n.thenBlock); node(n.elseBlock); return null; }
        @Override public Void visitWhile(WhileNode n) { expr(n.condition); node(n.body); return null; }
        @Override public Void visitCallStmt(CallStmtNode n) { expr(n.call); return null; }
        @Override public Void visitReturn(ReturnNode n) { expr(n.value); return null; }
        @Override public Void visitPrint(PrintNode n) { expr(n.value); return null; }
        @Override public Void visitRead(ReadNode n) { expr(n.target); return null; }
        @Override public Void visitBinaryExpr(BinaryExprNode n) { expr(n.left); expr(n.right); return null; }
        @Override public Void visitUnaryExpr(UnaryExprNode n) { expr(n.operand); return null; }
        @Override public Void visitCallExpr(CallExprNode n) { n.arguments.forEach(this::expr); return null; }
    }

    private static void caseFile(Path file) throws IOException {
        String name = file.getFileName().toString();
        Analisador.Resultado r = Analisador.analisar(Files.readString(file));
        check(r.errosSintaticos.isEmpty(), name + ": erro sintatico inesperado: " + r.errosSintaticos);
        List<String> want = expected(Path.of("test/esperado", name));
        List<String> got = r.mensagens();
        check(got.equals(want), name + "\nEsperado: " + want + "\nObtido:   " + got);
        if (r.semErros()) {
            Untyped u = new Untyped();
            r.ast.accept(u);
            check(u.missing.isEmpty(), name + ": expressoes sem tipo: " + u.missing);
        }
        passed++;
        System.out.println("OK " + name + " | " + r.erros.size() + " erro(s), " + r.avisos.size() + " aviso(s)");
    }

    public static void main(String[] args) throws Exception {
        List<Path> paths;
        try (var stream = Files.list(Path.of("test/programas"))) {
            paths = stream.filter(p -> p.toString().endsWith(".txt")).sorted().toList();
        }
        check(!paths.isEmpty(), "nenhum programa em test/programas");
        for (Path path : paths) {
            check(Files.exists(Path.of("test/esperado", path.getFileName().toString())),
                    "falta test/esperado/" + path.getFileName());
            caseFile(path);
        }
        System.out.println("TOTAL: " + passed + " programas comparados com test/esperado/");
    }
}
