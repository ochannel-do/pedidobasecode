package com.exemplo.loja.pedidos.infrastructure.web;

import com.exemplo.loja.pedidos.core.domain.exception.EstoqueInsuficienteException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler({
    IllegalArgumentException.class,
    MethodArgumentNotValidException.class,
    HttpMessageNotReadableException.class
  })
  ProblemDetail invalido(Exception e) {
    return problem(400, "ENTRADA_INVALIDA", "Verifique os campos e as quantidades do pedido");
  }

  @ExceptionHandler(EstoqueInsuficienteException.class)
  ProblemDetail estoque(Exception e, HttpServletRequest request) {
    request.setAttribute(HttpRequestLoggingFilter.BUSINESS_CODE_ATTRIBUTE, "ESTOQUE_INSUFICIENTE");
    return problem(409, "ESTOQUE_INSUFICIENTE", "Estoque insuficiente para os itens solicitados");
  }

  @ExceptionHandler(Exception.class)
  ProblemDetail inesperado(Exception e) {
    org.slf4j.LoggerFactory.getLogger(getClass()).error("Falha ao processar pedido", e);
    return problem(500, "ERRO_INTERNO", "Não foi possível processar o pedido");
  }

  private ProblemDetail problem(int status, String code, String detail) {
    var p = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(status), detail);
    p.setTitle(code);
    p.setProperty("code", code);
    return p;
  }
}
