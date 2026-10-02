package com.exemplo.loja.pedidos.core.domain.model;

import java.util.List;
import java.util.UUID;

public record Pedido(UUID id, List<ItemPedido> itens) {
  public Pedido {
    if (id == null
        || itens == null
        || itens.isEmpty()
        || itens.stream().anyMatch(java.util.Objects::isNull))
      throw new IllegalArgumentException("Pedido exige identificador e itens");
    itens = List.copyOf(itens);
    if (itens.stream().map(ItemPedido::produtoId).distinct().count() != itens.size())
      throw new IllegalArgumentException("Produtos duplicados");
  }
}
