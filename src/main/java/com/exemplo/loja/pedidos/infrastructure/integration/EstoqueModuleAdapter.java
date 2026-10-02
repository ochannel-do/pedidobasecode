package com.exemplo.loja.pedidos.infrastructure.integration;

import com.exemplo.loja.estoque.core.application.port.in.ReservarItensUseCase;
import com.exemplo.loja.pedidos.core.application.port.out.ReservarEstoque;
import com.exemplo.loja.pedidos.core.domain.exception.EstoqueInsuficienteException;
import java.util.List;

public final class EstoqueModuleAdapter implements ReservarEstoque {
  private final ReservarItensUseCase estoque;

  public EstoqueModuleAdapter(ReservarItensUseCase estoque) {
    this.estoque = estoque;
  }

  public void reservar(List<Item> itens) {
    try {
      estoque.executar(
          itens.stream()
              .map(i -> new ReservarItensUseCase.Item(i.produtoId(), i.quantidade()))
              .toList());
    } catch (ReservarItensUseCase.Indisponivel e) {
      throw new EstoqueInsuficienteException();
    }
  }
}
