package com.exemplo.loja.pedidos.infrastructure.transaction;

import com.exemplo.loja.pedidos.core.application.port.in.CriarPedidoUseCase;
import org.springframework.transaction.support.TransactionTemplate;

public final class CriarPedidoTransacional implements CriarPedidoUseCase {
  private final CriarPedidoUseCase delegate;
  private final TransactionTemplate transaction;

  public CriarPedidoTransacional(CriarPedidoUseCase delegate, TransactionTemplate transaction) {
    this.delegate = delegate;
    this.transaction = transaction;
  }

  public Resultado executar(Comando comando) {
    return transaction.execute(status -> delegate.executar(comando));
  }
}
