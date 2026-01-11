# Architecture Decision Records

This directory contains **Architecture Decision Records (ADRs)** for Kartoush.

ADRs are used to document significant architectural decisions, including the context in which they were made and the tradeoffs involved.

Their purpose is simple: to make architectural reasoning explicit and durable.

---

## What Is an ADR?

An Architecture Decision Record captures:

- A specific architectural decision
- The problem or context that led to the decision
- Alternatives that were considered
- The reasons a particular option was chosen
- Known consequences and limitations

ADRs exist to answer the question:

> Why does the system work this way?

They are preferred over undocumented assumptions or relying on memory.

---

## When to Write an ADR

An ADR should be written when a decision:

- Affects system structure or boundaries  
- Introduces constraints that will be difficult to reverse  
- Impacts multiple modules or domains  
- Represents a meaningful tradeoff rather than an obvious choice  

Not every technical decision needs an ADR.  
If a decision is easy to change or isolated in scope, documentation in code or a task description is usually sufficient.

---

## How ADRs Evolve

Architectural decisions are not permanent.

When a decision changes:

- A **new ADR** is created
- The original ADR remains unchanged
- The newer ADR references the earlier one

This preserves historical context and makes architectural evolution visible over time.

ADRs are not rewritten to reflect current thinking. They represent what was believed to be correct at the time.

---

## Relationship to the Code

ADRs explain **why** a decision was made.  
The code shows **how** that decision is implemented.

If the code no longer reflects an ADR:

- Either the code has drifted  
- Or a new ADR is needed  

In both cases, the mismatch should be resolved explicitly.

---

## Format and Naming

ADRs are stored as Markdown files and named using a simple numeric prefix:

- 0001-use-modular-monolith-architecture.md
- 0002-introduce-facade-layer.md

Lower numbers indicate earlier decisions. Gaps in numbering are acceptable.

---

## What ADRs Are Not

ADRs are intentionally not:

- Design documents for every class or method
- Tutorials on architecture patterns
- Justifications of best practices

They exist to document **decisions**, not implementations.

---

## Independence Disclaimer

These Architecture Decision Records are part of an independent project created on personal time and equipment. They are not affiliated with, derived from, or representative of any employer or proprietary system.
