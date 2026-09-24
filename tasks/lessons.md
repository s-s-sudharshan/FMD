# Lessons

- **Cross-check a plan's narrative sections against its own reference tables before coding.**
  plan.md's Phase 1 story list (Section 1) and its API table (Section 6) put Change Password and
  Forgot Password on `AuthService`/`AuthController`, while its Services section (Section 5)
  independently puts the same methods on a `UserService` that doesn't exist until Phase 2. Both
  can't be literally true. When a plan has more than one place describing "who owns this
  method," diff them explicitly during the readiness review — don't discover the conflict
  mid-implementation.

  - **A plan's exception list (Section 7) can be incomplete relative to its own
  service spec (Section 5).** Change User Role's "prevent setting same role"
  rule had no named exception in Section 7. Don't silently reuse a generic
  exception for a 400 case that deserves its own type — add a small,
  narrowly-scoped exception following the existing naming/HTTP-status pattern,
  and call it out explicitly rather than burying the decision in code.
  
  - **A correctly-configured Spring Security AccessDeniedHandler doesn't catch
  everything.** It only sees denials the filter chain itself raises (URL-based
  authorizeHttpRequests rules). A @PreAuthorize denial happens later, during
  controller invocation, and gets caught by any catch-all @ExceptionHandler(Exception.class)
  in a @RestControllerAdvice first. If method security is used anywhere, the
  global exception handler needs its own explicit AccessDeniedException case —
  don't assume the SecurityConfig-level handler covers it.
- **Session-based auth caches authority/state at login time.** Any feature that
  changes a user's role or active/inactive status needs an explicit plan for
  already-open sessions, or admin actions silently don't take effect until the
  affected user happens to log out. Call this out at design time for every
  future "change a user's access" story, not just after a review catches it.
  
 - **When a design hook says "re-occurrence," define what it does to lifecycle state, not just counters.**
  The first Phase 7 ingestion only incremented `occurrence`, leaving a recurring fault marked
  ACKNOWLEDGED/CLEARED so it looked handled. For any "same event happens again" rule, decide
  explicitly which state fields reset, and add a test for each starting status.
  
- **A derived Spring Data query and a hand-written `@Query` do not treat `%` and `_` the same.** `findByXContainingIgnoreCase` escapes LIKE wildcards, while `like concat('%', :s, '%')` in JPQL does not. When one feature uses both approaches, document the difference in `todo.md` so testing is not surprised by inconsistent behavior.

- **Keep `todo.md` aligned with plan changes, not only code changes.** When a decision drops or defers work, update its checkbox immediately. Before marking a behavior as done, verify that the code actually implements it.

- **Read uploaded plan files from disk first.** The conversation provides only the file path, not necessarily the file contents. 

- **When a later plan overrides an earlier lifecycle rule, sweep every place the old rule was written down.** The extra-features plan changed recurrence from "re-open" to "correlate only active alarms"; the enum javadoc, lessons and todo checkboxes all still described the old behaviour.
