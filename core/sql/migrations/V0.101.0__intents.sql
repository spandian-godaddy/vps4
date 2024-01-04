create table if not exists intent (
    id integer primary key,
    name text not null
);

create table if not exists vm_intent (
    vm_id UUID not null references virtual_machine(vm_id),
    intent_id int not null references intent(id),
    description text,
    PRIMARY KEY(vm_id, intent_id)
);

insert into intent (id, name) values (1, 'WEBSITE_HOSTING'),
                                 (2, 'APPLICATION_DEVELOPMENT_AND_TESTING'),
                                 (3, 'GAME_SERVER'),
                                 (4, 'FILE_STORAGE_AND_BACKUP'),
                                 (5, 'REMOTE_DESKTOP_ACCESS'),
                                 (6, 'EMAIL_SERVER'),
                                 (7, 'DATA_ANALYTICS'),
                                 (8, 'VOICE_OVER_IP'),
                                 (9, 'PROXY_SERVER'),
                                 (10, 'OTHER')
ON CONFLICT DO NOTHING;