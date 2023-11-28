CREATE TABLE locks (
    id SERIAL NOT NULL,
    lock_key varchar(255) UNIQUE,
    token varchar(255),
    expireAt TIMESTAMP,
    PRIMARY KEY(id)
);
