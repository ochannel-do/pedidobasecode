package com.exemplo.loja.pedidos.core.application.usecase;

import com.exemplo.loja.pedidos.core.application.port.in.CriarPedidoUseCase;
import com.exemplo.loja.pedidos.core.application.port.out.*;
import com.exemplo.loja.pedidos.core.domain.model.*;
import java.util.UUID;

public final class CriarPedidoService implements CriarPedidoUseCase {
  private final PedidoRepository repository;
  private final ReservarEstoque estoque;

  public CriarPedidoService(PedidoRepository repository, ReservarEstoque estoque) {
    this.repository = repository;
    this.estoque = estoque;
  }

  public Resultado executar(Comando comando) {
    if (comando == null || comando.itens() == null)
      throw new IllegalArgumentException("Itens obrigatórios");
    Pedido pedido =
        new Pedido(
            UUID.randomUUID(),
            comando.itens().stream()
                .map(i -> new ItemPedido(i.produtoId(), i.quantidade()))
                .toList());
    estoque.reservar(
        pedido.itens().stream()
            .map(i -> new ReservarEstoque.Item(i.produtoId(), i.quantidade()))
            .toList());
    repository.salvar(pedido);
    return new Resultado(pedido.id(), "CRIADO");
  }
}
