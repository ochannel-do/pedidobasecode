package com.exemplo.loja.estoque.core.application.port.out;

import java.util.UUID;

public interface EstoqueRepository {
  boolean reservar(UUID produtoId, int quantidade);
}
