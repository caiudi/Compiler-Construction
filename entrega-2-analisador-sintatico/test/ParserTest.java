import java.util.List;

/** Suite de testes do Checkpoint 2, secao 3.3. Sem dependencias externas. */
public class ParserTest {
    private static int aprovados = 0;
    private static int reprovados = 0;

    private static void testar(String categoria, String nome, String codigo,
                               boolean deveAceitar, int minimoErros) {
        Parser parser = new Parser(codigo);
        boolean aceito = parser.analisar();
        List<String> erros = parser.getErros();
        boolean localizacaoCorreta = true;
        for (String erro : erros) {
            if (!erro.contains("linha ") || !erro.contains("coluna ")) {
                localizacaoCorreta = false;
            }
        }
        boolean passou = (aceito == deveAceitar)
                && (deveAceitar ? erros.isEmpty() : erros.size() >= minimoErros)
                && localizacaoCorreta;

        System.out.println("\n[" + (passou ? "OK" : "FALHOU") + "] " + categoria + " - " + nome);
        System.out.println("  Esperado: " + (deveAceitar ? "ACEITO" : "REJEITADO")
                + " | Obtido: " + (aceito ? "ACEITO" : "REJEITADO"));
        System.out.println("  Quantidade de erros: " + erros.size());
        for (int i = 0; i < erros.size(); i++) {
            System.out.println("  Erro " + (i + 1) + ": " + erros.get(i));
        }
        if (passou) aprovados++; else reprovados++;
    }

    public static void main(String[] args) {
        System.out.println("=== CHECKPOINT 2 - SECAO 3.3: SUITE DE TESTES ===");

        // Tres programas validos: declaracoes/tipos, funcoes, controle e precedencia.
        testar("VALIDO 1", "tipos e inicializacoes",
                "int x; double d=3.14; bool b=true; char c; string s=\"oi\";",
                true, 0);
        testar("VALIDO 2", "funcoes, parametros e chamada",
                "int soma(int a,int b){return a+b;} void main(){int x=soma(2,3);return;}",
                true, 0);
        testar("VALIDO 3", "while, if/else, escopos e operadores",
                "void f(){int x=0; while(x<10){if(x==5 || !false){x=x+2;}else{x=x+1;}}return;}",
                true, 0);

        // Tres erros sintaticos diferentes.
        testar("INVALIDO 1", "ponto e virgula ausente",
                "void f(){int x=2 return;}", false, 1);
        testar("INVALIDO 2", "expressao ausente apos atribuicao",
                "void f(){int x=;return;}", false, 1);
        testar("INVALIDO 3", "parametro incompleto apos virgula",
                "int soma(int a,){return a;}", false, 1);

        // Duas instrucoes invalidas no mesmo bloco: o parser precisa continuar
        // depois do primeiro ';' e registrar o segundo erro na mesma analise.
        testar("PANICO", "dois erros na mesma rodada",
                "void f(){int x=; int y=; return;}", false, 2);

        System.out.println("\n=== RESUMO ===");
        System.out.println("Aprovados: " + aprovados + " | Reprovados: " + reprovados);
        if (reprovados > 0) System.exit(1);
    }
}
