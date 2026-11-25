package dev.alisonsantos.appgastos.contafixa;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ContaFixaMesRepository extends JpaRepository<ContaFixaMes, Long> {

    /** As contas de um mês, já com a conta fixa e o destino (pessoa ou divisão). */
    @EntityGraph(attributePaths = {"contaFixa", "contaFixa.pessoa", "contaFixa.divisaoSalva",
            "contaFixa.divisaoSalva.partes", "contaFixa.divisaoSalva.partes.pessoa"})
    List<ContaFixaMes> findByCompetencia(LocalDate competencia);

    /** O último valor pago de uma conta (para prever o próximo mês das contas variáveis). */
    Optional<ContaFixaMes> findFirstByContaFixaIdAndValorPagoIsNotNullOrderByCompetenciaDesc(Long contaFixaId);
}
