# 0005 – Build Tool

## Status
Proposed

## Context

Kartoush is transitioning from early design decisions toward implementation.
The build tool will shape day-to-day development workflows, dependency
management, test execution, and how modular boundaries are represented in the
codebase.

We want a tool that:

- supports a modular monolith structure cleanly
- scales from a small project to a multi-module build without friction
- is widely understood and easy to maintain

---

## Decision

Adopt **Gradle** as the build tool.

---

## Rationale

Gradle is a strong fit for a modular monolith because it supports:

- first-class multi-module builds
- flexible build logic when modular boundaries and conventions evolve
- strong ecosystem support for Java projects and common tooling

This choice also aligns with the expectation that Kartoush will benefit from
clear module structure early.

---

## Alternatives Considered

### Maven

**Pros**

- widely known and consistent convention set
- simple mental model for many Java developers

**Cons**

- multi-module builds can become more verbose and rigid
- build customization is possible but often less ergonomic

### Bazel or other build systems

**Pros**

- strong caching and reproducibility characteristics

**Cons**

- higher adoption and maintenance cost
- not necessary for the current project phase

---

## Consequences

- The project will use Gradle conventions for dependency management and testing.
- Module boundaries can be represented directly as Gradle modules.
- Some contributors may need minor onboarding if they are Maven-first.

These tradeoffs are acceptable given the project’s goals and architecture style.

---

## Notes

This decision can be revisited if future constraints require it, but the
default assumption is that Gradle remains the standard build tool for Kartoush.
