import ast.*;
import types.Type;
import visitor.TypeChecker;
import java.util.*;

/**
 * CP5: regras isoladas, sem passar pelo parser. Cada linha da tabela de operadores
 * (regras-de-tipos-rascunho.md, Secao 2) aparece aqui, aceita e recusada, montando
 * a expressao diretamente com nos da AST.
 */
public final class TypeRulesTest {
    private static int total = 0;

    private static void check(boolean ok, String description) {
        total++;
        if (!ok) throw new AssertionError(description);
    }

    /** Uma expressao-folha de cada tipo; "erro" e um identificador sem declaracao. */
    private static ExprNode leaf(String type) {
        return switch (type) {
            case "int" -> new IntLiteralNode(1, 1, "1");
            case "double" -> new DoubleLiteralNode(1, 1, "1.5");
            case "bool" -> new BoolLiteralNode(1, 1, true);
            case "char" -> new CharLiteralNode(1, 1, 'a');
            case "string" -> new StringLiteralNode(1, 1, "\"s\"");
            case "erro" -> new IdNode(1, 1, "naoDeclarado");
            default -> throw new IllegalArgumentException(type);
        };
    }

    private static final Map<String, Operator> OPS = Map.ofEntries(
            Map.entry("+", Operator.MAIS), Map.entry("-", Operator.MENOS),
            Map.entry("*", Operator.MULTIPLICACAO), Map.entry("/", Operator.DIVISAO),
            Map.entry("<", Operator.MENOR), Map.entry("<=", Operator.MENOR_IGUAL),
            Map.entry(">", Operator.MAIOR), Map.entry(">=", Operator.MAIOR_IGUAL),
            Map.entry("==", Operator.IGUAL), Map.entry("!=", Operator.DIFERENTE),
            Map.entry("&&", Operator.E_LOGICO), Map.entry("||", Operator.OU_LOGICO),
            Map.entry("!", Operator.NEGACAO));

    /** Linha aceita: "l op r = resultado". Linha recusada: "l op r = erro" + mensagem esperada. */
    private static void binary(String row) {
        String[] p = row.split(" ");
        String l = p[0], op = p[1], r = p[2], expected = p[4];
        TypeChecker tc = new TypeChecker();
        Type got = tc.typeOf(new BinaryExprNode(1, 1, OPS.get(op), leaf(l), leaf(r)));
        check(got.toString().equals(expected), row + " -> obtido " + got);
        List<String> errors = tc.getErrors();
        if (expected.equals("erro") && !l.equals("erro") && !r.equals("erro")) {
            String msg = "linha 1: operador '" + op + "' não se aplica a " + l + " e " + r;
            check(errors.equals(List.of(msg)), row + " -> mensagem " + errors);
        } else {
            check(errors.isEmpty(), row + " -> nao deveria haver mensagem: " + errors);
        }
        System.out.println("OK " + row);
    }

    private static void unary(String row) {
        String[] p = row.split(" ");
        String op = p[0], t = p[1], expected = p[3];
        TypeChecker tc = new TypeChecker();
        Type got = tc.typeOf(new UnaryExprNode(1, 1, OPS.get(op), leaf(t)));
        check(got.toString().equals(expected), row + " -> obtido " + got);
        List<String> errors = tc.getErrors();
        if (expected.equals("erro") && !t.equals("erro"))
            check(errors.equals(List.of("linha 1: operador '" + op + "' não se aplica a " + t)), row + " -> " + errors);
        else
            check(errors.isEmpty(), row + " -> nao deveria haver mensagem: " + errors);
        System.out.println("OK " + row);
    }

    private static Type t(String name) {
        return switch (name) {
            case "int" -> Type.INT; case "double" -> Type.DOUBLE; case "bool" -> Type.BOOL;
            case "char" -> Type.CHAR; case "string" -> Type.STRING; case "erro" -> Type.ERROR;
            default -> throw new IllegalArgumentException(name);
        };
    }

    /** [COMPAT]: "destino <- valor = sim|nao" */
    private static void compat(String row) {
        String[] p = row.split(" ");
        boolean expected = p[4].equals("sim");
        check(TypeChecker.assignable(t(p[0]), t(p[2])) == expected, row);
        System.out.println("OK compat " + row);
    }

    public static void main(String[] args) {
        // ---- aritmeticos: + - * /   [ARIT-INT] [ARIT-DOUBLE] [CONCAT]
        for (String row : List.of(
                "int + int = int", "int + double = double", "double + int = double",
                "double + double = double", "char + int = int", "char + char = int",
                "char + double = double", "string + string = string",
                "int - int = int", "int - double = double", "char - char = int",
                "int * int = int", "double * int = double", "char * int = int",
                "int / int = int", "int / double = double", "double / double = double",
                // recusados
                "int + bool = erro", "bool + bool = erro", "string + int = erro",
                "int + string = erro", "string + char = erro", "string - string = erro",
                "bool - int = erro", "bool * double = erro", "string / int = erro"))
            binary(row);

        // ---- relacionais: < <= > >=   [REL]
        for (String row : List.of(
                "int < int = bool", "int <= double = bool", "double > int = bool",
                "char >= char = bool", "char < int = bool",
                "string < string = erro", "bool > bool = erro", "int <= bool = erro",
                "char >= string = erro"))
            binary(row);

        // ---- igualdade: == !=   [IGUAL]
        for (String row : List.of(
                "int == int = bool", "int == double = bool", "char == int = bool",
                "char != char = bool", "bool == bool = bool", "string != string = bool",
                "double == double = bool",
                "string == int = erro", "bool == int = erro", "bool != string = erro",
                "char == string = erro"))
            binary(row);

        // ---- logicos: && ||   [LOG]
        for (String row : List.of(
                "bool && bool = bool", "bool || bool = bool",
                "int && int = erro", "bool || int = erro", "string && bool = erro"))
            binary(row);

        // ---- unarios   [NEG] [NAO]
        for (String row : List.of(
                "- int = int", "- double = double", "- char = int", "! bool = bool",
                "- bool = erro", "- string = erro", "! int = erro", "! string = erro"))
            unary(row);

        // ---- sem cascata: um operando com erro devolve erro, sem nova mensagem
        for (String row : List.of(
                "erro + int = erro", "int * erro = erro", "erro == string = erro",
                "erro && bool = erro"))
            binary(row);
        for (String row : List.of("- erro = erro", "! erro = erro"))
            unary(row);

        // ---- compatibilidade de atribuicao/argumento/return   [COMPAT]
        for (String row : List.of(
                "int <- int = sim", "double <- int = sim", "double <- char = sim",
                "int <- char = sim", "string <- string = sim",
                "int <- double = nao", "char <- int = nao", "bool <- int = nao",
                "string <- char = nao", "int <- bool = nao",
                "int <- erro = sim", "erro <- string = sim"))
            compat(row);

        System.out.println("TOTAL: " + total + " verificacoes das regras");
    }
}
