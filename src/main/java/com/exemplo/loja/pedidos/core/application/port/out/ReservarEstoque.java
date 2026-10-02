package com.exemplo.loja.pedidos.core.application.port.out;

import java.util.List;
import java.util.UUID;

public interface ReservarEstoque {
  void reservar(List<Item> itens);

  record Item(UUID produtoId, int quantidade) {}
}
