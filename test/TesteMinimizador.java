import afds.*;

public class TesteMinimizador {

    public static void main(String[] args) throws Exception {
        // 1) AFD do exemplo da Aula 07
        AFD aula07 = new AFD();
        aula07.ler("./test/minimizacao/aula07.XML");
        System.out.println("=== Exemplo da Aula 07 ===");
        System.out.println(new MinimizadorAFD(aula07));

        // 2) AFD do professor (funcao programa parcial)
        AFD parcial = new AFD();
        parcial.ler("./test/AFD.XML");
        System.out.println("=== AFD.XML do professor (funcao parcial) ===");
        System.out.println(new MinimizadorAFD(parcial));

        // 3) Entradas invalidas: cada uma deve gerar um erro claro
        System.out.println("=== Entradas invalidas ===");

        AFD semInicial = new AFD();
        testar("AFD vazio", semInicial);

        AFD a = aula07.clonar();
        a.setEstadoInicial(new Estado("zz"));
        testar("Inicial inexistente", a);

        AFD b = aula07.clonar();
        TransicaoD t = new TransicaoD();
        t.setOrigem(new Estado("q0"));
        t.setSimbolo(new Simbolo('a'));
        t.setDestino(new Estado("q3"));
        ConjuntoTransicaoD f = b.getFuncaoPrograma();
        f.inclui(t);
        b.setFuncaoPrograma(f);
        testar("Nao deterministico", b);

        AFD c = aula07.clonar();
        TransicaoD t2 = new TransicaoD();
        t2.setOrigem(new Estado("q0"));
        t2.setSimbolo(new Simbolo('c'));
        t2.setDestino(new Estado("q1"));
        ConjuntoTransicaoD f2 = c.getFuncaoPrograma();
        f2.inclui(t2);
        c.setFuncaoPrograma(f2);
        testar("Simbolo fora do alfabeto", c);

        AFD d = aula07.clonar();
        ConjuntoEstados finais = d.getEstadosFinais();
        finais.inclui(new Estado("q9"));
        d.setEstadosFinais(finais);
        testar("Final inexistente", d);

        // 4) Parte 2: preparar a entrada (inalcancaveis e estado d)
        preparar("Aula 07 (ja pronto: nada muda)", "./test/minimizacao/aula07.XML");
        preparar("AFD.XML do professor (parcial: cria d)", "./test/AFD.XML");
        preparar("Com inalcancaveis e parcial", "./test/minimizacao/inalcancaveis.XML");
        preparar("Ja existe um estado chamado d", "./test/minimizacao/colisao_d.XML");

        // 5) Parte 3: tabela de pares (passos a e b)
        tabela("Aula 07", "./test/minimizacao/aula07.XML");
        tabela("Com inalcancaveis e parcial", "./test/minimizacao/inalcancaveis.XML");

        // 6) Parte 4: passo (c), marcacao com listas de dependencia
        marcar("Aula 07 (passo a passo)", "./test/minimizacao/aula07.XML", true);
        marcar("AFD.XML do professor", "./test/AFD.XML", false);

        // 7) Parte 5: minimizacao completa (ainda sem excluir inuteis)
        minimizarEMostrar("Aula 07 (passo a passo)", "./test/minimizacao/aula07.XML", "saida_aula07_min", true);
        minimizarEMostrar("AFD.XML do professor", "./test/AFD.XML", "saida_afd_min", false);

        minimizarEMostrar("Com estados inuteis (q2,q3 sao mortos)", "./test/minimizacao/inuteis.XML", "saida_inuteis_min", true);
        minimizarEMostrar("Linguagem vazia", "./test/minimizacao/vazia.XML", "saida_vazia_min", true);
    }

    private static void tabela(String titulo, String caminho) throws Exception {
        AFD afd = new AFD();
        afd.ler(caminho);
        MinimizadorAFD m = new MinimizadorAFD(afd);
        m.prepararEntrada();
        m.montarTabela();
        System.out.println("=== Tabela de pares: " + titulo + " ===");
        System.out.println(m);
        System.out.println("(. = nao marcado, X = final x nao-final)");
        System.out.println(m.getTabelaDePares());
    }

    private static void marcar(String titulo, String caminho, boolean passoAPasso) throws Exception {
        AFD afd = new AFD();
        afd.ler(caminho);
        MinimizadorAFD m = new MinimizadorAFD(afd);
        m.prepararEntrada();
        m.montarTabela();
        System.out.println("=== Passo (c): " + titulo + " ===");
        m.setVerbose(passoAPasso);
        m.marcarNaoEquivalentes();
        System.out.println("(. = nao marcado, X = final x nao-final, + = marcado no passo c)");
        System.out.println(m.getTabelaDePares());
        System.out.println("Pares equivalentes: " + m.getEquivalentes());
        System.out.println();
    }

    private static void minimizarEMostrar(String titulo, String caminho, String saida, boolean passoAPasso) throws Exception {
        AFD original = new AFD();
        original.ler(caminho);
        MinimizadorAFD m = new MinimizadorAFD(original);
        m.setVerbose(passoAPasso);
        AFD minimo = m.minimizar();

        System.out.println("=== Minimizacao: " + titulo + " ===");
        System.out.println("Estados: " + original.getEstados().size() + " -> " + minimo.getEstados().size());
        System.out.println("Estados do AFD minimizado: " + nomesDosEstados(minimo));
        System.out.println("Tabela do AFD minimizado:");
        System.out.println(m);

        // Conferencia rapida: o original e o minimizado aceitam as mesmas palavras?
        String[] palavras = {"", "a", "b", "ab", "ba", "aa", "bb", "abb", "baab", "aabb", "bbaa"};
        for (String w : palavras) {
            System.out.println("  \"" + w + "\": original=" + original.Aceita(w) + " minimizado=" + minimo.Aceita(w));
        }

        // Gravar em XML e ler de volta (os nomes com virgula sobrevivem?)
        minimo.toXML(saida);
        AFD relido = new AFD();
        relido.ler("./" + saida + ".xml");
        System.out.println("Relido de " + saida + ".xml: " + nomesDosEstados(relido));
        System.out.println();
    }

    // Mostra cada nome entre colchetes, para nomes como q2,q3 nao se confundirem.
    private static String nomesDosEstados(AFD afd) {
        StringBuilder sb = new StringBuilder();
        for (Object o : afd.getEstados().getElementos()) {
            sb.append('[').append(((Estado) o).getNome()).append("] ");
        }
        return sb.toString();
    }

    private static void preparar(String titulo, String caminho) throws Exception {
        AFD afd = new AFD();
        afd.ler(caminho);
        MinimizadorAFD m = new MinimizadorAFD(afd);
        System.out.println("=== " + titulo + " ===");
        System.out.println("--- antes ---");
        System.out.println(m);
        m.prepararEntrada();
        System.out.println("--- depois ---");
        System.out.println("Removidos (inalcancaveis): " + m.getRemovidos());
        System.out.println("Estado criado: " + (m.getEstadoMorto() == null ? "nenhum" : m.getEstadoMorto()));
        System.out.println(m);
    }

    private static void testar(String descricao, AFD afd) {
        try {
            new MinimizadorAFD(afd);
            System.out.println(descricao + ": ACEITOU (inesperado)");
        } catch (IllegalArgumentException e) {
            System.out.println(descricao + ": erro -> " + e.getMessage());
        }
    }
}