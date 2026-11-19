public enum TokenType {

    // Identificadores
    IDENTIFICADOR,

    // Palavras reservadas
    INT,
    DOUBLE,
    BOOL,
    CHAR,
    STRING,
    VOID,
    IF,
    ELSE,
    WHILE,
    RETURN,
    TRUE,
    FALSE,
    PRINT,             // extensao do CP5
    READ,              // extensao do CP5

    // Literais
    NUMERO,
    STRING_LITERAL,
    CHAR_LITERAL,      // extensao lexica do CP3: 'a', '\n'

    // Operadores
    MAIS,
    MENOS,
    MULTIPLICACAO,
    DIVISAO,

    ATRIBUICAO,        // =
    IGUAL,             // ==
    DIFERENTE,         // !=
    MENOR,             // <
    MENOR_IGUAL,       // <=
    MAIOR,             // >
    MAIOR_IGUAL,       // >=

    E_LOGICO,          // &&
    OU_LOGICO,         // ||
    NEGACAO,           // !

    // Delimitadores
    ABRE_PARENTESES,   // (
    FECHA_PARENTESES,  // )
    ABRE_CHAVES,       // {
    FECHA_CHAVES,      // }
    VIRGULA,           // ,
    PONTO_VIRGULA,     // ;

    // Controle
    EOF,
    ERRO
}
