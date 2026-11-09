import java.util.Map;
/** Scanner compativel com a interface do CP1, estendido para CHAR_LITERAL.
 *  Reconhece um unico caractere ou um escape simples entre aspas simples.
 */
public final class Scanner {
 private final String source; private int pos=0, line=1, column=1;
 private static final Map<String,TokenType> WORDS=Map.ofEntries(
  Map.entry("int",TokenType.INT),Map.entry("double",TokenType.DOUBLE),Map.entry("bool",TokenType.BOOL),
  Map.entry("char",TokenType.CHAR),Map.entry("string",TokenType.STRING),Map.entry("void",TokenType.VOID),
  Map.entry("if",TokenType.IF),Map.entry("else",TokenType.ELSE),Map.entry("while",TokenType.WHILE),
  Map.entry("return",TokenType.RETURN),Map.entry("true",TokenType.TRUE),Map.entry("false",TokenType.FALSE));
 public Scanner(String source){this.source=source==null?"":source;}
 public boolean hasNext(){return pos<source.length();}
 public char peek(){return hasNext()?source.charAt(pos):'\0';}
 private char peekNext(){return pos+1<source.length()?source.charAt(pos+1):'\0';}
 public char advance(){char c=source.charAt(pos++);if(c=='\n'){line++;column=1;}else column++;return c;}
 private Token token(TokenType type,int start,int l,int c){return new Token(type,source.substring(start,pos),l,c);}
 private void skip(){while(hasNext()){
  if(Character.isWhitespace(peek())){advance();continue;}
  if(peek()=='/'&&peekNext()=='/'){while(hasNext()&&peek()!='\n')advance();continue;}
  if(peek()=='/'&&peekNext()=='*'){advance();advance();while(hasNext()){
   if(peek()=='*'&&peekNext()=='/'){advance();advance();break;}advance();}continue;}
  break;
 }}
 public Token nextToken(){skip();int start=pos,l=line,c=column;
  if(!hasNext())return new Token(TokenType.EOF,"",l,c);
  char ch=advance();
  if(Character.isLetter(ch)||ch=='_'){
   while(hasNext()&&(Character.isLetterOrDigit(peek())||peek()=='_'))advance();
   String w=source.substring(start,pos);return token(WORDS.getOrDefault(w,TokenType.IDENTIFICADOR),start,l,c);
  }
  if(Character.isDigit(ch)){
   while(hasNext()&&Character.isDigit(peek()))advance();
   if(peek()=='.'&&Character.isDigit(peekNext())){advance();while(hasNext()&&Character.isDigit(peek()))advance();}
   if((peek()=='e'||peek()=='E')&&(Character.isDigit(peekNext())||peekNext()=='+'||peekNext()=='-')){
    int save=pos;advance();if(peek()=='+'||peek()=='-')advance();
    if(!Character.isDigit(peek())){int consumed=pos-save;pos=save;column-=consumed;}else while(hasNext()&&Character.isDigit(peek()))advance();
   }
   return token(TokenType.NUMERO,start,l,c);
  }
  if(ch=='\''){
   boolean valid=true;
   if(!hasNext()||peek()=='\''||peek()=='\n'){valid=false;}
   else if(peek()=='\\'){
    advance();if(!hasNext()||"ntr0\\\'\"".indexOf(peek())<0)valid=false;
    else advance();
   }else {char val=advance();if(val=='\r'||val=='\n')valid=false;}
   if(!hasNext()||peek()!='\'')valid=false;
   if(valid){advance();return token(TokenType.CHAR_LITERAL,start,l,c);}
   // Consumir o restante do literal malformado sem travar.
   while(hasNext()&&peek()!='\''&&peek()!='\n'&&peek()!=';'&&peek()!='}')advance();
   if(peek()=='\'')advance();return token(TokenType.ERRO,start,l,c);
  }
  if(ch=='"'){
   boolean closed=false;
   while(hasNext()&&peek()!='\n'){
    if(peek()=='\\'){advance();if(hasNext()&&peek()!='\n')advance();continue;}
    if(advance()=='"'){closed=true;break;}
   }
   return token(closed?TokenType.STRING_LITERAL:TokenType.ERRO,start,l,c);
  }
  return switch(ch){
   case '+'->token(TokenType.MAIS,start,l,c);case '-'->token(TokenType.MENOS,start,l,c);
   case '*'->token(TokenType.MULTIPLICACAO,start,l,c);case '/'->token(TokenType.DIVISAO,start,l,c);
   case '('->token(TokenType.ABRE_PARENTESES,start,l,c);case ')'->token(TokenType.FECHA_PARENTESES,start,l,c);
   case '{'->token(TokenType.ABRE_CHAVES,start,l,c);case '}'->token(TokenType.FECHA_CHAVES,start,l,c);
   case ','->token(TokenType.VIRGULA,start,l,c);case ';'->token(TokenType.PONTO_VIRGULA,start,l,c);
   case '='->{if(peek()=='='){advance();yield token(TokenType.IGUAL,start,l,c);}yield token(TokenType.ATRIBUICAO,start,l,c);}
   case '!'->{if(peek()=='='){advance();yield token(TokenType.DIFERENTE,start,l,c);}yield token(TokenType.NEGACAO,start,l,c);}
   case '<'->{if(peek()=='='){advance();yield token(TokenType.MENOR_IGUAL,start,l,c);}yield token(TokenType.MENOR,start,l,c);}
   case '>'->{if(peek()=='='){advance();yield token(TokenType.MAIOR_IGUAL,start,l,c);}yield token(TokenType.MAIOR,start,l,c);}
   case '&'->{if(peek()=='&'){advance();yield token(TokenType.E_LOGICO,start,l,c);}yield token(TokenType.ERRO,start,l,c);}
   case '|'->{if(peek()=='|'){advance();yield token(TokenType.OU_LOGICO,start,l,c);}yield token(TokenType.ERRO,start,l,c);}
   default->token(TokenType.ERRO,start,l,c);
  };
 }
}
