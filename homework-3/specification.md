# Card Replacement Flow — Specification

---

## High-Level Objective

Enable a cardholder to report a payment card as lost or stolen, trigger its immediate deactivation, and receive a physical replacement card — with every state transition recorded in an immutable audit log and all sensitive operations gated by role-based access control.

---

## Mid-Level Objectives

1. **Customer self-service reporting** — A cardholder can submit a card-replacement request (reason: LOST | STOLEN | DAMAGED) through a secure, authenticated endpoint; duplicate requests for the same active card are rejected via idempotency key.
2. **Automatic card deactivation** — Upon request submission, the card's status transitions from `ACTIVE` to `SUSPENDED` (pending review) or directly to `TERMINATED` (confirmed theft) within the same transaction, preventing further authorisations.
3. **Ops / compliance review workflow** — An operator (role: `OPS`) can approve or reject a replacement request; approval triggers issuance of a replacement card record with status `ISSUED` and a delivery tracking reference; rejection records the reason and terminates the workflow.
4. **Replacement card issuance** — A new `Card` record is created with a newly generated masked PAN reference, linked to the original card via `replacesCardId`; the original card transitions to `TERMINATED` upon issuance approval.
5. **Immutable audit log** — Every card state transition and every workflow action (submit, approve, reject, issue) appends an `AuditEvent` record containing: `eventType`, `actorId`, `actorRole`, `cardId`, `previousStatus`, `newStatus`, `timestampUtc`, and a `correlationId`. Audit records are never updated or deleted.
6. **RBAC enforcement** — Three roles govern the system: `CUSTOMER` (submit request, view own card), `OPS` (review, approve/reject, view all requests), `COMPLIANCE` (read-only access to audit log and all requests).

---

## Implementation Notes

- **Runtime**: Java 21, Spring Boot 3.x
- **Persistence**: PostgreSQL 15; schema migrations via Flyway
- **ORM**: Hibernate / Spring Data JPA; all entities use `UUID` primary keys
- **Auth**: Spring Security with JWT; role extracted from `roles` claim; `@PreAuthorize` annotations on all service-layer methods
- **Monetary values**: `BigDecimal` with explicit scale for any fee fields; never `double` or `float`
- **PAN handling**: Full PAN stored encrypted (AES-256) in the database; only `maskedPan` (format `****-****-****-1234`) is returned in any API response or log statement
- **Card state machine** (enforced in `CardService`):
  ```
  ACTIVE → SUSPENDED (report received, pending ops review)
  ACTIVE → TERMINATED (confirmed theft, immediate lock)
  SUSPENDED → TERMINATED (after ops approval of replacement)
  SUSPENDED → ACTIVE (ops rejection — card reinstated)
  ISSUED (new replacement card initial state)
  ISSUED → ACTIVE (card activated by cardholder)
  ```
- **Idempotency**: `CardReplacementRequest` carries a client-supplied `idempotencyKey` (UUID); duplicate submissions return the existing request with HTTP 200
- **Audit log immutability**: `AuditEvent` table has no `UPDATE` or `DELETE` grants; application role is insert + select only
- **Error handling**: domain exceptions (`CardNotFoundException`, `InvalidCardStateException`, `DuplicateRequestException`) map to structured `ProblemDetail` (RFC 7807) responses
- **Testing**: JUnit 5 + Mockito for unit tests; Testcontainers (PostgreSQL) for repository and service integration tests; MockMvc for controller tests
- **Logging**: SLF4J + Logback; log level `INFO` for state transitions, `WARN` for rejected requests, `ERROR` for unexpected failures; never log `cardNumber` or full PAN

---

## Context

### Beginning context

- `Card` entity is present with fields: `id` (UUID), `accountId` (UUID), `encryptedPan` (String, encrypted), `maskedPan` (String), `status` (String — will be replaced by `CardStatus` enum in Task 1), `createdAt` (OffsetDateTime), `updatedAt` (OffsetDateTime).
- `Account` entity and `AccountRepository` exist and are used to validate card ownership; do not modify them.
- Spring Security is configured with a JWT authentication filter. `UserPrincipal` (carrying `userId` and `roles`) is available from the security context in any authenticated request.
- Flyway migration `V1__init.sql` establishes the `cards` and `accounts` tables. Do not modify this file; all new schema changes go in subsequent versioned migrations.
- `AuditLogService` exists as a declared interface with no implementation. Task 3 provides the implementation; all other tasks may inject and call it.

### Ending context

- `CardStatus` enum is created with values `ACTIVE`, `SUSPENDED`, `TERMINATED`, `ISSUED`, and replaces the raw String status field on `Card`. (→ Task 1)
- `CardStateMachine` enforces all allowed transitions and writes an `AuditEvent` on every state change. (→ Task 1)
- `CardReplacementRequest` entity and `CardReplacementRequestRepository` are persisted via Flyway migration `V2__card_replacement.sql`. (→ Task 2)
- `AuditEvent` entity, `AuditEventRepository`, and `AuditLogServiceImpl` are in place; the `audit_events` table has UPDATE and DELETE revoked from the application role via `V3__audit_events.sql`. (→ Task 3)
- `NotificationService` interface and `NotificationServiceStub` exist; the stub can be replaced by a real implementation without changing any caller. (→ Task 4)
- `CardReplacementService` handles the full replacement workflow: submit (customer), approve (ops), and reject (ops). (→ Tasks 5, 6)
- `CardReplacementController` exposes all REST endpoints; all responses return DTOs — no JPA entity is ever serialised directly. (→ Task 7)
- `SecurityConfig` enforces endpoint-level RBAC for all new routes; JWT roles are mapped to Spring `GrantedAuthority` with the `ROLE_` prefix. (→ Task 8)
- Unit tests (Mockito) and integration tests (Testcontainers) cover all new service and repository components; controller tests (MockMvc) verify HTTP status codes, response shape, and role enforcement.

---

## Low-Level Tasks

---

### 1. Create `CardStatus` enum and `CardStateMachine`

**Prompt**
Create the `CardStatus` enum with all required values and a `CardStateMachine` component that validates and enforces allowed transitions, throwing `InvalidCardStateException` for illegal ones.

**Functions / classes**
`CardStatus` (enum), `CardStateMachine.transition(Card, CardStatus, ActorContext)`, `InvalidCardStateException`

**Details**
- `CardStatus` values: `ACTIVE`, `SUSPENDED`, `TERMINATED`, `ISSUED`
- Allowed transitions are exactly those listed in Implementation Notes; all other transitions throw `InvalidCardStateException`
- `InvalidCardStateException` must carry `currentStatus` and `attemptedStatus` fields so the `ProblemDetail` response handler can surface them
- `CardStateMachine.transition()` must be `@Transactional` and must call `AuditLogService.record()` before saving the updated card — no transition may be committed without a corresponding audit entry

---

### 2. Create `CardReplacementRequest` entity and repository

**Prompt**
Create the `CardReplacementRequest` JPA entity, the supporting enums, a Spring Data repository, and the Flyway migration for the new table.

**Functions / classes**
`CardReplacementRequest` (entity), `ReplacementReason` (enum), `RequestStatus` (enum), `CardReplacementRequestRepository` (JpaRepository)

**Details**
- Entity fields: `id` (UUID PK), `cardId` (UUID FK → cards), `requestorId` (UUID), `reason` (`ReplacementReason`: LOST, STOLEN, DAMAGED), `status` (`RequestStatus`: PENDING, APPROVED, REJECTED), `idempotencyKey` (UUID, unique), `deliveryTrackingRef` (nullable String), `rejectionReason` (nullable String), `createdAt`, `updatedAt`
- `idempotencyKey` must have a unique constraint in both `@Column(unique=true)` and the Flyway migration DDL
- Repository must expose: `findByIdempotencyKey(UUID)`, `findByCardIdAndStatus(UUID, RequestStatus)`

---

### 3. Create `AuditEvent` entity, repository, and `AuditLogServiceImpl`

**Prompt**
Create the `AuditEvent` JPA entity, its repository, the `AuditLogServiceImpl` that writes append-only records, and the Flyway migration that revokes UPDATE/DELETE from the application role.

**Functions / classes**
`AuditEvent` (entity), `AuditEventRepository` (JpaRepository), `AuditLogServiceImpl.record(AuditEventDto)`

**Details**
- Entity fields: `id` (UUID PK), `eventType` (String), `actorId` (UUID), `actorRole` (String), `cardId` (UUID), `previousStatus` (String, nullable), `newStatus` (String, nullable), `correlationId` (UUID), `timestampUtc` (OffsetDateTime, DB `DEFAULT now()`)
- Entity must carry Hibernate's `@Immutable` annotation to prevent accidental updates at the ORM level
- Flyway migration must execute `REVOKE UPDATE, DELETE ON audit_events FROM app_role`
- Repository exposes: `findByCardIdOrderByTimestampUtcAsc(UUID)`, `findByCorrelationId(UUID)`

---

### 4. Create `NotificationService` interface and stub

**Prompt**
Create a `NotificationService` interface and a no-op stub implementation that logs notification intent via SLF4J without sending anything externally.

**Functions / classes**
`NotificationService` (interface), `NotificationServiceStub` (stub implementation)

**Details**
- Interface methods: `notifyCardDeactivated(UUID cardId, UUID customerId)`, `notifyReplacementApproved(UUID cardId, UUID customerId, String deliveryTrackingRef)`, `notifyReplacementRejected(UUID cardId, UUID customerId, String reason)`
- Stub logs at `INFO` level; if a card reference is included in the log message, use `maskedPan` — never a raw PAN
- Design the interface so a real email/SMS implementation can replace the stub without any changes to callers

---

### 5. Implement `CardReplacementService` — submit request

**Prompt**
Implement `submitReplacementRequest`: enforce idempotency, validate card ownership, trigger card deactivation via `CardStateMachine`, persist the replacement request, and notify the cardholder.

**Functions / classes**
`CardReplacementService.submitReplacementRequest(SubmitReplacementCommand)`

**Details**
- Check `findByIdempotencyKey()` first; if a record exists, return it immediately (idempotent — do not create a duplicate)
- Validate the card belongs to the requesting customer; throw `CardNotFoundException` if it does not exist or is not owned by the caller
- Invoke `CardStateMachine.transition(card, SUSPENDED, actorContext)` for LOST or DAMAGED; use `TERMINATED` for STOLEN
- Persist `CardReplacementRequest` with status `PENDING`
- Call `NotificationService.notifyCardDeactivated(cardId, customerId)`
- Annotate the method `@Transactional` and `@PreAuthorize("hasRole('CUSTOMER')")`

---

### 6. Implement `CardReplacementService` — ops approve and reject

**Prompt**
Implement `approveReplacementRequest` and `rejectReplacementRequest`, both restricted to the `OPS` role.

**Functions / classes**
`CardReplacementService.approveReplacementRequest(UUID requestId, ActorContext)`, `CardReplacementService.rejectReplacementRequest(UUID requestId, String reason, ActorContext)`

**Details**
- Both methods annotated `@Transactional` and `@PreAuthorize("hasRole('OPS')")`
- **Approve**: transition original card to `TERMINATED`; create a new `Card` record with status `ISSUED` and set `replacesCardId` to the original card's ID; generate a `deliveryTrackingRef` (UUID string); update request status to `APPROVED`; call `NotificationService.notifyReplacementApproved()`
- **Reject**: if original card was `SUSPENDED`, transition it back to `ACTIVE`; update request status to `REJECTED`; persist `rejectionReason`; call `NotificationService.notifyReplacementRejected()`

---

### 7. Create `CardReplacementController`

**Prompt**
Create the REST controller that exposes the card replacement endpoints, delegating all logic to `CardReplacementService` and returning DTO responses.

**Functions / classes**
`CardReplacementController`

**Details**
- `POST /cards/{cardId}/replacement-requests` — authenticated as `CUSTOMER`; request body: `{ reason, idempotencyKey }`; returns `201 Created` with `CardReplacementRequestDto`
- `POST /replacement-requests/{requestId}/approve` — `OPS` only; returns `200 OK` with updated `CardReplacementRequestDto`
- `POST /replacement-requests/{requestId}/reject` — `OPS` only; request body: `{ reason }`; returns `200 OK`
- `GET /audit/cards/{cardId}/events` — `OPS` or `COMPLIANCE`; returns list of `AuditEventDto`
- All responses use DTOs — never return JPA entities directly; `maskedPan` is the only card number field permitted in any DTO

---

### 8. Configure `SecurityConfig` with RBAC rules

**Prompt**
Update `SecurityConfig` to register HTTP-level authorization rules for all new endpoints and configure JWT role extraction.

**Functions / classes**
`SecurityConfig.securityFilterChain(HttpSecurity)`

**Details**
- `POST /cards/*/replacement-requests` → `CUSTOMER`
- `POST /replacement-requests/*/approve` → `OPS`
- `POST /replacement-requests/*/reject` → `OPS`
- `GET /audit/cards/*/events` → `COMPLIANCE`, `OPS`
- All other `/audit/**` paths → `COMPLIANCE` only
- JWT role claim key: `roles` (list of strings); map each value to a Spring `GrantedAuthority` with the `ROLE_` prefix
