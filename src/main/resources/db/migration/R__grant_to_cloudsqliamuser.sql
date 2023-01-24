REVOKE ALL ON ALL TABLES IN SCHEMA public FROM cloudsqliamuser;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO cloudsqliamuser;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO "isyfo-analyse";
DO $$
BEGIN
  CREATE USER "disykefravar-x4wt";
  EXCEPTION WHEN DUPLICATE_OBJECT THEN
  RAISE NOTICE 'not creating role disykefravar-x4wt -- it already exists';
END
$$;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO "disykefravar-x4wt";

-- GRANT SELECT, UPDATE ON ALL TABLES IN SCHEMA public TO cloudsqliamuser;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO cloudsqliamuser;
-- GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO cloudsqliamuser;
