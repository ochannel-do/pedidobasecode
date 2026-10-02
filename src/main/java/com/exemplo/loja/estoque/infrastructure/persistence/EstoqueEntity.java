package com.exemplo.loja.estoque.infrastructure.persistence;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "estoque")
public class EstoqueEntity {
  @Id
  @Column(name = "produto_id")
  UUID produtoId;

  @Column(nullable = false)
  int disponivel;

  protected EstoqueEntity() {}
}
