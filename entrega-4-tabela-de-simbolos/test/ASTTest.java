import ast.*;
import visitor.*;
import java.nio.file.*;
import java.util.*;

/** Expectativas sao arquivos independentes, definidos previamente. */
public final class ASTTest {
 static int total=0;
 static void check(boolean ok,String descricao){total++;if(!ok)throw new AssertionError(descricao);System.out.println("OK " + descricao);}
 static Parser.ParseResult parse(String code){return new Parser(code).parse();}
 static void valid(String filename,String source) throws Exception {
  Parser.ParseResult r=parse(source);check(r.valido(),filename+" valido");
  AstPrinter printer=new AstPrinter();String got=printer.print(r.ast);
  String expected=Files.readString(Path.of("test/esperado/"+filename+".txt"));
  check(got.equals(expected),filename+" arvore esperada");
  SourcePrinter sourcePrinter=new SourcePrinter();String once=sourcePrinter.print(r.ast);
  Parser.ParseResult second=parse(once);check(second.valido(),filename+" roundtrip aceito");
  String twice=sourcePrinter.print(second.ast);
  check(once.equals(twice),filename+" fonte estavel");
  check(got.equals(printer.print(second.ast)),filename+" AST estavel");
 }
 static ExprNode value(String exp){Parser.ParseResult r=parse("int f(){return "+exp+";}");check(r.valido(),"expressao valida: "+exp);FunctionDeclNode f=(FunctionDeclNode)r.ast.declarations.get(0);return ((ReturnNode)f.body.statements.get(0)).value;}
 static void same(String exp,String tree){String got=new AstPrinter().print(value(exp));check(got.equals(tree),"precedencia: "+exp);}
 public static void main(String[]args)throws Exception{
  valid("valido1","int x; double d=3.14; bool b=true; char c; string s=\"oi\";");
  valid("valido2","int soma(int a,int b){return a+b;} void main(){int x=soma(2,3);return;}");
  valid("valido3","void f(){int x=0; while(x<10){if(x==5 || !false){x=x+2;}else{x=x+1;}}return;}");
  same("a+b*c","BinaryExprNode(operator=MAIS)\n  IdNode(name=a)\n  BinaryExprNode(operator=MULTIPLICACAO)\n    IdNode(name=b)\n    IdNode(name=c)\n");
  same("a-b-c","BinaryExprNode(operator=MENOS)\n  BinaryExprNode(operator=MENOS)\n    IdNode(name=a)\n    IdNode(name=b)\n  IdNode(name=c)\n");
  same("a-(b-c)","BinaryExprNode(operator=MENOS)\n  IdNode(name=a)\n  BinaryExprNode(operator=MENOS)\n    IdNode(name=b)\n    IdNode(name=c)\n");
  String abc=new AstPrinter().print(value("(((a)))"));String a=new AstPrinter().print(value("a"));check(abc.equals(a),"parenteses redundantes descartados");
  for(String bad:List.of("void f(){int x=2 return;}","void f(){int x=;return;}","int soma(int a,){return a;}")){Parser.ParseResult r=parse(bad);check(!r.valido()&&r.ast==null&&!r.errors.isEmpty(),"erro rejeitado e sem AST");}
  Parser.ParseResult panic=parse("void f(){int x=; int y=; return;}");check(!panic.valido()&&panic.ast==null&&panic.errors.size()>=2,"modo panico");
  System.out.println("TOTAL: "+total+" verificacoes");
 }
}
