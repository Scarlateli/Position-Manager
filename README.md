# Position Manager

Sistema de gestão de posições financeiras inspirado no [Nasdaq Calypso](https://www.nasdaq.com/solutions/calypso-technology), software usado por bancos para gestão de risco e posições.

> ⚠️ **Status:** Em desenvolvimento — Sprint 3 de 9 concluída

## Visão do Projeto

**Objetivo final:** Sistema completo de gestão de trades e posições com:
- Cadastro de instrumentos financeiros
- Registro e lifecycle de trades
- Cálculo de posições agregadas por instrumento
- Eventos assíncronos (RabbitMQ)
- Cálculo de P&L (Profit & Loss)
- Cobertura de testes

## Por que esse projeto existe

Em vez de fazer mais um CRUD genérico de todo-list, decidi construir algo que reflete o que bancos e fintechs realmente usam: um sistema de posições financeiras.

O projeto implementa conceitos reais do mercado:
- Cadastro de instrumentos financeiros (ações, bonds, derivativos)
- Registro de trades com cálculo de posição
- Soft delete (padrão em sistemas financeiros onde nada é realmente deletado)
- Arquitetura em camadas que escala

## Stack

```
Java 17 + Spring Boot 3.2 + Spring Data JPA
PostgreSQL (Docker)
Maven
```

Planejado para as próximas semanas: RabbitMQ para eventos de trade, testes com JUnit 5 + Mockito.

## Rodando localmente

```bash
# sobe o postgres
docker-compose up -d

# roda a aplicação
./mvnw spring-boot:run
```

API disponível em `http://localhost:8080/api/v1`  
Swagger em `http://localhost:8080/swagger-ui.html`

## API

### Instrumentos

```
GET    /api/v1/instruments             → lista ativos
GET    /api/v1/instruments/{id}        → busca por id
POST   /api/v1/instruments             → cria novo
DELETE /api/v1/instruments/{id}        → soft delete (desativa)
PATCH  /api/v1/instruments/{id}/reactivate → reativa instrumento
```

Exemplo de payload:
```json
{
  "ticker": "PETR4",
  "name": "Petrobras PN",
  "type": "STOCK",
  "currency": "BRL"
}
```

### Trades

```
POST   /api/v1/trades           → cria novo trade
PATCH  /api/v1/trades/{id}/settle → liquida trade
PATCH  /api/v1/trades/{id}/cancel → cancela trade
```

Exemplo de payload:
```json
{
  "instrumentId": 1,
  "tradeDate": "2026-02-19",
  "direction": "BUY",
  "quantity": 100,
  "price": 25.50,
  "counterparty": "Banco XYZ"
}
```

**Regras de negócio:**
- Não é permitido criar trades em instrumentos inativos
- Settlement Date é calculado automaticamente (Trade Date + 2 dias úteis)
- Trade liquidado (`SETTLED`) não pode ser cancelado
- Trade cancelado (`CANCELLED`) não pode ser liquidado

## Estrutura

```
src/main/java/com/trading/position_manager/
├── controller/     # endpoints REST
├── service/        # regras de negócio
├── repository/     # acesso a dados
├── model/          # entidades JPA e enums
├── dto/            # objetos de transferência (request/response)
└── exception/      # tratamento global de erros
```

> **Nota:** Todos os pacotes devem estar dentro de `com.trading.position_manager` para o Spring Boot realizar o component scan corretamente.

## Roadmap (9 Sprints)

### Concluídas ✅
- **Sprint 1:** Setup inicial + Docker + PostgreSQL
- **Sprint 2:** Entidade Instrument com CRUD e soft delete
- **Sprint 3:** Entidade Trade + relacionamentos + regras de negócio (D+2, validações, estados)

### Em andamento / Próximas 🚧
- **Sprint 4:** Cálculo de posições (quantidade líquida por instrumento)
- **Sprint 5:** Eventos assíncronos com RabbitMQ
- **Sprint 6:** Testes unitários (JUnit 5 + Mockito)
- **Sprint 7:** Testes de integração
- **Sprint 8:** P&L básico (Profit & Loss)
- **Sprint 9:** Refatoração e documentação final

## Decisões técnicas

**Por que soft delete?**  
Em sistemas financeiros, você nunca deleta dados. Auditoria, compliance, e a possibilidade de reverter operações exigem que tudo seja mantido. O campo `active` permite "deletar" sem perder histórico.

**Por que separar DTO do Model?**  
A entidade JPA tem campos que o cliente não precisa ver (timestamps, flags internas). O DTO expõe apenas o necessário e permite evoluir a API sem quebrar o banco.

**Por que não usar `@Data` na entidade?**  
Lombok `@Data` gera `equals/hashCode` baseado em todos os campos, o que quebra com JPA quando a entidade ainda não foi persistida. Usamos `@Getter/@Setter` + implementação manual de `equals/hashCode` baseado apenas no ID:

```java
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Instrument that = (Instrument) o;
    return id != null && Objects.equals(id, that.id);
}

@Override
public int hashCode() {
    return getClass().hashCode();
}
```

Essa implementação garante que entidades não persistidas (id = null) nunca sejam consideradas iguais, e o hashCode constante evita problemas com collections quando o ID muda após persist.

---

## Progresso

```
Sprint 1  ██████████ Concluída
Sprint 2  ██████████ Concluída
Sprint 3  ██████████ Concluída
Sprint 4  ░░░░░░░░░░ Próxima
Sprint 5  ░░░░░░░░░░
Sprint 6  ░░░░░░░░░░
Sprint 7  ░░░░░░░░░░
Sprint 8  ░░░░░░░░░░
Sprint 9  ░░░░░░░░░░
```

Projeto em desenvolvimento ativo. Acompanhe o progresso nos commits.
