# Validação da implementação

## Exclusão de probes dos logs HTTP — 2026-10-18

- RED: `./mvnw spotless:apply test -Dtest=HttpRequestLoggingFilterTest` falhou no novo cenário porque as quatro chamadas de health ainda emitiam eventos de conclusão.
- GREEN: `./mvnw spotless:apply clean verify` aprovou os testes Surefire, Spotless e os limites de cobertura. O novo cenário confirma a exclusão de health/liveness/readiness, inclusive com context path, preservando a execução da cadeia HTTP e os logs de `/pedidos` e `/actuator/healthcheck`.
- A fase de integração continua impedida pela indisponibilidade do Docker/Testcontainers; nenhum deploy ou push foi realizado.

## Observabilidade HTTP — 2026-10-18

- JDK Temurin 25 sem preview, obtido para validação local; versão Java do projeto preservada.
- `./mvnw spotless:apply` e verificação de formato aprovadas.
- `./mvnw verify`: os 18 testes da fase Surefire passaram; os seis cenários de `PedidoFlowIT` falharam na criação do contexto porque o daemon Docker está indisponível. Integração PostgreSQL/Testcontainers permanece pendente, sem substituição por H2 ou skip silencioso.
- Tomcat real de teste, sem banco e com a porta de negócio mockada: cinco POST retornaram 409 com ProblemDetail preservado e cinco eventos JSON de conclusão, sem stack trace. Verificados status, método, caminho, duração, código de negócio, header de correlação e MDC.
- Propagação de traceparent W3C e logs correlacionados validadas com sampling 0%; requisições independentes receberam IDs distintos no mesmo worker HTTP. Cobertura adicional de 201, 400, 500 tratado, fallback sem tracer, caminho não reconhecido e restauração/limpeza de MDC inclusive após exceção.
- Captura do stdout confirmou o formato JSON Logstash efetivo e ausência de corpo, query string e token enviados pelo teste. A árvore de dependências confirmou `micrometer-registry-otlp:1.17.1` vindo do starter OpenTelemetry; endpoint padrão de métricas é independente do endpoint de traces.
- Nenhum deploy ou acesso ao cluster/Grafana foi realizado. Coleta efetiva, consultas LogQL e cinco chamadas via port-forward dependem de validação em HML após deploy autorizado, conforme [roteiro](http-observability.md).

## Validação inicial

Executada em 2026-10-01, com JDK 25 e Docker Desktop.

- `./mvnw verify`: 21 testes, zero falhas, zero erros e zero ignorados. Inclui Spotless, domínio, aplicação, HTTP, ArchUnit e seis cenários de integração com PostgreSQL 17.6/Flyway.
- Reserva de múltiplos itens, rollback por insuficiência, rollback após escrita de pedido seguida de falha e duas transações disputando uma unidade: aprovados.
- Teste de tracing captura spans em memória, verifica trace e parent span IDs e confirma ausência de exportador OTLP HTTP no contexto.
- Compose validado e Dockerfile compilado/executado com JDK/JRE 25.
- HTTP real: `/actuator/health` responde UP e `POST /pedidos` com dois itens responde 201.
- Jaeger local confirmou `http post /pedidos` → `pedidos.criar` → `estoque.reservar` e `pedidos.persistir` no mesmo trace.

CI foi configurado para GitHub Actions, mas sua execução remota depende de publicar o projeto em um repositório. Autenticação e infraestrutura de produção permanecem fora do escopo da especificação.
