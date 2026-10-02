package com.exemplo.loja.pedidos.infrastructure.web;

import com.exemplo.loja.pedidos.core.application.port.in.CriarPedidoUseCase;
import com.exemplo.loja.pedidos.infrastructure.web.dto.CriarPedidoRequest;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {
  private final CriarPedidoUseCase useCase;

  public PedidoController(CriarPedidoUseCase useCase) {
    this.useCase = useCase;
  }

  @PostMapping
  public ResponseEntity<CriarPedidoUseCase.Resultado> criar(
      @Valid @RequestBody CriarPedidoRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(useCase.executar(request.comando()));
  }
}
