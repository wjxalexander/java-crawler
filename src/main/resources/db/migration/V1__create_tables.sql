CREATE TABLE news(
    id bigint primary key auto_increment,
    title text,
    content text,
    url varchar(1000),
    created_at timestamp,
    modified_at timestamp
);
create TABLE LINKS_TO_BE_PROCESSED(
    link varchar(1000)
);
create TABLE LINKS_ALREADY_PROCESSED(
    link varchar(1000)
);