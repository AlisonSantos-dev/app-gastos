package dev.alisonsantos.appgastos.sync;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Placar de uma sincronização: o que foi criado, o que já existia,
 * o que foi ignorado e os erros. Vira o JSON de resposta do POST /api/sync.
 */
public class ResumoSincronizacao {

    private final Instant inicio = Instant.now();
    private Instant fim;

    private int contasNovas;
    private int contasExistentes;
    private int cartoesNovos;
    private int faturasNovas;
    private int faturasAtualizadas;
    private int transacoesNovas;
    private int transacoesExistentes;
    private int transacoesIgnoradas;
    private int pendentesRemovidas;
    private final List<String> erros = new ArrayList<>();

    // ------------------------------------------------------------------
    // Contadores
    // ------------------------------------------------------------------

    public void contaNova() {
        contasNovas++;
    }

    public void contaExistente() {
        contasExistentes++;
    }

    public void cartaoNovo() {
        cartoesNovos++;
    }

    public void faturaNova() {
        faturasNovas++;
    }

    public void faturaAtualizada() {
        faturasAtualizadas++;
    }

    public void transacaoNova() {
        transacoesNovas++;
    }

    public void transacaoExistente() {
        transacoesExistentes++;
    }

    public void transacaoIgnorada() {
        transacoesIgnoradas++;
    }

    public void pendentesRemovidas(int quantidade) {
        pendentesRemovidas += quantidade;
    }

    public void adicionarErro(String erro) {
        erros.add(erro);
    }

    public void finalizar() {
        this.fim = Instant.now();
    }

    // ------------------------------------------------------------------
    // Getters (o Jackson usa para montar o JSON)
    // ------------------------------------------------------------------

    public Instant getInicio() {
        return inicio;
    }

    public Instant getFim() {
        return fim;
    }

    public long getDuracaoSegundos() {
        return fim == null ? 0 : Duration.between(inicio, fim).toSeconds();
    }

    public int getContasNovas() {
        return contasNovas;
    }

    public int getContasExistentes() {
        return contasExistentes;
    }

    public int getCartoesNovos() {
        return cartoesNovos;
    }

    public int getFaturasNovas() {
        return faturasNovas;
    }

    public int getFaturasAtualizadas() {
        return faturasAtualizadas;
    }

    public int getTransacoesNovas() {
        return transacoesNovas;
    }

    public int getTransacoesExistentes() {
        return transacoesExistentes;
    }

    public int getTransacoesIgnoradas() {
        return transacoesIgnoradas;
    }

    public int getPendentesRemovidas() {
        return pendentesRemovidas;
    }

    public List<String> getErros() {
        return erros;
    }

    @Override
    public String toString() {
        return "contas novas=" + contasNovas
                + ", cartões novos=" + cartoesNovos
                + ", faturas novas=" + faturasNovas
                + ", transações novas=" + transacoesNovas
                + ", existentes=" + transacoesExistentes
                + ", ignoradas=" + transacoesIgnoradas
                + ", pendentes removidas=" + pendentesRemovidas
                + ", erros=" + erros.size();
    }
}
