ALTER TABLE PERSON_OVERSIKT_STATUS
ADD COLUMN aktivitetskrav VARCHAR(30),
ADD COLUMN aktivitetskrav_stoppunkt DATE,
ADD COLUMN aktivitetskrav_updated_at timestamptz;
