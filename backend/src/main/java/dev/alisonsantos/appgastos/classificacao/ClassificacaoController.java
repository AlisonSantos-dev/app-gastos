package dev.alisonsantos.appgastos.classificacao;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * POST /api/classificacao/reclassificar
 *
 * Aplica as regras de classificação em todas as transações do banco,
 * preservando as que foram corrigidas à mão.
 */
@RestController
@RequestMapping("/api/classificacao")
public class ClassificacaoController {

    private final ClassificacaoService classificacaoService;

    public ClassificacaoController(ClassificacaoService classificacaoService) {
        this.classificacaoService = classificacaoService;
    }

    @PostMapping("/reclassificar")
    public ResumoClassificacao reclassificar() {
        return classificacaoService.reclassificarTodas();
    }
}
