UPDATE person_oversikt_status
SET is_aktiv_aktivitetskrav_vurdering = false
WHERE aktivitetskrav_stoppunkt <= '2023-03-10' and is_aktiv_aktivitetskrav_vurdering = true;
