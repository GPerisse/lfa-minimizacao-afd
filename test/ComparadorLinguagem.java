import afds.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Verificadores usados nos testes: comparam linguagens e conferem minimalidade
 * por forca bruta (testando todas as palavras ate um certo tamanho).
 */
public class ComparadorLinguagem {

    // Alfabeto do AFD, como lista de caracteres sem repeticao.
    static List<Character> alfabeto(AFD afd) {
        List<Character> resultado = new ArrayList<Character>();
        for (Object o : afd.getSimbolos().getElementos()) {
            Character c = Character.valueOf(((Simbolo) o).getSimbolo());
            if (!resultado.contains(c)) {
                resultado.add(c);
            }
        }
        return resultado;
    }

    // Todas as palavras de tamanho 0 ate max sobre o alfabeto.
    static List<String> palavras(List<Character> alfabeto, int max) {
        List<String> todas = new ArrayList<String>();
        List<String> nivel = new ArrayList<String>();
        nivel.add("");
        todas.add("");
        for (int tamanho = 1; tamanho <= max; tamanho++) {
            List<String> proximo = new ArrayList<String>();
            for (String w : nivel) {
                for (Character c : alfabeto) {
                    proximo.add(w + c);
                }
            }
            todas.addAll(proximo);
            nivel = proximo;
        }
        return todas;
    }

    /**
     * Compara as linguagens de x e y. Devolve null se aceitam exatamente as mesmas
     * palavras ate o tamanho max; senao, devolve uma palavra que os diferencia.
     */
    public static String contraExemplo(AFD x, AFD y, int max) {
        List<Character> alfa = alfabeto(x);
        for (Character c : alfabeto(y)) {
            if (!alfa.contains(c)) {
                alfa.add(c);
            }
        }
        for (String w : palavras(alfa, max)) {
            if (x.Aceita(w) != y.Aceita(w)) {
                return w;
            }
        }
        return null;
    }

    /**
     * Confere se o AFD e minimo (como AFD parcial, sem estado morto):
     * todos os estados alcancaveis, nenhum inutil e todos distinguiveis entre si.
     * Devolve null se estiver tudo certo; senao, uma mensagem com o problema.
     */
    public static String verificarMinimo(AFD afd) {
        List<String> nomes = new ArrayList<String>();
        for (Object o : afd.getEstados().getElementos()) {
            nomes.add(((Estado) o).getNome());
        }
        int n = nomes.size();
        List<Character> alfa = alfabeto(afd);

        // 1) Todos os estados sao alcancaveis a partir do inicial?
        List<String> vistos = new ArrayList<String>();
        vistos.add(afd.getEstadoInicial().getNome());
        for (int i = 0; i < vistos.size(); i++) {
            for (Character c : alfa) {
                Estado destino = afd.p(new Estado(vistos.get(i)), new Simbolo(c.charValue()));
                if (destino != null && !vistos.contains(destino.getNome())) {
                    vistos.add(destino.getNome());
                }
            }
        }
        for (String nome : nomes) {
            if (!vistos.contains(nome)) {
                return "estado inalcancavel: " + nome;
            }
        }

        // 2) Para cada estado, quais palavras (ate tamanho n) levam a um estado final?
        List<String> ws = palavras(alfa, n);
        boolean[][] aceita = new boolean[n][ws.size()];
        boolean algumaAceita = false;
        for (int i = 0; i < n; i++) {
            Estado e = new Estado(nomes.get(i));
            for (int k = 0; k < ws.size(); k++) {
                aceita[i][k] = afd.getEstadosFinais().pertence(afd.pe(e, ws.get(k)));
                algumaAceita = algumaAceita || aceita[i][k];
            }
        }

        // 3) Nenhum estado e inutil (todos chegam a algum final)?
        //    Excecao: linguagem vazia, em que o unico estado restante e o inicial.
        if (algumaAceita || n > 1) {
            for (int i = 0; i < n; i++) {
                boolean chegaAFinal = false;
                for (int k = 0; k < ws.size(); k++) {
                    chegaAFinal = chegaAFinal || aceita[i][k];
                }
                if (!chegaAFinal) {
                    return "estado inutil: " + nomes.get(i);
                }
            }
        }

        // 4) Dois estados quaisquer se distinguem por alguma palavra?
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                boolean distinguem = false;
                for (int k = 0; k < ws.size() && !distinguem; k++) {
                    distinguem = (aceita[i][k] != aceita[j][k]);
                }
                if (!distinguem) {
                    return "estados equivalentes: " + nomes.get(i) + " e " + nomes.get(j);
                }
            }
        }
        return null;
    }
}