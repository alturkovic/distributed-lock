CREATE TABLE locks (
    id int NOT NULL AUTO_INCREMENT PRIMARY KEY,
    lock_key varchar(255) UNIQUE,
    token varchar(255),
    expireAt TIMESTAMP,
    PRIMARY KEY(`id`)
);