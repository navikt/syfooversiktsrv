CREATE SEQUENCE PERSON_OVERSIKT_STATUS_ID_SEQ;

CREATE TABLE PERSON_OVERSIKT_STATUS (
  id                       CHAR(64)           PRIMARY KEY,
  uuid                     VARCHAR(50)        NOT NULL UNIQUE,
  fnr                      VARCHAR(11)        NOT NULL,
  tildelt_veileder         VARCHAR(7),
  tildelt_enhet            VARCHAR(4),
  opprettet                TIMESTAMP NOT NULL,
  sist_endret              TIMESTAMP NOT NULL,
  CONSTRAINT VEILEDER_FNR_UNIQUE UNIQUE(fnr, tildelt_veileder)
);
