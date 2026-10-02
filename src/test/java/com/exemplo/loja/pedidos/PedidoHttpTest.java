package com.exemplo.loja.pedidos;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.exemplo.loja.pedidos.core.application.port.in.CriarPedidoUseCase;
import com.exemplo.loja.pedidos.core.domain.exception.EstoqueInsuficienteException;
import com.exemplo.loja.pedidos.infrastructure.web.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PedidoController.class)
@Import(ApiExceptionHandler.class)
class PedidoHttpTest {
  @Autowired MockMvc mvc;
  @MockitoBean CriarPedidoUseCase useCase;
  String json =
      "{\"itens\":[{\"produtoId\":\"00000000-0000-0000-0000-000000000001\",\"quantidade\":1}]}";

  @Test
  void sucesso() throws Exception {
    when(useCase.executar(any()))
        .thenReturn(new CriarPedidoUseCase.Resultado(java.util.UUID.randomUUID(), "CRIADO"));
    mvc.perform(post("/pedidos").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("CRIADO"));
  }

  @Test
  void invalido() throws Exception {
    mvc.perform(post("/pedidos").contentType(MediaType.APPLICATION_JSON).content("{\"itens\":[]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ENTRADA_INVALIDA"));
    verifyNoInteractions(useCase);
  }

  @Test
  void insuficiente() throws Exception {
    when(useCase.executar(any())).thenThrow(new EstoqueInsuficienteException());
    mvc.perform(post("/pedidos").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ESTOQUE_INSUFICIENTE"));
  }

  @Test
  void inesperado() throws Exception {
    when(useCase.executar(any())).thenThrow(new IllegalStateException("segredo"));
    mvc.perform(post("/pedidos").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.detail").value("Não foi possível processar o pedido"));
  }
}
