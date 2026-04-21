create table terms_acceptance
(
    id            varchar(26) primary key,
    customer_id   varchar(26) not null,
    terms_version varchar(50) not null,
    accepted_at   timestamptz not null,
    constraint fk_terms_acceptance_customer
        foreign key (customer_id)
            references customer (id)
            on delete cascade
);

create index idx_terms_acceptance_customer_id on terms_acceptance (customer_id);
