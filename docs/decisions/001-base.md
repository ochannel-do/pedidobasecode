# ADR 001 — Base inicial

Status: aceita. Data: 2026-10-01.

- Monólito modular em Maven único: reduz operação e mantém limites verificáveis por ArchUnit. Extração para serviços exigiria nova decisão.
- Core puro: domínio e aplicação usam Java; frameworks são detalhes de infraestrutura.
- Portas entre módulos: integração consome contratos públicos, sem compartilhamento de entidades; falhas são traduzidas.
- Transação compartilhada: reserva e pedido usam PostgreSQL e o mesmo transaction manager. Atomicidade depende desse acoplamento intencional; não usar transação independente de reserva.
- Concorrência: update condicional atômico e ordem fixa de produtos. Sem leitura seguida de gravação vulnerável. Insuficiência reverte todos os itens.
- PostgreSQL e Testcontainers: comportamento real de migrations, constraints, locking, commit e rollback, sem H2.
- OpenTelemetry/OTLP e Jaeger: exportação direta, observações de infraestrutura e captura em memória nos testes, sem Collector obrigatório.
- Versões: Java 25 e Spring Boot 4.1.1 estável confirmado no Maven Central. Dependências gerenciadas pelo Boot; ArchUnit e Spotless fixados explicitamente.
- CI: GitHub Actions é o exemplo inicial porque não existe remoto configurado; adaptar ao destino se necessário.

Referências: https://docs.spring.io/spring-boot/4.1/reference/actuator/tracing.html e https://docs.spring.io/spring-framework/reference/data-access/transaction/programmatic.html.
