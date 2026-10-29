# Background job delivery

Business-triggered jobs now use a transactional outbox. Flyway migration V17 adds
`background_job_outbox`. Scheduling serializes the request and inserts an intent
in the caller's transaction. Rollback removes both the business change and intent.
Scheduling no longer contacts JobRunr after the business commit, so a queue outage
cannot turn a committed customer registration into a scheduling error response.

On nodes with `jobrunr.background-job-server.enabled=true`, a relay polls due intents
every second. `kartoush.jobs.outbox.poll-interval-ms` controls the interval. Each poll
publishes up to 100 intents. PostgreSQL `FOR UPDATE SKIP LOCKED` coordinates relay
nodes; each publication has its own transaction. Failed publication leaves the
intent pending for the next poll. Future intents remain pending until due.

JobRunr persists an invocation of `OutboxJobExecutor.execute(outboxId)`, rather than
the sensitive request payload. The executor locks the outbox record, dispatches its
payload, and marks it completed in one transaction. Completed payloads are cleared.
Handler exceptions roll back completion and propagate to JobRunr for retry. A
normal handler return, including an intentional skip, completes the intent.

If a process crashes after enqueueing but before recording publication, the relay
may enqueue another invocation. Both invocations reference the same intent. The
execution lock serializes them, and completed intents return without running the
handler again. Completed records are retained as deduplication tombstones.

Activation and welcome email jobs attach stable UUID idempotency keys. Activation
keys distinguish resends by their encrypted token; welcome keys identify the
customer's welcome event. Brevo receives the key in its transactional-email JSON
`headers` object. Its specific already-processed idempotency response counts as
success; unrelated provider errors still fail. Raw provider error bodies are not
logged.

The external email send and database commit cannot be atomic. Brevo's finite
idempotency window reduces duplicates when a response is lost or completion fails
after provider acceptance. It does not provide indefinite exactly-once delivery;
Mailtrap does not receive this provider-specific protection. Reconcile ambiguous
sends before manually retrying them outside the provider's window.

Inspect pending intents when publication is unavailable. Once published, failed
executions remain in JobRunr for inspection and manual retry. Retrying the same
JobRunr invocation preserves its outbox ID. Existing jobs already stored before
this migration retain their original dispatcher entry point and remain executable.
Recurring cleanup registration continues to use JobRunr directly.

Provider references: [Brevo send API](https://developers.brevo.com/reference/send-transac-email)
and [idempotency behavior](https://developers.brevo.com/docs/heterogenous-versions-batch-emails).

## Durable email delivery tracking

Migration V18 adds notification-owned `email_delivery` and `email_delivery_attempt`
tables. Enabled Brevo and Mailtrap deliveries use the same tracked delivery service;
NOOP deliveries retain their existing behavior. Each real send first commits a
`SENDING` record and attempt in a `REQUIRES_NEW` transaction. Acceptance and the
provider message ID (when supplied) commit in another independent transaction.
A later business/outbox rollback therefore does not erase confirmed acceptance.
No message body, recipient, or action token is stored in these tables.

The delivery ID equals the email's stable UUID idempotency key. Existing callers
without a key use a deterministic fingerprint of the complete message. Identical
unkeyed messages are treated as the same logical email; callers requiring separate
identical emails must supply different event keys. Retries of one event must keep
the same key. Changing provider for an existing delivery is rejected.

`ACCEPTED` means provider acceptance, not delivery to the recipient's inbox.
Accepted records suppress further sends. Definitive rejection can be `FAILED`
(permanent) or `RETRYABLE` (confirmed safe to retry). Mailtrap's explicit unsuccessful
response is classified as permanent rejection. Other client exceptions, including
transport failures and unclassified provider responses, are conservatively
`UNKNOWN`. A malformed success receipt is also uncertain. No automatic replay of
an uncertain send is attempted, even inside Brevo's idempotency window.

An in-flight `SENDING` attempt prevents another sender from calling the provider.
On a subsequent execution or reconciliation, an attempt at least five minutes old
is treated as uncertain. This threshold permits investigation; it does not prove
that the remote operation stopped. A crashed attempt that receives no further
execution remains visibly `SENDING` until investigated. Attempt numbers fence late
results so they cannot overwrite a newer attempt or a reconciliation decision.

### Reconciliation procedure

Inspect unresolved records without reading sensitive outbox payloads:

```sql
select id, provider, status, attempt_count, attempt_started_at, provider_message_id
from email_delivery
where status in ('UNKNOWN', 'FAILED')
   or (status = 'SENDING' and attempt_started_at <= current_timestamp - interval '5 minutes')
order by attempt_started_at;
```

For `UNKNOWN` or stale `SENDING`, investigate provider activity/support and any
available receipts. A missing receipt or elapsed timeout is **not** proof of
non-acceptance. Ensure the original sender is no longer running before authorizing
a new send. An authorized internal operational caller can invoke
`EmailDeliveryReconciliationService.reconcile(id, accepted, providerMessageId,
evidenceReference)` with a nonblank support-case or other evidence reference:

- Confirmed acceptance: `accepted=true` records `ACCEPTED`; retrying the same
  JobRunr invocation completes its outbox record without sending again.
- Confirmed non-acceptance: `accepted=false` permits one new attempt on the next
  execution. Retry the original JobRunr invocation, retaining its outbox ID/key.
- No conclusive evidence: leave the delivery held. Do not reset it just because
  the provider's idempotency window elapsed.

There is no public reconciliation endpoint or automatic provider lookup/webhook
integration. Reconciliation is an explicit internal service operation based on
external evidence. JobRunr still records held executions as failures; after its
normal retry budget is exhausted, manual retry is needed following reconciliation.
`FAILED` deliveries remain held for investigation rather than automatic resend.
The attempt table retains per-attempt outcomes and receipts; the delivery record
retains the latest reconciliation reference. Restrict operational access to these
tables and never put message contents or tokens into evidence references.
