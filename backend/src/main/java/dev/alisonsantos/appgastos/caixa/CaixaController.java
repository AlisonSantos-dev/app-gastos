package dev.alisonsantos.appgastos.caixa;

import dev.alisonsantos.appgastos.comum.PaginaResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * A caixa de entrada.
 *
 * GET  /api/caixa                       os pendentes (mais recentes primeiro)
 * GET  /api/caixa?motivo=SEM_DONO       só os sem dono (ou SEM_LEGENDA)
 * GET  /api/caixa/resumo                os números (para o badge da tela inicial)
 * POST /api/caixa/{id}/dono             escolhe o dono e tira da caixa
 * POST /api/caixa/{id}/confirmar        confirma uma compra como está
 * POST /api/caixa/confirmar             confirma várias de uma vez
 * POST /api/caixa/confirmar-ate         limpeza do histórico até uma data
 */
@RestController
@RequestMapping("/api/caixa")
public class CaixaController {

    private final CaixaService caixaService;

    public CaixaController(CaixaService caixaService) {
        this.caixaService = caixaService;
    }

    @GetMapping
    public PaginaResponse<ItemCaixaResponse> listar(
            @RequestParam(required = false) MotivoPendencia motivo,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamanho) {
        return caixaService.listar(motivo, pagina, tamanho);
    }

    @GetMapping("/resumo")
    public ResumoCaixaResponse resumo() {
        return caixaService.resumo();
    }

    @PostMapping("/{id}/dono")
    public ResultadoConfirmacao definirDono(@PathVariable Long id, @Valid @RequestBody DonoRequest pedido) {
        return caixaService.definirDono(id, pedido.pessoaId());
    }

    @PostMapping("/{id}/confirmar")
    public ResultadoConfirmacao confirmarUma(@PathVariable Long id) {
        return caixaService.confirmar(List.of(id));
    }

    @PostMapping("/confirmar")
    public ResultadoConfirmacao confirmarVarias(@Valid @RequestBody ConfirmarRequest pedido) {
        return caixaService.confirmar(pedido.ids());
    }

    @PostMapping("/confirmar-ate")
    public ResultadoConfirmacao confirmarAte(@Valid @RequestBody ConfirmarAteRequest pedido) {
        return caixaService.confirmarAte(pedido.data());
    }
}
