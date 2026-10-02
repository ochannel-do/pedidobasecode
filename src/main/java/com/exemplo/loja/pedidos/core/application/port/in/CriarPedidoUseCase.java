package com.exemplo.loja.pedidos.core.application.port.in;

import java.util.List;
import java.util.UUID;

public interface CriarPedidoUseCase {
  Resultado executar(Comando comando);

  record Item(UUID produtoId, int quantidade) {}

  record Comando(List<Item> itens) {
    public Comando {
      if (itens != null) {
        if (itens.stream().anyMatch(java.util.Objects::isNull))
          throw new IllegalArgumentException("Item obrigatório");
        itens = List.copyOf(itens);
      }
    }
  }

  record Resultado(UUID id, String status) {}
}
