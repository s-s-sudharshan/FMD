# Lessons

- **Cross-check a plan's narrative sections against its own reference tables before coding.**
  plan.md's Phase 1 story list (Section 1) and its API table (Section 6) put Change Password and
  Forgot Password on `AuthService`/`AuthController`, while its Services section (Section 5)
  independently puts the same methods on a `UserService` that doesn't exist until Phase 2. Both
  can't be literally true. When a plan has more than one place describing "who owns this
  method," diff them explicitly during the readiness review — don't discover the conflict
  mid-implementation.
