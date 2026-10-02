package com.exemplo.loja.estoque;

import static org.assertj.core.api.Assertions.*;

import com.exemplo.loja.estoque.core.domain.model.ItemEstoque;
import org.junit.jupiter.api.Test;

class ItemEstoqueTest {
  @Test
  void valida() {
    assertThatThrownBy(() -> new ItemEstoque(null, 1)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new ItemEstoque(java.util.UUID.randomUUID(), -1))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
