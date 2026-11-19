package ast;

public abstract class ExprNode extends Node {
    /**
     * CP5: tipo calculado pelo verificador (TypeChecker); null antes da verificacao.
     * Unica mudanca do CP5 na hierarquia de expressoes. E o campo que a geracao
     * de codigo vai ler (por exemplo, para saber onde converter int em double).
     */
    public types.Type type;

    protected ExprNode(int linha, int coluna) { super(linha, coluna); }
}
