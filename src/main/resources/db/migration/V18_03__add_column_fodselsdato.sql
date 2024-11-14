ALTER TABLE person_oversikt_status
    ADD COLUMN fodselsdato DATE;

CREATE INDEX ix_person_oversikt_status_fodselsdato
    ON person_oversikt_status (fodselsdato)
    WHERE person_oversikt_status.fodselsdato IS NOT NULL;
