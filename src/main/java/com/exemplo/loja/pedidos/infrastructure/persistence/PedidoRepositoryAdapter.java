package com.exemplo.loja.pedidos.infrastructure.persistence;

import com.exemplo.loja.pedidos.core.application.port.out.PedidoRepository;
import com.exemplo.loja.pedidos.core.domain.model.Pedido;
import io.micrometer.observation.*;

public final class PedidoRepositoryAdapter implements PedidoRepository {
  private final PedidoJpaRepository repository;
  private final ObservationRegistry registry;

  public PedidoRepositoryAdapter(PedidoJpaRepository repository, ObservationRegistry registry) {
    this.repository = repository;
    this.registry = registry;
  }

  public void salvar(Pedido pedido) {
    Observation.createNotStarted("pedidos.persistir", registry)
        .observe(
            () ->
                repository.saveAndFlush(
                    new PedidoEntity(
                        pedido.id(),
                        pedido.itens().stream()
                            .map(i -> new PedidoEntity.ItemEntity(i.produtoId(), i.quantidade()))
                            .toList())));
  }
}
