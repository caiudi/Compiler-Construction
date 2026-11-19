package types;

import ast.BasicType;

/**
 * Tipo semantico de uma expressao ou declaracao.
 *
 * Cada tipo e uma instancia unica (singleton): a comparacao e feita com ==.
 * VOID e o "tipo" de um procedimento; ERROR e o tipo de uma expressao que ja
 * gerou erro e e compativel com tudo, para nao haver erros em cascata.
 */
public final class Type {

    public enum Kind { INT, DOUBLE, BOOL, CHAR, STRING, VOID, ERROR }

    public static final Type INT = new Type(Kind.INT, "int");
    public static final Type DOUBLE = new Type(Kind.DOUBLE, "double");
    public static final Type BOOL = new Type(Kind.BOOL, "bool");
    public static final Type CHAR = new Type(Kind.CHAR, "char");
    public static final Type STRING = new Type(Kind.STRING, "string");
    public static final Type VOID = new Type(Kind.VOID, "void");
    public static final Type ERROR = new Type(Kind.ERROR, "erro");

    public final Kind kind;
    private final String name;

    private Type(Kind kind, String name) {
        this.kind = kind;
        this.name = name;
    }

    /** Converte o tipo escrito no programa (TypeNode.kind) no tipo semantico. */
    public static Type of(BasicType basic) {
        return switch (basic) {
            case INT -> INT;
            case DOUBLE -> DOUBLE;
            case BOOL -> BOOL;
            case CHAR -> CHAR;
            case STRING -> STRING;
        };
    }

    public boolean isError() {
        return this == ERROR;
    }

    /** int, double e char participam da aritmetica (decisao 2: char vale como int). */
    public boolean isNumeric() {
        return this == INT || this == DOUBLE || this == CHAR;
    }

    @Override
    public String toString() {
        return name;
    }
}
