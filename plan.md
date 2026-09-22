# Fault Management Dashboard — Backend Implementation Plan

**Source of truth:** Frontend SRS (UI-driven flows) + Backend SRS (API-driven flows), reconciled per your decisions below.

## Assumptions / Decisions Locked In
- Alarm entity carries only the fields the frontend table + actions need (not the full 30-field XML schema). Backend is *designed* so a future Simulator/XML-ingestion layer can map into this entity without a rewrite.
- Simulator (US16) is **out of scope for now**, but the Device and Alarm models, plus a `source`/`createdAt` style field, are designed so it can be added later as a scheduled job without breaking the schema.
- `secretQuestion` / `secretAnswer` are included on User (for Forgot Password), with a fixed enum of pre-defined questions.
- `DeviceType` enum = `HUB, SWITCH, ROUTER` (extendable later).
- Report API supports **both** severity-based and status-based grouping via a `type` query param.
- Auth is **server-side session based** (Spring Security session/cookie), not JWT. Role-based access for `ADMIN`, `OPERATOR`, `MANAGER`.
- No forced password change on first login.
- Bulk actions: **included** for Acknowledge and Clear (the two SRS explicitly calls out as needing multi-select), single-item endpoints also provided. Terminate stays single-item only (SRS doesn't mention bulk terminate).

---

## Tech Stack Additions

| Library | Version | Purpose | Where used |
|---|---|---|---|
| ModelMapper | 2.3.5 | Entity ↔ DTO conversion | A single `ModelMapperConfig` `@Configuration` bean, created once in Phase 0, injected into every service that needs entity↔DTO mapping (all of Section 5). |
| Log4j2 | 2.20.0 | Application logging | Replaces Spring Boot's default Logback. Excluded via a `spring-boot-starter-logging` exclusion on `spring-boot-starter-web`, added via `spring-boot-starter-log4j2`. Config file `src/main/resources/log4j2.properties` (matching your existing project's format, rather than the XML alternative). Logging added at: controller entry (request received), service-layer key decisions (validation failures, state transitions), and in `GlobalExceptionHandler` (every caught exception logged with stack trace at `ERROR`, validation-type failures at `WARN`). |
| Lombok | latest stable | Boilerplate reduction | All entities and DTOs (see Sections 2 and 4 for per-class annotation guidance). |

**Maven dependencies to add (Phase 0):**
```xml
<dependency>
    <groupId>org.modelmapper</groupId>
    <artifactId>modelmapper</artifactId>
    <version>2.3.5</version>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-log4j2</artifactId>
    <version>2.19.0</version>
</dependency>
<!-- exclude default logging from spring-boot-starter-web -->
```
`ModelMapperConfig` (`@Configuration`, bean `ModelMapper modelMapper()`) is created once in Phase 0
and reused everywhere — not recreated per module.

**Lombok usage convention:**
- **Entities:** `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`. Deliberately
  **not** `@Data` on entities — `@Data`'s generated `equals()`/`hashCode()` over all fields (and
  its `toString()` pulling in lazy-loaded relations) causes classic JPA pitfalls. `@EqualsAndHashCode(of = "id")`
  is added explicitly where equality checks are needed.
- **DTOs:** `@Data @NoArgsConstructor @AllArgsConstructor @Builder` is fine — no JPA relations
  to worry about, so the shortcut is safe here.
- **Enums:** no Lombok needed.

---

## Project Structure

Adapted from your previous (wellness-tracker) project outline. Two small additions beyond the
7 packages originally specified, both justified below; naming convention (interface + `Impl`
for services, `DTO` suffix) matches your existing project's style.

```
fault-management-dashboard/
│
├── src/main/java/com/infy/
│   │
│   ├── FaultManagementDashboardApplication.java
│   │
│   ├── api/                                    (REST Controllers)
│   │   ├── AuthAPI.java
│   │   ├── UserAPI.java
│   │   ├── DeviceAPI.java
│   │   ├── AlarmAPI.java
│   │   └── ReportAPI.java
│   │
│   ├── dto/
│   │   ├── LoginRequestDTO.java
│   │   ├── LoginResponseDTO.java
│   │   ├── ChangePasswordRequestDTO.java
│   │   ├── ForgotPasswordStep1RequestDTO.java
│   │   ├── ForgotPasswordStep2RequestDTO.java
│   │   ├── ResetPasswordRequestDTO.java
│   │   ├── UserRequestDTO.java
│   │   ├── UserResponseDTO.java
│   │   ├── ChangeRoleRequestDTO.java
│   │   ├── DeactivateUserRequestDTO.java
│   │   ├── DeviceRequestDTO.java
│   │   ├── DeviceResponseDTO.java
│   │   ├── EditDeviceRequestDTO.java
│   │   ├── AlarmResponseDTO.java
│   │   ├── AlarmNoteUpdateRequestDTO.java
│   │   ├── BulkAlarmActionRequestDTO.java
│   │   ├── AlarmSearchRequestDTO.java
│   │   ├── ReportRequestDTO.java
│   │   ├── ReportSliceDTO.java
│   │   ├── ReportResponseDTO.java
│   │   ├── ApiResponseDTO.java
│   │   └── ErrorResponseDTO.java
│   │
│   ├── entity/
│   │   ├── User.java
│   │   ├── Device.java
│   │   └── Alarm.java
│   │
│   ├── enums/
│   │   ├── Role.java
│   │   ├── UserState.java
│   │   ├── SecretQuestion.java
│   │   ├── DeviceType.java
│   │   ├── DeviceState.java
│   │   ├── Severity.java
│   │   ├── TrapType.java
│   │   ├── AlarmStatus.java
│   │   └── ReportType.java
│   │
│   ├── exception/
│   │   ├── UserNotFoundException.java
│   │   ├── DuplicateUsernameException.java
│   │   ├── InvalidCredentialsException.java
│   │   ├── UserDeactivatedException.java
│   │   ├── WeakPasswordException.java
│   │   ├── PasswordMismatchException.java
│   │   ├── InvalidSecretAnswerException.java
│   │   ├── DeviceNotFoundException.java
│   │   ├── DuplicateDeviceException.java
│   │   ├── InvalidIpAddressException.java
│   │   ├── AlarmNotFoundException.java
│   │   ├── InvalidAlarmStateTransitionException.java
│   │   ├── UnauthorizedActionException.java
│   │   └── GlobalExceptionHandler.java              (@ControllerAdvice)
│   │
│   ├── repository/                             (Spring Data Repositories — see note 1 below)
│   │   ├── UserRepository.java
│   │   ├── DeviceRepository.java
│   │   └── AlarmRepository.java
│   │
│   ├── security/
│   │   ├── SecurityConfig.java
│   │   ├── CustomUserDetailsService.java
│   │   ├── PasswordEncoderConfig.java
│   │   └── CurrentUserResolver.java                  (fetches logged-in user from SecurityContext — used by change-password, and future audit fields)
│   │
│   ├── service/                                (Interfaces + Impl, matching your existing pattern)
│   │   ├── AuthService.java / AuthServiceImpl.java
│   │   ├── UserService.java / UserServiceImpl.java
│   │   ├── DeviceService.java / DeviceServiceImpl.java
│   │   ├── AlarmService.java / AlarmServiceImpl.java
│   │   └── ReportService.java / ReportServiceImpl.java
│   │
│   └── config/                                 (see note 2 below)
│       └── ModelMapperConfig.java
│
└── src/main/resources/
    ├── application.properties
    ├── log4j2.properties
    ├── TableScript.sql                          (reference DDL, mirrors your entities)
    └── ValidationMessages.properties            (externalized Bean Validation messages)
```

**Note 1 — `repository` package added:** Spring Data JPA repositories don't reasonably fit into
any of the 7 originally-specified packages (`entity`, `dto`, `enums`, `service`, `api`,
`exception`, `security`) — your own wellness-tracker project reached the same conclusion and
added one. Kept minimal: interfaces only, no custom impl classes needed for this project's
query complexity.

**Note 2 — `config` package added:** a single `ModelMapperConfig` bean doesn't belong in
`security` (unrelated concern) and doesn't fit the other 6 packages either. Rather than force it
into `security` for the sake of avoiding one more package, it gets its own minimal `config`
package — same reasoning as `repository` above.

**Deviations from your reference project, called out explicitly:**
- **No `JwtAuthenticationFilter` / `JwtService`** — this project uses server-side session auth
  per your earlier instruction, not JWT, so those two classes have no equivalent here.
- **No `LoggingAspect` (AOP-based logging)** — logging is done via direct Log4j2 calls at
  controller entry, service-layer decisions, and in `GlobalExceptionHandler`, per the "Tech
  Stack Additions" section above. Say the word if you'd rather have an AOP aspect instead — easy
  to add.
- **No OTP-based password reset (`PasswordResetOtp` entity/repository)** — forgot-password here
  uses the secret-question/answer flow from the frontend SRS instead, per your earlier
  instruction to include that. Will revisit if your new user stories file specifies OTP.

---

## Working Agreement: Story-by-Story Delivery

Implementation proceeds **one user story at a time**, in the order given in `user-stories.md`,
not phase-in-bulk. Within a phase, stories are still built in the sequence listed below (Section
1) since some stories share entities/config that must exist first (e.g. within Phase 1, Login
must exist before Change Password can be tested against a real session).

**After every user story (or, where a story is trivially small, after every phase) is
implemented, the response will include a list of every file created or modified for that
story** — full paths, grouped by package — before moving to the next story. This makes each step
independently reviewable and gives a clean commit boundary.

---

## 1. Implementation Phases

Full FE ↔ BE user story mapping for every phase lives in **`user-stories.md`** — this section
just shows the build order and which stories each phase closes out.

### Phase 0 — Project Bootstrap (infrastructure, no user stories)
- Spring Boot project setup, MySQL config, base package structure (`com.infy.*`).
- All enum classes (Section 3) — created first since entities depend on them.
- Base entity + repository skeletons, `application.properties`.
- Global exception handler skeleton (`com.infy.exception`) and `ApiResponseDto`/`ErrorResponseDto`.

### Phase 1 — Authentication & Session Management
**Stories:** FE US01, US08, US09, US20/US21 ↔ BE US01. This phase gates every other phase —
nothing else can be tested end-to-end without it. Built in this story order:

1. **Story 1.0 (shared groundwork, not a numbered story on its own):** `User` entity (incl.
   `secretQuestion`/`secretAnswer`), `Role`/`UserState`/`SecretQuestion` enums, `UserRepository`,
   `PasswordEncoderConfig`, base `SecurityConfig` (CSRF cookie repo, CORS, session policy),
   `CustomUserDetailsService`.
2. **FE US01 / BE US01 — Login:** `LoginRequestDto`, `LoginResponseDto`, `AuthService.login()`,
   `AuthController` `/api/auth/login`. Testable end-to-end after this step alone.
3. **FE US08 — Logout:** `AuthService.logout()`, `/api/auth/logout`.
4. **FE US09 — Change Password:** `ChangePasswordRequestDto`, `AuthService.changePassword()`,
   `/api/users/change-password` (kept on the User controller since it's per-account, not
   session-level).
5. **FE US20/US21 — Forgot Password:** the 3 DTOs (`ForgotPasswordStep1/2RequestDto`,
   `ResetPasswordRequestDto`), `AuthService` methods for question-lookup / answer-verify /
   reset, and the 3 `/api/auth/forgot-password/*` endpoints.

### Phase 2 — Admin: User Management
**Stories:** FE US02–US06 ↔ BE US02–US05. Role-based access restricted to ADMIN throughout.
Built in this story order:

1. **FE US02 — Home Page (Admin):** frontend-only, no backend task — noted here so it isn't
   mistaken for a missed step.
2. **FE US03 / BE US02 — Display all users:** `UserResponseDto`, `UserService.getAllUsers()`
   (paginated), `GET /api/users`.
3. **FE US04 / BE US03 — Add user:** `UserRequestDto`, `UserService.addUser()` (uniqueness +
   password-strength validation), `POST /api/users`.
4. **FE US05 / BE US04 — Change user role:** `ChangeRoleRequestDto`,
   `UserService.changeUserRole()`, `PUT /api/users/role`.
5. **FE US06 / BE US05 — Delete (deactivate) user:** `DeactivateUserRequestDto`,
   `UserService.deactivateUser()`, `PUT /api/users/deactivate`.

### Phase 3 — Operator: Device Management
**Stories:** FE US10–US14 ↔ BE US07–US10. Role-based access restricted to OPERATOR throughout.
Built in this story order:

1. **FE US10 — Home Page (Operator):** frontend-only, no backend task.
2. **Shared groundwork:** `Device` entity, `DeviceType`/`DeviceState` enums, `DeviceRepository`.
3. **FE US11 / BE US07 — Device list:** `DeviceResponseDto`,
   `DeviceService.getAllActiveDevices()` (paginated), `GET /api/devices`.
4. **FE US12 / BE US08 — Add device:** `DeviceRequestDto` (unique serial+IP, IPv4 validation),
   `DeviceService.addDevice()`, `POST /api/devices`.
5. **FE US13 / BE US9 — Edit device:** `EditDeviceRequestDto`, `DeviceService.editDevice()`,
   `PUT /api/devices`.
6. **FE US14 / BE US10 — Deactivate device:** `DeviceService.deactivateDevice()`,
   `PUT /api/devices/deactivate`.

### Phase 4 — Manager: Fault/Alarm Handling
**Stories:** FE US15–US19 ↔ BE US12–US15. Read access open to any authenticated user; action
endpoints (ack/clear/terminate/notes) restricted to MANAGER. Built in this story order:

1. **FE US15 — Home Page (Manager):** frontend-only, no backend task.
2. **Shared groundwork:** `Alarm` entity, `Severity`/`TrapType`/`AlarmStatus` enums,
   `AlarmRepository` (device seeded via Phase 3 — this is where the "insert a test alarm
   manually since Simulator isn't built yet" step from Section 10 applies).
3. **FE US16 / BE US12 — Fault Details (list):** `AlarmResponseDto`,
   `AlarmSearchRequestDto`, `AlarmService.getAllAlarms()` (paginated, filterable),
   `GET /api/alarms`. Also covers the notes-update sub-feature (`AlarmNoteUpdateRequestDto`,
   `PUT /api/alarms/{id}/notes`).
4. **FE US17 / BE US13 — Fault Acknowledgement:** `AlarmService.acknowledgeAlarm()` +
   `acknowledgeAlarmsBulk()`, `PUT /api/alarms/{id}/acknowledge` and
   `PUT /api/alarms/acknowledge/bulk`.
5. **FE US18 / BE US14 — Fault Clearance:** `AlarmService.clearAlarm()` +
   `clearAlarmsBulk()`, `PUT /api/alarms/{id}/clear` and `PUT /api/alarms/clear/bulk`.
6. **FE US19 / BE US15 — Fault Termination:** `AlarmService.terminateAlarm()`,
   `PUT /api/alarms/{id}/terminate` (single-item only, no bulk per Section 6).

### Phase 5 — Reporting (cross-cutting)
**Stories:** FE US07 (Admin Report) ↔ BE US06. Depends on Phase 4's `Alarm` entity existing,
hence it comes after Fault Handling rather than alongside Admin in Phase 2. Single story, built
in one step:

1. **FE US07 / BE US06 — Report:** `ReportType` enum, `ReportRequestDto`, `ReportSliceDto`,
   `ReportResponseDto`, `ReportService.generateReport()` (branches on `SEVERITY` vs `STATUS`),
   `ReportController`, `GET /api/reports?type=`.

### Phase 6 — Testing & Integration Pass
- Unit tests (services), integration tests (controllers), manual Postman collection covering
  every phase above end-to-end.
- Confirm every frontend screen has a matching, correctly-shaped API (cross-check against
  Section 9).

### Phase 7 — Simulator (Future / Deferred)
**Story:** BE US16 (no frontend equivalent)
- Not built now, per your instruction. `AlarmService` carries a placeholder
  `ingestAlarmsFromXml(...)` method signature so this can be added later — scheduled XML
  generator + XML-to-DB ingestion job — without reshaping the `Alarm` entity.

---

## 2. Entities and Database

*(Lombok annotations for every entity below follow the convention in "Tech Stack Additions" —
`@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`, no `@Data`.)*

### 2.1 `User`
| Field | Type | Notes |
|---|---|---|
| id | Long (PK) | auto |
| username | String | unique, not null |
| password | String | encrypted (BCrypt) |
| role | Enum `Role` | ADMIN / OPERATOR / MANAGER |
| userState | Enum `UserState` | ACTIVATED / DEACTIVATED, default ACTIVATED |
| secretQuestion | Enum `SecretQuestion` | for forgot password |
| secretAnswer | String | stored (consider hashing later, plain for now per SRS simplicity) |

### 2.2 `Device`
| Field | Type | Notes |
|---|---|---|
| id | Long (PK) | auto |
| serialNumber | String | unique, not null |
| ipAddress | String | unique, not null, validated IPv4 format (reject 0.0.0.0/255.255.255.255 etc.) |
| deviceType | Enum `DeviceType` | HUB / SWITCH / ROUTER |
| deviceState | Enum `DeviceState` | ACTIVATED / DEACTIVATED, default ACTIVATED |

### 2.3 `Alarm` (Fault)
| Field | Type | Notes |
|---|---|---|
| id | Long (PK) | auto |
| device | Device (FK, ManyToOne) | links to configured device |
| deviceIp | String | denormalized for quick table read (from device, kept for report/history even if device is later deactivated) |
| serialNumber | String | denormalized, same reason |
| deviceType | Enum `DeviceType` | denormalized |
| severity | Enum `Severity` | CLEAR / WARNING / MAJOR / SEVERE / CRITICAL |
| trap | Enum `TrapType` | one of the 4 CISCO-BGP4-MIB traps |
| notes | String | free text, editable by operator/manager |
| occurrence | Integer | count of times this fault has re-occurred, default 1 |
| status | Enum `AlarmStatus` | UNACKNOWLEDGED / ACKNOWLEDGED / CLEARED / TERMINATED |
| createdAt | LocalDateTime | when the alarm was first recorded (also doubles as the future "ingested from XML at" timestamp) |
| updatedAt | LocalDateTime | last status change |

**Relationships:** `Device 1 —* Alarm`. Deleting a device is soft (deactivate), never hard-deleted, so alarm history stays valid.

**Constraints:**
- `Device.serialNumber`, `Device.ipAddress`: unique.
- `User.username`: unique.
- All state/status fields: NOT NULL with DB default matching entity default.

---

## 3. Enums (`com.infy.enums`)

| Enum | Values | Used in |
|---|---|---|
| `Role` | ADMIN, OPERATOR, MANAGER | User, security role checks |
| `UserState` | ACTIVATED, DEACTIVATED | User, login validation |
| `SecretQuestion` | e.g. FIRST_PET, BIRTH_CITY, FAVOURITE_TEACHER (fixed pre-defined list) | User, Forgot Password |
| `DeviceType` | HUB, SWITCH, ROUTER | Device, Alarm (denormalized) |
| `DeviceState` | ACTIVATED, DEACTIVATED | Device |
| `Severity` | CLEAR, WARNING, MAJOR, SEVERE, CRITICAL | Alarm, color-coded display, severity report |
| `TrapType` | CBGP_FSM_STATE_CHANGE, CBGP_BACKWARD_TRANSITION, CBGP_PREFIX_THRESHOLD_EXCEEDED, CBGP_PREFIX_THRESHOLD_CLEAR | Alarm |
| `AlarmStatus` | UNACKNOWLEDGED, ACKNOWLEDGED, CLEARED, TERMINATED | Alarm, status report, ACK/Clear/Terminate logic |
| `ReportType` | SEVERITY, STATUS | Report API grouping switch |

---

## 4. DTOs (`com.infy.dto`)

*(Lombok: `@Data @NoArgsConstructor @AllArgsConstructor @Builder` on every DTO below, per "Tech
Stack Additions." Entity↔DTO conversion goes through the shared `ModelMapper` bean inside each
service — no manual field-by-field mapping code.)*

### Auth
- `LoginRequestDto` — `username`, `password` (both `@NotBlank`)
- `LoginResponseDto` — `username`, `role`, `message`

### User Module
- `UserRequestDto` (Add User) — `username` (@NotBlank), `password` (@NotBlank, pattern-checked for strength), `role` (@NotNull), `secretQuestion` (@NotNull), `secretAnswer` (@NotBlank)
- `UserResponseDto` — `id`, `username`, `role`, `userState` (never expose password)
- `ChangeRoleRequestDto` — `username`, `newRole`
- `DeactivateUserRequestDto` — `username`
- `ForgotPasswordStep1RequestDto` — `username`
- `ForgotPasswordStep2RequestDto` — `username`, `answer`
- `ResetPasswordRequestDto` — `username`, `newPassword`, `confirmPassword` (cross-field match validation)
- `ChangePasswordRequestDto` — `currentPassword`, `newPassword`, `confirmPassword`

### Device Module
- `DeviceRequestDto` (Add Device) — `serialNumber` (@NotBlank), `ipAddress` (@NotBlank + IPv4 pattern), `deviceType` (@NotNull)
- `DeviceResponseDto` — `id`, `serialNumber`, `ipAddress`, `deviceType`, `deviceState`
- `EditDeviceRequestDto` — `serialNumber` (identifies device), `newIpAddress`

### Alarm / Fault Module
- `AlarmResponseDto` — `id`, `deviceIp`, `serialNumber`, `deviceType`, `severity`, `trap`, `notes`, `occurrence`, `status`
- `AlarmNoteUpdateRequestDto` — `alarmId`, `notes`
- `BulkAlarmActionRequestDto` — `alarmIds: List<Long>`
- `AlarmSearchRequestDto` — optional filters: `deviceIp`, `severity`, `status` (used for the search bar)

### Report Module
- `ReportRequestDto` — `type` (SEVERITY / STATUS)
- `ReportResponseDto` — `List<ReportSliceDto>` where `ReportSliceDto` = `label`, `count`

### Common
- `ApiResponseDto<T>` — generic wrapper: `success`, `message`, `data`
- `ErrorResponseDto` — `timestamp`, `status`, `error`, `message`, `path`

**Key validations:** password strength regex per SRS tiers (0–3 not accepted, 4–6 weak, 6–8 medium, >8 strong — enforce **minimum 4 chars accepted**, classify rest), IPv4 format + reject `0.0.0.0`/`255.255.255.255`, unique username/serial/IP checked in service layer (not just DB constraint) so a clean error message can be returned.

---

## 5. Services (`com.infy.service`)

### `AuthService`
- `login(LoginRequestDto)` → validate credentials, check `UserState`, create session, return role-specific welcome message.
- `logout()` → invalidate session.

### `UserService`
- `getAllUsers()` — paginated (10/page) list with role.
- `addUser(UserRequestDto)` — validate uniqueness + password strength, encode password, set default `ACTIVATED`, save.
- `changeUserRole(ChangeRoleRequestDto)` — update role, prevent setting same role.
- `deactivateUser(username)` — set `userState = DEACTIVATED` (never hard delete).
- `changePassword(username, ChangePasswordRequestDto)` — verify current password, validate strength, confirm match, re-encode, save.
- `getSecretQuestion(username)` — step 1 of forgot password.
- `verifySecretAnswer(username, answer)` — step 2.
- `resetPassword(ResetPasswordRequestDto)` — step 3, only after step 2 success (short-lived server-side flag/token in session).

### `DeviceService`
- `getAllActiveDevices()` — paginated (10/page), operator's active devices.
- `addDevice(DeviceRequestDto)` — validate uniqueness (serial + IP) + IP format, default `ACTIVATED`.
- `editDevice(EditDeviceRequestDto)` — update IP only, re-validate uniqueness/format.
- `deactivateDevice(serialNumber)` — set `deviceState = DEACTIVATED`, soft delete.

### `AlarmService`
- `getAllAlarms(AlarmSearchRequestDto, page)` — paginated, filterable list, default = all monitored devices' alarms.
- `acknowledgeAlarm(id)` / `acknowledgeAlarmsBulk(ids)` — only if currently `UNACKNOWLEDGED`, else throw.
- `clearAlarm(id)` / `clearAlarmsBulk(ids)` — only if currently `ACKNOWLEDGED`, else throw.
- `terminateAlarm(id)` — only if `ACKNOWLEDGED` or `CLEARED`, else throw.
- `updateNotes(AlarmNoteUpdateRequestDto)` — free text update, any status.
- (Design hook, not built now) `ingestAlarmsFromXml(...)` — placeholder method signature reserved for Simulator integration.

### `ReportService`
- `generateReport(ReportRequestDto)` — branch on `type`:
  - `SEVERITY` → group alarm counts by `Severity`.
  - `STATUS` → group alarm counts by `AlarmStatus`.

---

## 6. APIs (`com.infy.api`)

All responses wrapped in `ApiResponseDto`. Auth = session cookie; role checks via `@PreAuthorize` / method security.

### Auth
| Method | Endpoint | Request | Response | Auth |
|---|---|---|---|---|
| POST | `/api/auth/login` | `LoginRequestDto` | `LoginResponseDto` | public |
| POST | `/api/auth/logout` | — | success message | any authenticated |
| POST | `/api/auth/forgot-password/question` | `{username}` | `{secretQuestion}` | public |
| POST | `/api/auth/forgot-password/verify` | `{username, answer}` | success/failure flag | public |
| POST | `/api/auth/forgot-password/reset` | `ResetPasswordRequestDto` | success message | public (only after verify step) |

### User (ADMIN only unless noted)
| Method | Endpoint | Request | Response | Validations |
|---|---|---|---|---|
| GET | `/api/users?page={n}` | — | paged `List<UserResponseDto>` | ADMIN |
| POST | `/api/users` | `UserRequestDto` | `UserResponseDto` | unique username, password strength, ADMIN |
| PUT | `/api/users/role` | `ChangeRoleRequestDto` | success message | valid role, ADMIN |
| PUT | `/api/users/deactivate` | `DeactivateUserRequestDto` | success message | user exists, ADMIN |
| PUT | `/api/users/change-password` | `ChangePasswordRequestDto` | success message | any authenticated user, own account only |

### Device (OPERATOR only unless noted)
| Method | Endpoint | Request | Response | Validations |
|---|---|---|---|---|
| GET | `/api/devices?page={n}` | — | paged `List<DeviceResponseDto>` | OPERATOR |
| POST | `/api/devices` | `DeviceRequestDto` | `DeviceResponseDto` | unique serial+IP, valid IP, OPERATOR |
| PUT | `/api/devices` | `EditDeviceRequestDto` | success message | unique IP, valid IP, OPERATOR |
| PUT | `/api/devices/deactivate` | `{serialNumber}` | success message | device exists, OPERATOR |

### Alarm / Fault (MANAGER for actions, all roles can read)
| Method | Endpoint | Request | Response | Validations |
|---|---|---|---|---|
| GET | `/api/alarms?page={n}&deviceIp=&severity=&status=` | — | paged `List<AlarmResponseDto>` | any authenticated |
| PUT | `/api/alarms/{id}/acknowledge` | — | success message | must be UNACKNOWLEDGED, MANAGER |
| PUT | `/api/alarms/acknowledge/bulk` | `BulkAlarmActionRequestDto` | success message | all must be UNACKNOWLEDGED, MANAGER |
| PUT | `/api/alarms/{id}/clear` | — | success message | must be ACKNOWLEDGED, MANAGER |
| PUT | `/api/alarms/clear/bulk` | `BulkAlarmActionRequestDto` | success message | all must be ACKNOWLEDGED, MANAGER |
| PUT | `/api/alarms/{id}/terminate` | — | success message | must be ACKNOWLEDGED or CLEARED, MANAGER |
| PUT | `/api/alarms/{id}/notes` | `AlarmNoteUpdateRequestDto` | success message | MANAGER |

### Report (any authenticated role)
| Method | Endpoint | Request | Response |
|---|---|---|---|
| GET | `/api/reports?type=SEVERITY` | — | `ReportResponseDto` |
| GET | `/api/reports?type=STATUS` | — | `ReportResponseDto` |

---

## 7. Exception Handling (`com.infy.exception`)

Custom exceptions:
- `UserNotFoundException`
- `DuplicateUsernameException`
- `InvalidCredentialsException`
- `UserDeactivatedException`
- `WeakPasswordException`
- `PasswordMismatchException`
- `InvalidSecretAnswerException`
- `DeviceNotFoundException`
- `DuplicateDeviceException`
- `InvalidIpAddressException`
- `AlarmNotFoundException`
- `InvalidAlarmStateTransitionException` (e.g., clearing an unacknowledged alarm)
- `UnauthorizedActionException`

`GlobalExceptionHandler` (`@ControllerAdvice`) maps each to an HTTP status + `ErrorResponseDto`:
- 400 — validation failures, weak password, mismatch, invalid IP, invalid state transition
- 401 — invalid credentials, not logged in
- 403 — wrong role attempting an action, user deactivated
- 404 — user/device/alarm not found
- 409 — duplicate username/serial/IP
- 500 — fallback generic handler

---

## 8. Security (`com.infy.security`)

- **Auth model:** server-side session (Spring Security default `HttpSession`, `JSESSIONID` cookie). No JWT.
- **CSRF: stays enabled** (not disabled). Since this is a session-cookie-based API consumed by
  a separate React origin, use Spring Security's cookie-based CSRF token repository so the SPA
  can read and echo the token:
  - `CookieCsrfTokenRepository.withHttpOnlyFalse()` — issues an `XSRF-TOKEN` cookie readable by
    JS (httpOnly must be false for the SPA to read it; the session cookie itself stays httpOnly).
  - `CsrfTokenRequestAttributeHandler` (or `XorCsrfTokenRequestAttributeHandler` per Spring
    Security version) configured so the token is available on every response, not just after a
    prior GET.
  - Frontend contract: on app load, React fires a lightweight `GET` (e.g. hits
    `/api/auth/session` or any authenticated-optional endpoint) to receive the `XSRF-TOKEN`
    cookie, then reads that cookie and sends it back as the `X-XSRF-TOKEN` header on every
    state-changing request (`POST`/`PUT`/`DELETE`). This is documented again in Section 9.
  - `GET` requests are exempt from CSRF by default (read-only), so list/report endpoints don't
    need the header.
- `SecurityConfig` — configure `/api/auth/login` as the entry point, session creation policy
  `IF_REQUIRED`, CORS config to allow the React frontend origin **with credentials** (`
  Access-Control-Allow-Credentials: true`, explicit allowed origin — not `*`, since credentialed
  requests can't use a wildcard origin), and CORS-expose the `XSRF-TOKEN` handling as needed.
- `CustomUserDetailsService` — loads `User` by username, checks `UserState` before allowing login, maps `Role` to Spring Security authority (`ROLE_ADMIN`, etc.).
- `PasswordEncoderConfig` — `BCryptPasswordEncoder` bean, used for storing and verifying passwords.
- Method-level security (`@EnableMethodSecurity` + `@PreAuthorize("hasRole('ADMIN')")` etc.) on controller methods per the role table in Section 6.
- Login failure vs deactivated-account messages kept distinct per SRS ("Wrong username or password" vs "User is Deactivated").

---

## 9. Frontend ↔ Backend Communication

| Frontend Screen | Backend API(s) | Notes |
|---|---|---|
| Login page | `POST /api/auth/login` | On success, session cookie set; frontend routes by `role` in response |
| Forgot Password (3-step flow) | `POST /api/auth/forgot-password/question` → `/verify` → `/reset` | Sequential; frontend keeps state between steps |
| Admin — User Details tab | `GET /api/users?page=` | Table of username + role |
| Admin — Add User | `POST /api/users` | Shows field errors from validation |
| Admin — Change Role | `PUT /api/users/role` | Dropdown limited to the other two roles client-side |
| Admin — Delete User | `PUT /api/users/deactivate` | Confirmation modal → API call |
| Admin/Operator/Manager — Change Password | `PUT /api/users/change-password` | On success, frontend redirects to login |
| Admin — Report (status pie chart) | `GET /api/reports?type=STATUS` | Chart categories: clear/acknowledged/terminated/unacknowledged |
| Operator — Device list | `GET /api/devices?page=` | Active devices only |
| Operator — Add Device | `POST /api/devices` | Field-level errors mapped from API |
| Operator — Edit Device | `PUT /api/devices` | IP-only edit, serial/type read-only in UI |
| Operator — Delete/Deactivate Device | `PUT /api/devices/deactivate` | Confirmation modal → API call |
| Manager — Fault Handling table | `GET /api/alarms?...` | Color-coded rows by `severity` |
| Manager — ACK button | `PUT /api/alarms/{id}/acknowledge` (or bulk) | Disabled if already ACKNOWLEDGED |
| Manager — Clear button | `PUT /api/alarms/{id}/clear` (or bulk) | Error toast if not yet acknowledged |
| Manager — Terminate button | `PUT /api/alarms/{id}/terminate` | Removed from default fault-handling view after termination (filtered client-side or via status query param) |
| Manager — Report (severity chart) | `GET /api/reports?type=SEVERITY` | |
| Logout (all roles) | `POST /api/auth/logout` | Clears session, redirect to login |

All list responses are wrapped: `{ success, message, data: [...] }` so the frontend can consistently check `success` before rendering.

**CSRF handshake the React app must implement:** fire one `GET` on app load to receive the
`XSRF-TOKEN` cookie, then read that cookie value and attach it as the `X-XSRF-TOKEN` header on
every subsequent `POST`/`PUT`/`DELETE` call (login, add/edit/deactivate user or device,
acknowledge/clear/terminate alarms, change/reset password). All `fetch`/`axios` calls also need
`credentials: 'include'` (or `withCredentials: true`) so the session and CSRF cookies are sent.

---

## 10. Testing

- **Unit tests (service layer):**
  - User: duplicate username rejection, password strength tiers, deactivate flow, forgot password 3-step flow, wrong current password on change-password.
  - Device: duplicate serial/IP rejection, invalid IP formats (including 0.0.0.0/255.255.255.255), deactivate flow.
  - Alarm: valid/invalid state transitions for ACK → Clear → Terminate, bulk ACK with mixed valid/invalid states, notes update.
  - Report: correct grouping counts for both SEVERITY and STATUS types.
- **Integration tests (controller layer):**
  - Login success/failure/deactivated-user cases.
  - Role-based access denial (e.g., OPERATOR hitting an ADMIN-only endpoint → 403).
  - Full CRUD happy paths for User, Device, Alarm.
- **Manual Postman scenarios:**
  - End-to-end: add device → (manually insert a test alarm row for now since simulator isn't built) → acknowledge → clear → terminate → verify it disappears from active fault list but remains in DB.
  - Session expiry / logout then hitting a protected endpoint.

---

## 11. Final Checklist

- [ ] **Phase 0:** Project setup + MySQL connection configured, ModelMapper 3.2.4 + Log4j2 2.19.0 + Lombok added to `pom.xml`, `log4j2.xml` configured, `ModelMapperConfig` bean created, all enums created, entity/repo skeletons in place
- [ ] **Phase 1, story by story:** groundwork → Login → Logout → Change Password → Forgot Password — file list reported after each
- [ ] **Phase 2, story by story:** Home Page (FE-only, skip) → Display users → Add user → Change role → Deactivate user — file list reported after each
- [ ] **Phase 3, story by story:** Home Page (FE-only, skip) → groundwork → Device list → Add device → Edit device → Deactivate device — file list reported after each
- [ ] **Phase 4, story by story:** Home Page (FE-only, skip) → groundwork → Fault list/notes → Acknowledge → Clear → Terminate — file list reported after each
- [ ] **Phase 5:** Report (severity + status grouping) — file list reported
- [ ] Global exception handler + all custom exceptions implemented and mapped to correct HTTP status
- [ ] CORS configured for React frontend origin with credentials; CSRF cookie handshake verified against a real frontend call
- [ ] **Phase 6:** Unit tests written and passing; integration tests written and passing; Postman collection built and manually verified end-to-end
- [ ] Every entry in `user-stories.md` cross-checked off as implemented
- [ ] **Phase 7 (deferred):** Simulator module design hook confirmed usable — `ingestAlarmsFromXml` placeholder in `AlarmService` reviewed
