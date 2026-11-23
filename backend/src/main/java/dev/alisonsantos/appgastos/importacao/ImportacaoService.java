package dev.alisonsantos.appgastos.importacao;

import dev.alisonsantos.appgastos.comum.RecursoNaoEncontradoException;
import dev.alisonsantos.appgastos.comum.RegraNegocioException;
import dev.alisonsantos.appgastos.comum.Texto;
import dev.alisonsantos.appgastos.legenda.Regra;
import dev.alisonsantos.appgastos.legenda.RegraRepository;
import dev.alisonsantos.appgastos.legenda.RegraRequest;
import dev.alisonsantos.appgastos.legenda.RegraResponse;
import dev.alisonsantos.appgastos.legenda.RegraService;
import dev.alisonsantos.appgastos.legenda.TipoChave;
import dev.alisonsantos.appgastos.pessoa.Pessoa;
import dev.alisonsantos.appgastos.pessoa.PessoaService;
import dev.alisonsantos.appgastos.transacao.TransacaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Importa a planilha de gastos e transforma o histórico em regras.
 *
 * PRIVACIDADE: a planilha é lida em memória e descartada. Nada do arquivo
 * é guardado: só as sugestões calculadas a partir dele.
 *
 * O dono só é sugerido quando a planilha é bem clara: a mesma pessoa (que
 * não seja você) em pelo menos 90% das compras. Nos seus gastos, o cartão
 * já resolve o dono, então a regra não precisa.
 */
@Service
public class ImportacaoService {

    private static final Logger log = LoggerFactory.getLogger(ImportacaoService.class);
    private static final double CONFIANCA_PARA_SUGERIR_DONO = 0.9;

    private final LeitorPlanilha leitor;
    private final GeradorSugestoes gerador;
    private final SugestaoRegraRepository sugestaoRepository;
    private final TransacaoRepository transacaoRepository;
    private final RegraRepository regraRepository;
    private final RegraService regraService;
    private final PessoaService pessoaService;

    public ImportacaoService(LeitorPlanilha leitor, GeradorSugestoes gerador,
                             SugestaoRegraRepository sugestaoRepository,
                             TransacaoRepository transacaoRepository, RegraRepository regraRepository,
                             RegraService regraService, PessoaService pessoaService) {
        this.leitor = leitor;
        this.gerador = gerador;
        this.sugestaoRepository = sugestaoRepository;
        this.transacaoRepository = transacaoRepository;
        this.regraRepository = regraRepository;
        this.regraService = regraService;
        this.pessoaService = pessoaService;
    }

    // ------------------------------------------------------------------
    // Importação
    // ------------------------------------------------------------------

    @Transactional
    public ResumoImportacao importar(MultipartFile arquivo, Map<SecaoPlanilha, Long> pessoasPorSecao) {
        validarArquivo(arquivo);
        Map<SecaoPlanilha, Pessoa> pessoas = carregarPessoas(pessoasPorSecao);

        List<LinhaPlanilha> linhas;
        try (InputStream conteudo = arquivo.getInputStream()) {
            linhas = leitor.ler(conteudo);
        } catch (IOException | RuntimeException e) {
            log.warn("Importação: falha ao ler a planilha", e);
            throw new RegraNegocioException("Não foi possível ler a planilha. Confira se é um .xlsx válido.");
        }

        // As descrições das transações que ainda não têm regra (para medir o impacto)
        List<String> descricoesSemRegra = transacaoRepository.findAll().stream()
                .filter(t -> t.isContaNosGastos() && t.getRegra() == null)
                .map(t -> Texto.normalizar(t.getDescricaoBanco()))
                .toList();

        // As chaves que já viraram regra (para não sugerir de novo)
        Set<String> chavesComRegra = regraRepository.findByAtivaTrue().stream()
                .map(Regra::getValorChave)
                .collect(Collectors.toSet());

        // Uma importação nova substitui as sugestões pendentes da anterior
        sugestaoRepository.apagarPorStatus(StatusSugestao.PENDENTE);

        int jaCobertas = 0;
        List<SugestaoRegra> novas = new ArrayList<>();
        for (SugestaoCalculada calculada : gerador.gerar(linhas, descricoesSemRegra)) {
            if (chavesComRegra.contains(calculada.valorChave())) {
                jaCobertas++;
                continue;
            }
            novas.add(new SugestaoRegra(calculada, donoSugerido(calculada, pessoas)));
        }
        sugestaoRepository.saveAll(novas);

        long comPessoa = novas.stream().filter(s -> s.getPessoa() != null).count();
        log.info("Importação: {} compras lidas, {} sugestões ({} com dono), {} já cobertas por regra",
                linhas.size(), novas.size(), comPessoa, jaCobertas);
        return new ResumoImportacao(linhas.size(), novas.size(), jaCobertas, comPessoa);
    }

    // ------------------------------------------------------------------
    // Sugestões
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<SugestaoResponse> listar(StatusSugestao status) {
        return sugestaoRepository.findByStatusOrderByTransacoesAfetadasDescComprasDesc(status).stream()
                .map(SugestaoResponse::de)
                .toList();
    }

    /** Aprova uma sugestão (com ajustes opcionais) e cria a regra. */
    @Transactional
    public SugestaoResponse aprovar(Long id, AprovacaoRequest ajustes) {
        SugestaoRegra sugestao = pendente(id);
        AprovacaoRequest pedido = ajustes == null ? new AprovacaoRequest(null, null, null, null) : ajustes;

        String descricao = pedido.descricao() != null && !pedido.descricao().isBlank()
                ? pedido.descricao().trim() : sugestao.getDescricao();
        Long pessoaId = Boolean.TRUE.equals(pedido.semPessoa()) ? null
                : pedido.pessoaId() != null ? pedido.pessoaId()
                : sugestao.getPessoa() == null ? null : sugestao.getPessoa().getId();

        RegraResponse regra = regraService.criar(paraRegra(sugestao, descricao, pedido.categoria(), pessoaId));
        sugestao.aprovar(regraRepository.getReferenceById(regra.id()));
        return SugestaoResponse.de(sugestao);
    }

    /** Aprova várias de uma vez, como foram sugeridas, reaplicando a legenda uma vez só. */
    @Transactional
    public List<SugestaoResponse> aprovarVarias(List<Long> ids) {
        List<SugestaoRegra> sugestoes = ids.stream().distinct().map(this::pendente).toList();

        List<RegraRequest> pedidos = sugestoes.stream()
                .map(s -> paraRegra(s, s.getDescricao(), null, s.getPessoa() == null ? null : s.getPessoa().getId()))
                .toList();
        List<RegraResponse> regras = regraService.criarVarias(pedidos);

        for (int i = 0; i < sugestoes.size(); i++) {
            sugestoes.get(i).aprovar(regraRepository.getReferenceById(regras.get(i).id()));
        }
        return sugestoes.stream().map(SugestaoResponse::de).toList();
    }

    @Transactional
    public void descartar(Long id) {
        pendente(id).descartar();
    }

    // ------------------------------------------------------------------
    // Apoio
    // ------------------------------------------------------------------

    private RegraRequest paraRegra(SugestaoRegra s, String descricao, String categoria, Long pessoaId) {
        return new RegraRequest(TipoChave.DESCRICAO_CONTEM, s.getValorChave(), null,
                descricao, categoria, pessoaId, null, null, null, null);
    }

    private Pessoa donoSugerido(SugestaoCalculada calculada, Map<SecaoPlanilha, Pessoa> pessoas) {
        boolean secaoDeOutraPessoa = calculada.secao() != SecaoPlanilha.EU
                && calculada.secao() != SecaoPlanilha.OUTRA;
        if (secaoDeOutraPessoa && calculada.confiancaSecao() >= CONFIANCA_PARA_SUGERIR_DONO) {
            return pessoas.get(calculada.secao());
        }
        return null;
    }

    private Map<SecaoPlanilha, Pessoa> carregarPessoas(Map<SecaoPlanilha, Long> ids) {
        Map<SecaoPlanilha, Pessoa> pessoas = new EnumMap<>(SecaoPlanilha.class);
        ids.forEach((secao, id) -> {
            if (id != null) {
                pessoas.put(secao, pessoaService.encontrar(id));
            }
        });
        return pessoas;
    }

    private void validarArquivo(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new RegraNegocioException("Envie a planilha no campo \"arquivo\".");
        }
        String nome = arquivo.getOriginalFilename() == null ? "" : arquivo.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!nome.endsWith(".xlsx")) {
            throw new RegraNegocioException("A planilha precisa estar no formato .xlsx.");
        }
    }

    private SugestaoRegra pendente(Long id) {
        SugestaoRegra sugestao = sugestaoRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Sugestão", id));
        if (!sugestao.isPendente()) {
            throw new RegraNegocioException("A sugestão " + id + " já foi " + sugestao.getStatus().name().toLowerCase(Locale.ROOT) + ".");
        }
        return sugestao;
    }
}
