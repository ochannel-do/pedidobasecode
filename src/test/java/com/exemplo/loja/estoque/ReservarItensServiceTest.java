package com.exemplo.loja.estoque;

import static org.assertj.core.api.Assertions.*;

import com.exemplo.loja.estoque.core.application.port.in.ReservarItensUseCase.*;
import com.exemplo.loja.estoque.core.application.usecase.ReservarItensService;
import java.util.*;
import org.junit.jupiter.api.Test;

class ReservarItensServiceTest {
  @Test
  void rejeitaListasInvalidasSemReservarNenhumProduto() {
    var chamadas = new ArrayList<UUID>();
    var service =
        new ReservarItensService(
            (id, quantidade) -> {
              chamadas.add(id);
              return true;
            });
    var produto = UUID.randomUUID();
    for (var itens :
        Arrays.asList(
            null,
            List.<Item>of(),
            Arrays.asList((Item) null),
            List.of(new Item(produto, 1), new Item(produto, 2)))) {
      assertThatThrownBy(() -> service.executar(itens))
          .isInstanceOf(IllegalArgumentException.class);
    }
    assertThat(chamadas).isEmpty();
  }

  @Test
  void reservaEmOrdemDeterministicaPreservandoQuantidades() {
    var primeiro = new UUID(0, 1);
    var segundo = new UUID(0, 2);
    var chamadas = new ArrayList<Item>();
    var service =
        new ReservarItensService(
            (id, quantidade) -> {
              chamadas.add(new Item(id, quantidade));
              return true;
            });
    service.executar(List.of(new Item(segundo, 7), new Item(primeiro, 3)));
    assertThat(chamadas).containsExactly(new Item(primeiro, 3), new Item(segundo, 7));
  }

  @Test
  void interrompeReservasAposPrimeiraIndisponibilidade() {
    var chamadas = new ArrayList<UUID>();
    var primeiro = new UUID(0, 1);
    var segundo = new UUID(0, 2);
    var service =
        new ReservarItensService(
            (id, quantidade) -> {
              chamadas.add(id);
              return false;
            });
    assertThatThrownBy(() -> service.executar(List.of(new Item(segundo, 1), new Item(primeiro, 1))))
        .isInstanceOf(Indisponivel.class);
    assertThat(chamadas).containsExactly(primeiro);
  }

  @Test
  void traduzIndisponibilidade() {
    assertThatThrownBy(
            () ->
                new ReservarItensService((id, q) -> false)
                    .executar(List.of(new Item(UUID.randomUUID(), 1))))
        .isInstanceOf(Indisponivel.class);
  }

  @Test
  void validaTodosAntesDeReservar() {
    var chamadas = new ArrayList<UUID>();
    var service =
        new ReservarItensService(
            (id, q) -> {
              chamadas.add(id);
              return true;
            });
    assertThatThrownBy(
            () ->
                service.executar(
                    List.of(new Item(UUID.randomUUID(), 1), new Item(UUID.randomUUID(), 0))))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(chamadas).isEmpty();
  }
}
