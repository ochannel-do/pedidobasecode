package com.exemplo.loja.pedidos;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.exemplo.loja.pedidos.core.application.port.in.CriarPedidoUseCase.*;
import com.exemplo.loja.pedidos.core.application.port.out.*;
import com.exemplo.loja.pedidos.core.application.usecase.CriarPedidoService;
import java.util.*;
import org.junit.jupiter.api.Test;

class CriarPedidoServiceTest {
  @Test
  void reservaAntesDePersistir() {
    var repo = mock(PedidoRepository.class);
    var estoque = mock(ReservarEstoque.class);
    var result =
        new CriarPedidoService(repo, estoque)
            .executar(
                new Comando(
                    List.of(new Item(UUID.randomUUID(), 2), new Item(UUID.randomUUID(), 1))));
    var ordem = inOrder(estoque, repo);
    ordem.verify(estoque).reservar(anyList());
    ordem.verify(repo).salvar(any());
    assertThat(result.status()).isEqualTo("CRIADO");
  }

  @Test
  void falhaNaoPersiste() {
    var repo = mock(PedidoRepository.class);
    var estoque = mock(ReservarEstoque.class);
    doThrow(new IllegalStateException()).when(estoque).reservar(anyList());
    assertThatThrownBy(
            () ->
                new CriarPedidoService(repo, estoque)
                    .executar(new Comando(List.of(new Item(UUID.randomUUID(), 1)))))
        .isInstanceOf(IllegalStateException.class);
    verifyNoInteractions(repo);
  }
}
