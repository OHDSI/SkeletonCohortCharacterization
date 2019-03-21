insert into @results_database_schema.cc_results (
     type,
     fa_type,
     covariate_id,
     covariate_name,
     analysis_id,
     analysis_name,
     concept_id,
     count_value,
     avg_value,
     cohort_definition_id,
     strata_id,
     strata_name,
     cc_generation_id)
select CAST('PREVALENCE' AS VARCHAR(255)) as type,
        CAST('CUSTOM_FE' AS VARCHAR(255)) as fa_type,
        CAST(covariate_id AS BIGINT) as covariate_id,
        CAST(covariate_name AS VARCHAR(1000)) as covariate_name,
        CAST(@analysisId AS INTEGER) as analysis_id,
        CAST('@analysisName' AS VARCHAR(1000)) as analysis_name,
        CAST(concept_id AS INTEGER) as concept_id,
        sum_value       as count_value,
        average_value   as stat_value,
        CAST(@cohortId AS BIGINT) as cohort_definition_id,
        CAST(@strataId AS BIGINT) as strata_id,
        CAST('@strataName' AS VARCHAR(1000)) as strata_name,
        CAST(@jobId AS BIGINT) as cc_generation_id
from (@design) subquery;
