create table activation_token
(
    id          varchar(26) primary key,
    customer_id varchar(26)  not null,
    token_hash  varchar(128) not null unique,
    expires_at  timestamptz  not null,
    consumed_at timestamptz  null,
    created_at  timestamptz  not null,
    constraint fk_activation_token_customer
        foreign key (customer_id)
            references customer (id)
);

create index idx_activation_token_customer_id on activation_token (customer_id);
