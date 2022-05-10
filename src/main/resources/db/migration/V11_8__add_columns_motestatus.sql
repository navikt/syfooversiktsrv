ALTER TABLE PERSON_OVERSIKT_STATUS
ADD COLUMN motestatus VARCHAR(20),
ADD COLUMN motestatus_generated_at timestamptz;
