package dev.alisonsantos.appgastos.importacao;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SugestaoRegraRepository extends JpaRepository<SugestaoRegra, Long> {

    /** As sugestões de um status, das que mais ajudam para as que menos. */
    @EntityGraph(attributePaths = "pessoa")
    List<SugestaoRegra> findByStatusOrderByTransacoesAfetadasDescComprasDesc(StatusSugestao status);

    /** Antes de uma nova importação, as pendentes antigas são substituídas. */
    @Modifying
    @Query("DELETE FROM SugestaoRegra s WHERE s.status = :status")
    int apagarPorStatus(@Param("status") StatusSugestao status);
}
