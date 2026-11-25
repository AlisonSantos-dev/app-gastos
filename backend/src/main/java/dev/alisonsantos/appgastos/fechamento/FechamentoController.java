package dev.alisonsantos.appgastos.fechamento;

import dev.alisonsantos.appgastos.comum.Competencia;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * O fechamento do mês.
 *
 * GET    /api/fechamentos/2026-11                       o mês (publicado, ou a prévia)
 * GET    /api/fechamentos/2026-11/itens?pessoaId=3      as linhas de uma pessoa
 * GET    /api/fechamentos/2026-11/itens?semDono=true    as linhas sem dono
 * POST   /api/fechamentos/2026-11/publicar              congela o mês (?forcar=true ignora os alertas)
 * DELETE /api/fechamentos/2026-11/publicacao            reabre o mês
 */
@RestController
@RequestMapping("/api/fechamentos")
public class FechamentoController {

    private final FechamentoService service;

    public FechamentoController(FechamentoService service) {
        this.service = service;
    }

    @GetMapping("/{competencia}")
    public FechamentoResponse consultar(@PathVariable String competencia) {
        return service.consultar(Competencia.ler(competencia));
    }

    @GetMapping("/{competencia}/itens")
    public List<ItemFechamentoResponse> itens(@PathVariable String competencia,
                                              @RequestParam(required = false) Long pessoaId,
                                              @RequestParam(defaultValue = "false") boolean semDono) {
        return service.itens(Competencia.ler(competencia), pessoaId, semDono);
    }

    @PostMapping("/{competencia}/publicar")
    public FechamentoResponse publicar(@PathVariable String competencia,
                                       @RequestParam(defaultValue = "false") boolean forcar) {
        return service.publicar(Competencia.ler(competencia), forcar);
    }

    @DeleteMapping("/{competencia}/publicacao")
    public ResponseEntity<Void> reabrir(@PathVariable String competencia) {
        service.reabrir(Competencia.ler(competencia));
        return ResponseEntity.noContent().build();
    }
}
