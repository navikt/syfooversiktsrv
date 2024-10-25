CREATE TABLE VEILEDER_HISTORIKK (
  id                        SERIAL             PRIMARY KEY,
  uuid                      CHAR(36)           NOT NULL UNIQUE,
  person_oversikt_status_id INTEGER REFERENCES PERSON_OVERSIKT_STATUS (id) ON DELETE CASCADE,
  tildelt_dato              DATE,
  tildelt_veileder          VARCHAR(7)         NOT NULL,
  tildelt_enhet             VARCHAR(4)         NOT NULL,
  tildelt_av                VARCHAR(7)
);

CREATE INDEX IX_VEILEDER_HISTORIKK_STATUS_ID ON VEILEDER_HISTORIKK (person_oversikt_status_id);

INSERT INTO VEILEDER_HISTORIKK (uuid,person_oversikt_status_id,tildelt_dato,tildelt_veileder,tildelt_enhet)
SELECT gen_random_uuid(),id,oppfolgingstilfelle_start,tildelt_veileder,tildelt_enhet
FROM PERSON_OVERSIKT_STATUS
WHERE tildelt_veileder IS NOT NULL AND tildelt_enhet IS NOT NULL;
