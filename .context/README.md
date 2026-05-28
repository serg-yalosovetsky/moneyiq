# `.context` for `one-money-clone`

This folder is the repository's canonical context layer for humans and AI agents.
It is not imported by the runtime application. Its job is to explain the system shape,
engineering rules, and decisions that are easy to miss when reading only code.

## Read Order

1. `SYSTEM_OVERVIEW.md`
2. `TECH_STACK.md`
3. `ADR_LOG.md`
4. Open one specialized document only for the area you are changing

## File Map

- `SYSTEM_OVERVIEW.md` - system purpose, modules, runtime boundaries, main flows
- `TECH_STACK.md` - languages, frameworks, libraries, and non-negotiable conventions
- `ADR_LOG.md` - architecture decisions that should not be changed casually
- `DB_SCHEMA.md` - current Room persistence model and migration notes
- `UI_CONTRACTS.md` - navigation, screen ownership, visual behavior, and layout constraints
- `CONFIGURATION.md` - build settings, SDK versions, app permissions, and local setup
- `USER_STORIES.md` - end-to-end workflows the app supports
- `TRANSACTION_ACCOUNTING.md` - balance mutation rules and transaction semantics
- `arch_code_style_guide.md` - implementation, UI, logging, and testing rules
- `bug_fixes.md` - regression-sensitive areas and bug-fix rules

## Update Policy

- Update `.context` in the same branch when architecture, persistence, or UI contracts change.
- Keep docs short, high-signal, and grounded in actual code.
- If code and docs disagree, fix the docs or the code immediately; do not leave the mismatch unresolved.
