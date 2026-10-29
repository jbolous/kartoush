create table background_job_outbox (
    id uuid primary key,
    request_type varchar(512) not null,
    payload text,
    available_at timestamptz not null,
    created_at timestamptz not null,
    dispatched_at timestamptz,
    completed_at timestamptz
);

create index ix_background_job_outbox_pending
    on background_job_outbox (available_at, created_at)
    where dispatched_at is null and completed_at is null;
