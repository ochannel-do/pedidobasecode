package com.exemplo.loja.estoque.core.domain.model;

import java.util.UUID;

public record ItemEstoque(UUID produtoId, int quantidade) {
  public ItemEstoque {
    if (produtoId == null || quantidade <= 0)
      throw new IllegalArgumentException("Produto e quantidade positiva são obrigatórios");
  }
}
