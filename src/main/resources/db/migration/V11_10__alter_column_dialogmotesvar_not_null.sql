UPDATE PERSON_OVERSIKT_STATUS
    SET dialogmotesvar_ubehandlet = false
    WHERE dialogmotesvar_ubehandlet IS null;

ALTER TABLE PERSON_OVERSIKT_STATUS
ALTER COLUMN dialogmotesvar_ubehandlet SET NOT NULL;
