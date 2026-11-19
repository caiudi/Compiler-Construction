import ast.ProgramNode;
import symbols.StackSymbolTable;
import visitor.IdentificationVisitor;
import visitor.TypeChecker;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Encadeia as fases de analise:
 *   1. sintatica (Parser) — com erro, para aqui;
 *   2. identificacao (CP4) — decora cada IdNode com o seu Symbol;
 *   3. tipos (CP5) — roda mesmo com erros de identificacao: um nome nao declarado
 *      tem decl = null e vira o tipo ERROR, sem mensagem repetida.
 *
 * Mensagens semanticas (identificacao + tipos) sao intercaladas por numero de linha;
 * na mesma linha, as de identificacao vem primeiro. Os avisos vem depois de todos os erros.
 * Com qualquer erro, o programa nao segue para a geracao de codigo.
 */
public final class Analisador {

    public static final class Resultado {
        public final ProgramNode ast;              // null se houve erro sintatico
        public final List<String> errosSintaticos;
        public final List<String> erros;           // identificacao + tipos
        public final List<String> avisos;

        Resultado(ProgramNode ast, List<String> errosSintaticos, List<String> erros, List<String> avisos) {
            this.ast = ast;
            this.errosSintaticos = List.copyOf(errosSintaticos);
            this.erros = List.copyOf(erros);
            this.avisos = List.copyOf(avisos);
        }

        /** Pode seguir para a geracao de codigo? */
        public boolean semErros() {
            return ast != null && errosSintaticos.isEmpty() && erros.isEmpty();
        }

        /** Todas as mensagens na ordem em que sao impressas (formato dos arquivos test/esperado). */
        public List<String> mensagens() {
            List<String> all = new ArrayList<>(errosSintaticos);
            all.addAll(erros);
            all.addAll(avisos);
            return all;
        }
    }

    private static final Pattern LINHA = Pattern.compile("^linha (\\d+):");

    private static int linha(String mensagem) {
        Matcher m = LINHA.matcher(mensagem);
        return m.find() ? Integer.parseInt(m.group(1)) : Integer.MAX_VALUE;
    }

    public static Resultado analisar(String fonte) {
        Parser.ParseResult parse = new Parser(fonte).parse();
        if (!parse.valido())
            return new Resultado(null, parse.errors, List.of(), List.of());

        IdentificationVisitor identificacao = new IdentificationVisitor(new StackSymbolTable());
        parse.ast.accept(identificacao);

        TypeChecker tipos = new TypeChecker();
        tipos.check(parse.ast);

        List<String> erros = new ArrayList<>(identificacao.getErrors());
        erros.addAll(tipos.getErrors());
        erros.sort(Comparator.comparingInt(Analisador::linha)); // estavel: identificacao antes na mesma linha
        return new Resultado(parse.ast, List.of(), erros, tipos.getWarnings());
    }
}
