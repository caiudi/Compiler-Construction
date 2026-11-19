import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * "Nenhuma entrada pode causar NullPointerException ou laco infinito."
 * Roda o analisador completo sobre todo prefixo de todo programa de teste
 * (CP4 e CP5) e sobre versoes com um caractere removido; cada entrada tem
 * 2 segundos. Qualquer excecao ou estouro de tempo reprova.
 */
public final class RobustnessTest {
    public static void main(String[] args) throws Exception {
        List<Path> files = new ArrayList<>();
        for (String dir : List.of("test/programas", "test/cp4/programas"))
            try (var s = Files.list(Path.of(dir))) { s.filter(p -> p.toString().endsWith(".txt")).sorted().forEach(files::add); }

        List<String> inputs = new ArrayList<>();
        for (Path f : files) {
            String src = Files.readString(f);
            for (int i = 0; i <= src.length(); i++) inputs.add(src.substring(0, i));
            for (int i = 0; i < src.length(); i += 3) inputs.add(src.substring(0, i) + src.substring(i + 1));
        }

        ExecutorService pool = Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r); t.setDaemon(true); return t; });
        int count = 0;
        for (String input : inputs) {
            Future<?> job = pool.submit(() -> Analisador.analisar(input));
            try {
                job.get(2, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                throw new AssertionError("possivel laco infinito na entrada:\n" + input);
            } catch (ExecutionException e) {
                AssertionError err = new AssertionError("excecao na entrada:\n" + input);
                err.initCause(e.getCause());
                throw err;
            }
            count++;
        }
        pool.shutdownNow();
        System.out.println("TOTAL: " + count + " entradas analisadas sem excecao nem laco infinito");
    }
}
