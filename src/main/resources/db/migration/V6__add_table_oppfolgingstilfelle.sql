CREATE TABLE PERSON_OPPFOLGINGSTILFELLE
(
  id                        SERIAL      NOT NULL,
  uuid                      VARCHAR(50) NOT NULL UNIQUE,
  person_oversikt_status_id INTEGER REFERENCES PERSON_OVERSIKT_STATUS (id) ON DELETE CASCADE,
  virksomhetsnummer         VARCHAR(9)  NOT NULL,
  fom                       TIMESTAMP   NOT NULL,
  tom                       TIMESTAMP   NOT NULL,
  gradert                   BOOLEAN     NOT NULL,
  opprettet                 TIMESTAMP   NOT NULL,
  sist_endret               TIMESTAMP   NOT NULL,
  PRIMARY KEY (id, person_oversikt_status_id),
  CONSTRAINT PERSON_VIRKSOMHETSNUMMER_UNIQUE UNIQUE (person_oversikt_status_id, virksomhetsnummer)
);

