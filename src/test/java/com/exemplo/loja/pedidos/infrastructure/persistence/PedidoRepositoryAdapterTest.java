package com.exemplo.loja.pedidos.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.exemplo.loja.pedidos.core.domain.model.*;
import io.micrometer.observation.ObservationRegistry;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PedidoRepositoryAdapterTest {
  @Test
  void persisteIdentificadorEItensSemPerderQuantidadesOuOrdem() {
    var repository = mock(PedidoJpaRepository.class);
    var pedido =
        new Pedido(
            UUID.randomUUID(),
            List.of(new ItemPedido(UUID.randomUUID(), 3), new ItemPedido(UUID.randomUUID(), 8)));
    new PedidoRepositoryAdapter(repository, ObservationRegistry.NOOP).salvar(pedido);
    var captor = ArgumentCaptor.forClass(PedidoEntity.class);
    verify(repository).saveAndFlush(captor.capture());
    var entity = captor.getValue();
    assertThat(entity.id).isEqualTo(pedido.id());
    assertThat(entity.itens)
        .extracting(i -> i.produtoId)
        .containsExactlyElementsOf(pedido.itens().stream().map(ItemPedido::produtoId).toList());
    assertThat(entity.itens).extracting(i -> i.quantidade).containsExactly(3, 8);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void propagaErroDeFlushParaPermitirRollback() {
    var repository = mock(PedidoJpaRepository.class);
    var falha = new IllegalStateException("restrição SQL");
    when(repository.saveAndFlush(any())).thenThrow(falha);
    var pedido = new Pedido(UUID.randomUUID(), List.of(new ItemPedido(UUID.randomUUID(), 1)));
    assertThatThrownBy(
            () -> new PedidoRepositoryAdapter(repository, ObservationRegistry.NOOP).salvar(pedido))
        .isSameAs(falha);
  }
}
