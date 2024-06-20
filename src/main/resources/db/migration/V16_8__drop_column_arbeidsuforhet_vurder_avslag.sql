ALTER TABLE person_oversikt_status
    DROP COLUMN arbeidsuforhet_vurder_avslag_ubehandlet;

CREATE INDEX IX_PERSON_OVERSIKT_STATUS_ENHETENS_OVERSIKT
    ON PERSON_OVERSIKT_STATUS (tildelt_enhet, dialogmotekandidat_generated_at)
    WHERE (motebehov_ubehandlet
        OR oppfolgingsplan_lps_bistand_ubehandlet
        OR dialogmotesvar_ubehandlet
        OR dialogmotekandidat
        OR ((aktivitetskrav = 'NY' OR aktivitetskrav = 'AVVENT') AND aktivitetskrav_stoppunkt > '2023-03-10')
        OR behandlerdialog_svar_ubehandlet
        OR behandlerdialog_ubesvart_ubehandlet
        OR behandlerdialog_avvist_ubehandlet
        OR aktivitetskrav_vurder_stans_ubehandlet
        OR trenger_oppfolging
        OR behandler_bistand_ubehandlet
        OR arbeidsuforhet_aktiv_vurdering
        );
