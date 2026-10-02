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
  void entradasInvalidasNaoReservamNemPersistem() {
    var repo = mock(PedidoRepository.class);
    var estoque = mock(ReservarEstoque.class);
    var service = new CriarPedidoService(repo, estoque);
    assertThatThrownBy(() -> service.executar(null)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.executar(new Comando(null)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.executar(new Comando(List.of())))
        .isInstanceOf(IllegalArgumentException.class);
    var produto = UUID.randomUUID();
    assertThatThrownBy(
            () ->
                service.executar(new Comando(List.of(new Item(produto, 1), new Item(produto, 2)))))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.executar(new Comando(List.of(new Item(produto, 0)))))
        .isInstanceOf(IllegalArgumentException.class);
    verifyNoInteractions(estoque, repo);
  }

  @Test
  void comandoRejeitaItemNuloECopiaListaRecebida() {
    assertThatThrownBy(() -> new Comando(Arrays.asList((Item) null)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Item obrigatório");
    var item = new Item(UUID.randomUUID(), 2);
    var origem = new ArrayList<>(List.of(item));
    var comando = new Comando(origem);
    origem.clear();
    assertThat(comando.itens()).containsExactly(item);
    assertThatThrownBy(() -> comando.itens().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }

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
