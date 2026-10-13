-- Apply before deploying the random transfer-phrase GET API.
-- Operators populate this catalog manually; the application only reads it.
CREATE TABLE transfer_phrase (
    id BIGINT NOT NULL AUTO_INCREMENT,
    transfer_phrase VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
