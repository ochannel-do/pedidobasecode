package com.exemplo.loja.estoque.infrastructure.observability;

import com.exemplo.loja.estoque.core.application.port.in.ReservarItensUseCase;
import io.micrometer.observation.*;

public final class ReservarItensObservado implements ReservarItensUseCase {
  private final ReservarItensUseCase delegate;
  private final ObservationRegistry registry;

  public ReservarItensObservado(ReservarItensUseCase delegate, ObservationRegistry registry) {
    this.delegate = delegate;
    this.registry = registry;
  }

  public void executar(java.util.List<Item> itens) {
    Observation.createNotStarted("estoque.reservar", registry)
        .observe(() -> delegate.executar(itens));
  }
}
