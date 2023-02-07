CREATE INDEX IX_PERSON_OVERSIKT_STATUS_ENHETENS_OVERSIKT_TEST_VEILEDER
ON PERSON_OVERSIKT_STATUS (tildelt_enhet, dialogmotekandidat_generated_at)
WHERE (motebehov_ubehandlet OR oppfolgingsplan_lps_bistand_ubehandlet OR dialogmotesvar_ubehandlet OR dialogmotekandidat OR ((aktivitetskrav = 'NY' OR aktivitetskrav = 'AVVENT') AND aktivitetskrav_stoppunkt > '2023-02-01'));
