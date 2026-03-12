# agents.md — AI Agent Guidelines: Card Replacement Service

This file defines rules and constraints that **agent** must follow when working on this project. Rules here are tooling-agnostic and derive from domain requirements, architectural decisions, and regulated-environment compliance obligations.

---

## 1. Project Summary

| Property | Value |
|----------|-------|
| Domain | Payment card replacement in a regulated financial environment |
| Core flow | Cardholder reports lost/stolen card → card deactivated → ops reviews → replacement issued |
| Compliance posture | Auditability, RBAC, immutable logs |
| Stack | Java 21, Spring Boot 3.x, PostgreSQL 15, Hibernate/JPA, Flyway |

---

## 2. Tech Stack

| Layer | Technology |
|-------|-----------|
| Runtime | Java 21 (LTS) |
| Framework | Spring Boot 3.x |
| Persistence | PostgreSQL 15 via Spring Data JPA / Hibernate |
| Schema migrations | Flyway |
| Auth | Spring Security + JWT |
| Testing | JUnit 5, Mockito, Testcontainers (PostgreSQL), MockMvc |
| Logging | SLF4J + Logback |
| Build | Maven (or Gradle — match whatever is already in the project) |

---

## 3. Naming Conventions

These conventions apply to all agents generating or modifying code in this project:

| Artifact | Convention | Examples |
|----------|-----------|----------|
| Classes | PascalCase | `CardReplacementService`, `AuditEvent` |
| Methods & variables | camelCase | `submitReplacementRequest`, `maskedPan` |
| Constants & enum values | SCREAMING_SNAKE_CASE | `REPLACEMENT_REASON_STOLEN`, `CARD_STATUS_ACTIVE` |
| Spring beans | Suffix with role | `*Controller`, `*Service`, `*Repository`, `*Config` |
| DTOs | Suffix `Dto` | `CardReplacementRequestDto`, `AuditEventDto` |
| Service input objects | Suffix `Command` | `SubmitReplacementCommand`, `ApproveRequestCommand` |
| Exceptions | Suffix `Exception` | `CardNotFoundException`, `InvalidCardStateException` |
| DB migration files | `V{n}__{description}.sql` | `V2__card_replacement.sql` |
| Test methods | `methodName_scenario_expectedOutcome` | `submitRequest_duplicateIdempotencyKey_returnsExistingRequest` |

---

## 4. Domain Rules

These rules apply everywhere — in every file, every method, every suggestion:

1. **PAN must never appear in plaintext outside the encrypted storage column.**
   - Only `maskedPan` (format `****-****-****-XXXX`) may be returned in API responses, log lines, or event payloads.
   - If a method would expose `encryptedPan` or a full card number, it is incorrect. Refactor it.

2. **Every card state transition must produce an `AuditEvent` record.**
   - There are no exceptions. A transition that does not call `AuditLogService.record()` before committing is a defect.
   - `AuditEvent` records are immutable: they must never be updated or deleted after insertion.

3. **Idempotency keys are required on all mutating cardholder requests.**
   - `CardReplacementRequest.idempotencyKey` must be unique. If a duplicate key is received, return the existing resource — do not create a second record.

4. **Monetary values use `BigDecimal` with explicit scale.**
   - `double`, `float`, or `Float` must not be used for any fee, limit, or monetary field.

5. **All sensitive operations are `@Transactional`.**
   - A card state change and its corresponding audit log write must happen in the same database transaction.

---

## 5. Architecture Constraints

- Follow clean architecture layering: **Controller → Service → Repository**. No business logic in controllers or repositories.
- All domain exceptions (`CardNotFoundException`, `InvalidCardStateException`, `DuplicateRequestException`) map to `ProblemDetail` (RFC 7807) error responses. Do not return raw exception messages to clients.
- Use `UUID` for all primary keys. Do not use auto-increment `Long` IDs for entities exposed in APIs.
- JPA entities must not be returned directly from controllers. Always map to DTOs.
- The `AuditEvent` entity must carry Hibernate's `@Immutable` annotation.
- Database application role (`app_role`) must have only `SELECT` and `INSERT` on the `audit_events` table — no `UPDATE` or `DELETE`. Enforce this in the Flyway migration.

---

## 6. RBAC Rules

Three roles exist in this system:

| Role | Permitted actions |
|------|------------------|
| `CUSTOMER` | Submit replacement request, view own card status |
| `OPS` | Approve or reject replacement requests, view all requests, view audit events |
| `COMPLIANCE` | Read-only: view audit events and replacement request history |

- Role is extracted from the `roles` JWT claim (list of strings) and mapped to Spring `GrantedAuthority` with the `ROLE_` prefix.
- Use `@PreAuthorize("hasRole('...')")` at the service layer — not only at the controller layer — so programmatic calls are also protected.

---

## 7. Testing Expectations

- **Unit tests** (Mockito): all `*Service` classes must have unit tests covering happy path and error paths (invalid state, duplicate idempotency key, card not found, wrong owner).
- **Integration tests** (Testcontainers PostgreSQL): `CardReplacementRepository`, `AuditEventRepository`, and `CardReplacementService` integration must be tested against a real database container.
- **Controller tests** (MockMvc): all endpoints tested for correct HTTP status codes, response shape, and rejection of unauthorized roles.
- Test coverage target: 80% line coverage on `replacement` and `audit` packages.
- Tests must not share mutable state between test methods.

---

## 8. Security & Compliance Constraints

- Do not log any field named `cardNumber`, `pan`, `encryptedPan`, or any value matching a 16-digit numeric pattern.
- All timestamps stored and returned in UTC (`OffsetDateTime` / `timestamptz` in PostgreSQL).
- JWT validation must check: signature, expiry (`exp`), issuer (`iss`), and audience (`aud`).
- Do not use `@Transactional(readOnly = false)` as a default class-level annotation on service classes that contain both read and write methods — annotate each method appropriately.
- Parameterize all SQL. No string concatenation in queries.
- Secrets (DB credentials, JWT signing key) must come from environment variables or a secrets manager — never hardcoded or committed.

---

## 9. What AI Agents Must Never Do

- Generate code that logs or returns a full PAN or `encryptedPan`.
- Skip the `AuditLogService.record()` call on any card state transition.
- Use `float` or `double` for monetary values.
- Return a JPA entity directly from a controller method.
- Generate `UPDATE` or `DELETE` statements targeting the `audit_events` table.
- Add business logic to a `*Controller` or `*Repository` class.
- Hardcode credentials, JWT secrets, or encryption keys.
- Use raw string concatenation to build JPQL or SQL queries.
