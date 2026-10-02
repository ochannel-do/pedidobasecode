package com.exemplo.loja.estoque.core.application.port.in;

import java.util.List;
import java.util.UUID;

public interface ReservarItensUseCase {
  void executar(List<Item> itens);

  record Item(UUID produtoId, int quantidade) {}

  final class Indisponivel extends RuntimeException {
    public Indisponivel() {
      super("Estoque indisponível");
    }
  }
}
