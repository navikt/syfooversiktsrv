CREATE TABLE VEILEDER_HISTORIKK (
  id                        SERIAL             PRIMARY KEY,
  created_at                TIMESTAMPTZ        NOT NULL,
  uuid                      CHAR(36)           NOT NULL UNIQUE,
  person_oversikt_status_id INTEGER REFERENCES PERSON_OVERSIKT_STATUS (id) ON DELETE CASCADE,
  tildelt_dato              DATE               NOT NULL,
  tildelt_veileder          VARCHAR(7)         NOT NULL,
  tildelt_enhet             VARCHAR(4)         NOT NULL,
  tildelt_av                VARCHAR(7)         NOT NULL
);

CREATE INDEX IX_VEILEDER_HISTORIKK_STATUS_ID ON VEILEDER_HISTORIKK (person_oversikt_status_id);

INSERT INTO VEILEDER_HISTORIKK (uuid,created_at,person_oversikt_status_id,tildelt_dato,tildelt_veileder,tildelt_enhet,tildelt_av)
SELECT gen_random_uuid(),now(),id,oppfolgingstilfelle_start,tildelt_veileder,tildelt_enhet,'X000000'
FROM PERSON_OVERSIKT_STATUS
WHERE tildelt_veileder IS NOT NULL AND tildelt_enhet IS NOT NULL AND oppfolgingstilfelle_start IS NOT NULL;
