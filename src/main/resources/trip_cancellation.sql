INSERT INTO CANCELLATION
    (deviation_case_id, status, start_date, route_id, direction_id, start_time, last_modified, data, ext_id_dvj)
    VALUES
    (?, ?::CANCELLATION_STATUS, ?, ?, ?, ?, ?, ?::JSON, ?)
    ON CONFLICT ON CONSTRAINT unique_cancellation_constraint DO UPDATE SET status = EXCLUDED.status, data = EXCLUDED.data, last_modified = EXCLUDED.last_modified;