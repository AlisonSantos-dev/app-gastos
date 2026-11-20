package dev.alisonsantos.appgastos.sync;

import dev.alisonsantos.appgastos.conta.TipoConta;
import dev.alisonsantos.appgastos.pluggy.dto.PluggyBill;
import dev.alisonsantos.appgastos.pluggy.dto.PluggyCreditCardMetadata;
import dev.alisonsantos.appgastos.pluggy.dto.PluggyFinanceCharge;
import dev.alisonsantos.appgastos.pluggy.dto.PluggyMerchant;
import dev.alisonsantos.appgastos.pluggy.dto.PluggyParticipante;
import dev.alisonsantos.appgastos.pluggy.dto.PluggyTransaction;
import dev.alisonsantos.appgastos.transacao.StatusTransacao;
import dev.alisonsantos.appgastos.transacao.TipoMovimento;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Converte os dados da Pluggy (em inglês, do jeito que a API manda)
 * para o formato do app.
 *
 * São só funções puras: recebem um valor e devolvem outro, sem acessar
 * banco nem API. Por isso são estáticas, e fáceis de testar depois.
 */
public final class PluggyConversor {

    /** Projeções duplicadas que o banco manda com essa data. Devem ser ignoradas. */
    private static final String PREVISAO_ESPURIA = "0001-01";

    private PluggyConversor() {
        // Classe só de funções estáticas: não deve ser instanciada
    }

    public static TipoConta tipoConta(String tipoPluggy) {
        return "CREDIT".equals(tipoPluggy) ? TipoConta.CARTAO_CREDITO : TipoConta.CORRENTE;
    }

    public static TipoMovimento tipoMovimento(String tipoPluggy) {
        return "CREDIT".equals(tipoPluggy) ? TipoMovimento.CREDITO : TipoMovimento.DEBITO;
    }

    public static StatusTransacao status(String statusPluggy) {
        return "PENDING".equals(statusPluggy) ? StatusTransacao.PENDENTE : StatusTransacao.CONFIRMADA;
    }

    /**
     * Limpa um texto: tira os espaços das pontas (o nome do cartão do
     * Santander vem cheio de espaços no final) e corta no tamanho da coluna.
     * Texto vazio vira null.
     */
    public static String texto(String valor, int tamanhoMaximo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String limpo = valor.trim();
        return limpo.length() > tamanhoMaximo ? limpo.substring(0, tamanhoMaximo) : limpo;
    }

    /** CNPJ só com números e exatamente 14 dígitos. Qualquer outra coisa vira null. */
    public static String cnpj(String valor) {
        if (valor == null) {
            return null;
        }
        String digitos = valor.replaceAll("\\D", "");
        return digitos.length() == 14 ? digitos : null;
    }

    /** Converte um instante (ex.: 2026-10-30T00:00:00Z) para a data, em UTC. */
    public static LocalDate data(Instant instante) {
        return instante == null ? null : LocalDate.ofInstant(instante, ZoneOffset.UTC);
    }

    /** Lançamentos com billForecastDate "0001-01" são projeções duplicadas do banco. */
    public static boolean isProjecaoEspuria(PluggyTransaction transacao) {
        PluggyCreditCardMetadata cartao = transacao.creditCardMetadata();
        return cartao != null && PREVISAO_ESPURIA.equals(cartao.billForecastDate());
    }

    /** Os 4 últimos dígitos do cartão usado, só se forem exatamente 4 números. */
    public static String ultimosDigitosCartao(PluggyCreditCardMetadata cartao) {
        if (cartao == null || cartao.cardNumber() == null) {
            return null;
        }
        String numero = cartao.cardNumber().trim();
        return numero.matches("\\d{4}") ? numero : null;
    }

    /** Nome do estabelecimento: a razão social, ou o nome, se ela vier vazia. */
    public static String nomeEstabelecimento(PluggyMerchant merchant) {
        if (merchant == null) {
            return null;
        }
        String razaoSocial = texto(merchant.businessName(), 150);
        return razaoSocial != null ? razaoSocial : texto(merchant.name(), 150);
    }

    /**
     * Nome da outra pessoa num Pix/TED: se o dinheiro saiu (débito), é quem
     * recebeu; se entrou (crédito), é quem pagou. Só o nome, nunca o documento.
     */
    public static String contraparte(PluggyTransaction transacao) {
        if (transacao.paymentData() == null) {
            return null;
        }
        PluggyParticipante outraPessoa = "CREDIT".equals(transacao.type())
                ? transacao.paymentData().payer()
                : transacao.paymentData().receiver();
        return outraPessoa == null ? null : texto(outraPessoa.name(), 150);
    }

    /** Descrição original do banco. Nunca fica vazia, porque a coluna é obrigatória. */
    public static String descricaoBanco(PluggyTransaction transacao) {
        String descricao = texto(transacao.description(), 255);
        if (descricao == null) {
            descricao = texto(transacao.descriptionRaw(), 255);
        }
        return descricao != null ? descricao : "(sem descrição)";
    }

    /** Soma dos encargos da fatura (juros, IOF, multa...). */
    public static BigDecimal encargos(PluggyBill fatura) {
        if (fatura.financeCharges() == null) {
            return BigDecimal.ZERO;
        }
        return fatura.financeCharges().stream()
                .map(PluggyFinanceCharge::amount)
                .filter(valor -> valor != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
