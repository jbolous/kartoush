create table terms_of_service
(
    id            varchar(26) primary key,
    version       varchar(50) not null unique,
    content       text        not null,
    content_type  varchar(20) not null,
    status        varchar(20) not null,
    effective_at  timestamptz null,
    superseded_at timestamptz null,
    created_at    timestamptz not null,
    updated_at    timestamptz not null
);

create unique index uq_terms_of_service_active_status
    on terms_of_service (status)
    where status = 'ACTIVE';

create unique index uq_terms_of_service_scheduled_status
    on terms_of_service (status)
    where status = 'SCHEDULED';

insert into terms_of_service (
    id,
    version,
    content,
    content_type,
    status,
    effective_at,
    superseded_at,
    created_at,
    updated_at
) values (
    '01JSV9VMTGEXKYEHA3J6NBK4N3',
    '2026.04.01',
    'Kartoush Terms of Service

Effective date: 2026-04-01

1. Overview
Kartoush provides software and related services for managing catalog, customer, inventory, reservation, checkout, and order workflows. By creating an account or using the service, you agree to these Terms of Service.

2. Account Information
You agree to provide accurate and complete information when creating or updating your account. You are responsible for keeping your account information current.

3. Acceptable Use
You may not use Kartoush in a way that is unlawful, fraudulent, abusive, or intended to disrupt the service. You may not attempt to bypass security controls or gain unauthorized access to data or system functionality.

4. Service Availability
Kartoush may change, suspend, or discontinue features at any time. We do not guarantee uninterrupted availability or error-free operation.

5. Customer and Order Data
You are responsible for the accuracy of customer, catalog, inventory, and order data entered into the system. Kartoush may process and store this data as required to operate the service.

6. Suspension and Termination
Kartoush may suspend or terminate access to the service if these Terms are violated or if continued access would create security, legal, or operational risk.

7. Changes to These Terms
Kartoush may update these Terms from time to time. When a new version becomes effective, continued use of the service may require acceptance of the updated Terms.

8. Contact
Questions about these Terms may be directed through the support or administrative contact channels designated for the service.',
    'PLAIN_TEXT',
    'ACTIVE',
    '2026-04-01T00:00:00Z',
    null,
    '2026-04-01T00:00:00Z',
    '2026-04-01T00:00:00Z'
);
