package afds;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimizacao de estados de um AFD (algoritmo da Aula 07).
 *
 * Parte 1: copiar o AFD do framework para tabelas simples e validar a entrada.
 * O AFD original nunca e alterado: so usamos seus getters.
 */
public class MinimizadorAFD {

    // Copia de trabalho do AFD, em tabelas simples.
    private final List<String> nomes = new ArrayList<String>();      // nomes.get(i) = nome do estado i
    private final List<Character> simbolos = new ArrayList<Character>(); // simbolos.get(k) = k-esimo simbolo
    private int[][] delta;      // delta[i][k] = indice do destino de i com o simbolo k, ou -1
    private boolean[] ehFinal;  // ehFinal[i] = true se o estado i e final
    private int inicial;        // indice do estado inicial
    private final List<String> removidos = new ArrayList<String>(); // inalcancaveis removidos
    private String estadoMorto;
    /**
     * Carrega e valida o AFD. Lanca IllegalArgumentException se a entrada
     * for invalida (ex.: nao deterministica).
     */
    public MinimizadorAFD(AFD afd) {
        if (afd == null) {
            throw new IllegalArgumentException("AFD nulo");
        }
        carregarEstados(afd);
        carregarSimbolos(afd);
        carregarTransicoes(afd);
        carregarInicialEFinais(afd);
    }

    // Copia os estados, descartando nomes repetidos (o framework permite repeticao).
    private void carregarEstados(AFD afd) {
        for (Object o : afd.getEstados().getElementos()) {
            String nome = ((Estado) o).getNome();
            if (!nomes.contains(nome)) {
                nomes.add(nome);
            }
        }
    }

    // Copia o alfabeto, tambem sem repeticoes.
    private void carregarSimbolos(AFD afd) {
        for (Object o : afd.getSimbolos().getElementos()) {
            Character c = Character.valueOf(((Simbolo) o).getSimbolo());
            if (!simbolos.contains(c)) {
                simbolos.add(c);
            }
        }
    }

    // Monta a tabela delta e confere o determinismo.
    private void carregarTransicoes(AFD afd) {
        int n = nomes.size();
        int m = simbolos.size();
        delta = new int[n][m];
        for (int i = 0; i < n; i++) {
            for (int k = 0; k < m; k++) {
                delta[i][k] = -1; // -1 = transicao indefinida
            }
        }

        for (Object o : afd.getFuncaoPrograma().getElementos()) {
            TransicaoD t = (TransicaoD) o;
            String nomeOrigem = t.getOrigem().getNome();
            String nomeDestino = t.getDestino().getNome();
            char c = t.getSimbolo().getSimbolo();

            int i = nomes.indexOf(nomeOrigem);
            int j = nomes.indexOf(nomeDestino);
            int k = simbolos.indexOf(Character.valueOf(c));

            if (i < 0) {
                throw new IllegalArgumentException("Transicao com origem inexistente: " + nomeOrigem);
            }
            if (j < 0) {
                throw new IllegalArgumentException("Transicao com destino inexistente: " + nomeDestino);
            }
            if (k < 0) {
                throw new IllegalArgumentException("Transicao com simbolo fora do alfabeto: " + c);
            }

            if (delta[i][k] == -1) {
                delta[i][k] = j;
            } else if (delta[i][k] != j) {
                throw new IllegalArgumentException("AFD nao deterministico: de " + nomeOrigem
                        + " com '" + c + "' vai para " + nomes.get(delta[i][k]) + " e para " + nomeDestino);
            }
            // se for a mesma transicao repetida, ignoramos
        }
    }

    private void carregarInicialEFinais(AFD afd) {
        Estado estadoInicial;
        try {
            estadoInicial = afd.getEstadoInicial();
        } catch (NullPointerException e) {
            // new AFD() deixa o estado inicial nulo
            throw new IllegalArgumentException("AFD sem estado inicial definido");
        }
        inicial = nomes.indexOf(estadoInicial.getNome());
        if (inicial < 0) {
            throw new IllegalArgumentException("Estado inicial nao pertence ao conjunto de estados: "
                    + estadoInicial.getNome());
        }

        ehFinal = new boolean[nomes.size()];
        for (Object o : afd.getEstadosFinais().getElementos()) {
            String nome = ((Estado) o).getNome();
            int i = nomes.indexOf(nome);
            if (i < 0) {
                throw new IllegalArgumentException("Estado final nao pertence ao conjunto de estados: " + nome);
            }
            ehFinal[i] = true;
        }
    }

        // ---- Parte 2: pre-requisitos (b) e (c) do professor ----

    /**
     * Deixa o AFD pronto para a minimizacao: remove os estados inalcancaveis
     * a partir do inicial e, se a funcao programa for parcial, completa
     * com um estado nao final (o "d" do slide).
     */
    public void prepararEntrada() {
        removerInalcancaveis();
        completarFuncao();
    }

    /** Nomes dos estados inalcancaveis que foram removidos. */
    public List<String> getRemovidos() {
        return new ArrayList<String>(removidos);
    }

    /** Nome do estado morto criado por completarFuncao(), ou null se nao foi preciso. */
    public String getEstadoMorto() {
        return estadoMorto;
    }

    // Pre-requisito (b): nao pode haver estados inatingiveis a partir do inicial.
    private void removerInalcancaveis() {
        int n = nomes.size();
        int m = simbolos.size();

        // Busca em largura a partir do estado inicial.
        boolean[] alcancavel = new boolean[n];
        int[] fila = new int[n];
        int inicio = 0;
        int fim = 0;
        fila[fim++] = inicial;
        alcancavel[inicial] = true;
        while (inicio < fim) {
            int i = fila[inicio++];
            for (int k = 0; k < m; k++) {
                int j = delta[i][k];
                if (j != -1 && !alcancavel[j]) {
                    alcancavel[j] = true;
                    fila[fim++] = j;
                }
            }
        }

        // Novo indice de cada estado que fica (-1 = sera removido).
        int[] novoIndice = new int[n];
        int restantes = 0;
        for (int i = 0; i < n; i++) {
            if (alcancavel[i]) {
                novoIndice[i] = restantes++;
            } else {
                novoIndice[i] = -1;
                removidos.add(nomes.get(i));
            }
        }
        if (restantes == n) {
            return; // nada a remover
        }

        // Reconstroi as tabelas so com os estados alcancaveis.
        List<String> novosNomes = new ArrayList<String>();
        int[][] novoDelta = new int[restantes][m];
        boolean[] novoFinal = new boolean[restantes];
        for (int i = 0; i < n; i++) {
            if (!alcancavel[i]) {
                continue;
            }
            int ni = novoIndice[i];
            novosNomes.add(nomes.get(i));
            novoFinal[ni] = ehFinal[i];
            for (int k = 0; k < m; k++) {
                int j = delta[i][k];
                novoDelta[ni][k] = (j == -1) ? -1 : novoIndice[j];
            }
        }
        nomes.clear();
        nomes.addAll(novosNomes);
        delta = novoDelta;
        ehFinal = novoFinal;
        inicial = novoIndice[inicial];
    }

    // Pre-requisito (c): a funcao programa deve ser total.
    private void completarFuncao() {
        int n = nomes.size();
        int m = simbolos.size();

        boolean parcial = false;
        for (int i = 0; i < n && !parcial; i++) {
            for (int k = 0; k < m; k++) {
                if (delta[i][k] == -1) {
                    parcial = true;
                    break;
                }
            }
        }
        if (!parcial) {
            return; // ja e total
        }

        // Escolhe um nome livre para o estado novo: d, d1, d2, ...
        String nome = "d";
        int sufixo = 1;
        while (nomes.contains(nome)) {
            nome = "d" + sufixo++;
        }
        int d = n; // indice do estado novo

        int[][] novoDelta = new int[n + 1][m];
        for (int i = 0; i < n; i++) {
            for (int k = 0; k < m; k++) {
                novoDelta[i][k] = (delta[i][k] == -1) ? d : delta[i][k];
            }
        }
        for (int k = 0; k < m; k++) {
            novoDelta[d][k] = d; // o estado novo fica preso nele mesmo
        }
        boolean[] novoFinal = new boolean[n + 1]; // d nao e final
        System.arraycopy(ehFinal, 0, novoFinal, 0, n);

        nomes.add(nome);
        delta = novoDelta;
        ehFinal = novoFinal;
        estadoMorto = nome;
    }
        // ---- Parte 3: tabela de pares (passos a e b do professor) ----

    // Um par de estados {qi,qj}, com i > j: uma celula da tabela do slide.
    private static class Par {
        final int i;
        final int j;
        char marca = '.';                                   // '.' = nao marcado, 'X' = trivial, '+' = propagado
        final List<Par> dependentes = new ArrayList<Par>(); // pares que dependem deste (passo c)

        Par(int i, int j) {
            this.i = i;
            this.j = j;
        }

        boolean marcado() {
            return marca != '.';
        }
    }

    // pares[i][j] so existe para j < i (meia tabela, como no slide).
    private Par[][] pares;

    /** Passos (a) e (b): monta a tabela de pares e marca os pares final x nao-final. */
    public void montarTabela() {
        construirTabelaDePares();
        marcarTriviais();
    }

    // Passo (a): uma celula para cada par de estados distintos.
    private void construirTabelaDePares() {
        int n = nomes.size();
        pares = new Par[n][];
        for (int i = 0; i < n; i++) {
            pares[i] = new Par[i];
            for (int j = 0; j < i; j++) {
                pares[i][j] = new Par(i, j);
            }
        }
    }

    // Passo (b): par (final x nao-final) e trivialmente nao equivalente.
    private void marcarTriviais() {
        for (int i = 0; i < pares.length; i++) {
            for (int j = 0; j < i; j++) {
                if (ehFinal[i] != ehFinal[j]) {
                    pares[i][j].marca = 'X';
                }
            }
        }
    }

    /** Mostra a tabela de pares no formato do slide (. = nao marcado, X = trivial, + = propagado). */
    public String getTabelaDePares() {
        if (pares == null) {
            return "(tabela ainda nao montada)\n";
        }
        int n = pares.length;
        if (n < 2) {
            return "(menos de 2 estados: nao ha pares)\n";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-8s", ""));
        for (int j = 0; j < n - 1; j++) {
            sb.append(String.format("%-8s", nomes.get(j)));
        }
        sb.append('\n');
        for (int i = 1; i < n; i++) {
            sb.append(String.format("%-8s", nomes.get(i)));
            for (int j = 0; j < i; j++) {
                sb.append(String.format("%-8s", pares[i][j].marca));
            }
            sb.append('\n');
        }
        return sb.toString();
    }

        // ---- Parte 4: marcacao dos pares nao equivalentes (passo c do professor) ----

    private boolean verbose; // se true, imprime o passo a passo no estilo do slide

    /** Liga ou desliga a impressao do passo a passo. */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    private void log(String texto) {
        if (verbose) {
            System.out.println(texto);
        }
    }

    // Nome de um par no estilo do slide: (q4,q0).
    private String nomePar(Par p) {
        return "(" + nomes.get(p.i) + "," + nomes.get(p.j) + ")";
    }

    // Devolve o par {a,b} (a != b), em qualquer ordem.
    private Par par(int a, int b) {
        return (a > b) ? pares[a][b] : pares[b][a];
    }

    /**
     * Passo (c): marca os pares nao equivalentes, usando listas de dependencia.
     * Precisa rodar depois de montarTabela().
     */
    public void marcarNaoEquivalentes() {
        int m = simbolos.size();
        // De baixo para cima (a ordem nao muda o resultado final).
        for (int i = pares.length - 1; i >= 1; i--) {
            for (int j = 0; j < i; j++) {
                Par p = pares[i][j];
                if (p.marcado()) {
                    continue; // so analisa pares ainda nao marcados
                }
                for (int k = 0; k < m; k++) {
                    int pu = delta[i][k]; // P(qi,a)
                    int pv = delta[j][k]; // P(qj,a)
                    if (pu == pv) {
                        continue; // mesmo destino: nao marca
                    }
                    Par destino = par(pu, pv);
                    String descricao = nomePar(p) + ": P(" + nomes.get(i) + "," + simbolos.get(k) + ")=" + nomes.get(pu)
                            + " e P(" + nomes.get(j) + "," + simbolos.get(k) + ")=" + nomes.get(pv) + ", ";
                    if (destino.marcado()) {
                        log(descricao + "par destino " + nomePar(destino) + " ja marcado");
                        marcar(p, "");
                        break; // p ja esta marcado: nao precisa olhar os outros simbolos
                    } else {
                        boolean jaExiste = destino.dependentes.contains(p);
                        log(descricao + "par destino " + nomePar(destino) + " nao marcado, "
                                + (jaExiste ? "mesma dependencia" : "cria dependencia " + nomePar(p) + " -> " + nomePar(destino)));
                        if (!jaExiste) {
                            destino.dependentes.add(p);
                        }
                    }
                }
            }
        }
    }

    // Marca o par e, recursivamente, todos os pares da lista que ele encabeca.
    private void marcar(Par p, String motivo) {
        if (p.marcado()) {
            return;
        }
        p.marca = '+';
        log("    " + nomePar(p) + " marcado" + motivo);
        for (Par dependente : p.dependentes) {
            marcar(dependente, " (estava na lista de " + nomePar(p) + ")");
        }
    }

    /** Pares que ficaram sem marca depois do passo (c): sao os estados equivalentes. */
    public List<String> getEquivalentes() {
        List<String> equivalentes = new ArrayList<String>();
        for (int i = 1; i < pares.length; i++) {
            for (int j = 0; j < i; j++) {
                if (!pares[i][j].marcado()) {
                    equivalentes.add(nomePar(pares[i][j]));
                }
            }
        }
        return equivalentes;
    }
        // ---- Parte 5: unificar os estados equivalentes e devolver o AFD (passo d do professor) ----

    // Separador usado para nomear estados unificados (como no slide: q2,q3).
    private static final String SEPARADOR = ",";

    /**
     * Executa o algoritmo e devolve o AFD minimizado (um AFD novo do framework).
     * O AFD original nao e alterado. Depois da chamada, as tabelas internas
     * deste objeto passam a descrever o AFD minimizado.
     */
    public AFD minimizar() {
        prepararEntrada();
        montarTabela();
        marcarNaoEquivalentes();
        if (verbose) {
            log("Tabela de pares apos o passo (c):");
            log(getTabelaDePares());
            log("Pares equivalentes: " + getEquivalentes());
        }
        unificar();
        excluirInuteis();
        return construirAFD();
    }

    // Passo (d): une os estados equivalentes. Depois dele a tabela de pares nao vale mais.
    private void unificar() {
        int n = nomes.size();
        int m = simbolos.size();

        // lider[i] = menor indice de estado equivalente a i (a equivalencia e transitiva).
        int[] lider = new int[n];
        for (int i = 0; i < n; i++) {
            lider[i] = i;
            for (int j = 0; j < i; j++) {
                if (!pares[i][j].marcado()) {
                    lider[i] = lider[j];
                    break;
                }
            }
        }

        // Numera as classes na ordem dos lideres.
        int[] classe = new int[n]; // classe[i] = indice do estado unificado que contem i
        int totalClasses = 0;
        for (int i = 0; i < n; i++) {
            if (lider[i] == i) {
                classe[i] = totalClasses++;
            } else {
                classe[i] = classe[lider[i]];
            }
        }

        // Nome de cada estado unificado: os nomes dos membros, na ordem original.
        List<String> novosNomes = new ArrayList<String>();
        for (int c = 0; c < totalClasses; c++) {
            novosNomes.add("");
        }
        for (int i = 0; i < n; i++) {
            String atual = novosNomes.get(classe[i]);
            novosNomes.set(classe[i], atual.isEmpty() ? nomes.get(i) : atual + SEPARADOR + nomes.get(i));
        }

        // Transicoes e finais: tomamos o lider de cada classe e conferimos os outros membros.
        int[][] novoDelta = new int[totalClasses][m];
        boolean[] novoFinal = new boolean[totalClasses];
        for (int i = 0; i < n; i++) {
            int c = classe[i];
            boolean ehLider = (lider[i] == i);
            if (ehLider) {
                novoFinal[c] = ehFinal[i];
            } else if (novoFinal[c] != ehFinal[i]) {
                throw new IllegalStateException("Estados equivalentes com finalidade diferente: " + nomes.get(i));
            }
            for (int k = 0; k < m; k++) {
                int destino = classe[delta[i][k]];
                if (ehLider) {
                    novoDelta[c][k] = destino;
                } else if (novoDelta[c][k] != destino) {
                    throw new IllegalStateException("Estados equivalentes com transicoes diferentes: " + nomes.get(i));
                }
            }
        }
        log("Estados apos a unificacao: " + novosNomes);

        // O AFD de trabalho passa a ser o unificado.
        int novoInicial = classe[inicial];
        nomes.clear();
        nomes.addAll(novosNomes);
        delta = novoDelta;
        ehFinal = novoFinal;
        inicial = novoInicial;
        pares = null;
    }

    // Monta um AFD do framework a partir das tabelas de trabalho.
    private AFD construirAFD() {
        ConjuntoSimbolo conjuntoSimbolos = new ConjuntoSimbolo();
        for (Character c : simbolos) {
            conjuntoSimbolos.inclui(new Simbolo(c.charValue()));
        }

        ConjuntoEstados conjuntoEstados = new ConjuntoEstados();
        ConjuntoEstados conjuntoFinais = new ConjuntoEstados();
        for (int i = 0; i < nomes.size(); i++) {
            Estado e = new Estado(nomes.get(i));
            conjuntoEstados.inclui(e);
            if (ehFinal[i]) {
                conjuntoFinais.inclui(e);
            }
        }

        ConjuntoTransicaoD funcaoPrograma = new ConjuntoTransicaoD();
        for (int i = 0; i < nomes.size(); i++) {
            for (int k = 0; k < simbolos.size(); k++) {
                if (delta[i][k] != -1) {
                    TransicaoD t = new TransicaoD();
                    t.setOrigem(new Estado(nomes.get(i)));
                    t.setSimbolo(new Simbolo(simbolos.get(k).charValue()));
                    t.setDestino(new Estado(nomes.get(delta[i][k])));
                    funcaoPrograma.inclui(t);
                }
            }
        }

        return new AFD(conjuntoSimbolos, conjuntoEstados, funcaoPrograma,
                new Estado(nomes.get(inicial)), conjuntoFinais);
    }
        // ---- Parte 6: excluir os estados inuteis (passo e do professor) ----

    private final List<String> inuteisRemovidos = new ArrayList<String>(); // inuteis removidos no passo (e)

    /** Nomes dos estados inuteis removidos no passo (e). */
    public List<String> getInuteisRemovidos() {
        return new ArrayList<String>(inuteisRemovidos);
    }

    // Passo (e): estado inutil e aquele do qual nao se alcanca nenhum estado final.
    private void excluirInuteis() {
        int n = nomes.size();
        int m = simbolos.size();

        // util[i] = true se de i se alcanca algum estado final.
        // Comeca com os finais e repete ate nada mais mudar.
        boolean[] util = new boolean[n];
        for (int i = 0; i < n; i++) {
            util[i] = ehFinal[i];
        }
        boolean mudou = true;
        while (mudou) {
            mudou = false;
            for (int i = 0; i < n; i++) {
                if (util[i]) {
                    continue;
                }
                for (int k = 0; k < m; k++) {
                    int j = delta[i][k];
                    if (j != -1 && util[j]) {
                        util[i] = true;
                        mudou = true;
                        break;
                    }
                }
            }
        }

        // O estado inicial sempre fica (se ele for inutil, a linguagem e vazia).
        boolean[] fica = util.clone();
        fica[inicial] = true;

        int[] novoIndice = new int[n];
        int restantes = 0;
        for (int i = 0; i < n; i++) {
            if (fica[i]) {
                novoIndice[i] = restantes++;
            } else {
                novoIndice[i] = -1;
                inuteisRemovidos.add(nomes.get(i));
            }
        }
        log("Estados inuteis removidos: " + inuteisRemovidos);
        if (restantes == n) {
            return; // nada a remover
        }

        // Reconstroi as tabelas sem os inuteis; transicoes para eles ficam indefinidas (-1).
        List<String> novosNomes = new ArrayList<String>();
        int[][] novoDelta = new int[restantes][m];
        boolean[] novoFinal = new boolean[restantes];
        for (int i = 0; i < n; i++) {
            if (!fica[i]) {
                continue;
            }
            int ni = novoIndice[i];
            novosNomes.add(nomes.get(i));
            novoFinal[ni] = ehFinal[i];
            for (int k = 0; k < m; k++) {
                int j = delta[i][k];
                novoDelta[ni][k] = (j == -1) ? -1 : novoIndice[j];
            }
        }
        int novoInicial = novoIndice[inicial];
        nomes.clear();
        nomes.addAll(novosNomes);
        delta = novoDelta;
        ehFinal = novoFinal;
        inicial = novoInicial;
    }
    /** Mostra as tabelas carregadas (-> = inicial, * = final, - = indefinida). */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Estados:  ").append(nomes).append('\n');
        sb.append("Alfabeto: ").append(simbolos).append('\n');
        sb.append("Inicial:  ").append(nomes.get(inicial)).append('\n');
        sb.append("Finais:   ");
        for (int i = 0; i < nomes.size(); i++) {
            if (ehFinal[i]) {
                sb.append(nomes.get(i)).append(' ');
            }
        }
        sb.append("\nTabela de transicoes:\n");
        sb.append(String.format("%-10s", ""));
        for (Character c : simbolos) {
            sb.append(String.format("%-8s", c));
        }
        sb.append('\n');
        for (int i = 0; i < nomes.size(); i++) {
            String marca = (i == inicial ? "->" : "") + (ehFinal[i] ? "*" : "");
            sb.append(String.format("%-10s", marca + nomes.get(i)));
            for (int k = 0; k < simbolos.size(); k++) {
                String destino = (delta[i][k] == -1) ? "-" : nomes.get(delta[i][k]);
                sb.append(String.format("%-8s", destino));
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}