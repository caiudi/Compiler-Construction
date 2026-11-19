package ast;
/** Operadores correspondentes aos TokenType do CP1, sem modificar o scanner. */
public enum Operator {
    OU_LOGICO, E_LOGICO, IGUAL, DIFERENTE,
    MENOR, MENOR_IGUAL, MAIOR, MAIOR_IGUAL,
    MAIS, MENOS, MULTIPLICACAO, DIVISAO, NEGACAO
}
