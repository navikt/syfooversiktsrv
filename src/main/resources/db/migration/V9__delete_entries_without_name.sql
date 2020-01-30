DELETE FROM person_oversikt_status
WHERE navn IS NULL
OR navn = '';
