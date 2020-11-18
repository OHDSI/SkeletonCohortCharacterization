{DEFAULT @useAggregatedValue = TRUE}
-- value of missingMeansZero is '@missingMeansZero'

SELECT ROW_NUMBER() OVER (partition by E.subject_id order by E.cohort_start_date) AS event_id, E.subject_id AS person_id, E.cohort_start_date AS start_date, E.cohort_end_date AS end_date, OP.observation_period_start_date AS op_start_date, OP.observation_period_end_date AS op_end_date
into #qualified_events
FROM @targetTable E
  JOIN @cdm_database_schema.observation_period OP ON E.subject_id = OP.person_id AND E.cohort_start_date >= OP.observation_period_start_date AND E.cohort_start_date <= OP.observation_period_end_date
WHERE cohort_definition_id = @cohortId;

WITH qualified_events AS (
  select person_id, event_id, start_date, end_date, op_start_date, op_end_date
  from #qualified_events
)
select
  v.person_id as person_id,
  v.event_id as event_id,
  @valueExpression as value_as_number
into #events_count
from ( @groupQuery ) v
{@aggregateJoinTable != ""} ? {@aggregateJoin @cdm_database_schema.@aggregateJoinTable on @aggregateCondition}
{@useAggregatedValue} ? {group by v.person_id, v.event_id}
;
@missingMeansZeroQuery
select
  CAST('DISTRIBUTION' AS VARCHAR(255)) as type,
  CAST('CRITERIA_SET' AS VARCHAR(255)) as fa_type,
  CAST(@covariateId AS BIGINT) as covariate_id,
  CAST('@covariateName' AS VARCHAR(1000)) as covariate_name,
  CAST(@analysisId AS INT) as analysis_id,
  CAST('@analysisName' AS VARCHAR(1000)) as analysis_name,
  CAST(@conceptId AS INT) as concept_id,
  CAST(@cohortId AS BIGINT) as cohort_definition_id,
  CAST(@executionId AS BIGINT) as cc_generation_id,
  CAST(@strataId AS BIGINT) as strata_id,
  CAST('@strataName' AS VARCHAR(255)) as strata_name,
  CAST(@aggregateId AS INTEGER) as aggregate_id,
  CAST('@aggregateName' AS VARCHAR(1000)) as aggregate_name,
  CAST(@missingMeansZero as INTEGER) as missing_means_zero,
  o.person_count as person_count,
  o.total as record_count,
  o.avg_value,
  coalesce(o.stdev_value, 0) as stdev_value,
  o.min_value,
  MIN(case when s.accumulated >= .10 * o.total then value_as_number else o.max_value end) as p10_value,
  MIN(case when s.accumulated >= .25 * o.total then value_as_number else o.max_value end) as p25_value,
  MIN(case when s.accumulated >= .50 * o.total then value_as_number else o.max_value end) as median_value,
  MIN(case when s.accumulated >= .75 * o.total then value_as_number else o.max_value end) as p75_value,
  MIN(case when s.accumulated >= .90 * o.total then value_as_number else o.max_value end) as p90_value,
  o.max_value
INTO #events_dist
FROM (
  select
    avg(1.0 * value_as_number) as avg_value,
    stdev(value_as_number) as stdev_value,
    min(value_as_number) as min_value,
    max(value_as_number) as max_value,
    count_big(*) as total,
    COUNT(distinct person_id) as person_count
  from #events_count
) o
cross join (
  select value_as_number, count_big(*) as total,
    sum(count_big(*)) over (order by value_as_number) as accumulated
  FROM #events_count
  group by value_as_number
) s
group by o.total, o.person_count, o.min_value, o.max_value, o.avg_value, o.stdev_value;
insert into @results_database_schema.cc_results(type, fa_type, covariate_id, covariate_name, analysis_id, analysis_name, missing_means_zero, concept_id,
  cohort_definition_id, cc_generation_id, strata_id, strata_name, aggregate_id, aggregate_name, count_value, min_value, max_value, avg_value, stdev_value, p10_value, p25_value, median_value, p75_value, p90_value)
select type, fa_type, covariate_id, covariate_name, analysis_id, analysis_name, missing_means_zero, concept_id,
  cohort_definition_id, cc_generation_id, strata_id, strata_name, aggregate_id, aggregate_name, person_count, min_value, max_value, avg_value, stdev_value, p10_value, p25_value, median_value, p75_value, p90_value
FROM #events_dist;

truncate table #events_dist;
drop table #events_dist;

truncate table #events_count;
drop table #events_count;

truncate table #qualified_events;
drop table #qualified_events;