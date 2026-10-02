package com.exemplo.loja.pedidos;

import static org.assertj.core.api.Assertions.*;

import com.exemplo.loja.pedidos.core.application.port.in.CriarPedidoUseCase;
import com.exemplo.loja.pedidos.core.application.port.in.CriarPedidoUseCase.*;
import com.exemplo.loja.pedidos.core.application.port.out.ReservarEstoque;
import com.exemplo.loja.pedidos.core.application.usecase.CriarPedidoService;
import com.exemplo.loja.pedidos.core.domain.exception.EstoqueInsuficienteException;
import com.exemplo.loja.pedidos.infrastructure.transaction.CriarPedidoTransacional;
import com.exemplo.loja.support.PostgresTestConfiguration;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(
    properties = {
      "management.tracing.sampling.probability=1.0",
      "spring.autoconfigure.exclude=org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure.otlp.OtlpTracingAutoConfiguration"
    })
@Import({PostgresTestConfiguration.class, PedidoFlowIT.TraceConfiguration.class})
class PedidoFlowIT {
  @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
  static class TraceConfiguration {
    @org.springframework.context.annotation.Bean
    io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter exporter() {
      return io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter.create();
    }

    @org.springframework.context.annotation.Bean
    org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure
            .SdkTracerProviderBuilderCustomizer
        capture(io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter exporter) {
      return builder ->
          builder.addSpanProcessor(
              io.opentelemetry.sdk.trace.export.SimpleSpanProcessor.create(exporter));
    }
  }

  @Autowired io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter exporter;
  @Autowired io.micrometer.observation.ObservationRegistry registry;
  @Autowired org.springframework.context.ApplicationContext context;

  @Test
  void tracingHierarquia() {
    assertThat(
            context.getBeansOfType(
                io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter.class))
        .isEmpty();
    exporter.reset();
    io.micrometer.observation.Observation.createNotStarted("http.test", registry)
        .observe(() -> useCase.executar(comando(new Item(a, 1))));
    var spans = exporter.getFinishedSpanItems();
    var root =
        spans.stream().filter(s -> s.getName().equals("http.test")).findFirst().orElseThrow();
    var criar =
        spans.stream().filter(s -> s.getName().equals("pedidos.criar")).findFirst().orElseThrow();
    assertThat(criar.getParentSpanId()).isEqualTo(root.getSpanId());
    for (String name : List.of("estoque.reservar", "pedidos.persistir")) {
      var child = spans.stream().filter(s -> s.getName().equals(name)).findFirst().orElseThrow();
      assertThat(child.getTraceId()).isEqualTo(root.getTraceId());
      assertThat(child.getParentSpanId()).isEqualTo(criar.getSpanId());
    }
  }

  @Autowired CriarPedidoUseCase useCase;
  @Autowired ReservarEstoque estoque;
  @Autowired JdbcTemplate jdbc;
  @Autowired PlatformTransactionManager manager;
  UUID a = UUID.fromString("00000000-0000-0000-0000-000000000001"),
      b = UUID.fromString("00000000-0000-0000-0000-000000000002");

  @BeforeEach
  void preparar() {
    jdbc.execute("truncate pedido_itens,pedidos,estoque");
    jdbc.update("insert into estoque values (?, 1), (?, 0)", a, b);
  }

  Comando comando(Item... itens) {
    return new Comando(List.of(itens));
  }

  int saldo(UUID id) {
    return jdbc.queryForObject(
        "select disponivel from estoque where produto_id=?", Integer.class, id);
  }

  int pedidos() {
    return jdbc.queryForObject("select count(*) from pedidos", Integer.class);
  }

  @Test
  void criaMultiplosItens() {
    jdbc.update("update estoque set disponivel=2 where produto_id=?", b);
    var result = useCase.executar(comando(new Item(a, 1), new Item(b, 2)));
    assertThat(result.id()).isNotNull();
    assertThat(saldo(a)).isZero();
    assertThat(saldo(b)).isZero();
    assertThat(pedidos()).isEqualTo(1);
    assertThat(jdbc.queryForObject("select count(*) from pedido_itens", Integer.class))
        .isEqualTo(2);
  }

  @Test
  void rollbackParcial() {
    assertThatThrownBy(() -> useCase.executar(comando(new Item(a, 1), new Item(b, 1))))
        .isInstanceOf(EstoqueInsuficienteException.class);
    assertThat(saldo(a)).isEqualTo(1);
    assertThat(pedidos()).isZero();
  }

  @Test
  void falhaPersistenciaReverteReserva() {
    var service =
        new CriarPedidoService(
            p -> {
              jdbc.update("insert into pedidos(id) values (?)", p.id());
              throw new IllegalStateException("falha simulada");
            },
            estoque);
    var transacional = new CriarPedidoTransacional(service, new TransactionTemplate(manager));
    assertThatThrownBy(() -> transacional.executar(comando(new Item(a, 1))))
        .isInstanceOf(IllegalStateException.class);
    assertThat(saldo(a)).isEqualTo(1);
    assertThat(pedidos()).isZero();
  }

  @Test
  void concorrenciaUltimaUnidade() throws Exception {
    var ready = new CountDownLatch(2);
    var start = new CountDownLatch(1);
    Callable<Boolean> task =
        () -> {
          ready.countDown();
          if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("timeout");
          try {
            useCase.executar(comando(new Item(a, 1)));
            return true;
          } catch (EstoqueInsuficienteException e) {
            return false;
          }
        };
    try (var executor = Executors.newFixedThreadPool(2)) {
      var first = executor.submit(task);
      var second = executor.submit(task);
      assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      assertThat(List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(true, false);
    }
    assertThat(saldo(a)).isZero();
    assertThat(pedidos()).isEqualTo(1);
  }

  @Test
  void migrations() {
    assertThat(
            jdbc.queryForObject(
                "select count(*) from flyway_schema_history where success", Integer.class))
        .isEqualTo(1);
  }
}
