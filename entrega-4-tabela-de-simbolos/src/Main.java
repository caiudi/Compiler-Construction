import java.nio.file.*;
import ast.*;
import visitor.*;
public final class Main {
 public static void main(String[] args) throws Exception {
  if(args.length!=1){System.err.println("Uso: java -cp out Main arquivo.txt");System.exit(2);}
  Parser.ParseResult r=new Parser(Files.readString(Path.of(args[0]))).parse();
  if(!r.valido()){r.errors.forEach(System.err::println);System.exit(1);}
  System.out.print(new AstPrinter().print(r.ast));
  System.out.println("=== FONTE CANONICO ===");System.out.println(new SourcePrinter().print(r.ast));
 }
}
