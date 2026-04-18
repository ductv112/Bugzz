# Phase 1: Foundation & Skeleton - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-18
**Phase:** 01-foundation-skeleton
**Areas discussed:** Module structure, Package naming, Navigation routes, Code style, Permissions library, Test scaffold, Launcher icon

---

## Module Structure

| Option | Description | Selected |
|--------|-------------|----------|
| Single `:app` | One app module, split later if needed. Solo dev + small app → premature modularization hurts | ✓ |
| Multi-module | `:app + :core:camera + :core:render + :feature:home` from day 1. Good for scale but slow when experimenting | |

**User's choice:** Single `:app` (Recommended)

---

## Package Naming

| Option | Description | Selected |
|--------|-------------|----------|
| `com.bugzz.filter.camera` | Match PROJECT.md draft, describes app correctly | ✓ |
| `com.bugzz.android` | Shorter | |
| `io.github.ductv112.bugzz` | Match GitHub handle | |

**User's choice:** `com.bugzz.filter.camera` (Recommended)

---

## Navigation Routes

| Option | Description | Selected |
|--------|-------------|----------|
| Type-safe `@Serializable` | navigation-compose 2.8+ syntax — compile-time check, 2026 standard | ✓ |
| String routes | Simple, familiar, tutorial-friendly, but runtime error-prone | |

**User's choice:** Type-safe `@Serializable` (Recommended)

---

## Code Style / Lint

| Option | Description | Selected |
|--------|-------------|----------|
| Skip at Phase 1 | No ktlint/detekt. Solo dev, no CI, reduce churn. Add later if needed | ✓ |
| ktlint from start | Auto-format Kotlin, prevent style drift | |
| detekt + ktlint | Full static analysis. Overkill for solo | |

**User's choice:** Skip at Phase 1 (Recommended)

---

## Permissions Library

| Option | Description | Selected |
|--------|-------------|----------|
| Raw ActivityResultContracts | `rememberLauncherForActivityResult` — Google standard, no extra dep | ✓ |
| Accompanist Permissions | Convenience wrapper but deprecated; Google recommends raw contracts | |

**User's choice:** Raw ActivityResultContracts (Recommended)

---

## Test Scaffold

| Option | Description | Selected |
|--------|-------------|----------|
| Unit + instrumented scaffold | JUnit 4 unit + AndroidX Test runner placeholder tests. ~10 min setup, pays off later | ✓ |
| Skip tests, add later | App module only, no test infra | |
| Full: unit + instrumented + Compose UI test | Add `ui-test-junit4` for composable tests. Overkill at Phase 1 | |

**User's choice:** Unit + instrumented scaffold (Recommended)

---

## Launcher Icon + App Name

| Option | Description | Selected |
|--------|-------------|----------|
| Placeholder "Bugzz" + AS default icon | Android Studio default adaptive icon, string "Bugzz", swap asset/UI at Phase 6 | ✓ |
| Extract icon from reference APK | Unzip icon.png + drawable resources, use immediately | |

**User's choice:** Placeholder "Bugzz" + AS default (Recommended)

---

## Claude's Discretion

Areas where exact values are Claude-picked during execution:
- Gradle wrapper version (latest compatible with AGP 8.9)
- Kotlinx Serialization version (stable matching Kotlin 2.1.21)
- Placeholder strings in `strings.xml`
- Version catalog plugin alias naming style

## Deferred Ideas

- ktlint / detekt / spotless — reconsider Phase 6 or 7
- Multi-module split — reconsider Phase 4
- Compose UI test harness — defer Phase 6
- Icon/branding extraction — Phase 6
- R8 / ProGuard minify — Phase 7
- CI/CD (GitHub Actions) — out of MVP
- Release signing config — out of MVP
