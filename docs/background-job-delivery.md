# Background job delivery

Business-triggered jobs now use a transactional outbox. Flyway migration V17 adds
`background_job_outbox`. Scheduling serializes the request and inserts an intent
in the caller's transaction. Rollback removes both the business change and intent.
Scheduling no longer contacts JobRunr after the business commit, so a queue outage
cannot turn a committed customer registration into a scheduling error response.

On nodes with `jobrunr.background-job-server.enabled=true`, a relay polls due intents
every second. `kartoush.jobs.outbox.poll-interval-ms` controls the interval. Each poll
publishes up to 100 intents. PostgreSQL `FOR UPDATE SKIP LOCKED` coordinates relay
nodes; each publication has its own transaction. Failed publication records a failure and defers the
intent with exponential backoff, allowing later due intents to proceed in the same
poll. Repeated failures quarantine the intent. Future intents remain pending until due.

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
On a subsequent execution, reconciliation, or scheduled recovery sweep, an attempt
at least five minutes old is treated as uncertain. This threshold permits investigation; it does not prove
that the remote operation stopped. Worker nodes sweep stale attempts even when JobRunr has exhausted its retries. Attempt numbers fence late
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


## Outbox publication failure policy

Migration V19 adds persisted publication failure counts, retry times, and quarantine
timestamps. Publication and failure bookkeeping each use the locked item's transaction.
A failed enqueue commits the failure count and next retry time instead of aborting the
poll. Previously published items stay committed, and later eligible items are attempted.
If claiming a row or committing publication state fails (for example, a database outage),
the current transaction rolls back and the poll stops; no failure state is invented.

Configuration defaults:

| Property under `kartoush.jobs.outbox` | Default | Meaning |
|---|---|---|
| `max-publication-failures` | `5` | Quarantine on this failure count |
| `initial-backoff` | `30s` | Delay after the first publication failure |
| `max-backoff` | `1h` | Maximum exponential retry delay |

With defaults, retry delays are 30, 60, 120, and 240 seconds, followed by quarantine
on the fifth failure. The failure budget applies to enqueue failures, not handler
execution failures; JobRunr continues to own execution retries. A global queue outage
can exhaust multiple intents' budgets, so investigate the queue before recovery.
Logs identify failed/quarantined intents by outbox ID without logging payloads.

Inspect deferred and quarantined work:

```sql
select id, publication_failures, next_publication_at, quarantined_at
from background_job_outbox
where dispatched_at is null and completed_at is null
  and (publication_failures > 0 or quarantined_at is not null)
order by created_at;
```

After fixing the publication cause, an authorized internal operator can call
`BackgroundJobOutboxRecoveryService.retryPublication(id)`. It locks and resets a
quarantined, unpublished, incomplete intent; the next poll reuses its original ID.
There is no public recovery endpoint. Do not delete or recreate the intent: an
ambiguous enqueue may already exist in JobRunr, and preserving the ID retains the
executor's duplicate suppression. Quarantine retains the original payload for recovery.
Apply V17, V18, and V19 before running this application version.


## Worker recovery and operational visibility

On worker nodes (`jobrunr.background-job-server.enabled=true`), a recovery monitor
runs once per minute by default, controlled by `kartoush.jobs.recovery.poll-interval-ms`.
It processes at most 100 stale deliveries per sweep, each in a separate transaction.
The notification service locks eligible `SENDING` rows with `SKIP LOCKED` and updates
both the delivery and its current attempt to `UNKNOWN`. Fresh attempts and accepted
records are untouched. Multiple worker nodes can sweep safely without waiting on
each other's locked rows. There is no automatic resend or evidence-free reconciliation.
Five minutes is an uncertainty threshold, not proof that a remote send stopped.

The monitor records Micrometer gauges from its latest successful snapshot:

| Meter | Tags / meaning |
|---|---|
| `jobs.recovery.unresolved` | Count by `kind`: `email-unknown`, `email-failed`, `outbox-quarantined`, `execution-failed` |
| `jobs.recovery.oldest.age` | Age in seconds of the oldest unresolved item for the same `kind` |
| `jobs.recovery.snapshot.age` | Seconds since the last successful refresh; NaN before the first refresh |
| `jobs.recovery.monitor.failures` | Counter of monitor refresh failures |

Email ages start at their latest status update; quarantine age starts when quarantine
was recorded, and failed-execution age uses JobRunr's job update time. `execution-failed`
counts jobs currently in JobRunr's `FAILED` state, not every failed attempt; scheduled
retries are tracked by JobRunr separately. Snapshot queries are approximate operational
observations rather than an atomic view across all stores. Scraping reads the snapshot,
not the database. Refresh failures retain the prior snapshot, increment the failure
counter, and emit an error. Unresolved work emits a warning with counts, and each newly
stale email logs its delivery ID. No recipient, message content, or token is logged.

These meters use the application's configured MeterRegistry. No new public management
endpoint, exporter, or paging integration is enabled. Configure alerts in the deployment's
existing monitoring backend for nonzero unknown/quarantined/failed counts, excessive
oldest-item age, monitor failures, and a missing/stale snapshot (for example, older than
three poll intervals). Alert on worker absence as well: no worker means no sweeps.
Operators should investigate and follow the evidence-based recovery procedures above.

A PostgreSQL integration test now starts a real worker JVM, waits until provider
acceptance is durably recorded while handler database writes are uncommitted, and kills
that JVM forcibly. A second worker uses JobRunr's normal orphan recovery to execute the
same intent. The test checks one committed business result, one simulated provider
acceptance, and outbox completion. Provider acceptance is simulated without network
calls. This exercises process death and restart, not a real provider or network partition.
The test uses five-second JobRunr polls and the supported four-poll server timeout;
production detection latency continues to depend on its configured JobRunr settings.

CI includes infrastructure/job and notification integration groups and checks Docker
availability before running integration tests, so the crash/recovery coverage is not
silently omitted by those groups.
