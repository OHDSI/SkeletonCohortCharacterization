{DEFAULT @temporal = FALSE}
insert into @results_database_schema.@results_table (type, fa_type, covariate_id, covariate_name, analysis_id, analysis_name, concept_id, count_value, avg_value,
                                                 strata_id, strata_name, cohort_definition_id, cc_generation_id {@temporal}?{, time_id, start_day, end_day} {@temporal_annual} ? {, event_year})
  select CAST('PREVALENCE' AS VARCHAR(255)) as type,
    CAST('PRESET' AS VARCHAR(255)) as fa_type,
    f.covariate_id,
    fr.covariate_name,
    ar.analysis_id,
    ar.analysis_name,
    fr.concept_id,
    f.sum_value     as count_value,
    f.average_value as stat_value,
    @strataId as strata_id,
    CAST('@strataName' AS VARCHAR(1000)) as strata_name,
    @cohortId as cohort_definition_id,
    @executionId as cc_generation_id
    {@temporal} ? {, t.time_id, t.start_day, t.end_day}
    {@temporal_annual} ? {, event_year}
  from (@features) f
    join (@featureRefs) fr on fr.covariate_id = f.covariate_id and fr.cohort_definition_id = f.cohort_definition_id
    join (@analysisRefs) ar
      on ar.analysis_id = fr.analysis_id and ar.cohort_definition_id = fr.cohort_definition_id
    {@temporal} ? {join #time_period t on t.time_id = f.time_id}
    left join @vocabulary_database_schema.concept c on c.concept_id = fr.concept_id;