# GitHub Copilot Instructions — Card Replacement Service

---

## Project Context

This is a **Spring Boot 3.x / Java 21** service for managing payment card replacement in a regulated environment. The core flow is: cardholder reports a lost/stolen card → card is deactivated → an ops agent reviews → a replacement card is issued. Auditability and RBAC are first-class concerns.

---

## Autocomplete Hints

When suggesting code in this project:

- If a card number field is needed in a DTO or log statement, **always use `maskedPan`**, never `encryptedPan` or `cardNumber`.
- If a service method changes card state, the next line should call `auditLogService.record(...)`.
- If generating a JPA entity for the `audit_events` table, include `@Immutable` (Hibernate annotation).
- If generating a REST endpoint that mutates state, include an `Idempotency-Key` header or body field.
- If generating a test class, prefer `@ExtendWith(MockitoExtension.class)` for unit tests and `@SpringBootTest` + `@Testcontainers` for integration tests.

---

## What NOT to Suggest

- Do not suggest `System.out.println(...)` — use `log.info(...)` / `log.warn(...)` / `log.error(...)` from an SLF4J `Logger`.
- Do not suggest logging any variable named `cardNumber`, `pan`, `encryptedPan`, or any 16-digit numeric string.
- Do not suggest `float` or `double` for monetary or fee fields — use `BigDecimal`.
- Do not suggest returning a JPA entity from a `@RestController` method.
- Do not suggest native SQL with string concatenation — use JPQL with named parameters or Spring Data method names.
- Do not suggest hardcoding secrets, JWT keys, or database passwords inline.
- Do not suggest placing business logic inside `*Controller` or `*Repository` classes.

---

## Spring Boot Patterns to Prefer

- Use `@PreAuthorize("hasRole('...')")` at the **service layer** — not only the controller.
- Prefer constructor injection over `@Autowired` field injection.
- Use `@Transactional` explicitly on individual service methods, not as a class-level default.
- Prefer `Optional<T>` return types from repository methods; unwrap with `.orElseThrow(() -> new CardNotFoundException(...))`.
- Use `ResponseEntity<ProblemDetail>` for error responses rather than raw exception propagation to the HTTP layer.
- Annotate read-only repository methods with `@Transactional(readOnly = true)`.
- Use Spring's `@RestControllerAdvice` + `@ExceptionHandler` for centralised error mapping.

---

## Test Generation Preferences

When generating tests:

- Unit tests: `@ExtendWith(MockitoExtension.class)`, mock all dependencies, test both success and failure branches.
- Integration tests: use `@Testcontainers` with a PostgreSQL `@Container`; roll back after each test with `@Transactional`.
- Controller tests: use `@WebMvcTest` + `MockMvc`; verify HTTP status, response body shape, and that unauthorized roles receive `403`.
- Name test methods: `methodName_scenario_expectedOutcome` (e.g., `submitRequest_duplicateIdempotencyKey_returnsExistingRequest`).
