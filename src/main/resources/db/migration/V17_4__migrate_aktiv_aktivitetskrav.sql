UPDATE person_oversikt_status
SET is_aktiv_aktivitetskrav_vurdering = true
WHERE aktivitetskrav IN ('NY', 'AVVENT','NY_VURDERING', 'FORHANDSVARSEL') AND aktivitetskrav_stoppunkt > '2023-03-10' and is_aktiv_aktivitetskrav_vurdering = false;
