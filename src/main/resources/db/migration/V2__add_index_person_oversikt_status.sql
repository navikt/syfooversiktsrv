DROP SEQUENCE PERSON_OVERSIKT_STATUS_ID_SEQ;

ALTER TABLE PERSON_OVERSIKT_STATUS DROP COLUMN id;
ALTER TABLE PERSON_OVERSIKT_STATUS ADD COLUMN id SERIAL PRIMARY KEY;
