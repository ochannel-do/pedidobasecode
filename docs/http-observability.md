# Diagnóstico e rastreamento de HTTP 409

## Causa encontrada no código

`POST /pedidos` chama a porta decorada (`pedidos.criar`), a transação e o serviço.
Uma reserva indisponível é traduzida por `EstoqueModuleAdapter` para
`EstoqueInsuficienteException`; a transação reverte e `ApiExceptionHandler` produz
409 com `ESTOQUE_INSUFICIENTE`. Antes desta alteração, esse handler não emitia log.
O decorador só emitia INFO no sucesso. Observations/spans não são logs de acesso;
o projeto não configurava um registro de conclusão HTTP. Portanto, as cinco
respostas podiam ocorrer sem nenhuma linha contendo 409, estoque ou /pedidos.
Não há evidência de falha do Grafana nessa ausência.

Logs de inicialização, encerramento e exportadores ocorrem fora de requisições e
normalmente não possuem traceId. O resultado examinado com 0% de traceId não
demonstra, sozinho, falha de propagação. A amostragem de spans padrão é 10%, mas
não determina se o evento HTTP é registrado. Port-forward não passa pelo Ingress,
logo seus access logs não podem provar essas chamadas.

Não há manifests de Kubernetes, configuração de Alloy/Promtail, Loki ou Grafana
no repositório. As configurações efetivas de HML, níveis de log, overrides,
pipelines de coleta e retenção precisam ser conferidas no ambiente. Esta
investigação e os testes locais não consultaram o cluster nem o Grafana.

## Implementação e formato

O filtro de infraestrutura executa dentro do escopo de `ServerHttpObservationFilter`
(ordem HIGHEST_PRECEDENCE + 1 no Boot 4.1.1; filtro de logs + 2). Em `finally`,
depois da resolução das exceções pelo MVC, emite um INFO `http_request_completed`.
Chamadas a `/actuator/health` e seus subcaminhos (incluindo liveness/readiness)
não passam por esse filtro de logs, para evitar ruído das probes do Kubernetes.
A exclusão respeita o context path e o limite do segmento: `/actuator/healthcheck`
continua sendo registrado. O health check e a instrumentação do Spring continuam
executando normalmente; a exclusão vale para qualquer status desses endpoints.
O handler apenas anota o código de negócio em atributo da requisição. Assim há
um evento de conclusão por conflito, sem log adicional de exceção ou stack trace.
O contrato ProblemDetail permanece igual; a resposta ganha `X-Correlation-Id`.

O filtro reutiliza o trace ativo do Micrometer. A propagação W3C `traceparent` e
o MDC traceId/spanId continuam sob responsabilidade da instrumentação existente.
`correlationId` fica no MDC durante a requisição, inclusive nos logs de negócio,
e é restaurado/removido em `finally`. O filtro não altera traceId/spanId nem cria
um segundo span. Sem tracer/span ativo, gera um UUID local, sem aceitar um
X-Correlation-Id arbitrário do cliente. O identificador de correlação retornado
permite achar a linha mesmo sem span exportado. TraceId identifica um trace,
que pode conter mais de uma chamada HTTP se o cliente reutilizar traceparent;
spanId identifica o span HTTP dentro desse trace.

O caminho registrado é o template MVC (`/pedidos`, ou `/pedidos/{id}` se existir
no futuro); rotas não reconhecidas usam `UNMATCHED`. Não se registram query
strings, valores de segmentos variáveis, corpos, headers de autenticação,
credenciais, IPs ou IDs de pedidos. Duração em milissegundos usa relógio monotônico.
Exceções tratadas preservam o status final; exceções que escapam ao MVC são
registradas como 500 e relançadas. O fluxo atual é síncrono: antes de introduzir
MVC assíncrono/streaming será necessário adaptar o registro para sua conclusão.

`logging.structured.format.console: logstash` usa o suporte nativo do Boot,
sem nova dependência. Os campos MDC e os pares estruturados SLF4J ficam no JSON
de stdout, uma linha por evento. O teste com Tomcat real capturou, por exemplo:

```json
{"@timestamp":"2026-10-18T10:16:56.591699-03:00","@version":"1","message":"HTTP request completed","logger_name":"com.exemplo.loja.pedidos.infrastructure.web.HttpRequestLoggingFilter","thread_name":"http-nio-auto-1-exec-1","level":"INFO","level_value":20000,"traceId":"0123456789abcdef0123456789abcdef","spanId":"63f89ff012a2817d","correlationId":"0123456789abcdef0123456789abcdef","event":"http_request_completed","method":"POST","path":"/pedidos","status":409,"durationMs":11,"businessCode":"ESTOQUE_INSUFICIENTE"}
```

Garanta INFO habilitado para o filtro. A coleta deve preservar a linha JSON
completa após remover o envelope CRI/container; revise regras de multiline
baseadas no antigo prefixo de texto. Não promova traceId, spanId, correlationId
ou IDs de pedidos a labels indexadas do Loki. Mantenha labels de infraestrutura
de baixa cardinalidade (namespace, aplicação, pod/container conforme política).
Os parsers abaixo extraem campos apenas durante a consulta.

## Consultas LogQL

Acrescente os labels reais da aplicação/container ao seletor se houver mais
aplicações no namespace. Todas as consultas pressupõem JSON direto na linha;
se houver envelope JSON externo, ajuste a extração ao formato efetivo do coletor.
Os aliases `http_event` e `http_status` evitam colisões com labels de infraestrutura.

Listar respostas 409:

```logql
{namespace="pedidobasecode-hml"}
| json http_event="event", http_status="status"
| __error__="" | http_event="http_request_completed" | http_status="409"
```

Filtrar o motivo de negócio:

```logql
{namespace="pedidobasecode-hml"}
| json http_event="event", http_status="status", business_code="businessCode"
| __error__="" | http_event="http_request_completed" | http_status="409"
| business_code="ESTOQUE_INSUFICIENTE"
```

Seguir todos os logs da requisição (substitua pelo X-Correlation-Id recebido):

```logql
{namespace="pedidobasecode-hml"}
| json request_correlation="correlationId"
| __error__="" | request_correlation="0123456789abcdef0123456789abcdef"
```

Para incluir logs da instrumentação que tenham somente traceId, use
`| json request_trace="traceId" | __error__="" | request_trace="<traceId>"`.
Não restrinja essa busca ao evento de conclusão. Startup e tarefas em background
continuam sem correlação HTTP. Um conflito pode ter somente o evento de conclusão;
spans `pedidos.criar`/`estoque.reservar` continuam disponíveis conforme amostragem.

Contar respostas 409 em janelas móveis de cinco minutos (Time series):

```logql
sum(count_over_time(
  {namespace="pedidobasecode-hml"}
  | json http_event="event", http_status="status"
  | __error__="" | http_event="http_request_completed" | http_status="409" [5m]
))
```

A extração seletiva evita criar séries por trace/span/correlationId na agregação.
Para um total no intervalo selecionado, use consulta Instant com `[$__range]`
no lugar de `[5m]`. Sem eventos, Loki pode retornar ausência de série em vez de zero.

## Validação em HML, após deploy autorizado

1. Confirmar a revisão/imagem implantada, INFO do filtro e formato JSON em stdout.
   Verificar overrides de `LOGGING_STRUCTURED_FORMAT_CONSOLE`, `LOGGING_CONFIG`,
   níveis e tracing. Escolher o pod/container correto, sem depender do Ingress.
2. Usar o mesmo pedido sintético que já retornou insuficiência, com um produto
   confirmado sem estoque/ausente em HML. Não executar seed nem alterar estoque
   em HML para este teste. Salvar esse JSON localmente em `pedido-insuficiente.json`.
   Se o cenário mudou e retornar 201, interromper e rever o produto de teste.
3. Em um terminal, substituir os placeholders de pod/container/porta pelos reais:

```sh
kubectl -n pedidobasecode-hml get pods
kubectl -n pedidobasecode-hml port-forward pod/<pod> 18080:8080
```

4. Em outro terminal, guardar o início da janela e fazer cinco chamadas sem
   traceparent (cada uma deve gerar correlação nova):

```sh
date -u
for i in 1 2 3 4 5; do
  status=$(curl -sS -D "/tmp/pedido-hml-$i.headers" \
    -o "/tmp/pedido-hml-$i.response.json" -w '%{http_code}' \
    -H 'Content-Type: application/json' \
    --data-binary @pedido-insuficiente.json http://localhost:18080/pedidos)
  echo "chamada=$i status=$status"
  rg -i '^X-Correlation-Id:' "/tmp/pedido-hml-$i.headers"
  test "$status" = 409 || break
done
date -u
kubectl -n pedidobasecode-hml logs <pod> -c <container> --since=10m \
  | rg 'http_request_completed'
```

5. Conferir cinco eventos POST /pedidos, status 409 e
   businessCode ESTOQUE_INSUFICIENTE, com duração não negativa e os cinco IDs
   dos headers. Conferir o corpo ProblemDetail atual. Nenhum stack trace é
   esperado para esses conflitos. Requisições concorrentes de outros usuários
   podem aumentar o total geral: comparar os cinco IDs, pod e janela exata.
6. No Explore, selecionar a fonte Loki correta e a janela que inclui os horários
   acima; aguardar a latência normal da coleta. Executar as quatro consultas,
   inspecionar campos JSON e comparar IDs com stdout. Um Instant total na janela
   isolada deve ser cinco; janelas móveis podem repetir a contagem em vários pontos.
   Se stdout tiver os eventos e Loki não, investigar coleta, filtros de nível,
   parsing/multiline, labels, tenant/datasource e horário. Se stdout não tiver,
   conferir imagem e configuração da aplicação antes de investigar Grafana.

## Avisos OTLP de métricas: diagnóstico independente

`spring-boot-starter-opentelemetry` traz o registry OTLP de métricas. A
auto-configuração habilita exportação por padrão
(`management.otlp.metrics.export.enabled=true`). Sem URL configurada, o registry
usa `http://localhost:4318/v1/metrics`. O projeto configura apenas traces:
`OTLP_ENDPOINT` alimenta `management.opentelemetry.tracing.export.otlp.endpoint`,
cujo default é localhost:4318/v1/traces. Isso não configura o endpoint de métricas.
No Kubernetes, localhost é o próprio pod: só funciona se houver um collector
escutando ali. O Compose também aponta apenas traces ao Jaeger.

O aviso é compatível com exportação de métricas habilitada sem receptor local;
confirmar o destino/configuração efetiva de HML para fechar esse diagnóstico.
Esse envio em background não cria logs de acesso nem impede stdout de chegar ao
Loki, e não explica a ausência dos logs de 409 que o código não emitia.

Nenhum comportamento de exportação foi alterado nesta correção. Separadamente,
se HML não coleta métricas OTLP, configurar
`MANAGEMENT_OTLP_METRICS_EXPORT_ENABLED=false`. Se coleta, configurar
`MANAGEMENT_OTLP_METRICS_EXPORT_URL=http://<collector>:4318/v1/metrics` com o
endereço real e receptor de métricas habilitado. Revisar também overrides OTEL
e de configuração externa do Boot. Não desabilitar tracing para suprimir avisos
de métricas. Os testes HTTP desabilitam apenas exportadores externos, mantendo
tracing ativo, inclusive com sampling 0%.

Referências: [logging estruturado do Spring Boot](https://docs.spring.io/spring-boot/4.1/reference/features/logging.html),
[LogQL](https://grafana.com/docs/loki/latest/query/),
[parsers e filtros](https://grafana.com/docs/loki/latest/query/log_queries/).
