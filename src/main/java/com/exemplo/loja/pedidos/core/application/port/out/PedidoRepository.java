package com.exemplo.loja.pedidos.core.application.port.out;

import com.exemplo.loja.pedidos.core.domain.model.Pedido;

public interface PedidoRepository {
  void salvar(Pedido pedido);
}
