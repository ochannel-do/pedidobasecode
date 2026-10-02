package com.exemplo.loja.estoque.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface EstoqueJpaRepository extends JpaRepository<EstoqueEntity, UUID> {
  @Modifying
  @Query(
      value =
          "update estoque set disponivel = disponivel - :quantidade where produto_id = :id and disponivel >= :quantidade",
      nativeQuery = true)
  int reservar(@Param("id") UUID id, @Param("quantidade") int quantidade);
}
