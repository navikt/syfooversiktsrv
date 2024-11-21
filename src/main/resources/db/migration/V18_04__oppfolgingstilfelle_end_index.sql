CREATE INDEX ix_person_oversikt_status_oppfolgingstilfelle_end
    ON person_oversikt_status (oppfolgingstilfelle_end)
    WHERE person_oversikt_status.oppfolgingstilfelle_end IS NOT NULL;
