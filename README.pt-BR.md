# Position Manager

[🇺🇸 English](README.md) · 🇧🇷 **Português**

Sistema de gestão de trades e posições inspirado no que bancos rodam de verdade — especificamente o [Nasdaq Calypso](https://www.nasdaq.com/solutions/calypso-technology), a plataforma que vários bancos grandes usam pra gestão de risco e posições.

> 🚧 **Status:** Sprint 3 de 9 entregue · Sprint 4 em andamento · **PRs são bem-vindos**

[![Java](https://img.shields.io/badge/Java-17-orange)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green)]()
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)]()
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

## Por que esse projeto existe

A maioria dos projetos pessoais é mais um CRUD de to-do. Esse aqui modela conceitos reais que bancos e corretoras usam:

- **Instrumentos** (ações, bonds, derivativos) com soft delete — porque dado financeiro nunca é realmente deletado (auditoria, compliance, reativação)
- **Trades** com lifecycle real (`PENDING → SETTLED → CANCELLED`) e **liquidação T+2** seguindo a regra da B3 (bolsa brasileira) desde 2019
- **Positions** como projeção estilo CQRS: trades são o write model, positions são o read model, recalculadas atomicamente no settle
- **Custo Médio Ponderado (WAC)** — `Σ(qty_buy × price_buy) / Σ(qty_buy)` considerando só trades de BUY

Construído pra aprender engenharia de sistemas financeiros em público. Melhor com você.

## Stack

```
Java 17 + Spring Boot 3.2 + Spring Data JPA
PostgreSQL 16 (Docker)
Maven · JUnit 5 (chega na Sprint 6)
```

Próximos passos: RabbitMQ pra eventos de trade (Sprint 5), cobertura de testes (Sprints 6–7), cálculo de P&L (Sprint 8).

## Rodando localmente

```bash
# Sobe o PostgreSQL
docker-compose up -d

# Roda a aplicação
./mvnw spring-boot:run
```

API em `http://localhost:8080/api/v1`
Swagger UI em `http://localhost:8080/swagger-ui.html`

## API

### Instrumentos

```
GET    /api/v1/instruments                   → lista ativos
GET    /api/v1/instruments/{id}              → busca por id
POST   /api/v1/instruments                   → cria
DELETE /api/v1/instruments/{id}              → soft delete
PATCH  /api/v1/instruments/{id}/reactivate   → reativa
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
POST   /api/v1/trades              → cria
PATCH  /api/v1/trades/{id}/settle  → liquida (transiciona pra SETTLED, dispara recálculo da position)
PATCH  /api/v1/trades/{id}/cancel  → cancela
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

**Regras de negócio:**
- Trades não podem ser criadas em instrumentos inativos
- `settlementDate` é auto-calculado: `tradeDate + 2 dias` (regra B3, desde 2019)
- Uma trade `SETTLED` não pode ser cancelada
- Uma trade `CANCELLED` não pode ser liquidada

## Arquitetura

```
src/main/java/com/trading/position_manager/
├── controller/   # endpoints REST
├── service/      # lógica de negócio
├── repository/   # acesso a dados (Spring Data JPA)
├── model/        # entidades JPA + enums
├── dto/          # payloads de request/response
└── exception/    # tratamento global de erros (@RestControllerAdvice)
```

Arquitetura em camadas padrão: `Controller → Service → Repository → Model`. Duas exceções de domínio dirigem todas as respostas de erro — `BusinessException` (HTTP 400) e `ResourceNotFoundException` (HTTP 404).

> 📖 Veja [`CLAUDE.md`](CLAUDE.md) pras convenções do projeto, e [`HELP.md`](HELP.md) pras notas de aprendizado por sprint (bastante detalhe sobre JPA, transações, CQRS e a matemática do WAC).

## Roadmap

| Sprint | Escopo | Status |
|---|---|---|
| 1 | Setup + Docker + PostgreSQL | ✅ Pronto |
| 2 | Instrument · CRUD · soft delete | ✅ Pronto |
| 3 | Trade · relacionamentos · regras D+2 | ✅ Pronto |
| 4 | **Agregação de Position · matemática do WAC** | 🚧 Em andamento |
| 5 | Eventos assíncronos com RabbitMQ | ⬜ Todo |
| 6 | Testes unitários (JUnit 5 + Mockito) | ⬜ Todo |
| 7 | Testes de integração | ⬜ Todo |
| 8 | Cálculo de P&L (realizado / não-realizado) | ⬜ Todo |
| 9 | Refactor · Auth (Spring Security + JWT) | ⬜ Todo |

## Contribuindo

**Sprint 4 está aberta. Qualquer PR é bem-vindo.** Se você é novo em Spring ou trabalha com fintech, tem peça pra todo mundo.

### Por onde começar

1. **Olha as issues com [`good first issue`](https://github.com/Scarlateli/Position-Manager/issues?q=is%3Aopen+is%3Aissue+label%3A%22good+first+issue%22)** — pequenas, escopadas, bem descritas. A maioria dá pra fechar em 30–60 minutos.
2. **Confere as [`help wanted`](https://github.com/Scarlateli/Position-Manager/issues?q=is%3Aopen+is%3Aissue+label%3A%22help+wanted%22)** pra coisa mais densa (PositionService com a matemática do WAC, eventos assíncronos com RabbitMQ).
3. **Lê [`CLAUDE.md`](CLAUDE.md)** antes de mexer no código — documenta convenções do projeto inteiro (regras do Lombok, precisão de BigDecimal, estratégia de FetchType, política de DTO de resposta). Seguir isso economiza ciclos de review.

### Workflow

1. Comenta na issue dizendo que quer pegar (evita duas pessoas fazendo a mesma coisa)
2. Faz fork e cria uma branch chamada `sprint-4/<descricao-curta>` ou `fix/<descricao-curta>`
3. Faz suas mudanças seguindo as convenções do `CLAUDE.md`
4. Abre um PR referenciando a issue (`Closes #X`)
5. Reviews chegam em 24–48h

### Discussão

Se você não tiver certeza sobre uma abordagem, **abre um draft PR ou comenta na issue** antes de mergulhar. É muito melhor alinhar a direção em 5 minutos do que gastar horas em algo que precisa ser refeito.

## Por que essas escolhas técnicas?

**Por que soft delete?**
Em sistemas financeiros, você nunca deleta dado. Auditoria, compliance e a possibilidade de reverter operações exigem que tudo fique guardado. A flag `active` deixa você "deletar" sem perder histórico.

**Por que separar DTO do Model?**
A entidade JPA tem campos que o cliente não precisa (timestamps, flags internas). O DTO expõe só o necessário e te deixa evoluir a API sem quebrar o schema do banco.

**Por que não usar `@Data` em entidades?**
O `@Data` do Lombok gera `equals`/`hashCode` baseado em todos os campos, o que quebra o JPA quando a entidade ainda não foi persistida (`id = null`) e quebra de novo quando o ID muda depois do `save()`. A gente usa `@Getter` + `@Setter` + `equals`/`hashCode` manual baseado só no ID:

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

**Por que CQRS-em-miniatura pra positions?**
Trades são o write model (fonte da verdade, auditado). Positions são o read model / projeção (leitura O(1) rápida). A Sprint 4 começa com recálculo síncrono dentro da transação do settle; a Sprint 5 vai publicar um `TradeSettledEvent` via RabbitMQ pra ter consistência eventual. *Esse* é o motivo do RabbitMQ ser a próxima sprint, não uma feature paralela.

## Licença

[MIT](LICENSE) — usa, faz fork, aprende com isso.

---

🇺🇸 *For the English version, see [README.md](README.md).*
