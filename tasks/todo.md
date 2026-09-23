# Fault Management Dashboard — Task Log

## Plan Readiness Review (before any code was written)

Reviewed: `plan.md`, `user-stories.md`, `claude.md`, `pom.xml`, `application.properties`,
existing `src/` skeleton.

**Verdict: plan.md is ready for implementation, with three small gaps resolved as noted below
(none blocking; all called out to the user before proceeding).**

1. **Service/endpoint ownership conflict for Change Password + Forgot Password.**
   plan.md Section 5 ("Services") assigns `changePassword()`, `getSecretQuestion()`,
   `verifySecretAnswer()`, `resetPassword()` to `UserService`. But plan.md Section 1 (Phase 1
   story breakdown) and Section 6 (API table) assign the same methods to `AuthService`, exposed
   at `/api/users/change-password` and `/api/auth/forgot-password/*`. `UserService`/`UserAPI`
   are not built until Phase 2 per the Project Structure notes, so Phase 1 can't literally
   follow Section 5.
   **Resolution taken:** all four methods live on `AuthService`/`AuthServiceImpl` (matching
   Section 1's narrative, which is the more specific of the two). A **new, deliberately thin**
   `UserAPI` was created now with only the `PUT /api/users/change-password` endpoint (matching
   Section 6's literal path), delegating to `AuthService`. Phase 2 will add the rest of
   `UserAPI`'s endpoints and a real `UserService` alongside it.
2. **No `spring.jpa.hibernate.ddl-auto` in `application.properties`.** Without it (and with no
   `TableScript.sql` yet), the `users` table has no creation path and Login would fail against
   an empty database.
   **Resolution taken:** added `spring.jpa.hibernate.ddl-auto=update` as a dev-time convenience.
   Swap for a real migration / the reference `TableScript.sql` before production.
3. **No CORS origin configured anywhere.** SecurityConfig needs a concrete allowed origin for
   the credentialed React SPA (Section 8: can't be `*` with credentials).
   **Resolution taken:** added `app.cors.allowed-origin=http://localhost:3000` (overridable) and
   read it in `SecurityConfig`.

`pom.xml` was reviewed and **not modified** — every dependency plan.md's "Tech Stack Additions"
calls for (ModelMapper, Log4j2 w/ the Logback exclusion, Lombok, Spring Security, Validation,
MySQL connector) is already present, just at slightly newer pinned versions than plan.md's
example snippet (e.g. ModelMapper 3.2.4 vs. the plan's 2.3.5) — those already-present versions
were kept as-is per the instruction not to touch `pom.xml` unless necessary.

**Not fixed / worth your attention, but not blocking:** `pom.xml`'s `<artifactId>`
(`wellness-tracker`) and empty `<name>`/`<description>` are leftover from the reference project
this one was adapted from. Cosmetic only — left untouched since changing it isn't necessary for
Phase 1.

---

## Phase 1 — Authentication & Session Management

### Story 1.0 — Shared groundwork
- [x] `enums/Role.java`
- [x] `enums/UserState.java`
- [x] `enums/SecretQuestion.java`
- [x] `entity/User.java`
- [x] `repository/UserRepository.java`
- [x] `security/PasswordEncoderConfig.java`
- [x] `security/CustomUserDetailsService.java`
- [x] `security/CurrentUserResolver.java`
- [x] `security/SecurityConfig.java`
- [x] `config/ModelMapperConfig.java`
- [x] `dto/ApiResponseDto.java`
- [x] `dto/ErrorResponseDto.java`
- [x] `exception/UserNotFoundException.java`
- [x] `exception/InvalidCredentialsException.java`
- [x] `exception/UserDeactivatedException.java`
- [x] `exception/WeakPasswordException.java`
- [x] `exception/PasswordMismatchException.java`
- [x] `exception/InvalidSecretAnswerException.java`
- [x] `exception/UnauthorizedActionException.java`
- [x] `exception/GlobalExceptionHandler.java`
- [x] `src/main/resources/log4j2.properties`
- [x] `application.properties` — added `ddl-auto` + `app.cors.allowed-origin`

### Addendum — CSRF bootstrap endpoint (added when testing steps were requested)
`plan.md` Section 8/9 calls for a lightweight public `GET` the client hits on app load solely to
receive the `XSRF-TOKEN` cookie, but no such endpoint existed after the initial Phase 1 pass —
every Phase 1 endpoint is `POST`/`PUT`, so there was no way to obtain the CSRF cookie before
making the very first state-changing call (Postman included). Added:
- [x] `api/AuthAPI.java` — `GET /api/auth/csrf` (injects `CsrfToken` to force the cookie write)
- [x] `security/SecurityConfig.java` — added `/api/auth/csrf` to the `permitAll` matcher

### FE US01 / BE US01 — Login
- [x] `dto/LoginRequestDto.java`
- [x] `dto/LoginResponseDto.java`
- [x] `service/AuthService.java` (`login`)
- [x] `service/AuthServiceImpl.java` (`login`)
- [x] `api/AuthAPI.java` — `POST /api/auth/login`

### FE US08 — Logout
- [x] `service/AuthService.java` / `AuthServiceImpl.java` (`logout`)
- [x] `api/AuthAPI.java` — `POST /api/auth/logout`

### FE US09 — Change Password
- [x] `dto/ChangePasswordRequestDto.java`
- [x] `service/AuthService.java` / `AuthServiceImpl.java` (`changePassword`)
- [x] `api/UserAPI.java` — `PUT /api/users/change-password` (see resolution #1 above)

### FE US20/US21 — Forgot Password
- [x] `dto/ForgotPasswordStep1RequestDto.java`
- [x] `dto/ForgotPasswordStep2RequestDto.java`
- [x] `dto/ResetPasswordRequestDto.java`
- [x] `service/AuthService.java` / `AuthServiceImpl.java` (`getSecretQuestion`,
      `verifySecretAnswer`, `resetPassword`)
- [x] `api/AuthAPI.java` — `POST /api/auth/forgot-password/question|verify|reset`

## Review Summary

All Phase 1 files listed above were created. No file outside Phase 1's scope was touched, except
the two `application.properties` additions and the `pom.xml` review (no change made).

**Verification limitation:** this environment's network egress does not include Maven Central,
so `mvnw` cannot download dependencies here and `mvn test`/`mvn compile` could not be run to
prove the build green. Everything was reviewed by hand for package/import correctness against
Spring Boot 3.2.4 / Spring Security 6.2.x (jakarta namespace) APIs, but that manual review is not
a substitute for an actual build — please run `./mvnw spring-boot:run` (or `mvn test`) in your
own environment before treating Phase 1 as verified, and report back anything that fails to
compile.

**Not yet done, deliberately out of scope for Phase 1:**
- No unit/integration tests written yet (plan.md schedules those in Phase 6).
- No `ValidationMessages.properties` created (nothing in Phase 1 depends on it — bean validation
  messages are inlined on the DTOs' annotations instead).
- Phase 2 (`UserService`, full `UserAPI`) not started beyond the one change-password method.

**Added after the fact:** `src/main/resources/TableScript.sql` — the reference DDL Phase 0's
Project Structure lists, plus a one-off seed INSERT for 4 test users (needed for Postman testing
since there's no Add User endpoint until Phase 2). `ddl-auto=update` still creates the table
automatically; the script exists for manual seeding and as the durable schema reference.

## Phase 2 — Admin: User Management

**Plan gap found:** Section 7's exception list has no exception for "Change
Role called with newRole == current role" even though Section 5 requires the
check. Resolved by adding `InvalidRoleChangeException` (400), following the
same one-exception-per-failure pattern as every other entry in that section.

- [x] FE US02 — Home Page (Admin): frontend-only, no backend task, skipped.
- [x] FE US03 / BE US02 — Display all users
  - dto/UserResponseDto.java
  - service/UserService.java, service/UserServiceImpl.java (getAllUsers)
  - api/UserAPI.java — GET /api/users?page= (ADMIN)
- [x] FE US04 / BE US03 — Add user
  - dto/UserRequestDto.java
  - exception/DuplicateUsernameException.java
  - service/UserServiceImpl.java (addUser)
  - api/UserAPI.java — POST /api/users (ADMIN)
- [x] FE US05 / BE US04 — Change user role
  - dto/ChangeRoleRequestDto.java
  - exception/InvalidRoleChangeException.java (new — see plan gap above)
  - service/UserServiceImpl.java (changeUserRole)
  - api/UserAPI.java — PUT /api/users/role (ADMIN)
- [x] FE US06 / BE US05 — Deactivate user
  - dto/DeactivateUserRequestDto.java
  - service/UserServiceImpl.java (deactivateUser)
  - api/UserAPI.java — PUT /api/users/deactivate (ADMIN)
- [x] exception/GlobalExceptionHandler.java — 2 new handlers (409, 400)
- [x] ValidationMessages.properties — user.* keys
- [x] application.properties — Service.DUPLICATE_USERNAME, Service.SAME_ROLE, API.* messages

No SecurityConfig change needed: `.anyRequest().authenticated()` plus
`@PreAuthorize("hasRole('ADMIN')")` (method security already enabled in
Phase 1) is sufficient. pom.xml untouched — no new dependency required.

**Not yet done:** unit/integration tests for User Management (Phase 6, per plan.md).

## Phase 2 — Post-review fixes (phase2_review_actions.md)

- [x] P3 — GlobalExceptionHandler.java: added @ExceptionHandler(AccessDeniedException.class)
      -> 403. Root cause: @RestControllerAdvice intercepts the exception before it can
      reach SecurityConfig's registered RestAccessDeniedHandler.
- [x] P2 — UserAPI.java: @Validated on the class + @Min(0) on getAllUsers' page param.
      ValidationMessages.properties: added user.page.negative.
- [x] P1 — security/SessionUserStatusFilter.java (new): re-validates role/state from
      the DB on every authenticated request. SecurityConfig.java: registered it via
      addFilterAfter(sessionUserStatusFilter, SecurityContextHolderFilter.class).

No pom.xml change for any of the three — AccessDeniedException, @Validated, and
OncePerRequestFilter all come from dependencies already present.
