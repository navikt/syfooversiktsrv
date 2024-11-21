CREATE INDEX ix_person_oversikt_status_name
    ON person_oversikt_status (name)
    WHERE person_oversikt_status.name IS NOT NULL;
