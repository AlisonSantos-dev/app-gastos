package dev.alisonsantos.appgastos.conta;

import dev.alisonsantos.appgastos.comum.RecursoNaoEncontradoException;
import dev.alisonsantos.appgastos.comum.RegraNegocioException;
import dev.alisonsantos.appgastos.destino.DestinoService;
import dev.alisonsantos.appgastos.pessoa.Pessoa;
import dev.alisonsantos.appgastos.pessoa.PessoaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Edição das contas: nome, banco, dono padrão e se está ativa.
 *
 * Quando o dono padrão muda, os lançamentos sem cartão da conta
 * (ex.: débitos) são redistribuídos na hora.
 */
@Service
public class ContaService {

    private final ContaRepository contaRepository;
    private final PessoaService pessoaService;
    private final DestinoService destinoService;

    public ContaService(ContaRepository contaRepository, PessoaService pessoaService,
                        DestinoService destinoService) {
        this.contaRepository = contaRepository;
        this.pessoaService = pessoaService;
        this.destinoService = destinoService;
    }

    @Transactional
    public void atualizar(Long id, ContaRequest pedido) {
        Conta conta = contaRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Conta", id));

        conta.setBanco(pedido.banco().trim());
        conta.setNome(pedido.nome().trim());
        if (pedido.ativa() != null) {
            conta.setAtiva(pedido.ativa());
        }

        Pessoa dono = null;
        if (pedido.donoPadraoId() != null) {
            dono = pessoaService.encontrar(pedido.donoPadraoId());
            if (!dono.isAtivo()) {
                throw new RegraNegocioException("A pessoa \"" + dono.getNome() + "\" está desativada.");
            }
        }
        conta.setDonoPadrao(dono);

        destinoService.reaplicarConta(id);
    }
}
