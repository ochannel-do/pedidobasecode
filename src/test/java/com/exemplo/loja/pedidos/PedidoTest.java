package com.exemplo.loja.pedidos;

import static org.assertj.core.api.Assertions.*;

import com.exemplo.loja.pedidos.core.domain.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class PedidoTest {
  @Test
  void validaInvariantes() {
    var id = UUID.randomUUID();
    assertThatThrownBy(() -> new ItemPedido(id, 0)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Pedido(id, List.of()))
        .isInstanceOf(IllegalArgumentException.class);
    var item = new ItemPedido(id, 1);
    assertThatThrownBy(() -> new Pedido(id, List.of(item, item)))
        .isInstanceOf(IllegalArgumentException.class);
    var itens = new ArrayList<>(List.of(item));
    var pedido = new Pedido(id, itens);
    itens.clear();
    assertThat(pedido.itens()).hasSize(1);
  }
}
