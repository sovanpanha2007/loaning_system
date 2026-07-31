# Loaning System

A loan management system for a small lending business — applicant onboarding, credit-committee-gated loan approval, amortized payment schedules, late-fee/delinquency tracking, and a full audit trail.

The business logic lives in one shared Java service (`LoaningSystem`) that is used by **two independent front ends**:

- a terminal **CLI** (`src.controller.Main`) — the original interface, still fully functional
- a **Spring Boot REST API** + separate **React SPA** — a browser-based UI for multiple concurrent users

Both front ends read and write the same SQLite database and enforce the same business rules; nothing is duplicated between them.

## Contents

- [Architecture](#architecture)
- [Domain & business rules](#domain--business-rules)
- [Getting started](#getting-started)
  - [Run the CLI](#run-the-cli)
  - [Run the web app (API + SPA)](#run-the-web-app-api--spa)
- [Configuration](#configuration)
- [REST API](#rest-api)
- [Authentication & security](#authentication--security)
- [Frontend structure](#frontend-structure)
- [Testing](#testing)
- [Project layout](#project-layout)

## Architecture

```
                     ┌─────────────────────┐
                     │   LoaningSystem     │   business logic, permission checks,
                     │  (src/controller)   │   BigDecimal money math, audit logging
                     └──────────┬──────────┘
                                │  ILoginable actingUser passed explicitly
                     ┌──────────┴──────────┐
                     │                     │
              ┌──────┴──────┐       ┌──────┴───────────────┐
              │  Main.java  │       │   src/web (Spring)   │
              │    (CLI)    │       │  REST controllers +   │
              └─────────────┘       │  Spring Security      │
                                    └──────────┬────────────┘
                                               │ session cookie + CSRF
                                    ┌──────────┴────────────┐
                                    │   frontend/ (React)   │
                                    │   Vite + TypeScript    │
                                    └────────────────────────┘
```

`LoaningSystem` is a **stateless, thread-safe service**: every method takes the acting user (`ILoginable actingUser`) as an explicit parameter instead of reading a session field, so a single instance can be shared as a Spring singleton bean across concurrent HTTP requests without one user's request leaking into another's. A single `ReentrantLock` serializes the check-then-write flows (`createContract`, `approveContract`, `rejectContract`, `makePayment`, `checkDelinquency`) against the embedded SQLite database, which only supports one writer at a time.

DAOs (`StaffDao`, `ApplicantDao`, `ContractDao`, `PaymentScheduleDao`, `AuditDao`) are unchanged hand-rolled JDBC classes — there is no ORM.

## Domain & business rules

- **Roles**: `Manager`, `LoanOfficer`, `CreditCommittee`, `Applicant` (all implement `ILoginable`); staff roles extend an abstract `Staff` class.
- **Loan approval**: a `LoanOfficer` can approve contracts within their personal approval limit; larger loans require committee quorum (default **2** required `CreditCommittee` votes, configurable by a `Manager`).
- **DTI check**: new contracts are rejected if the applicant's resulting debt-to-income ratio exceeds the configured maximum (configurable by a `Manager`).
- **Co-signers**: a contract may have at most **3** co-signers.
- **Payments**: partial payments are supported and roll over onto the next scheduled installment; a payoff that exceeds the remaining balance is capped at what's actually owed.
- **Delinquency**: a flat **$25.00** late fee is applied to overdue installments; contracts that stay unpaid past the grace period are marked `DEFAULTED`. A `Manager` can trigger a delinquency sweep on demand.
- **Audit log**: every state-changing action (login, contract creation/approval/rejection, payments, config changes, staff/applicant creation) is recorded with actor, action, subject, and timestamp.

## Getting started

Prerequisites: **Java 17**, **Maven**, **Node.js** (for the frontend), and no other services on ports `8080`/`5173`.

Both front ends share one SQLite file (`loaning_system.db`, created automatically on first run in the project root).

### Run the CLI

```bash
mvn exec:java@run
```

Logs in interactively, drives the same 25 menu flows as always (create applicant/staff, create/approve/reject contracts, make payments, view schedules/balances, audit log, config, etc.).

### Run the web app (API + SPA)

```bash
# Terminal 1 — backend, http://localhost:8080
mvn spring-boot:run

# Terminal 2 — frontend, http://localhost:5173
cd frontend
npm install
npm run dev
```

Open **http://localhost:5173**. The Vite dev server proxies `/api/*` requests to the backend on `:8080`, so the browser sees everything as same-origin — this matters because the session cookie defaults to `SameSite=Lax`, which would otherwise block a true cross-origin `fetch()`/XHR from the SPA even though tools like `curl` ignore `SameSite` entirely.

A default admin account is seeded automatically on first backend startup (see startup log: `System ready. Default admin account seeded.`).

To build a production frontend bundle: `npm run build` (in `frontend/`) → outputs to `frontend/dist/`.

## Configuration

`src/main/resources/application.properties` (Spring Boot app only — the CLI takes bank name/interest rate as constructor args):

| Property | Default | Meaning |
|---|---|---|
| `server.port` | `8080` | Spring Boot HTTP port |
| `lms.bank-name` | `KH Bank` | Display name used in audit/print output |
| `lms.interest-rate` | `0.05` | Default annual interest rate for new contracts |
| `lms.db-file` | `loaning_system.db` | SQLite file path |
| `lms.cors.allowed-origin` | `http://localhost:5173` | Origin allowed to make credentialed cross-origin requests (the Vite dev server) |

## REST API

Session-cookie authenticated. All request/response bodies are JSON. Error responses are `{"error": "..."}` with an appropriate HTTP status (400/401/403/404/409/500 — see [`GlobalExceptionHandler`](src/main/java/src/web/exception/GlobalExceptionHandler.java)).

| Method | Path | Role | Purpose |
|---|---|---|---|
| POST | `/api/auth/login` | any | Log in, start a session |
| POST | `/api/auth/logout` | any | Log out, invalidate session |
| GET | `/api/auth/me` | any | Current authenticated user (or 401) |
| POST | `/api/applicants` | Manager | Create an applicant |
| GET | `/api/applicants` | Manager | List all applicants |
| GET | `/api/applicants/me/balance` | Applicant | View own account balance |
| POST | `/api/applicants/{applicantId}/balance` | Manager | Add funds to an applicant's balance |
| POST | `/api/staff` | Manager | Create a staff member |
| GET | `/api/staff` | Manager | List all staff |
| PUT | `/api/staff/{staffId}/deactivate` | Manager | Deactivate a staff member |
| PUT | `/api/staff/{staffId}/approval-limit` | Manager | Set a loan officer's approval limit |
| PUT | `/api/me/username` | any | Change own username |
| PUT | `/api/me/password` | any | Change own password |
| POST | `/api/contracts` | Applicant, LoanOfficer | Create a contract (an Applicant may only create one for themselves) |
| GET | `/api/contracts` | Manager, LoanOfficer, CreditCommittee | List all contracts |
| GET | `/api/contracts/mine` | Applicant | List own contracts |
| GET | `/api/contracts/{contractId}/schedule` | Applicant | View own payment schedule |
| POST | `/api/contracts/{contractId}/approve` | LoanOfficer, CreditCommittee | Approve a contract |
| POST | `/api/contracts/{contractId}/reject` | LoanOfficer | Reject a contract |
| POST | `/api/contracts/{contractId}/cosigners` | LoanOfficer, CreditCommittee | Add a co-signer |
| POST | `/api/contracts/{contractId}/payments` | Applicant | Make a payment on own contract |
| GET | `/api/audit-log` | Manager | Full audit log |
| GET | `/api/audit-log/contract/{contractId}` | Manager | Audit log for one contract |
| GET | `/api/config` | Manager | View current system config |
| PUT | `/api/config/required-votes` | Manager | Set required committee votes |
| PUT | `/api/config/max-dti` | Manager | Set max DTI ratio |
| POST | `/api/config/delinquency-check` | Manager | Trigger a delinquency sweep |

DTOs (`src/main/java/src/web/dto/`) exist specifically so entities are never serialized directly: password hashes are never exposed, and nested object graphs (e.g. `Contract → Applicant/Staff`) are flattened to IDs and names.

## Authentication & security

- **Spring Security**, session-cookie based (not JWT). `LmsAuthenticationProvider` delegates credential checking to `LoaningSystem.authenticate(...)` directly — the app's own password hashing doesn't need to be Spring-`PasswordEncoder`-compatible.
- **CSRF**: cookie-based double-submit pattern (`CookieCsrfTokenRepository.withHttpOnlyFalse()`), with a custom `CsrfCookieFilter` that forces eager token resolution (a documented Spring Security 6 gotcha — without it, the `XSRF-TOKEN` cookie is never written on a plain GET). The SPA reads the cookie and echoes it back as an `X-XSRF-TOKEN` header on every mutating request.
- **Authorization**: method-level `@PreAuthorize` role checks map onto the existing `ILoginable`/`Staff` class hierarchy (`ROLE_MANAGER`, `ROLE_LOAN_OFFICER`, `ROLE_CREDIT_COMMITTEE`, `ROLE_APPLICANT`). ID-scoped endpoints additionally verify the authenticated user owns the specific resource (e.g. an Applicant can only ever see/pay their own contract, never another applicant's, regardless of role).
- **CORS**: restricted to the single configured origin (`lms.cors.allowed-origin`), credentials allowed.

## Frontend structure

`frontend/` — Vite + React + TypeScript, `react-router-dom` for routing, `@tanstack/react-query` for data fetching/caching.

```
frontend/src/
├── api/         typed fetch client (client.ts) + request/response types (types.ts)
├── auth/        AuthContext — fetches /api/auth/me on load, exposes login/logout
├── components/  Layout (nav bar), ProtectedRoute (role-gated route guard)
└── pages/       one page per CLI flow area (dashboard, applicants, staff,
                 contracts, schedule, audit log, settings, profile, ...)
```

Route access in `App.tsx` mirrors the backend's `@PreAuthorize` gates exactly, so the nav and the API agree on who can do what.

## Testing

```bash
mvn test            # unit tests (LoaningSystem) + MockMvc REST integration tests
cd frontend && npm run build   # type-checks and production-builds the SPA
```

Notable test coverage: full lending lifecycle (create → approve → pay → payoff) both at the service layer and end-to-end through the REST API; concurrency tests for the borrowing-cap race and concurrent-payoff race under the singleton-service model; delinquency/late-fee/default flows; DTI threshold edge cases; partial-payment rollover.

## Project layout

```
src/main/java/src/
├── controller/   LoaningSystem (business logic), Main (CLI), NotFoundException
├── model/        domain entities (Applicant, Staff/Manager/LoanOfficer/CreditCommittee,
│                 Contract, PaymentSchedule, Payment, AuditEntry, ...)
├── dao/          hand-rolled JDBC DAOs
└── web/          Spring Boot app: config/, security/, controller/, dto/, exception/
src/main/resources/application.properties
src/test/java/src/controller/   unit tests
src/test/java/src/web/          MockMvc REST integration tests
frontend/                       React SPA (Vite + TypeScript)
```
