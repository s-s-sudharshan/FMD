# Fault Management Dashboard — User Story Registry

This file is the single source of truth linking **frontend SRS user stories** to **backend SRS
user stories**. The two documents number their stories independently and don't line up 1:1, so
this mapping exists to avoid ambiguity while implementing.

Legend: **FE** = Frontend SRS story ID, **BE** = Backend SRS story ID, **Phase** = which
implementation phase (see `plan.md`) delivers it.

---

## Auth Module (shared across all roles)

| FE ID | FE Story | BE ID | BE Story | Phase | Notes |
|---|---|---|---|---|---|
| US01 | Login | US01 | Login | Phase 1 | Direct match. Role-specific welcome message + deactivated-user check on both sides. |
| US08 | Logout | — | *(not in BE TOC)* | Phase 1 | Backend SRS doesn't list Logout explicitly, but a session-based API needs it — implemented anyway. |
| US09 | Change Password | — | *(not in BE TOC)* | Phase 1 | Same as above; strength rules from FE SRS section 3.1.9 apply. |
| US20/US21 | Forgot Password | — | *(not in BE TOC)* | Phase 1 | FE doc itself is inconsistent — Table of Contents labels this **US21**, but the section heading in the body says **US20**. Treated as one story; secret question/answer flow per your instruction to include it. |

## Admin Module — User Management

| FE ID | FE Story | BE ID | BE Story | Phase | Notes |
|---|---|---|---|---|---|
| US02 | Home Page - Admin | — | *(no backend equivalent)* | Phase 2 | Frontend-only layout/navigation story. No dedicated API; landing page just re-uses the Login response's `role`. |
| US03 | Display all the users | US02 | Display all the users | Phase 2 | Direct match. FE adds pagination (10/page) — included. |
| US04 | Add users to the system | US03 | Add users to the system | Phase 2 | Direct match. FE adds secret question/answer capture — included per your instruction. |
| US05 | Change user role in the system | US04 | Change user role in the system | Phase 2 | Direct match. |
| US06 | Delete users to the system | US05 | Delete users from the system | Phase 2 | Direct match — both specify **soft delete** (state flips to Deactivated, row stays in DB). |

## Operator Module — Device Management

| FE ID | FE Story | BE ID | BE Story | Phase | Notes |
|---|---|---|---|---|---|
| US10 | Home Page – Operator | — | *(no backend equivalent)* | Phase 3 | Frontend-only, same as US02 above. |
| US11 | Device Configuration (device list) | US07 | Device Details | Phase 3 | Direct match — "list configured devices." |
| US12 | Add device to monitor | US08 | Add device to monitor | Phase 3 | Direct match. |
| US13 | Edit device | US9 | Edit Device | Phase 3 | Direct match — IP address only, per both docs. |
| US14 | Deactivate device monitoring | US10 | Deactivate Device | Phase 3 | Direct match — soft delete, same pattern as User. |

## Manager Module — Fault Handling

| FE ID | FE Story | BE ID | BE Story | Phase | Notes |
|---|---|---|---|---|---|
| US15 | Home Page - Manager | — | *(no backend equivalent)* | Phase 4 | Frontend-only. |
| US16 | Fault Handling | US12 | Fault Details | Phase 4 | Direct match — display/list of faults with severity color-coding (FE-only visual concern). |
| US17 | Fault Acknowledgement | US13 | Fault Acknowledgement | Phase 4 | Direct match. FE adds bulk-select in the UI copy ("acknowledge... multiple alarms in a single action") — bulk endpoint included. |
| US18 | Fault Clearance | US14 | Fault Clearance | Phase 4 | Direct match — only ACKNOWLEDGED faults may be cleared. Bulk endpoint included, same reasoning as US17. |
| US19 | Fault Termination | US15 | Fault Termination | Phase 4 | Direct match — only ACKNOWLEDGED/CLEARED faults may be terminated. Single-item only (no doc mentions bulk terminate). |

## Reporting (cross-cutting)

| FE ID | FE Story | BE ID | BE Story | Phase | Notes |
|---|---|---|---|---|---|
| US07 | Report (Admin tab) | US06 | Report | Phase 5 | FE mockup groups by **status** (clear/acknowledged/terminated/unacknowledged); BE text says "filtering based on severity, acknowledged, cleared, terminated." Implemented as **one API supporting both** grouping types (`?type=SEVERITY` / `?type=STATUS`) per your earlier decision. Depends on the Alarm module existing, hence its own later phase. |

## Backend-only / Deferred

| FE ID | FE Story | BE ID | BE Story | Phase | Notes |
|---|---|---|---|---|---|
| — | *(not in FE SRS)* | US16 | Simulator | Phase 6 (deferred) | No frontend screen for this — it's a backend-internal scheduled job that generates/ingests XML alarms. Per your instruction, **not built now**; `AlarmService` will carry a placeholder method so it can be added later without reshaping the Alarm entity. |

---

## Open Discrepancies Worth Flagging (not blocking, just documented)

1. **Forgot Password numbering** — FE doc's own Table of Contents (US21) disagrees with its section heading (US20). Registry above uses "US20/US21" to avoid picking a wrong number.
2. **Backend ID gap** — Backend TOC jumps from US10 (Deactivate Device) to US12 (Fault Details), skipping US11 entirely in the original document. Nothing appears to be missing functionally — just a numbering gap in the source SRS.
3. **Home Page stories (FE US02, US10, US15)** — These are pure frontend layout/navigation stories with no distinct backend behavior beyond what Login already returns (`role`). Listed here for completeness but they don't produce a backend task on their own.
