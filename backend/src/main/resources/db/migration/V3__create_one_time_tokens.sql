-- Magic-link tokens, managed by Spring Security's JdbcOneTimeTokenService.
-- Adapted from org/springframework/security/core/ott/jdbc/one-time-tokens-schema.sql:
-- "username" widened to hold a full email address.
CREATE TABLE one_time_tokens (
                                 token_value VARCHAR(36)  NOT NULL PRIMARY KEY,
                                 username    VARCHAR(254) NOT NULL,
                                 expires_at  TIMESTAMPTZ  NOT NULL
);
