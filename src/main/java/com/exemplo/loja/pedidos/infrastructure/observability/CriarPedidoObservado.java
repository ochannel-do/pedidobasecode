package com.exemplo.loja.pedidos.infrastructure.observability;

import com.exemplo.loja.pedidos.core.application.port.in.CriarPedidoUseCase;
import io.micrometer.observation.*;

public final class CriarPedidoObservado implements CriarPedidoUseCase {
  private final CriarPedidoUseCase delegate;
  private final ObservationRegistry registry;

  public CriarPedidoObservado(CriarPedidoUseCase delegate, ObservationRegistry registry) {
    this.delegate = delegate;
    this.registry = registry;
  }

  public Resultado executar(Comando comando) {
    return Observation.createNotStarted("pedidos.criar", registry)
        .observe(
            () -> {
              var resultado = delegate.executar(comando);
              org.slf4j.LoggerFactory.getLogger(CriarPedidoObservado.class)
                  .info("Pedido criado e estoque reservado");
              return resultado;
            });
  }
}
