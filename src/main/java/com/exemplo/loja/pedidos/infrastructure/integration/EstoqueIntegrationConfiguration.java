package com.exemplo.loja.pedidos.infrastructure.integration;

import com.exemplo.loja.estoque.core.application.port.in.ReservarItensUseCase;
import com.exemplo.loja.pedidos.core.application.port.out.ReservarEstoque;
import org.springframework.context.annotation.*;

@Configuration
public class EstoqueIntegrationConfiguration {
  @Bean
  ReservarEstoque reservarEstoque(ReservarItensUseCase useCase) {
    return new EstoqueModuleAdapter(useCase);
  }
}
