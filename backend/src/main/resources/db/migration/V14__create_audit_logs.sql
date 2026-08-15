create table audit_logs (
    id bigserial primary key,
    user_id bigint,
    username varchar(150),
    action varchar(40) not null,
    module varchar(60) not null,
    entity_type varchar(80) not null,
    entity_id varchar(100) not null,
    description varchar(1000) not null,
    old_value text,
    new_value text,
    created_at timestamptz not null default now()
);

create index idx_audit_logs_created_at on audit_logs(created_at desc);
create index idx_audit_logs_module on audit_logs(module);
create index idx_audit_logs_entity on audit_logs(entity_type, entity_id);
create index idx_audit_logs_user_id on audit_logs(user_id);
