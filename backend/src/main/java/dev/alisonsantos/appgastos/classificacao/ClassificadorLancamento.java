package dev.alisonsantos.appgastos.classificacao;

import dev.alisonsantos.appgastos.conta.TipoConta;
import dev.alisonsantos.appgastos.transacao.OrigemClassificacao;
import dev.alisonsantos.appgastos.transacao.TipoLancamento;
import dev.alisonsantos.appgastos.transacao.TipoMovimento;
import dev.alisonsantos.appgastos.transacao.Transacao;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * Decide o que cada lançamento é: compra, estorno, pagamento de fatura,
 * tarifa, transferência entre contas próprias, recebimento ou outro.
 *
 * As regras vêm do que a gente observou nos dados reais da Pluggy.
 * Elas são testadas em ordem, e a primeira que bater decide.
 *
 * IMPORTANTE: a categoria da Pluggy sozinha não é confiável. Por isso
 * cada regra combina vários sinais: a operação, o tipo do movimento
 * (débito/crédito), a descrição e quem está do outro lado.
 */
@Component
public class ClassificadorLancamento {

    // Categorias da Pluggy (vêm em inglês)
    private static final String CATEGORIA_PAGAMENTO_CARTAO = "Credit card payment";
    private static final String CATEGORIA_ID_PAGAMENTO_CARTAO = "05100000";
    private static final String CATEGORIA_TRANSFERENCIA_PROPRIA = "Same person transfer";
    private static final String CATEGORIA_ID_TRANSFERENCIA_PROPRIA = "04000000";

    // Trechos de descrição que indicam pagamento de fatura (já sem acento e em maiúsculas)
    private static final List<String> TEXTOS_PAGAMENTO_FATURA = List.of(
            "PAGAMENTO DE FATURA", "PAGAMENTO RECEBIDO", "PAGTO FATURA", "PAGAMENTO FATURA",
            "PGTO FATURA", "PAG FATURA");

    // Nomes de bancos emissores de cartão, para reconhecer o pagamento da fatura
    // feito pela conta corrente (boleto, débito ou Pix para o banco do cartão).
    private static final List<String> BANCOS_EMISSORES = List.of(
            "SANTANDER", "NU PAGAMENTOS", "NUBANK", "DIGIO");

    // Formas de pagamento usadas para pagar uma fatura pela conta corrente
    private static final List<String> OPERACOES_PAGAMENTO_FATURA = List.of(
            "CARTAO", "BOLETO", "PIX");

    /**
     * Classifica e grava o resultado na transação, a não ser que você
     * já tenha corrigido a classificação à mão.
     *
     * @return true se a classificação mudou
     */
    public boolean aplicar(Transacao transacao, TipoConta tipoConta) {
        if (transacao.isClassificacaoManual()) {
            return false;
        }
        Classificacao resultado = classificar(transacao, tipoConta);
        boolean mudou = transacao.getTipoLancamento() != resultado.tipo()
                || transacao.isContaNosGastos() != resultado.contaNosGastos();
        transacao.definirClassificacao(resultado.tipo(), resultado.contaNosGastos(),
                OrigemClassificacao.AUTOMATICA);
        return mudou;
    }

    /** Só decide, sem alterar nada. */
    public Classificacao classificar(Transacao transacao, TipoConta tipoConta) {
        return tipoConta == TipoConta.CARTAO_CREDITO
                ? classificarCartao(transacao)
                : classificarContaCorrente(transacao);
    }

    // ------------------------------------------------------------------
    // Cartão de crédito
    // ------------------------------------------------------------------

    private Classificacao classificarCartao(Transacao t) {
        String descricao = normalizar(t.getDescricaoBanco());
        String operacao = normalizar(t.getOperacaoPluggy());

        // 1. Pagamento da fatura: crédito que quita a fatura anterior. Não é gasto.
        if ("PAGAMENTO_FATURA".equals(operacao)
                || CATEGORIA_ID_PAGAMENTO_CARTAO.equals(t.getCategoriaPluggyId())
                || CATEGORIA_PAGAMENTO_CARTAO.equalsIgnoreCase(nulo(t.getCategoriaPluggy()))
                || (t.getTipoMovimento() == TipoMovimento.CREDITO && contemAlgum(descricao, TEXTOS_PAGAMENTO_FATURA))) {
            return new Classificacao(TipoLancamento.PAGAMENTO_FATURA, false,
                    "Cartão: pagamento da fatura anterior");
        }

        // 2. Tarifa (anuidade e afins). É um custo, entra nos gastos.
        if ("TARIFA".equals(operacao)
                || nulo(t.getCategoriaPluggy()).toLowerCase(Locale.ROOT).contains("fees")
                || descricao.startsWith("ANUIDADE")) {
            return new Classificacao(TipoLancamento.TARIFA, true, "Cartão: tarifa ou anuidade");
        }

        // 3. Estorno: qualquer outro crédito no cartão. Entra nos gastos,
        //    com valor negativo, abatendo o total (inclusive os centavos de
        //    arredondamento de parcelas).
        if ("ESTORNO".equals(operacao) || t.getTipoMovimento() == TipoMovimento.CREDITO) {
            return new Classificacao(TipoLancamento.ESTORNO, true, "Cartão: estorno ou devolução");
        }

        // 4. O resto é compra.
        return new Classificacao(TipoLancamento.COMPRA, true, "Cartão: compra");
    }

    // ------------------------------------------------------------------
    // Conta corrente
    // ------------------------------------------------------------------

    private Classificacao classificarContaCorrente(Transacao t) {
        String descricao = normalizar(t.getDescricaoBanco());
        String operacao = normalizar(t.getOperacaoPluggy());
        // Quem está do outro lado: o nome da contraparte, do estabelecimento
        // e a própria descrição (ex.: "PIX ENVIADO Banco Digio S A")
        String outroLado = normalizar(t.getContraparteNome()) + " "
                + normalizar(t.getMerchantNome()) + " " + descricao;

        // 1. Transferência entre as suas próprias contas. Não é gasto nem recebimento.
        if (CATEGORIA_ID_TRANSFERENCIA_PROPRIA.equals(t.getCategoriaPluggyId())
                || CATEGORIA_TRANSFERENCIA_PROPRIA.equalsIgnoreCase(nulo(t.getCategoriaPluggy()))) {
            return new Classificacao(TipoLancamento.TRANSFERENCIA_PROPRIA, false,
                    "Conta: transferência entre contas próprias");
        }

        // 2. Pagamento de fatura de cartão feito pela conta corrente
        //    (boleto, débito ou Pix para o banco do cartão).
        //    Não é gasto: os gastos são as compras dentro da fatura.
        if (t.getTipoMovimento() == TipoMovimento.DEBITO
                && (CATEGORIA_ID_PAGAMENTO_CARTAO.equals(t.getCategoriaPluggyId())
                || CATEGORIA_PAGAMENTO_CARTAO.equalsIgnoreCase(nulo(t.getCategoriaPluggy()))
                || descricao.contains("FATURA")
                || (OPERACOES_PAGAMENTO_FATURA.contains(operacao)
                && contemAlgum(outroLado, BANCOS_EMISSORES)))) {
            return new Classificacao(TipoLancamento.PAGAMENTO_FATURA, false,
                    "Conta: pagamento de fatura de cartão");
        }

        // 3. Tarifa bancária.
        if ("TARIFA".equals(operacao)
                || nulo(t.getCategoriaPluggy()).toLowerCase(Locale.ROOT).contains("fees")) {
            return new Classificacao(TipoLancamento.TARIFA, true, "Conta: tarifa bancária");
        }

        // 4. Dinheiro que entrou: Pix, TED, depósito... Vira recebimento,
        //    que é tratado na Fase 3 (quem já pagou o que devia).
        if (t.getTipoMovimento() == TipoMovimento.CREDITO) {
            return new Classificacao(TipoLancamento.RECEBIMENTO, false, "Conta: dinheiro recebido");
        }

        // 5. Outras saídas: Pix enviado, boleto, débito... Ficam como "outro"
        //    para revisão. Contas fixas (Fase 3) e destinos (Fase 2) refinam isso.
        return new Classificacao(TipoLancamento.OUTRO, true, "Conta: outra saída");
    }

    // ------------------------------------------------------------------
    // Apoio
    // ------------------------------------------------------------------

    /** Maiúsculas, sem acentos e sem espaços duplicados, para comparar textos. */
    static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcento.toUpperCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private static boolean contemAlgum(String texto, List<String> trechos) {
        return trechos.stream().anyMatch(texto::contains);
    }

    private static String nulo(String texto) {
        return texto == null ? "" : texto;
    }
}
