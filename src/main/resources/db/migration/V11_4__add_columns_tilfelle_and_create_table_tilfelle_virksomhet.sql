ALTER TABLE PERSON_OVERSIKT_STATUS
ADD COLUMN oppfolgingstilfelle_updated_at               timestamptz,
ADD COLUMN oppfolgingstilfelle_generated_at             timestamptz,
ADD COLUMN oppfolgingstilfelle_start                    DATE,
ADD COLUMN oppfolgingstilfelle_end                      DATE,
ADD COLUMN oppfolgingstilfelle_bit_referanse_uuid       CHAR(36),
ADD COLUMN oppfolgingstilfelle_bit_referanse_inntruffet timestamptz;

CREATE TABLE PERSON_OPPFOLGINGSTILFELLE_VIRKSOMHET
(
    id                        SERIAL PRIMARY KEY,
    uuid                      CHAR(36)    NOT NULL UNIQUE,
    created_at                timestamptz NOT NULL,
    person_oversikt_status_id INTEGER REFERENCES PERSON_OVERSIKT_STATUS (id) ON DELETE CASCADE,
    virksomhetsnummer         CHAR(9)     NOT NULL,
    virksomhetsnavn           VARCHAR,
    CONSTRAINT PERSON_OPPFOLGINGSTILFELLE_VIRKSOMHET_UNIQUE UNIQUE (person_oversikt_status_id, virksomhetsnummer)
);

CREATE INDEX IX_PERSON_TILFELLE_VIRKSOMHET_ID ON PERSON_OPPFOLGINGSTILFELLE_VIRKSOMHET (person_oversikt_status_id);
