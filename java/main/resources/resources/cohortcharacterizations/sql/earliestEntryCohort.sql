CREATE TABLE @earliestEntryTable (cohort_definition_id INTEGER, subject_id BIGINT, cohort_start_date DATE, cohort_end_date DATE);

INSERT INTO @earliestEntryTable (cohort_definition_id, subject_id, cohort_start_date, cohort_end_date)
SELECT e.cohort_definition_id, e.subject_id, e.cohort_start_date, e.cohort_end_date
FROM (
  SELECT c.cohort_definition_id, c.subject_id, c.cohort_start_date, c.cohort_end_date,
    ROW_NUMBER() OVER (PARTITION BY c.cohort_definition_id, c.subject_id ORDER BY c.cohort_start_date, c.cohort_end_date) AS rn
  FROM @cohortTable c
  WHERE c.cohort_definition_id IN (@cohortIds)
) e
WHERE e.rn = 1;
