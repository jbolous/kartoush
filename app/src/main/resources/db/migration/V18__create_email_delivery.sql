create table email_delivery (
    id uuid primary key,
    provider varchar(32) not null,
    status varchar(16) not null check (status in ('SENDING', 'ACCEPTED', 'RETRYABLE', 'FAILED', 'UNKNOWN')),
    attempt_count integer not null,
    attempt_started_at timestamptz not null,
    updated_at timestamptz not null,
    provider_message_id text,
    reconciliation_reference text
);

create index ix_email_delivery_unresolved on email_delivery (status, attempt_started_at)
    where status in ('SENDING', 'UNKNOWN', 'FAILED');

create table email_delivery_attempt (
    delivery_id uuid not null references email_delivery (id),
    attempt_number integer not null,
    status varchar(16) not null check (status in ('SENDING', 'ACCEPTED', 'RETRYABLE', 'FAILED', 'UNKNOWN')),
    started_at timestamptz not null,
    finished_at timestamptz,
    provider_message_id text,
    primary key (delivery_id, attempt_number)
);
