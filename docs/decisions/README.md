# Decision Records

This directory contains records of significant design and technical decisions for Kartoush.

The goal of these records is to make decision-making **explicit, reviewable, and durable** as the project evolves.

Kartoush is intentionally documenting decisions before implementation begins.

---

## What Is a Decision Record?

A decision record captures:
- The context of a problem or choice
- The decision being considered or made
- The alternatives evaluated
- The tradeoffs involved

Not every choice needs a decision record.  
Decision records are reserved for **meaningful or potentially irreversible decisions**.

---

## Decision Lifecycle

Decision records may move through the following states:

- **Proposed**  
  An open question or area of exploration

- **Accepted**  
  A decision has been made and agreed upon

- **Superseded**  
  A previous decision has been replaced by a newer one

The current status should be clearly stated at the top of each record.

---

## File Naming

Decision records are named using a simple numeric prefix for ordering:

0001-project-goals.md  
0002-architecture-style.md  
0003-build-tool.md  

Numbers are sequential and do not imply priority.

---

## What Belongs Here (Examples)

- Project goals and non-goals
- Architectural approach
- Build tooling and language choices
- Data storage or integration strategies
- Major constraints or assumptions

---

## What Does Not Belong Here

- Minor implementation details
- Temporary experiments
- Formatting or style preferences
- Decisions that are easily reversible

---

## Making Changes to Decisions

- New decisions should be proposed via GitHub issues or pull requests
- Updates to existing decisions should preserve prior context
- When a decision is superseded, link to the newer record

Decision records are expected to evolve as understanding improves.

---

## Guiding Principle

The purpose of these records is not to prove correctness,  
but to **make reasoning visible and decisions intentional**.
