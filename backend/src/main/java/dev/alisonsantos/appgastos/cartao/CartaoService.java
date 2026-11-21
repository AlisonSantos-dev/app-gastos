package dev.alisonsantos.appgastos.cartao;

import dev.alisonsantos.appgastos.comum.RecursoNaoEncontradoException;
import dev.alisonsantos.appgastos.comum.RegraNegocioException;
import dev.alisonsantos.appgastos.destino.DestinoService;
import dev.alisonsantos.appgastos.pessoa.Pessoa;
import dev.alisonsantos.appgastos.pessoa.PessoaService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Configuração dos cartões: apelido, tipo, cor e, principalmente, de quem é.
 *
 * Quando a configuração muda, as compras do cartão são redistribuídas
 * na hora: ao dizer que o 6741 é da Gabriella, todas as compras dele
 * (inclusive as antigas) passam a ser dela, menos as que você já
 * tiver mudado à mão ou que uma regra tiver decidido.
 */
@Service
public class CartaoService {

    private static final int MINIMO_CANDIDATOS = 2;

    private final CartaoRepository cartaoRepository;
    private final PessoaService pessoaService;
    private final DestinoService destinoService;

    public CartaoService(CartaoRepository cartaoRepository, PessoaService pessoaService,
                         DestinoService destinoService) {
        this.cartaoRepository = cartaoRepository;
        this.pessoaService = pessoaService;
        this.destinoService = destinoService;
    }

    @Transactional(readOnly = true)
    public List<CartaoResponse> listar() {
        return cartaoRepository.findAll(Sort.by("conta.nome", "ultimosDigitos")).stream()
                .map(CartaoResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public CartaoResponse buscar(Long id) {
        return CartaoResponse.de(encontrar(id));
    }

    @Transactional
    public CartaoResponse configurar(Long id, CartaoRequest pedido) {
        Cartao cartao = encontrar(id);

        cartao.setApelido(textoOuNulo(pedido.apelido()));
        cartao.setTipo(pedido.tipo());
        cartao.setCor(pedido.cor() == null ? null : pedido.cor().toUpperCase(Locale.ROOT));
        cartao.setModoDono(pedido.modoDono());

        Pessoa dono = pedido.donoPadraoId() == null ? null : pessoaAtiva(pedido.donoPadraoId());

        if (pedido.modoDono() == ModoDono.FIXO) {
            // FIXO: precisa de um dono, e não tem candidatos
            if (dono == null) {
                throw new RegraNegocioException("No modo FIXO, informe o dono do cartão (donoPadraoId).");
            }
            cartao.definirCandidatos(Set.of());
        } else {
            // PERGUNTAR: precisa de pelo menos 2 candidatos; o dono, se vier, é a sugestão
            Set<Pessoa> candidatos = new LinkedHashSet<>();
            for (Long pessoaId : pedido.candidatosIds() == null ? List.<Long>of() : pedido.candidatosIds()) {
                candidatos.add(pessoaAtiva(pessoaId));
            }
            if (candidatos.size() < MINIMO_CANDIDATOS) {
                throw new RegraNegocioException(
                        "No modo PERGUNTAR, informe pelo menos " + MINIMO_CANDIDATOS + " pessoas em candidatosIds.");
            }
            if (dono != null && !candidatos.contains(dono)) {
                throw new RegraNegocioException(
                        "A sugestão (donoPadraoId) precisa ser uma das pessoas em candidatosIds.");
            }
            cartao.definirCandidatos(candidatos);
        }

        cartao.setDonoPadrao(dono);
        cartao.setStatus(StatusCartao.ATIVO);

        destinoService.reaplicarCartao(id);
        return CartaoResponse.de(cartao);
    }

    private Cartao encontrar(Long id) {
        return cartaoRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Cartão", id));
    }

    private Pessoa pessoaAtiva(Long id) {
        Pessoa pessoa = pessoaService.encontrar(id);
        if (!pessoa.isAtivo()) {
            throw new RegraNegocioException("A pessoa \"" + pessoa.getNome() + "\" está desativada.");
        }
        return pessoa;
    }

    private String textoOuNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
