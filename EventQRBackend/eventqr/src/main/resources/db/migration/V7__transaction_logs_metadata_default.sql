alter table if exists transaction_logs
    alter column metadata set default '{}';

update transaction_logs
set metadata = '{}'
where metadata is null
   or btrim(metadata) = '';
