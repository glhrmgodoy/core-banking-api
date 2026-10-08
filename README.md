# Core Banking API (PIX)

API de core bancário com clientes, contas, ledger imutável e transferências PIX com idempotência.

## Stack

Java 21 · Spring Boot 4.1 · PostgreSQL 16 · Flyway · MapStruct · springdoc-openapi · Testcontainers · Spring Cloud AWS · Amazon Cognito

## Como rodar

Pré-requisitos: JDK 21 e Docker em execução.

Crie o arquivo `.env` a partir do modelo e ajuste as credenciais se quiser:

```bash
cp .env.example .env
```

Suba a aplicação:

```bash
./mvnw spring-boot:run
```

O Spring Boot sobe o Postgres do `compose.yaml` automaticamente.

- Swagger UI: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health

## Testes

```bash
./mvnw verify
```
