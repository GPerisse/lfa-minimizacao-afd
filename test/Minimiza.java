import afds.*;

/**
 * Programa de demonstracao: le um AFD em XML, minimiza mostrando o passo a passo,
 * valida o resultado e grava o AFD minimizado em XML.
 *
 * Uso:  java -cp out Minimiza <arquivo.xml> [nome-de-saida]
 */
public class Minimiza {

    private static final int TAMANHO_MAXIMO = 12; // limite de tamanho das palavras na validacao

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Uso: java -cp out Minimiza <arquivo.xml> [nome-de-saida]");
            System.out.println("Exemplo: java -cp out Minimiza test/minimizacao/aula07.XML saida_aula07");
            return;
        }
        String caminho = args[0];
        String saida = (args.length > 1) ? args[1] : "saida_minimizado";

        AFD original = new AFD();
        original.ler(caminho);

        // Este objeto mostra o algoritmo passo a passo.
        MinimizadorAFD passos = new MinimizadorAFD(original);
        titulo("1. AFD de entrada (" + caminho + ")");
        System.out.println(passos);

        passos.prepararEntrada();
        titulo("2. Pre-requisitos do algoritmo");
        System.out.println("Estados inalcancaveis removidos: " + passos.getRemovidos());
        System.out.println("Estado criado para completar a funcao: "
                + (passos.getEstadoMorto() == null ? "nenhum (a funcao ja era total)" : passos.getEstadoMorto()));
        System.out.println(passos);

        passos.montarTabela();
        titulo("3. Tabela de pares apos marcar final x nao-final (X)");
        System.out.println(passos.getTabelaDePares());

        titulo("4. Passo (c): transicoes, dependencias e propagacao");
        passos.setVerbose(true);
        passos.marcarNaoEquivalentes();
        System.out.println();
        System.out.println("Tabela de pares apos o passo (c) (+ = marcado no passo c, . = equivalente):");
        System.out.println(passos.getTabelaDePares());

        titulo("5. Pares equivalentes");
        System.out.println(passos.getEquivalentes());

        // Outro objeto faz o algoritmo completo para obter o AFD final.
        MinimizadorAFD completo = new MinimizadorAFD(original);
        AFD minimo = completo.minimizar();
        titulo("6. AFD minimizado (unificacao e remocao dos inuteis)");
        System.out.println("Estados: " + original.getEstados().size() + " -> " + minimo.getEstados().size());
        System.out.println("Estados inuteis removidos: " + completo.getInuteisRemovidos());
        System.out.println(completo);

        titulo("7. Validacao");
        int tamanho = Math.min(original.getEstados().size() + minimo.getEstados().size(), TAMANHO_MAXIMO);
        String contra = ComparadorLinguagem.contraExemplo(original, minimo, tamanho);
        System.out.println("Mesma linguagem (todas as palavras ate tamanho " + tamanho + "): "
                + (contra == null ? "sim" : "NAO, palavra \"" + contra + "\""));
        String problema = ComparadorLinguagem.verificarMinimo(minimo);
        System.out.println("Resultado e minimo (alcancaveis, sem inuteis, estados distinguiveis): "
                + (problema == null ? "sim" : "NAO, " + problema));

        minimo.toXML(saida);
        System.out.println("AFD minimizado gravado em " + saida + ".xml");
    }

    private static void titulo(String texto) {
        System.out.println();
        System.out.println("=== " + texto + " ===");
    }
}