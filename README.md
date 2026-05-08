# Position Manager

A trade & position management system inspired by what banks actually run on — specifically [Nasdaq Calypso](https://www.nasdaq.com/solutions/calypso-technology), the platform several major banks use for risk and position management.

> 🚧 **Status:** Sprint 3 of 9 shipped · Sprint 4 in progress · **PRs welcome**

[![Java](https://img.shields.io/badge/Java-17-orange)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green)]()
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)]()
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

## Why this exists

Most personal projects are yet another to-do CRUD. This one models real concepts that banks and brokers use:

- **Instruments** (stocks, bonds, derivatives) with soft delete — because financial data is never actually deleted (audit, compliance, reactivation)
- **Trades** with a real lifecycle (`PENDING → SETTLED → CANCELLED`) and **T+2 settlement** following the B3 (Brazilian exchange) rule since 2019
- **Positions** as a CQRS-style projection: trades are the write model, positions are the read model, recalculated atomically on settle
- **Weighted Average Cost (WAC)** — `Σ(buy_qty × buy_price) / Σ(buy_qty)` over BUY trades only

Built to learn engineering of financial systems out in the open. Better with you.

## Stack

```
Java 17 + Spring Boot 3.2 + Spring Data JPA
PostgreSQL 16 (Docker)
Maven · JUnit 5 (coming Sprint 6)
```

Coming next: RabbitMQ for trade events (Sprint 5), full test coverage (Sprints 6–7), P&L calculation (Sprint 8).

## Run locally

```bash
# Start PostgreSQL
docker-compose up -d

# Run the application
./mvnw spring-boot:run
```

API at `http://localhost:8080/api/v1`
Swagger UI at `http://localhost:8080/swagger-ui.html`

## API

### Instruments

```
GET    /api/v1/instruments                   → list active
GET    /api/v1/instruments/{id}              → get by id
POST   /api/v1/instruments                   → create
DELETE /api/v1/instruments/{id}              → soft delete
PATCH  /api/v1/instruments/{id}/reactivate   → reactivate
```

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
POST   /api/v1/trades              → create
PATCH  /api/v1/trades/{id}/settle  → settle (transitions to SETTLED, triggers position recalc)
PATCH  /api/v1/trades/{id}/cancel  → cancel
```

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

**Business rules:**
- Trades cannot be created on inactive instruments
- `settlementDate` is auto-calculated: `tradeDate + 2 days` (B3 rule, since 2019)
- A `SETTLED` trade cannot be cancelled
- A `CANCELLED` trade cannot be settled

## Architecture

```
src/main/java/com/trading/position_manager/
├── controller/   # REST endpoints
├── service/      # business logic
├── repository/   # data access (Spring Data JPA)
├── model/        # JPA entities + enums
├── dto/          # request/response payloads
└── exception/    # global error handling (@RestControllerAdvice)
```

Standard layered architecture: `Controller → Service → Repository → Model`. Two domain exceptions drive all error responses — `BusinessException` (HTTP 400) and `ResourceNotFoundException` (HTTP 404).

> 📖 See [`CLAUDE.md`](CLAUDE.md) for project conventions, and [`HELP.md`](HELP.md) for sprint-by-sprint learning notes (lots of detail on JPA, transactions, CQRS, and WAC math).

## Roadmap

| Sprint | Scope | Status |
|---|---|---|
| 1 | Setup + Docker + PostgreSQL | ✅ Done |
| 2 | Instrument · CRUD · soft delete | ✅ Done |
| 3 | Trade · relationships · D+2 rules | ✅ Done |
| 4 | **Position aggregation · WAC math** | 🚧 In progress |
| 5 | Async events with RabbitMQ | ⬜ Todo |
| 6 | Unit tests (JUnit 5 + Mockito) | ⬜ Todo |
| 7 | Integration tests | ⬜ Todo |
| 8 | P&L calculation (realized / unrealized) | ⬜ Todo |
| 9 | Refactor · Auth (Spring Security + JWT) | ⬜ Todo |

## Contributing

**Sprint 4 is open. Any PR is welcome.** Whether you're new to Spring or you build fintech for a living, there's a piece for you.

### Where to start

1. **Browse [`good first issue`](https://github.com/Scarlateli/Position-Manager/issues?q=is%3Aopen+is%3Aissue+label%3A%22good+first+issue%22) issues** — small, scoped, well-described. Most can be done in 30–60 minutes.
2. **Check [`help wanted`](https://github.com/Scarlateli/Position-Manager/issues?q=is%3Aopen+is%3Aissue+label%3A%22help+wanted%22)** for meatier work (PositionService with WAC math, async events with RabbitMQ).
3. **Read [`CLAUDE.md`](CLAUDE.md)** before touching code — it documents project-wide conventions (Lombok rules, BigDecimal precision, FetchType strategy, response DTO policy). Following these saves review cycles.

### Workflow

1. Comment on the issue saying you'd like to take it (avoids two people doing the same thing)
2. Fork and create a branch named `sprint-4/<short-description>` or `fix/<short-description>`
3. Make your changes following the conventions in `CLAUDE.md`
4. Open a PR referencing the issue (`Closes #X`)
5. Reviews land within 24–48h

### Discussion

If you're unsure about an approach, **open a draft PR or comment on the issue** before going deep. It's far better to align on direction in 5 minutes than to spend hours on something that needs reshaping.

## Why these technical choices?

**Why soft delete?**
In financial systems, you never delete data. Audit, compliance, and the possibility of reverting operations require everything to be kept. The `active` flag lets you "delete" without losing history.

**Why separate DTO from Model?**
The JPA entity has fields the client doesn't need (timestamps, internal flags). The DTO exposes only what's necessary and lets you evolve the API without breaking the database schema.

**Why not `@Data` on entities?**
Lombok's `@Data` generates `equals`/`hashCode` based on all fields, which breaks JPA when the entity hasn't been persisted yet (`id = null`) and again when the ID changes after `save()`. We use `@Getter` + `@Setter` + manual `equals`/`hashCode` based on ID only:

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

**Why CQRS-in-miniature for positions?**
Trades are the write model (source of truth, audited). Positions are the read model / projection (fast O(1) reads). Sprint 4 starts with synchronous recalculation inside the settle transaction; Sprint 5 will publish a `TradeSettledEvent` via RabbitMQ for eventual consistency. This is *the* reason RabbitMQ is the next sprint, not a parallel feature.

## License

[MIT](LICENSE) — use it, fork it, learn from it.

---

🇧🇷 *Para a versão em português, veja o [README anterior no histórico do git](https://github.com/Scarlateli/Position-Manager/blob/main/README.md).*
