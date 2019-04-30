-- TODO Once finished put the Schema to some other repository, here just for bootstrapping the development.

\c vehicles

SET ROLE creator;

CREATE TYPE CANCELLATION_STATUS AS ENUM ('RUNNING', 'CANCELED');

CREATE TABLE cancellation (
    id                    BIGSERIAL PRIMARY KEY,
    created_at            TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    modified_at           TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    status                CANCELLATION_STATUS NOT NULL,
    start_date            DATE,
    route_id              TEXT,
    direction_id          SMALLINT,
    start_time            TEXT, -- String hhmmss 30 hour clock?
    ext_id_dvj            TEXT -- Optional field for dvj-id. Might be useful at some point or for troubleshooting
);

ALTER TABLE cancellation ADD CONSTRAINT unique_cancellation_constraint UNIQUE (status, start_date, route_id, direction_id, start_time);

CREATE INDEX cancellation_start_date_time_idx ON cancellation(start_date, start_time);
CREATE INDEX cancellation_trip_identifier_tuple_idx ON cancellation(start_date, route_id, direction_id, start_time);

GRANT INSERT, UPDATE ON TABLE cancellation TO hfp_writer;
GRANT SELECT ON TABLE cancellation TO PUBLIC;

GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA PUBLIC TO hfp_writer;

-- TimescaleDB Hypertable required?
-- SELECT create_hypertable('trip_events',
--                         'created_at',
--                         partitioning_column => 'start_date',
--                         chunk_time_interval => interval '1 hour');
