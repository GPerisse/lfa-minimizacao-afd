import afds.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Teste de estresse: gera AFDs aleatorios (com inalcancaveis, inuteis,
 * transicoes indefinidas e estados equivalentes), minimiza cada um e confere
 * o resultado com verificadores independentes do algoritmo.
 */
public class TesteAleatorio {

    private static final int QUANTIDADE = 500; // quantos AFDs gerar
    private static final long SEMENTE = 2026L; // mesma semente = mesmos AFDs em toda execucao

    public static void main(String[] args) {
        Random sorteio = new Random(SEMENTE);
        int falhas = 0;
        int reduziram = 0;
        int estadosAntes = 0;
        int estadosDepois = 0;

        for (int caso = 1; caso <= QUANTIDADE; caso++) {
            AFD afd = gerar(sorteio);
            int antes = afd.getEstados().size();
            String problema;
            int depois = antes;
            try {
                AFD minimo = new MinimizadorAFD(afd).minimizar();
                depois = minimo.getEstados().size();
                problema = verificar(afd, minimo);
            } catch (RuntimeException e) {
                problema = "excecao: " + e;
            }

            estadosAntes += antes;
            estadosDepois += depois;
            if (depois < antes) {
                reduziram++;
            }
            if (problema != null) {
                falhas++;
                if (falhas <= 3) { // mostra so as primeiras falhas
                    System.out.println("[ERRO] caso " + caso + ": " + problema);
                    System.out.println("       AFD: " + afd);
                }
            }
        }

        System.out.println("AFDs gerados: " + QUANTIDADE + " (semente " + SEMENTE + ")");
        System.out.println("Casos em que o numero de estados diminuiu: " + reduziram);
        System.out.println("Total de estados: " + estadosAntes + " -> " + estadosDepois);
        System.out.println(falhas == 0 ? "RESULTADO: TODOS OS CASOS PASSARAM" : "RESULTADO: " + falhas + " FALHA(S)");
    }

    // Gera um AFD aleatorio: 1 a 6 estados, alfabeto de 2 ou 3 simbolos.
    private static AFD gerar(Random sorteio) {
        int n = 1 + sorteio.nextInt(6);
        int k = 2 + sorteio.nextInt(2);

        ConjuntoSimbolo simbolos = new ConjuntoSimbolo();
        for (int s = 0; s < k; s++) {
            simbolos.inclui(new Simbolo((char) ('a' + s)));
        }

        ConjuntoEstados estados = new ConjuntoEstados();
        ConjuntoEstados finais = new ConjuntoEstados();
        for (int i = 0; i < n; i++) {
            Estado e = new Estado("q" + i);
            estados.inclui(e);
            if (sorteio.nextInt(100) < 40) { // 40% de chance de ser final
                finais.inclui(e);
            }
        }

        ConjuntoTransicaoD funcao = new ConjuntoTransicaoD();
        for (int i = 0; i < n; i++) {
            for (int s = 0; s < k; s++) {
                if (sorteio.nextInt(100) < 20) { // 20% de chance de a transicao nao existir
                    continue;
                }
                TransicaoD t = new TransicaoD();
                t.setOrigem(new Estado("q" + i));
                t.setSimbolo(new Simbolo((char) ('a' + s)));
                t.setDestino(new Estado("q" + sorteio.nextInt(n)));
                funcao.inclui(t);
            }
        }
        return new AFD(simbolos, estados, funcao, new Estado("q0"), finais);
    }

    // Devolve null se o AFD minimizado passou nas tres conferencias; senao, o problema.
    private static String verificar(AFD original, AFD minimo) {
        String palavra = diferenca(original, minimo);
        if (palavra != null) {
            return "linguagens diferentes, palavra \"" + palavra + "\"";
        }
        String naoMinimo = ComparadorLinguagem.verificarMinimo(minimo);
        if (naoMinimo != null) {
            return "resultado nao e minimo: " + naoMinimo;
        }
        int novamente = new MinimizadorAFD(minimo).minimizar().getEstados().size();
        if (novamente != minimo.getEstados().size()) {
            return "minimizar de novo mudou " + minimo.getEstados().size() + " -> " + novamente;
        }
        return null;
    }

    /**
     * Comparacao EXATA das linguagens (sem limite de tamanho): percorre os pares
     * de estados alcancaveis (um de x, um de y). Se algum par tem um final e o
     * outro nao, devolve a menor palavra que chega nele. null = linguagens iguais.
     * Uma transicao indefinida leva a um "estado morto" (representado por null).
     */
    private static String diferenca(AFD x, AFD y) {
        List<Character> alfa = ComparadorLinguagem.alfabeto(x);
        for (Character c : ComparadorLinguagem.alfabeto(y)) {
            if (!alfa.contains(c)) {
                alfa.add(c);
            }
        }

        List<String[]> fila = new ArrayList<String[]>(); // cada item: {estado de x, estado de y, palavra}
        List<String> vistos = new ArrayList<String>();
        String[] inicio = {x.getEstadoInicial().getNome(), y.getEstadoInicial().getNome(), ""};
        fila.add(inicio);
        vistos.add(inicio[0] + "|" + inicio[1]);

        for (int i = 0; i < fila.size(); i++) {
            String[] par = fila.get(i);
            boolean finalX = par[0] != null && x.getEstadosFinais().pertence(new Estado(par[0]));
            boolean finalY = par[1] != null && y.getEstadosFinais().pertence(new Estado(par[1]));
            if (finalX != finalY) {
                return par[2];
            }
            for (Character c : alfa) {
                String[] proximo = {destino(x, par[0], c), destino(y, par[1], c), par[2] + c};
                String chave = proximo[0] + "|" + proximo[1];
                if (!vistos.contains(chave)) {
                    vistos.add(chave);
                    fila.add(proximo);
                }
            }
        }
        return null;
    }

    // Estado para onde o AFD vai a partir de 'estado' com o simbolo c (null = morto).
    private static String destino(AFD afd, String estado, Character c) {
        if (estado == null) {
            return null;
        }
        Estado d = afd.p(new Estado(estado), new Simbolo(c.charValue()));
        return (d == null) ? null : d.getNome();
    }
}