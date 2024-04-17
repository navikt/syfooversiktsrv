ALTER TABLE person_oversikt_status
    ADD COLUMN friskmelding_til_arbeidsformidling_fom DATE;

CREATE INDEX IX_PERSON_OVERSIKT_FTA_FOM
    ON PERSON_OVERSIKT_STATUS (tildelt_enhet, friskmelding_til_arbeidsformidling_fom)
    WHERE (friskmelding_til_arbeidsformidling_fom IS NOT NULL);
