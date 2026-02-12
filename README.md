# Position Manager

Sistema de gestão de posições financeiras inspirado no [Nasdaq Calypso](https://www.nasdaq.com/solutions/calypso-technology) o mesmo software usado para gestão de risco e posições.

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
GET    /api/v1/instruments      → lista ativos
GET    /api/v1/instruments/{id} → busca por id
POST   /api/v1/instruments      → cria novo
DELETE /api/v1/instruments/{id} → soft delete
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

## Estrutura

```
src/main/java/com/trading/position_manager/
├── controller/     # endpoints REST
├── service/        # regras de negócio
├── repository/     # acesso a dados
├── model/          # entidades JPA
├── dto/            # objetos de request/response
└── exception/      # tratamento de erros
```

## Roadmap

- [x] Setup inicial + Docker + PostgreSQL
- [x] Entidade Instrument com soft delete
- [ ] CRUD completo via API REST
- [ ] Entidade Trade + relacionamentos
- [ ] Cálculo de posições (quantidade líquida por instrumento)
- [ ] Eventos assíncronos com RabbitMQ
- [ ] Testes unitários e de integração
- [ ] P&L básico

## Decisões técnicas

**Por que soft delete?**  
Em sistemas financeiros, você nunca deleta dados. Auditoria, compliance, e a possibilidade de reverter operações exigem que tudo seja mantido. O campo `active` permite "deletar" sem perder histórico.

**Por que separar DTO do Model?**  
A entidade JPA tem campos que o cliente não precisa ver (timestamps, flags internas). O DTO expõe apenas o necessário e permite evoluir a API sem quebrar o banco.

**Por que não usar `@Data` na entidade?**  
Lombok `@Data` gera `equals/hashCode` baseado em todos os campos, o que quebra com JPA quando a entidade ainda não foi persistida. Uso `@Getter/@Setter` + implemento `equals/hashCode` só com o ID.

---

Projeto em desenvolvimento. Acompanhe o progresso nos commits.
