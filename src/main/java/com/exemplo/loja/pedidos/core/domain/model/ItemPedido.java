package com.exemplo.loja.pedidos.core.domain.model;

import java.util.UUID;

public record ItemPedido(UUID produtoId, int quantidade) {
  public ItemPedido {
    if (produtoId == null || quantidade <= 0)
      throw new IllegalArgumentException("Produto e quantidade positiva são obrigatórios");
  }
}
