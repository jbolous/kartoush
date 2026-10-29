alter table background_job_outbox
    add column publication_failures integer not null default 0 check (publication_failures >= 0),
    add column next_publication_at timestamptz,
    add column quarantined_at timestamptz;

drop index ix_background_job_outbox_pending;
create index ix_background_job_outbox_pending
    on background_job_outbox (available_at, created_at)
    where dispatched_at is null and completed_at is null and quarantined_at is null;
