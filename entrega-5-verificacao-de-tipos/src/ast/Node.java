package ast;
import visitor.Visitor;
public abstract class Node {
    public final int linha;
    public final int coluna;
    protected Node(int linha, int coluna) {
        if (linha < 1 || coluna < 1) throw new IllegalArgumentException("Posicao deve ser positiva");
        this.linha = linha; this.coluna = coluna;
    }
    public abstract <T> T accept(Visitor<T> v);
}
