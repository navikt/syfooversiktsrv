UPDATE PERSON_OVERSIKT_STATUS
SET is_aktiv_aktivitetskrav_vurdering =
    (aktivitetskrav IN ('NY', 'AVVENT','NY_VURDERING', 'FORHANDSVARSEL') AND aktivitetskrav_stoppunkt > '2023-03-10');
