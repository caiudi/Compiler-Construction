import java.nio.file.*;

/**
 * Uso: java -cp out Main arquivo.txt [--ast]
 * Analisa o programa (sintaxe, identificacao e tipos) e imprime erros e avisos.
 * Saida 0: sem erros (pode seguir para a geracao de codigo). Saida 1: houve erro.
 */
public final class Main {
    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args.length > 2) {
            System.err.println("Uso: java -cp out Main arquivo.txt [--ast]");
            System.exit(2);
        }
        Analisador.Resultado r = Analisador.analisar(Files.readString(Path.of(args[0])));
        r.mensagens().forEach(System.out::println);
        if (!r.semErros()) {
            System.out.println("Programa com erros: nao segue para a geracao de codigo.");
            System.exit(1);
        }
        if (args.length == 2 && args[1].equals("--ast")) {
            System.out.print(new visitor.AstPrinter().print(r.ast));
            System.out.println("=== FONTE CANONICO ===");
            System.out.println(new visitor.SourcePrinter().print(r.ast));
        }
        System.out.println("OK: programa sem erros" + (r.avisos.isEmpty() ? "." : " (com avisos)."));
    }
}
