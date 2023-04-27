ALTER TABLE PERSON_OVERSIKT_STATUS
ADD COLUMN behandlerdialog_ubehandlet BOOLEAN DEFAULT FALSE;

DROP INDEX IX_PERSON_OVERSIKT_STATUS_ENHETENS_OVERSIKT;

CREATE INDEX IX_PERSON_OVERSIKT_STATUS_ENHETENS_OVERSIKT
    ON PERSON_OVERSIKT_STATUS (tildelt_enhet, dialogmotekandidat_generated_at)
    WHERE (motebehov_ubehandlet OR oppfolgingsplan_lps_bistand_ubehandlet OR dialogmotesvar_ubehandlet OR dialogmotekandidat OR ((aktivitetskrav = 'NY' OR aktivitetskrav = 'AVVENT') AND aktivitetskrav_stoppunkt > '2023-03-10') OR behandlerdialog_ubehandlet);

