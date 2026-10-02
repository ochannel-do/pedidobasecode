package com.exemplo.loja.support;

import static org.assertj.core.api.Assertions.*;

import com.exemplo.loja.estoque.core.application.port.in.ReservarItensUseCase;
import com.exemplo.loja.estoque.infrastructure.observability.ReservarItensObservado;
import com.exemplo.loja.pedidos.core.application.port.in.CriarPedidoUseCase;
import com.exemplo.loja.pedidos.infrastructure.observability.CriarPedidoObservado;
import io.micrometer.observation.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class ObservabilidadeTest {
  private final ObservationRegistry registry = ObservationRegistry.create();
  private final List<Observation.Context> concluidas = new ArrayList<>();

  ObservabilidadeTest() {
    registry
        .observationConfig()
        .observationHandler(
            new ObservationHandler<Observation.Context>() {
              public boolean supportsContext(Observation.Context context) {
                return true;
              }

              public void onStop(Observation.Context context) {
                concluidas.add(context);
              }
            });
  }

  @Test
  void observacaoDePedidoPreservaResultadoComandoEEscopo() {
    var comando =
        new CriarPedidoUseCase.Comando(List.of(new CriarPedidoUseCase.Item(UUID.randomUUID(), 2)));
    var esperado = new CriarPedidoUseCase.Resultado(UUID.randomUUID(), "CRIADO");
    var observado =
        new CriarPedidoObservado(
            recebido -> {
              assertThat(recebido).isSameAs(comando);
              assertThat(registry.getCurrentObservation().getContext().getName())
                  .isEqualTo("pedidos.criar");
              return esperado;
            },
            registry);
    assertThat(observado.executar(comando)).isSameAs(esperado);
    assertThat(concluidas)
        .singleElement()
        .satisfies(
            c -> {
              assertThat(c.getName()).isEqualTo("pedidos.criar");
              assertThat(c.getError()).isNull();
            });
    assertThat(registry.getCurrentObservation()).isNull();
  }

  @Test
  void observacaoDePedidoRegistraErroSemEngolirExcecao() {
    var falha = new IllegalStateException("falha no pedido");
    var observado =
        new CriarPedidoObservado(
            c -> {
              throw falha;
            },
            registry);
    assertThatThrownBy(() -> observado.executar(new CriarPedidoUseCase.Comando(List.of())))
        .isSameAs(falha);
    assertThat(concluidas).singleElement().satisfies(c -> assertThat(c.getError()).isSameAs(falha));
    assertThat(registry.getCurrentObservation()).isNull();
  }

  @Test
  void observacaoDeEstoquePreservaItensEEscopo() {
    var itens = List.of(new ReservarItensUseCase.Item(UUID.randomUUID(), 4));
    var observado =
        new ReservarItensObservado(
            recebidos -> {
              assertThat(recebidos).isSameAs(itens);
              assertThat(registry.getCurrentObservation().getContext().getName())
                  .isEqualTo("estoque.reservar");
            },
            registry);
    observado.executar(itens);
    assertThat(concluidas)
        .singleElement()
        .satisfies(
            c -> {
              assertThat(c.getName()).isEqualTo("estoque.reservar");
              assertThat(c.getError()).isNull();
            });
    assertThat(registry.getCurrentObservation()).isNull();
  }

  @Test
  void observacaoDeEstoqueRegistraIndisponibilidadeERestauraEscopoPai() {
    var falha = new ReservarItensUseCase.Indisponivel();
    var pai = Observation.start("pedido.pai", registry);
    try (var scope = pai.openScope()) {
      var observado =
          new ReservarItensObservado(
              itens -> {
                throw falha;
              },
              registry);
      assertThatThrownBy(() -> observado.executar(List.of())).isSameAs(falha);
      assertThat(registry.getCurrentObservation()).isSameAs(pai);
      assertThat(concluidas)
          .singleElement()
          .satisfies(
              c -> {
                assertThat(c.getName()).isEqualTo("estoque.reservar");
                assertThat(c.getError()).isSameAs(falha);
              });
    } finally {
      pai.stop();
    }
    assertThat(registry.getCurrentObservation()).isNull();
  }
}
