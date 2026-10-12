import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

/** Executa o reconhecedor sobre um arquivo de código-fonte. */
public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Uso: java -cp out Main caminho/do/programa.txt");
            System.exit(2);
        }
        String codigo = Files.readString(Path.of(args[0]), StandardCharsets.UTF_8);
        Parser parser = new Parser(codigo);
        boolean valido = parser.analisar();
        if (valido) {
            System.out.println("Programa sintaticamente valido.");
        } else {
            for (String erro : parser.getErros()) System.err.println(erro);
            System.err.println("Programa rejeitado: " + parser.getErros().size() + " erro(s).");
            System.exit(1);
        }
    }
}
