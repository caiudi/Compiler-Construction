import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

/** Reconhecedor LL(1) sem AST, compatível com o Scanner do Checkpoint 1. */
public final class Parser {
    private static final EnumSet<TokenType> TIPOS = EnumSet.of(
            TokenType.INT, TokenType.DOUBLE, TokenType.BOOL, TokenType.CHAR, TokenType.STRING);
    private static final EnumSet<TokenType> INICIO_DECL = EnumSet.of(
            TokenType.INT, TokenType.DOUBLE, TokenType.BOOL, TokenType.CHAR, TokenType.STRING, TokenType.VOID);
    private static final EnumSet<TokenType> INICIO_COMANDO = EnumSet.of(
            TokenType.INT, TokenType.DOUBLE, TokenType.BOOL, TokenType.CHAR, TokenType.STRING,
            TokenType.IDENTIFICADOR, TokenType.RETURN, TokenType.WHILE, TokenType.IF, TokenType.ABRE_CHAVES);
    private static final EnumSet<TokenType> INICIO_EXP = EnumSet.of(
            TokenType.NUMERO, TokenType.STRING_LITERAL, TokenType.TRUE, TokenType.FALSE,
            TokenType.IDENTIFICADOR, TokenType.ABRE_PARENTESES, TokenType.NEGACAO, TokenType.MENOS);
    private static final EnumSet<TokenType> FOLLOW_EXP = EnumSet.of(
            TokenType.PONTO_VIRGULA, TokenType.FECHA_PARENTESES, TokenType.VIRGULA);
    private static final EnumSet<TokenType> REL = EnumSet.of(
            TokenType.MENOR, TokenType.MENOR_IGUAL, TokenType.MAIOR, TokenType.MAIOR_IGUAL);
    private static final EnumSet<TokenType> FOLLOW_OU = copy(FOLLOW_EXP);
    private static final EnumSet<TokenType> FOLLOW_E = union(FOLLOW_OU, TokenType.OU_LOGICO);
    private static final EnumSet<TokenType> FOLLOW_IGUAL = union(FOLLOW_E, TokenType.E_LOGICO);
    private static final EnumSet<TokenType> FOLLOW_REL = union(FOLLOW_IGUAL, TokenType.IGUAL, TokenType.DIFERENTE);
    private static final EnumSet<TokenType> FOLLOW_ADITIVA = union(FOLLOW_REL,
            TokenType.MENOR, TokenType.MENOR_IGUAL, TokenType.MAIOR, TokenType.MAIOR_IGUAL);
    private static final EnumSet<TokenType> FOLLOW_MULT = union(FOLLOW_ADITIVA, TokenType.MAIS, TokenType.MENOS);
    private static final EnumSet<TokenType> FOLLOW_PRIMARIA = union(FOLLOW_MULT,
            TokenType.MULTIPLICACAO, TokenType.DIVISAO);

    private final Scanner scanner;
    private Token lookahead;
    private final List<String> erros = new ArrayList<>();

    public Parser(String codigoFonte) {
        scanner = new Scanner(codigoFonte);
        lookahead = scanner.nextToken();
    }

    public List<String> getErros() { return Collections.unmodifiableList(erros); }
    public boolean analisar() { parsePrograma(); return erros.isEmpty(); }
    private TokenType atual() { return lookahead.getTipo(); }
    private boolean is(TokenType tipo) { return atual() == tipo; }
    private boolean in(EnumSet<TokenType> tipos) { return tipos.contains(atual()); }
    private void avancar() { lookahead = scanner.nextToken(); }
    private static EnumSet<TokenType> copy(EnumSet<TokenType> a) { return EnumSet.copyOf(a); }
    private static EnumSet<TokenType> union(EnumSet<TokenType> a, TokenType... extras) {
        EnumSet<TokenType> r = copy(a);
        Collections.addAll(r, extras);
        return r;
    }
    private void erro(String esperado) {
        String mensagem = "Erro sintatico na linha " + lookahead.getLinha() + ", coluna "
                + lookahead.getColuna() + ": esperado " + esperado + ", encontrado "
                + atual() + " ('" + lookahead.getLexema() + "')";
        erros.add(mensagem);
        throw new ErroSintatico();
    }
    private void match(TokenType esperado) {
        if (!is(esperado)) erro(esperado.name());
        avancar();
    }
    private void escolherVazio(EnumSet<TokenType> follow, String contexto) {
        if (!in(follow)) erro("continuação de " + contexto + " ou " + follow);
    }
    private static final class ErroSintatico extends RuntimeException { }

    /** Sincronização no nível da declaração: próximo tipo/void ou EOF. */
    private void sincronizarGlobal() {
        while (!is(TokenType.EOF) && !in(INICIO_DECL)) avancar();
    }
    /** Sincronização em comandos: ; ou }, preservando } para fechar o bloco. */
    private void sincronizarComando() {
        while (!is(TokenType.EOF) && !is(TokenType.PONTO_VIRGULA) && !is(TokenType.FECHA_CHAVES)) avancar();
        if (is(TokenType.PONTO_VIRGULA)) avancar();
    }

    public void parsePrograma() {
        parseListaDeclaracoes();
        if (!is(TokenType.EOF)) {
            try { erro("EOF"); } catch (ErroSintatico ex) { sincronizarGlobal(); }
        }
        // O EOF é o último token do Scanner; não é necessário solicitar outro token.
    }
    private void parseListaDeclaracoes() {
        while (true) {
            if (in(INICIO_DECL)) {
                try { parseDeclaracaoGlobal(); }
                catch (ErroSintatico ex) { sincronizarGlobal(); }
            } else if (is(TokenType.EOF)) return; // ε: EOF pertence a FOLLOW
            else {
                try { erro("declaração global ou EOF"); }
                catch (ErroSintatico ex) {
                    avancar(); // garante progresso mesmo diante de } ou ; isolados
                    sincronizarGlobal();
                }
            }
        }
    }
    private void parseTipo() {
        if (!in(TIPOS)) erro("tipo int/double/bool/char/string");
        avancar();
    }
    private void parseDeclaracaoGlobal() {
        if (in(TIPOS)) {
            parseTipo(); match(TokenType.IDENTIFICADOR); parseComplementoGlobal();
        } else if (is(TokenType.VOID)) {
            match(TokenType.VOID); match(TokenType.IDENTIFICADOR);
            match(TokenType.ABRE_PARENTESES); parseParametros();
            match(TokenType.FECHA_PARENTESES); parseBloco();
        } else erro("declaração global");
    }
    private void parseComplementoGlobal() {
        if (is(TokenType.ATRIBUICAO) || is(TokenType.PONTO_VIRGULA)) {
            parseInicializacao(); match(TokenType.PONTO_VIRGULA);
        } else if (is(TokenType.ABRE_PARENTESES)) {
            match(TokenType.ABRE_PARENTESES); parseParametros();
            match(TokenType.FECHA_PARENTESES); parseBloco();
        } else erro("=, ; ou (");
    }
    private void parseDeclaracaoLocal() {
        parseTipo(); match(TokenType.IDENTIFICADOR);
        parseInicializacao(); match(TokenType.PONTO_VIRGULA);
    }
    private void parseInicializacao() {
        if (is(TokenType.ATRIBUICAO)) { match(TokenType.ATRIBUICAO); parseExpressao(); }
        else escolherVazio(EnumSet.of(TokenType.PONTO_VIRGULA), "Inicializacao");
    }
    private void parseParametros() {
        if (in(TIPOS)) { parseParametro(); parseListaParametros(); }
        else escolherVazio(EnumSet.of(TokenType.FECHA_PARENTESES), "Parametros");
    }
    private void parseParametro() { parseTipo(); match(TokenType.IDENTIFICADOR); }
    private void parseListaParametros() {
        while (is(TokenType.VIRGULA)) { match(TokenType.VIRGULA); parseParametro(); }
        escolherVazio(EnumSet.of(TokenType.FECHA_PARENTESES), "ListaParametros");
    }
    private void parseBloco() {
        match(TokenType.ABRE_CHAVES);
        parseListaComandos();
        match(TokenType.FECHA_CHAVES);
    }
    private void parseListaComandos() {
        while (true) {
            if (in(INICIO_COMANDO)) {
                try { parseComando(); }
                catch (ErroSintatico ex) { sincronizarComando(); }
            } else if (is(TokenType.FECHA_CHAVES)) return; // ε somente em FOLLOW
            else if (is(TokenType.EOF)) {
                // EOF não pertence a FOLLOW(ListaComandos): bloco não fechado.
                erro("comando ou }");
            } else {
                try { erro("comando ou }"); }
                catch (ErroSintatico ex) { sincronizarComando(); }
            }
        }
    }
    private void parseComando() {
        if (in(TIPOS)) parseDeclaracaoLocal();
        else if (is(TokenType.IDENTIFICADOR)) parseComandoIdentificador();
        else if (is(TokenType.RETURN)) parseComandoRetorno();
        else if (is(TokenType.WHILE)) parseComandoWhile();
        else if (is(TokenType.IF)) parseComandoIf();
        else if (is(TokenType.ABRE_CHAVES)) parseBloco();
        else erro("comando");
    }
    private void parseComandoIdentificador() {
        match(TokenType.IDENTIFICADOR); parseComplementoIdentificador();
    }
    private void parseComplementoIdentificador() {
        if (is(TokenType.ATRIBUICAO)) {
            match(TokenType.ATRIBUICAO); parseExpressao(); match(TokenType.PONTO_VIRGULA);
        } else if (is(TokenType.ABRE_PARENTESES)) {
            match(TokenType.ABRE_PARENTESES); parseArgumentos();
            match(TokenType.FECHA_PARENTESES); match(TokenType.PONTO_VIRGULA);
        } else erro("= ou (");
    }
    private void parseComandoRetorno() {
        match(TokenType.RETURN); parseValorRetorno(); match(TokenType.PONTO_VIRGULA);
    }
    private void parseValorRetorno() {
        if (in(INICIO_EXP)) parseExpressao();
        else escolherVazio(EnumSet.of(TokenType.PONTO_VIRGULA), "ValorRetorno");
    }
    private void parseComandoWhile() {
        match(TokenType.WHILE); match(TokenType.ABRE_PARENTESES);
        parseExpressao(); match(TokenType.FECHA_PARENTESES); parseBloco();
    }
    private void parseComandoIf() {
        match(TokenType.IF); match(TokenType.ABRE_PARENTESES);
        parseExpressao(); match(TokenType.FECHA_PARENTESES);
        parseBloco(); parseOpcaoElse();
    }
    private void parseOpcaoElse() {
        if (is(TokenType.ELSE)) { match(TokenType.ELSE); parseBloco(); }
        else {
            EnumSet<TokenType> follow = union(INICIO_COMANDO, TokenType.FECHA_CHAVES);
            escolherVazio(follow, "OpcaoElse");
        }
    }
    private void parseArgumentos() {
        if (in(INICIO_EXP)) { parseExpressao(); parseListaArgumentos(); }
        else escolherVazio(EnumSet.of(TokenType.FECHA_PARENTESES), "Argumentos");
    }
    private void parseListaArgumentos() {
        while (is(TokenType.VIRGULA)) { match(TokenType.VIRGULA); parseExpressao(); }
        escolherVazio(EnumSet.of(TokenType.FECHA_PARENTESES), "ListaArgumentos");
    }
    private void parseExpressao() { parseOuLogico(); }
    private void parseOuLogico() { parseELogico(); parseOuLogicoLinha(); }
    private void parseOuLogicoLinha() {
        while (is(TokenType.OU_LOGICO)) { match(TokenType.OU_LOGICO); parseELogico(); }
        escolherVazio(FOLLOW_OU, "OuLogicoLinha");
    }
    private void parseELogico() { parseIgualdade(); parseELogicoLinha(); }
    private void parseELogicoLinha() {
        while (is(TokenType.E_LOGICO)) { match(TokenType.E_LOGICO); parseIgualdade(); }
        escolherVazio(FOLLOW_E, "ELogicoLinha");
    }
    private void parseIgualdade() { parseRelacional(); parseIgualdadeLinha(); }
    private void parseIgualdadeLinha() {
        while (is(TokenType.IGUAL) || is(TokenType.DIFERENTE)) {
            avancar(); parseRelacional();
        }
        escolherVazio(FOLLOW_IGUAL, "IgualdadeLinha");
    }
    private void parseRelacional() { parseAditiva(); parseRelacionalLinha(); }
    private void parseRelacionalLinha() {
        while (in(REL)) { parseOperadorRelacional(); parseAditiva(); }
        escolherVazio(FOLLOW_REL, "RelacionalLinha");
    }
    private void parseOperadorRelacional() {
        if (!in(REL)) erro("operador relacional");
        avancar();
    }
    private void parseAditiva() { parseMultiplicativa(); parseAditivaLinha(); }
    private void parseAditivaLinha() {
        while (is(TokenType.MAIS) || is(TokenType.MENOS)) {
            avancar(); parseMultiplicativa();
        }
        escolherVazio(FOLLOW_ADITIVA, "AditivaLinha");
    }
    private void parseMultiplicativa() { parseUnaria(); parseMultiplicativaLinha(); }
    private void parseMultiplicativaLinha() {
        while (is(TokenType.MULTIPLICACAO) || is(TokenType.DIVISAO)) {
            avancar(); parseUnaria();
        }
        escolherVazio(FOLLOW_MULT, "MultiplicativaLinha");
    }
    private void parseUnaria() {
        if (is(TokenType.NEGACAO) || is(TokenType.MENOS)) { avancar(); parseUnaria(); }
        else if (is(TokenType.NUMERO) || is(TokenType.STRING_LITERAL) || is(TokenType.TRUE)
                || is(TokenType.FALSE) || is(TokenType.IDENTIFICADOR) || is(TokenType.ABRE_PARENTESES)) {
            parsePrimaria();
        } else erro("expressão unária ou primária");
    }
    private void parsePrimaria() {
        if (is(TokenType.NUMERO) || is(TokenType.STRING_LITERAL) || is(TokenType.TRUE) || is(TokenType.FALSE)) {
            avancar();
        } else if (is(TokenType.IDENTIFICADOR)) {
            match(TokenType.IDENTIFICADOR); parseSufixoIdentificador();
        } else if (is(TokenType.ABRE_PARENTESES)) {
            match(TokenType.ABRE_PARENTESES); parseExpressao(); match(TokenType.FECHA_PARENTESES);
        } else erro("literal, identificador ou (");
    }
    private void parseSufixoIdentificador() {
        if (is(TokenType.ABRE_PARENTESES)) {
            match(TokenType.ABRE_PARENTESES); parseArgumentos(); match(TokenType.FECHA_PARENTESES);
        } else escolherVazio(FOLLOW_PRIMARIA, "SufixoIdentificador");
    }
}
