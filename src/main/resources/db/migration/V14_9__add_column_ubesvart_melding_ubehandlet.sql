ALTER TABLE person_oversikt_status
ADD COLUMN behandlerdialog_ubesvart_ubehandlet BOOLEAN DEFAULT FALSE;

ALTER TABLE person_oversikt_status
    RENAME COLUMN behandlerdialog_ubehandlet TO behandlerdialog_svar_ubehandlet;


DROP INDEX IX_PERSON_OVERSIKT_STATUS_ENHETENS_OVERSIKT;

CREATE INDEX IX_PERSON_OVERSIKT_STATUS_ENHETENS_OVERSIKT
    ON PERSON_OVERSIKT_STATUS (tildelt_enhet, dialogmotekandidat_generated_at)
    WHERE (motebehov_ubehandlet
               OR oppfolgingsplan_lps_bistand_ubehandlet
               OR dialogmotesvar_ubehandlet
               OR dialogmotekandidat
               OR ((aktivitetskrav = 'NY' OR aktivitetskrav = 'AVVENT') AND aktivitetskrav_stoppunkt > '2023-03-10')
               OR behandlerdialog_svar_ubehandlet
               OR behandlerdialog_ubesvart_ubehandlet
    );

