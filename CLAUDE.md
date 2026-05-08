# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Start the database (required before running the app)
docker-compose up -d

# Run the application
./mvnw spring-boot:run

# Build (skip tests)
./mvnw clean package -DskipTests

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=PositionManagerApplicationTests

# Stop the database
docker-compose down
```

API runs at `http://localhost:8080/api/v1`
Swagger UI at `http://localhost:8080/swagger-ui.html`

## Architecture

Standard Spring Boot layered architecture: `Controller → Service → Repository → Model`.

**Base package:** `com.trading.position_manager` — note the underscore. Not `positionmanager`, not `positionManager`. All new classes must respect this.

**Request flow:** Controllers receive request DTOs with Bean Validation (`@Valid`), pass them to Services which contain all business logic, Services use Repositories (Spring Data JPA) to persist JPA entities. Response strategy evolves by sprint — see the "Response DTO policy" section below.

**Exception strategy:** Two domain exceptions drive all error responses — `BusinessException` (→ HTTP 400) for violated business rules, `ResourceNotFoundException` (→ HTTP 404) for missing entities. `GlobalExceptionHandler` uses `@RestControllerAdvice`. **Never add a generic `RuntimeException` or `Exception` handler** — it breaks Springdoc's internal exception flow and causes 500 errors on `/v3/api-docs`.

**Database:** PostgreSQL via Docker (`positiondb`, user `position_user`, password `position_pass`). Schema managed by Hibernate `ddl-auto: update`.

## Entities

- **`Instrument`** — financial instrument (stock, bond, derivative). Has soft delete via `active` boolean; reactivation is supported. Ticker is unique. Never hard-deleted — the `DELETE /api/v1/instruments/{id}` endpoint sets `active = false`.
- **`Trade`** — a buy/sell operation on an instrument. States: `PENDING → SETTLED` or `PENDING → CANCELLED`. Settlement date is auto-calculated to T+2 in `@PrePersist` (T+2 is the Brazilian B3 exchange rule since May 2019, when B3 migrated from T+3; do not change to T+1 or T+3 regardless of other markets). `SETTLED` trades cannot be cancelled and vice versa. Uses `@ManyToOne(fetch = FetchType.EAGER)` on `instrument` because the controller returns the entity directly (see Response DTO policy).
- **`Position`** — **partially implemented (Sprint 4 in progress, SCRUM-39)**. Currently contains class-level annotations, `id` field, and `instrument` field (`@ManyToOne` with `FetchType.LAZY`). Remaining fields to add in SCRUM-39: `positionDate` (LocalDate), `quantity` (BigDecimal 18,4), `averagePrice` (BigDecimal 18,6), `createdAt` (LocalDateTime), `updatedAt` (LocalDateTime). `marketValue` and `unrealizedPnL` are intentionally **deferred to Sprint 8** (P&L calculation) — adding them now would leave dead/null columns for several sprints. Uses `FetchType.LAZY` because serialization will be controlled by `PositionResponseDTO` within the transaction — LAZY only works safely when a DTO mediates the response.

## Conventions

These are project-wide rules. Follow them in all new code.

**BigDecimal precision for monetary/quantity fields:**
- Quantities: `@Column(precision = 18, scale = 4)`
- Prices: `@Column(precision = 18, scale = 6)`
- Monetary values (marketValue, P&L, etc.): `@Column(precision = 18, scale = 2)`
- Rounding: always `RoundingMode.HALF_UP` (financial standard).

**Lombok on entities:** Use `@Getter`/`@Setter`/`@Builder`/`@NoArgsConstructor`/`@AllArgsConstructor` — **never `@Data`**. `@Data` generates `equals`/`hashCode` on all fields, which breaks JPA collections when the `id` changes after `save()`. `Instrument` has a manual `equals`/`hashCode` keyed only on `id` (with `id != null` guard) and a fixed `hashCode()` return. Apply this same pattern to any new entity that will live in collections or be compared.

**FetchType strategy:**
- Use `FetchType.EAGER` when the controller returns the entity directly (current Trade case) — avoids `LazyInitializationException` during JSON serialization.
- Use `FetchType.LAZY` when a response DTO mediates serialization (current Position case) — the DTO accesses the related fields while the transaction is still open.
- **Default preference going forward: LAZY + DTO.** EAGER is a legacy from Sprint 3 before DTOs were introduced, not a pattern to replicate.

**Static imports (convention adopted from Sprint 4 onward):** New entities should use `import static jakarta.persistence.FetchType.*;` to write `LAZY`/`EAGER` without the `FetchType.` prefix (see `Position.java`). Trade.java predates this convention and uses the explicit `FetchType.EAGER` form — do not refactor it just for consistency. This differs from the community-standard explicit form; the choice is deliberate for new code.

**Column annotations:** Always set `nullable = false` at the JPA level for non-optional fields AND enforce the same constraint at the DTO level via Bean Validation (`@NotNull`). Defense in depth — the DB catches bugs even if validation is bypassed. **Note:** `Trade.java` predates this convention and is missing `nullable = false` on `direction`, `quantity`, `price`, and `status`. It is considered pre-convention legacy; do not refactor unless there is a real bug. All new entities (starting with `Position`) must follow the rule.

**ID generation:** Use `GenerationType.IDENTITY` for PostgreSQL. Do not use `AUTO` (delegates to Hibernate heuristics) or `SEQUENCE` (requires extra configuration and is only worth it for batch inserts, which this project doesn't do yet).

## Response DTO policy

- Sprints 1-3 (Instrument, Trade): Controllers return entities directly. This is **legacy behavior, not the target pattern.**
- Sprint 4 onward (Position and beyond): Controllers must return response DTOs. Reasons:
    1. Prevents exposing internal DB structure.
    2. Avoids Jackson circular references on `@ManyToOne` relationships.
    3. Allows API evolution without breaking clients.
    4. Enables `FetchType.LAZY` on entities safely.

When generating new code for any entity beyond Sprint 3, **always include a `{Entity}ResponseDTO`** and map from entity to DTO inside the service layer.

## Position recalculation strategy

Positions are **persisted** (a real row in the `positions` table per `instrument_id`), not derived on every read. Recalculation timing:

- **Sprint 4 (current): synchronous, inside the settle transaction.** After `TradeService.settle(id)` updates a trade to `SETTLED`, it calls `PositionService.recalculate(instrumentId)` in the same `@Transactional` boundary. If recalculation fails, the settle is rolled back — a trade is never `SETTLED` without its position reflecting it.
- **Sprint 5: async via RabbitMQ.** The settle handler will publish a `TradeSettledEvent`; a listener consumes it and recalculates. Trade-offs (eventual consistency vs. decoupling) will be documented when that sprint starts.
- **Read path:** `GET /api/v1/positions/{instrumentId}` reads the persisted row directly — no recalculation on read.
- **If no `Position` row exists** for an instrument that has never had a settled trade, return `{ quantity: 0, averagePrice: 0 }` (see Business Rules) — do not 404.

**Architectural rationale:** This follows the CQRS pattern in miniature. Trades are the write model (source of truth / events). Positions are the read model / projection. Starting synchronous and moving async in Sprint 5 is the intended evolution — it is *why* RabbitMQ is the next sprint.

## Business Rules

- Trades cannot be created on inactive instruments.
- `settlementDate` is always `tradeDate + 2 days` (set by `@PrePersist`, not in the service layer).
- A `SETTLED` trade cannot be cancelled; a `CANCELLED` trade cannot be settled.
- Positions are calculated only from `SETTLED` trades. `PENDING` and `CANCELLED` trades do not affect position.
- Weighted average price uses only `BUY` trades: `Σ(buy_qty × buy_price) / Σ(buy_qty)`. `SELL` trades reduce quantity but do not affect the average cost. **Known limitation (Sprint 4 simplification):** this cumulative formula ignores position resets — if a position is fully sold and then re-opened with new buys, the formula still averages across the original buys instead of starting fresh. This is acceptable for Sprint 4 (the focus is aggregation, not P&L correctness) and will be refined in Sprint 8 to reset the average whenever `quantity` reaches zero. Do not fix this early.
- Net position: `Σ(BUY qty) - Σ(SELL qty)`.
- If no settled trades exist for an instrument, position is `quantity = 0, averagePrice = 0` (not an error, not a 404).

## Project Status

**Sprint 3 of 9 complete.** Sprint 4 is in progress — SCRUM-39 (Position entity) partially done; SCRUM-40 through SCRUM-44 still pending.

Roadmap:
- Sprint 4 (current): Position calculation — net quantity and weighted average price from settled trades.
- Sprint 5: RabbitMQ event-driven architecture.
- Sprint 6: Unit tests with JUnit 5 + Mockito.
- Sprint 7: Integration tests.
- Sprint 8: P&L calculation (realized and unrealized).
- Sprint 9 (**optional / deferred**): Spring Security with JWT. Intentionally pushed to the end — core business logic is more valuable for interview prep than auth infrastructure.

## Jira Integration

- Project key: `SCRUM`
- Cloud ID: `ac383c46-824e-42db-9e28-7ce68b8e6b8b`
- Current sprint tasks: SCRUM-39 (entity), SCRUM-40 (repository), SCRUM-41 (service — core logic), SCRUM-42 (response DTO), SCRUM-43 (controller), SCRUM-44 (end-to-end testing).
- Workflow expectation: move task to "In Progress" when starting, "Done" when finished. Do not batch updates at end of sprint.

## Known Files to Be Aware Of

- `CHANGELOG-FIXES.txt` in the repo root — manual log of fixes applied during development. Not auto-generated. Append to it when fixing non-trivial bugs.