package com.exemplo.loja.estoque.infrastructure.config;

import com.exemplo.loja.estoque.core.application.port.in.ReservarItensUseCase;
import com.exemplo.loja.estoque.core.application.usecase.ReservarItensService;
import com.exemplo.loja.estoque.infrastructure.observability.ReservarItensObservado;
import com.exemplo.loja.estoque.infrastructure.persistence.*;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.context.annotation.*;

@Configuration
public class EstoqueConfiguration {
  @Bean
  ReservarItensUseCase reservarItensUseCase(
      EstoqueJpaRepository repository, ObservationRegistry registry) {
    return new ReservarItensObservado(
        new ReservarItensService(new EstoqueRepositoryAdapter(repository)), registry);
  }
}
