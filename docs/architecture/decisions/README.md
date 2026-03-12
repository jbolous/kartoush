# Architecture Decision Records

This directory contains **Architecture Decision Records (ADRs)** for Kartoush.

ADRs document significant architectural and technical decisions along with the context in which they were made and the tradeoffs involved.

Their purpose is simple: to make architectural reasoning **explicit, reviewable, and durable** as the project evolves.

Kartoush intentionally records decisions early in the design process so that architectural thinking remains visible even as the system evolves.

---

## What Is an ADR?

An Architecture Decision Record captures:

- The context of a problem or design choice
- The decision being considered or made
- Alternatives that were evaluated
- The tradeoffs involved
- The consequences or limitations of the decision

ADRs exist to answer the question:

> Why does the system work this way?

They are preferred over undocumented assumptions or relying on memory.

---

## When to Write an ADR

An ADR should be written when a decision:

- Affects system structure or module boundaries
- Introduces constraints that will be difficult to reverse
- Impacts multiple domains or components
- Represents a meaningful tradeoff rather than an obvious choice

Not every technical choice needs an ADR.  
If a decision is easy to change or isolated in scope, documentation in code or a task description is usually sufficient.

---

## Decision Lifecycle

Decision records may move through several states:

- **Proposed**  
  An open question or architectural exploration.

- **Accepted**  
  A decision has been made and agreed upon.

- **Superseded**  
  A previous decision has been replaced by a newer one.

The status of the decision should be clearly stated at the top of each record.

---

## How ADRs Evolve

Architectural decisions are not permanent.

When a decision changes:

- A **new ADR** is created
- The original ADR remains unchanged
- The newer ADR references the earlier one

This preserves historical context and makes architectural evolution visible over time.

ADRs are not rewritten to reflect current thinking.  
They represent what was believed to be correct at the time.

---

## Relationship to the Code

ADRs explain **why** a decision was made.  
The code shows **how** that decision is implemented.

If the code no longer reflects an ADR:

- Either the code has drifted
- Or a new ADR is needed

In both cases the mismatch should be resolved explicitly.

---

## File Naming

ADRs are stored as Markdown files and named using a simple numeric prefix:

- 0001-project-goals.md
- 0002-architecture-style.md
- 0003-data-ownership.md

Numbers indicate chronological order and do not imply priority.

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
