package com.exemplo.loja.estoque;

import static org.assertj.core.api.Assertions.*;

import com.exemplo.loja.estoque.core.application.port.in.ReservarItensUseCase.*;
import com.exemplo.loja.estoque.core.application.usecase.ReservarItensService;
import java.util.*;
import org.junit.jupiter.api.Test;

class ReservarItensServiceTest {
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
