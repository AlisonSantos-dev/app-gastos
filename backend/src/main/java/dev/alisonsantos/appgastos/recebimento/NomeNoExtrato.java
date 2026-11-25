package dev.alisonsantos.appgastos.recebimento;

import dev.alisonsantos.appgastos.comum.Texto;

/**
 * Compara o nome de quem mandou o Pix (como veio no extrato) com o
 * "nome no extrato" cadastrado na pessoa.
 *
 * Os bancos costumam CORTAR nomes longos ("MARLENE APARECIDA DOS SAN"),
 * então vale se um contiver o outro, desde que o trecho em comum seja
 * grande o bastante para não confundir pessoas ("ANA" bateria com muita gente).
 *
 * PRIVACIDADE: só o nome é usado. O CPF de quem mandou nunca é guardado.
 */
public final class NomeNoExtrato {

    static final int TAMANHO_MINIMO = 8;

    private NomeNoExtrato() {
    }

    public static boolean bate(String nomeDoExtrato, String nomeCadastrado) {
        String extrato = Texto.normalizar(nomeDoExtrato);
        String cadastrado = Texto.normalizar(nomeCadastrado);
        if (extrato.length() < TAMANHO_MINIMO || cadastrado.length() < TAMANHO_MINIMO) {
            return extrato.equals(cadastrado) && !extrato.isEmpty();
        }
        return extrato.contains(cadastrado) || cadastrado.contains(extrato);
    }
}
