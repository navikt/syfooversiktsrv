ALTER TABLE person_oversikt_status
    ADD COLUMN is_aktiv_manglende_medvirkning_vurdering BOOLEAN DEFAULT FALSE;

DROP INDEX IX_PERSON_OVERSIKT_STATUS_ENHETENS_OVERSIKT;

CREATE INDEX IX_PERSON_OVERSIKT_STATUS_ENHETENS_OVERSIKT
    ON PERSON_OVERSIKT_STATUS (tildelt_enhet, dialogmotekandidat_generated_at)
    WHERE (motebehov_ubehandlet
        OR oppfolgingsplan_lps_bistand_ubehandlet
        OR dialogmotesvar_ubehandlet
        OR dialogmotekandidat
        OR ((aktivitetskrav = 'NY' OR aktivitetskrav = 'AVVENT' OR aktivitetskrav = 'NY_VURDERING') AND
            aktivitetskrav_stoppunkt > '2023-03-10')
        OR behandlerdialog_svar_ubehandlet
        OR behandlerdialog_ubesvart_ubehandlet
        OR behandlerdialog_avvist_ubehandlet
        OR aktivitetskrav_vurder_stans_ubehandlet
        OR trenger_oppfolging
        OR behandler_bistand_ubehandlet
        OR arbeidsuforhet_aktiv_vurdering
        OR friskmelding_til_arbeidsformidling_fom IS NOT NULL
        OR is_aktiv_sen_oppfolging_kandidat
        OR is_aktiv_aktivitetskrav_vurdering
        OR is_aktiv_manglende_medvirkning_vurdering
        );
