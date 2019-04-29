-- TODO Once finished put the Schema to some other repository, here just for bootstrapping the development.
CREATE ROLE creator LOGIN CREATEDB CREATEROLE;
\password creator;

SET ROLE creator;

CREATE ROLE trip_writer LOGIN;
\password trip_writer;

RESET ROLE;

CREATE database trips;

\c trips;

SET ROLE creator;

CREATE TABLE trip (
    id                    BIGSERIAL PRIMARY KEY,
    created_at            TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    modified_at           TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    start_date            DATE,
    route_id              TEXT,
    direction_id          SMALLINT,
    start_time            TEXT, -- String hhmmss 30 hour clock?
    json_schema_version   SMALLINT DEFAULT 1,
    trip_data             jsonb,
    ext_id_dvj            TEXT -- Optional field for dvj-id. Might be useful at some point or for troubleshooting
);

CREATE INDEX trip_start_date_idx ON trip(start_date, route_id, direction_id, start_time);

GRANT INSERT ON TABLE trip TO trip_writer;
GRANT SELECT ON TABLE trip TO PUBLIC;

-- TimescaleDB Hypertable required?
-- SELECT create_hypertable('trip_events',
--                         'created_at',
--                         partitioning_column => 'start_date',
--                         chunk_time_interval => interval '1 hour');
