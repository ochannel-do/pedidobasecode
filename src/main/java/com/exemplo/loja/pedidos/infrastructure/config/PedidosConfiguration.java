package com.exemplo.loja.pedidos.infrastructure.config;

import com.exemplo.loja.pedidos.core.application.port.in.CriarPedidoUseCase;
import com.exemplo.loja.pedidos.core.application.port.out.ReservarEstoque;
import com.exemplo.loja.pedidos.core.application.usecase.CriarPedidoService;
import com.exemplo.loja.pedidos.infrastructure.observability.CriarPedidoObservado;
import com.exemplo.loja.pedidos.infrastructure.persistence.*;
import com.exemplo.loja.pedidos.infrastructure.transaction.CriarPedidoTransacional;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.context.annotation.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class PedidosConfiguration {
  @Bean
  CriarPedidoUseCase criarPedidoUseCase(
      PedidoJpaRepository repository,
      ReservarEstoque estoque,
      PlatformTransactionManager manager,
      ObservationRegistry registry) {
    var service =
        new CriarPedidoService(new PedidoRepositoryAdapter(repository, registry), estoque);
    return new CriarPedidoObservado(
        new CriarPedidoTransacional(service, new TransactionTemplate(manager)), registry);
  }
}
