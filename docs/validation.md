# Validação da implementação

Executada em 2026-10-01, com JDK 25 e Docker Desktop.

- `./mvnw verify`: 21 testes, zero falhas, zero erros e zero ignorados. Inclui Spotless, domínio, aplicação, HTTP, ArchUnit e seis cenários de integração com PostgreSQL 17.6/Flyway.
- Reserva de múltiplos itens, rollback por insuficiência, rollback após escrita de pedido seguida de falha e duas transações disputando uma unidade: aprovados.
- Teste de tracing captura spans em memória, verifica trace e parent span IDs e confirma ausência de exportador OTLP HTTP no contexto.
- Compose validado e Dockerfile compilado/executado com JDK/JRE 25.
- HTTP real: `/actuator/health` responde UP e `POST /pedidos` com dois itens responde 201.
- Jaeger local confirmou `http post /pedidos` → `pedidos.criar` → `estoque.reservar` e `pedidos.persistir` no mesmo trace.

CI foi configurado para GitHub Actions, mas sua execução remota depende de publicar o projeto em um repositório. Autenticação e infraestrutura de produção permanecem fora do escopo da especificação.
