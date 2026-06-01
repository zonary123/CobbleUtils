# Skill Registry

**Delegator use only.** Any agent that launches sub-agents reads this registry to resolve compact rules, then injects them directly into sub-agent prompts. Sub-agents do NOT read this registry or individual SKILL.md files.

See `_shared/skill-resolver.md` for the full resolution protocol.

## User Skills

| Trigger | Skill | Path |
|---------|-------|------|
| When writing functions, handling errors, choosing variable names, or reviewing code quality | clean-code | file:///C:/Users/Zonary123/.gemini/config/skills/clean-code/SKILL.md |
| When writing or refactoring classes, modules, APIs, or systems in any programming language | solid-patterns | file:///C:/Users/Zonary123/.gemini/config/skills/solid-patterns/SKILL.md |

## Compact Rules

Pre-digested rules per skill. Delegators copy matching blocks into sub-agent prompts as `## Project Standards (auto-resolved)`.

### clean-code
- **Naming**: Ensure booleans read as questions (`isActive`, `hasPermission`), functions use verbs, classes use nouns, and names reveal intent clearly without cryptic abbreviations.
- **Functions**: Each function MUST do ONE thing only, have ≤ 3 parameters, and be ≤ 20 lines. Use early returns instead of nested if/else.
- **Error Handling**: Fail fast with specific exceptions. Never swallow exceptions silently; provide enough context for debugging.
- **Code Quality**: Keep DRY (zero duplication), remove commented-out or dead code, and ensure comments explain "why", never "what". Keep files < 200 lines if possible.
- **Testing**: Follow AAA structure (Arrange-Act-Assert). Ensure tests are independent, cover edge cases, and describe scenarios clearly in the test name.

### solid-patterns
- **God Objects**: NEVER create a God Object — split into small, focused collaborators.
- **Inheritance vs Composition**: NEVER use inheritance for code reuse alone — prefer composition.
- **Singletons**: NEVER create a Singleton without concrete, documented justification (e.g., hardware/shared resource).
- **Abstractions**: ALWAYS program to interfaces/abstractions, not concrete implementations.
- **Constructors**: ALWAYS keep constructors simple; use Factory or Builder patterns for complex creation.
- **Law of Demeter**: ALWAYS apply the principle of least knowledge.

## Project Conventions

| File | Path | Notes |
|------|------|-------|

No project-level conventions found in the project root.
