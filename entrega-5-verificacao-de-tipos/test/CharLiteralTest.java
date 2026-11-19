import ast.*;
import visitor.*;

public final class CharLiteralTest {
 private static int checks=0;
 private static void check(boolean value,String name){checks++;if(!value)throw new AssertionError(name);System.out.println("OK "+name);}
 private static void valid(String literal,char value){
  String source="char letra = "+literal+"; void f(){char outra = "+literal+"; return;}";
  Parser.ParseResult result=new Parser(source).parse();
  check(result.valido(),"aceita "+literal+" "+result.errors);
  VarDeclNode decl=(VarDeclNode)result.ast.declarations.get(0);
  check(decl.initializer instanceof CharLiteralNode && ((CharLiteralNode)decl.initializer).value==value,"AST char "+literal);
  String tree=new AstPrinter().print(result.ast);
  check(tree.contains("CharLiteralNode"),"impressora AST "+literal);
  String printed=new SourcePrinter().print(result.ast);
  Parser.ParseResult again=new Parser(printed).parse();
  check(again.valido(),"reparse char "+literal+" "+again.errors);
  check(tree.equals(new AstPrinter().print(again.ast)),"AST roundtrip "+literal);
  check(printed.equals(new SourcePrinter().print(again.ast)),"fonte roundtrip "+literal);
 }
 private static void invalid(String literal){
  Token first=new Scanner(literal).nextToken();
  check(first.getTipo()==TokenType.ERRO,"scanner rejeita literal "+literal);
  Parser.ParseResult result=new Parser("char c = "+literal+";").parse();
  check(!result.valido() && result.ast==null && !result.errors.isEmpty(),"parser rejeita literal "+literal);
 }
 public static void main(String[] args){
  valid("'A'",'A');valid("'7'",'7');valid("'\\n'",'\n');valid("'\\t'",'\t');
  valid("'\\''",'\'');valid("'\\\\'",'\\');valid("'\\r'",'\r');valid("'\\0'",'\0');
  invalid("''");invalid("'ab'");invalid("'\\x'");invalid("'abc'");
  System.out.println("TOTAL CHAR: "+checks+" verificacoes");
 }
}
