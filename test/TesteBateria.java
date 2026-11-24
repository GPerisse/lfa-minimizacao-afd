import afds.*;

/**
 * Bateria de testes da minimizacao: para cada XML confere o numero de estados,
 * a linguagem, a minimalidade e a idempotencia.
 */
public class TesteBateria {

    // Cada caso: arquivo XML e numero esperado de estados do AFD minimizado.
    private static final String[][] CASOS = {
        {"./test/minimizacao/aula07.XML", "4"},
        {"./test/AFD.XML", "3"},
        {"./test/minimizacao/inalcancaveis.XML", "2"},
        {"./test/minimizacao/colisao_d.XML", "2"},
        {"./test/minimizacao/inuteis.XML", "2"},
        {"./test/minimizacao/vazia.XML", "1"},
        {"./test/minimizacao/sem_equivalentes.XML", "3"},
        {"./test/minimizacao/varios_pares.XML", "3"},
        {"./test/minimizacao/ja_minimo.XML", "4"},
        {"./test/minimizacao/todos_finais.XML", "1"},
    };

    private static final int TAMANHO_MAXIMO = 10; // palavras testadas: ate este tamanho

    public static void main(String[] args) throws Exception {
        int falhas = 0;
        for (String[] caso : CASOS) {
            if (!rodar(caso[0], Integer.parseInt(caso[1]))) {
                falhas++;
            }
        }

        // Os verificadores tambem precisam saber dizer "nao": testamos casos errados de proposito.
        System.out.println();
        System.out.println("--- Os verificadores detectam erros? ---");
        AFD aula07 = carregar("./test/minimizacao/aula07.XML");
        AFD professor = carregar("./test/AFD.XML");
        String w = ComparadorLinguagem.contraExemplo(aula07, professor, TAMANHO_MAXIMO);
        System.out.println("Aula 07 x AFD.XML (linguagens diferentes): palavra que diferencia = "
                + (w == null ? "NENHUMA (ruim!)" : "\"" + w + "\""));
        if (w == null) {
            falhas++;
        }
        String problema = ComparadorLinguagem.verificarMinimo(aula07);
        System.out.println("Aula 07 original (nao e minimo): " + (problema == null ? "NENHUM PROBLEMA (ruim!)" : problema));
        if (problema == null) {
            falhas++;
        }

        System.out.println();
        System.out.println(falhas == 0 ? "RESULTADO: TODOS OS TESTES PASSARAM" : "RESULTADO: " + falhas + " FALHA(S)");
    }

    private static AFD carregar(String caminho) throws Exception {
        AFD afd = new AFD();
        afd.ler(caminho);
        return afd;
    }

    private static boolean rodar(String caminho, int esperado) throws Exception {
        AFD original = carregar(caminho);
        AFD minimo = new MinimizadorAFD(original).minimizar();

        int obtido = minimo.getEstados().size();
        String contra = ComparadorLinguagem.contraExemplo(original, minimo, TAMANHO_MAXIMO);
        String naoMinimo = ComparadorLinguagem.verificarMinimo(minimo);
        int novamente = new MinimizadorAFD(minimo).minimizar().getEstados().size();

        boolean ok = (obtido == esperado) && contra == null && naoMinimo == null && novamente == obtido;
        System.out.println((ok ? "[OK]   " : "[ERRO] ") + caminho);
        System.out.println("       estados: " + original.getEstados().size() + " -> " + obtido
                + " (esperado " + esperado + "); minimizar de novo: " + novamente);
        System.out.println("       linguagem igual ate tamanho " + TAMANHO_MAXIMO + ": "
                + (contra == null ? "sim" : "NAO, palavra \"" + contra + "\""));
        System.out.println("       resultado e minimo: " + (naoMinimo == null ? "sim" : "NAO, " + naoMinimo));
        return ok;
    }
}