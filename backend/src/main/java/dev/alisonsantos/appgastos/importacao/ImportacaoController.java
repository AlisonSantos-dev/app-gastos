package dev.alisonsantos.appgastos.importacao;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Importação da planilha antiga.
 *
 * POST /api/importacao/planilha                    envia o .xlsx e gera as sugestões
 * GET  /api/importacao/sugestoes                   as pendentes (ou ?status=APROVADA / DESCARTADA)
 * POST /api/importacao/sugestoes/{id}/aprovar      cria a regra (com ajustes opcionais)
 * POST /api/importacao/sugestoes/aprovar           aprova várias de uma vez
 * POST /api/importacao/sugestoes/{id}/descartar
 */
@RestController
@RequestMapping("/api/importacao")
public class ImportacaoController {

    private final ImportacaoService importacaoService;

    public ImportacaoController(ImportacaoService importacaoService) {
        this.importacaoService = importacaoService;
    }

    /**
     * multipart/form-data: o formato dos formulários com upload de arquivo.
     * O arquivo vai no campo "arquivo", e os ids das pessoas de cada seção
     * da planilha vão como campos de texto.
     */
    @PostMapping(value = "/planilha", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResumoImportacao importar(
            @RequestPart("arquivo") MultipartFile arquivo,
            @RequestParam(required = false) Long pessoaEu,
            @RequestParam(required = false) Long pessoaMae,
            @RequestParam(required = false) Long pessoaPai,
            @RequestParam(required = false) Long pessoaMinhaVida) {

        Map<SecaoPlanilha, Long> pessoas = new EnumMap<>(SecaoPlanilha.class);
        pessoas.put(SecaoPlanilha.EU, pessoaEu);
        pessoas.put(SecaoPlanilha.MAE, pessoaMae);
        pessoas.put(SecaoPlanilha.PAI, pessoaPai);
        pessoas.put(SecaoPlanilha.MINHA_VIDA, pessoaMinhaVida);
        return importacaoService.importar(arquivo, pessoas);
    }

    @GetMapping("/sugestoes")
    public List<SugestaoResponse> sugestoes(@RequestParam(defaultValue = "PENDENTE") StatusSugestao status) {
        return importacaoService.listar(status);
    }

    @PostMapping("/sugestoes/{id}/aprovar")
    public SugestaoResponse aprovar(@PathVariable Long id,
                                    @Valid @RequestBody(required = false) AprovacaoRequest ajustes) {
        return importacaoService.aprovar(id, ajustes);
    }

    @PostMapping("/sugestoes/aprovar")
    public List<SugestaoResponse> aprovarVarias(@Valid @RequestBody AprovacaoLoteRequest pedido) {
        return importacaoService.aprovarVarias(pedido.ids());
    }

    @PostMapping("/sugestoes/{id}/descartar")
    public ResponseEntity<Void> descartar(@PathVariable Long id) {
        importacaoService.descartar(id);
        return ResponseEntity.noContent().build();
    }
}
