# Homework 3 — Specification-Driven Design

## Student & Task Summary

**Student**: Denys Kobernik  
**Homework**: Homework 3 — Specification-Driven Design  
**Task**: Produce a specification package (no code) for a finance-oriented application operating in a regulated environment. The package includes a full product specification, cross-agent AI rules, Copilot-specific editor rules, and this README explaining the choices and industry practices applied.

**Feature chosen**: Payment card replacement flow — a cardholder reports a card as lost or stolen, the card is immediately deactivated, an ops team member reviews and approves or rejects the replacement, and a new card is issued with full auditability at every step.

---

## Rationale

### Why the card replacement flow?

Card replacement is a bounded, realistic workflow with a clear linear state machine (ACTIVE → SUSPENDED/TERMINATED → replacement ISSUED → ACTIVE). This makes it well-suited for specification-driven design: the state machine is explicit enough to be tested, the compliance requirements are concrete (PAN handling, audit trails, ops oversight), and the RBAC surface is naturally multi-role (customer, ops, compliance). It is also a domain where AI code generation errors carry meaningful risk, which makes thorough agent rules especially valuable.

### Why Java / Spring Boot?

Spring Boot is the dominant stack for regulated financial services on the JVM. Its annotation-driven RBAC (`@PreAuthorize`), transactional semantics, and mature ecosystem (Flyway, Testcontainers, Spring Security) map directly to the compliance concerns in the spec. Choosing a concretely named stack allows `agents.md` and `copilot-instructions.md` to give precise, actionable rules rather than vague generalizations.

### Why separate `agents.md` from `copilot-instructions.md`?

`agents.md` is **cross-agentic**: any AI tool (Claude Code, Cursor, Copilot, a custom CI agent) reads the same domain invariants, architecture constraints, and security rules. Those rules are technology-neutral at the AI-tooling level.

`copilot-instructions.md` is **editor-specific**: it controls Copilot's inline autocomplete and ghost-text suggestions — naming conventions, which Spring idioms to prefer in this codebase, which patterns to avoid. A Cursor or Claude agent does not consume this file; Copilot does not consume `agents.md` directly (though it reads this file which summarizes the context).

### Why this specification structure?

The Banking-Specific template from `specification-TEMPLATE-example.md` was used as the structural model because it places compliance and audit concerns at the same level as functional requirements. The progression from High-Level → Mid-Level → Implementation Notes → Context → Low-Level Tasks mirrors how an AI agent should consume the document: understand the goal first, then the constraints, then execute specific tasks. Each Low-Level Task contains an explicit prompt, file target, function name, and implementation details — this removes ambiguity and makes the tasks directly executable by a code-generation agent.

---

## Industry Best Practices

The table below identifies each applied practice and where it appears in the deliverables.

| Practice | Where it appears |
|----------|-----------------|
| **PAN masking** — only `maskedPan` in logs, API responses, and events; full PAN stored encrypted | `specification.md` → Implementation Notes; `agents.md` → §3 Domain Invariants rule 1; `copilot-instructions.md` → Autocomplete Hints + What NOT to Suggest |
| **Immutable audit log** — every state change appends to `audit_events`; no UPDATE/DELETE on the table | `specification.md` → Mid-Level Objective 5, Low-Level Task 5; `agents.md` → §3 Domain Invariants rule 2, §4 Architecture Constraints; `copilot-instructions.md` → Autocomplete Hints |
| **Role-Based Access Control (RBAC)** — three roles (CUSTOMER, OPS, COMPLIANCE) with least-privilege enforcement | `specification.md` → Mid-Level Objective 6, Low-Level Task 8; `agents.md` → §5 RBAC Rules; `copilot-instructions.md` → Spring Boot Idioms |
| **Idempotency keys** — duplicate mutating requests return the existing resource without side effects | `specification.md` → Implementation Notes, Low-Level Task 2 (unique constraint), Low-Level Task 3; `agents.md` → §3 Domain Invariants rule 3 |
| **Card state machine** — explicit allowed transitions, illegal transitions throw a typed exception | `specification.md` → Implementation Notes (state machine diagram), Low-Level Task 1; `agents.md` → §3 Domain Invariants rule 2 |
| **Structured error responses (RFC 7807 ProblemDetail)** — clients receive machine-readable error bodies | `specification.md` → Implementation Notes; `agents.md` → §4 Architecture Constraints |
| **`BigDecimal` for monetary values** — eliminates IEEE 754 rounding errors for fees and limits | `specification.md` → Implementation Notes; `agents.md` → §3 Domain Invariants rule 4; `copilot-instructions.md` → What NOT to Suggest |
| **Transactional consistency** — state change and audit log write share one DB transaction | `specification.md` → Implementation Notes, Low-Level Task 1; `agents.md` → §3 Domain Invariants rule 5, §5 |
| **Parameterized queries** — no raw SQL string concatenation | `agents.md` → §7 Security & Compliance; `copilot-instructions.md` → What NOT to Suggest |
| **Secrets management** — credentials and keys from environment variables or secrets manager | `agents.md` → §7 Security & Compliance, §8; `copilot-instructions.md` → What NOT to Suggest |
| **UTC timestamps** — all temporal data in UTC using `OffsetDateTime` / `timestamptz` | `agents.md` → §7 Security & Compliance |
| **Clean architecture layering** — Controller → Service → Repository; no business logic leakage | `agents.md` → §4 Architecture Constraints; `copilot-instructions.md` → What NOT to Suggest |
| **DTO projection** — JPA entities never returned from controllers | `agents.md` → §4 Architecture Constraints; `copilot-instructions.md` → What NOT to Suggest |
| **Testcontainers for integration tests** — repository tests run against real PostgreSQL, not H2 | `specification.md` → Implementation Notes; `agents.md` → §6 Testing Expectations; `copilot-instructions.md` → Test Generation Preferences |
| **Test naming convention** — `methodName_scenario_expectedOutcome` for readability and discoverability | `agents.md` → §6 Testing Expectations; `copilot-instructions.md` → Test Generation Preferences |

---

## Deliverables

| File | Description |
|------|-------------|
| [specification.md](specification.md) | Full product spec: High-Level Objective, Mid-Level Objectives, Implementation Notes, Context, 8 Low-Level Tasks |
| [agents.md](agents.md) | Cross-agent AI rules: tech stack, naming conventions, domain invariants, architecture, RBAC, testing, security, forbidden patterns |
| [.github/copilot-instructions.md](.github/copilot-instructions.md) | Copilot-specific editor rules: naming, Spring idioms, autocomplete hints, what not to suggest |
| [README.md](README.md) | This file |
