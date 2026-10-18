package com.exemplo.loja.pedidos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.exemplo.loja.pedidos.core.application.port.in.CriarPedidoUseCase;
import com.exemplo.loja.pedidos.core.domain.exception.EstoqueInsuficienteException;
import com.exemplo.loja.pedidos.infrastructure.web.ApiExceptionHandler;
import com.exemplo.loja.pedidos.infrastructure.web.HttpRequestLoggingFilter;
import com.exemplo.loja.pedidos.infrastructure.web.PedidoController;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(
    classes = HttpObservabilityTest.HttpConfiguration.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "server.tomcat.threads.max=1",
      "server.tomcat.threads.min-spare=1",
      "management.tracing.sampling.probability=0.0",
      "management.tracing.export.otlp.enabled=false",
      "management.otlp.metrics.export.enabled=false",
      "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"
    })
@ExtendWith(OutputCaptureExtension.class)
@ActiveProfiles("http-observability-test")
class HttpObservabilityTest {
  @Configuration(proxyBeanMethods = false)
  @Profile("http-observability-test")
  @EnableAutoConfiguration
  @Import({PedidoController.class, ApiExceptionHandler.class, HttpRequestLoggingFilter.class})
  static class HttpConfiguration {}

  @LocalServerPort int port;
  @MockitoBean CriarPedidoUseCase useCase;
  private final JsonMapper mapper = JsonMapper.builder().build();
  private static final String TRACE = "0123456789abcdef0123456789abcdef";
  private static final String BODY =
      "{\"itens\":[{\"produtoId\":\"00000000-0000-0000-0000-000000000001\",\"quantidade\":1}]}";

  @Test
  void conflitosCorrelacionadosMesmoSemAmostragem(CapturedOutput output) throws Exception {
    List<String> contexts = new CopyOnWriteArrayList<>();
    when(useCase.executar(any()))
        .thenAnswer(
            invocation -> {
              contexts.add(MDC.get("correlationId"));
              LoggerFactory.getLogger(HttpObservabilityTest.class).info("request_context_probe");
              throw new EstoqueInsuficienteException();
            });
    try (var client = HttpClient.newHttpClient()) {
      for (int i = 0; i < 5; i++) {
        var builder = request(BODY);
        if (i == 0) builder.header("traceparent", "00-" + TRACE + "-0123456789abcdef-00");
        var response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(409);
        var body = mapper.readTree(response.body());
        assertThat(body.get("code").asText()).isEqualTo("ESTOQUE_INSUFICIENTE");
        assertThat(body.get("title").asText()).isEqualTo("ESTOQUE_INSUFICIENTE");
        assertThat(body.get("detail").asText())
            .isEqualTo("Estoque insuficiente para os itens solicitados");
        assertThat(body.get("instance").asText()).isEqualTo("/pedidos");
        assertThat(body.get("status").asInt()).isEqualTo(409);
        assertThat(response.headers().firstValue("X-Correlation-Id")).contains(contexts.get(i));
      }
    }
    await().untilAsserted(() -> assertThat(events(output)).hasSize(5));
    assertThat(contexts).doesNotHaveDuplicates();
    assertThat(contexts.getFirst()).isEqualTo(TRACE);
    var probes =
        output
            .getOut()
            .lines()
            .filter(
                line ->
                    line.startsWith("{") && line.contains("\"message\":\"request_context_probe\""))
            .map(mapper::readTree)
            .toList();
    assertThat(probes).hasSize(5);
    for (int i = 0; i < probes.size(); i++) {
      assertThat(probes.get(i).get("traceId").asText()).isEqualTo(contexts.get(i));
      assertThat(probes.get(i).get("correlationId").asText()).isEqualTo(contexts.get(i));
    }
    var events = events(output);
    for (int i = 0; i < events.size(); i++) {
      var event = events.get(i);
      assertThat(event.get("status").asInt()).isEqualTo(409);
      assertThat(event.get("method").asText()).isEqualTo("POST");
      assertThat(event.get("path").asText()).isEqualTo("/pedidos");
      assertThat(event.get("durationMs").asLong()).isNotNegative();
      assertThat(event.get("businessCode").asText()).isEqualTo("ESTOQUE_INSUFICIENTE");
      assertThat(event.get("correlationId").asText()).isEqualTo(contexts.get(i));
      assertThat(event.get("traceId").asText()).isEqualTo(contexts.get(i));
      assertThat(event.get("spanId").asText()).matches("[a-f0-9]{16}");
      assertThat(event.has("stack_trace")).isFalse();
    }
    assertThat(output.getOut())
        .doesNotContain("sensitive-token", "private@example.com", "produtoId");
  }

  @Test
  void sucessoValidacaoEFalhaTratada(CapturedOutput output) throws Exception {
    when(useCase.executar(any()))
        .thenReturn(new CriarPedidoUseCase.Resultado(java.util.UUID.randomUUID(), "CRIADO"))
        .thenThrow(new IllegalStateException("falha simulada"));
    try (var client = HttpClient.newHttpClient()) {
      assertThat(
              client
                  .send(request(BODY).build(), HttpResponse.BodyHandlers.discarding())
                  .statusCode())
          .isEqualTo(201);
      assertThat(
              client
                  .send(request("{\"itens\":[]}").build(), HttpResponse.BodyHandlers.discarding())
                  .statusCode())
          .isEqualTo(400);
      assertThat(
              client
                  .send(request(BODY).build(), HttpResponse.BodyHandlers.discarding())
                  .statusCode())
          .isEqualTo(500);
    }
    await().untilAsserted(() -> assertThat(events(output)).hasSize(3));
    assertThat(events(output).stream().map(e -> e.get("status").asInt()))
        .containsExactly(201, 400, 500);
    assertThat(events(output))
        .allSatisfy(e -> assertThat(e.get("businessCode").asText()).isEqualTo("NONE"));
  }

  private HttpRequest.Builder request(String body) {
    return HttpRequest.newBuilder(
            URI.create("http://localhost:" + port + "/pedidos?email=private@example.com"))
        .header("Content-Type", "application/json")
        .header("Authorization", "Bearer sensitive-token")
        .POST(HttpRequest.BodyPublishers.ofString(body));
  }

  private List<JsonNode> events(CapturedOutput output) {
    return output
        .getOut()
        .lines()
        .filter(
            line -> line.startsWith("{") && line.contains("\"event\":\"http_request_completed\""))
        .map(mapper::readTree)
        .toList();
  }
}
