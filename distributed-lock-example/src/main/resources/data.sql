-- do not rename this file, Spring Boot runs 'data.sql' on startup

CREATE TABLE distributed_lock (
    id int NOT NULL AUTO_INCREMENT PRIMARY KEY,
    lock_key varchar(255) UNIQUE,
    token varchar(255),
    expireAt TIMESTAMP,
    PRIMARY KEY(`id`)
);

create trigger log_trigger
after insert
on distributed_lock
for each row
call "com.github.alturkovic.lock.example.trigger.LoggingDatabaseTrigger"
