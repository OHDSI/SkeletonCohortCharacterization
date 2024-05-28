package org.ohdsi.cohortcharacterization;

import static org.ohdsi.cohortcharacterization.Constants.Params.CDM_DATABASE_SCHEMA;
import static org.ohdsi.cohortcharacterization.Constants.Params.RESULTS_DATABASE_SCHEMA;
import static org.ohdsi.cohortcharacterization.Constants.Params.TEMP_DATABASE_SCHEMA;
import static org.ohdsi.cohortcharacterization.Constants.Params.VOCABULARY_DATABASE_SCHEMA;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.text.MessageFormat;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.ohdsi.analysis.Utils;
import org.ohdsi.analysis.WithId;
import org.ohdsi.analysis.cohortcharacterization.design.AggregateFunction;
import org.ohdsi.analysis.cohortcharacterization.design.BaseCriteriaFeature;
import org.ohdsi.analysis.cohortcharacterization.design.CcResultType;
import org.ohdsi.analysis.cohortcharacterization.design.CohortCharacterization;
import org.ohdsi.analysis.cohortcharacterization.design.CohortCharacterizationParam;
import org.ohdsi.analysis.cohortcharacterization.design.CohortCharacterizationStrata;
import org.ohdsi.analysis.cohortcharacterization.design.CriteriaFeature;
import org.ohdsi.analysis.cohortcharacterization.design.DemographicCriteriaFeature;
import org.ohdsi.analysis.cohortcharacterization.design.FeatureAnalysis;
import org.ohdsi.analysis.cohortcharacterization.design.FeatureAnalysisAggregate;
import org.ohdsi.analysis.cohortcharacterization.design.FeatureAnalysisWithCriteria;
import org.ohdsi.analysis.cohortcharacterization.design.StandardFeatureAnalysisType;
import org.ohdsi.analysis.cohortcharacterization.design.WindowedCriteriaFeature;
import org.ohdsi.circe.cohortdefinition.CohortExpressionQueryBuilder;
import org.ohdsi.circe.cohortdefinition.ConceptSet;
import org.ohdsi.circe.cohortdefinition.DemographicCriteria;
import org.ohdsi.circe.cohortdefinition.WindowedCriteria;
import org.ohdsi.circe.cohortdefinition.builders.BuilderOptions;
import org.ohdsi.circe.cohortdefinition.builders.CriteriaColumn;
import org.ohdsi.circe.helper.ResourceHelper;
import org.ohdsi.cohortcharacterization.design.CohortCharacterizationImpl;
import com.odysseusinc.arachne.commons.utils.QuoteUtils;
import org.ohdsi.cohortcharacterization.utils.SafeFeature;
import org.ohdsi.featureExtraction.FeatureExtraction;
import org.ohdsi.sql.SqlRender;
import org.ohdsi.sql.SqlSplit;

public class CCQueryBuilder {

    private static final String[] CUSTOM_PARAMETERS = {"analysisId", "analysisName", "cohortId", "jobId", "design"};
    private static final String[] RETRIEVING_PARAMETERS = {"features", "featureRefs", "analysisRefs", "cohortId", "executionId", "results_table"};
    private static final String[] DAIMONS = {RESULTS_DATABASE_SCHEMA, CDM_DATABASE_SCHEMA, TEMP_DATABASE_SCHEMA, VOCABULARY_DATABASE_SCHEMA};

    private static final String COHORT_STATS_QUERY = ResourceHelper.GetResourceAsString("/resources/cohortcharacterizations/sql/prevalenceWithCriteria.sql");
    private static final String COHORT_DIST_QUERY = ResourceHelper.GetResourceAsString("/resources/cohortcharacterizations/sql/distributionWithCriteria.sql");
    private static final String MISSING_MEANS_ZERO_QUERY = ResourceHelper.GetResourceAsString("/resources/cohortcharacterizations/sql/missingMeansZero.sql");

    private static final String COHORT_STRATA_QUERY = ResourceHelper.GetResourceAsString("/resources/cohortcharacterizations/sql/strataWithCriteria.sql");

    private static final String TIME_PERIOD_QUERY = ResourceHelper.GetResourceAsString("/resources/cohortcharacterizations/sql/createTimePeriod.sql");

    private static final String[] CRITERIA_REGEXES = new String[]{"groupQuery", "targetTable", "totalsTable", "aggregateJoinTable", "aggregateJoin", "aggregateCondition", "valueExpression", "useAggregatedValue"};
    private static final String[] STRATA_REGEXES = new String[]{"strataQuery", "targetTable", "strataCohortTable", "eventsTable"};

    private static final Collection<String> CRITERIA_PARAM_NAMES = ImmutableList.<String>builder()
            .add("cohortId", "executionId", "analysisId", "analysisName", "covariateName", "conceptId", "covariateId", "strataId", "strataName", "aggregateId", "aggregateName", "missingMeansZero")
            .build();

    private static final Collection<String> STRATA_PARAM_NAMES = ImmutableList.<String>builder()
            .add("cohortId")
            .add("strataId")
            .build();

    private static final Function<String, String> COMPLETE_DOTCOMMA = s -> s.trim().endsWith(";") ? s : s + ";";
    private static final String COUNT_SQLFUNC = "count(*)";

    private final String prevalenceRetrievingQuery = ResourceHelper.GetResourceAsString("/resources/cohortcharacterizations/sql/prevalenceRetrieving.sql");

    private final String distributionRetrievingQuery = ResourceHelper.GetResourceAsString("/resources/cohortcharacterizations/sql/distributionRetrieving.sql");

    private final String customDistributionQueryWrapper = ResourceHelper.GetResourceAsString("/resources/cohortcharacterizations/sql/customDistribution.sql");

    private final String customPrevalenceQueryWrapper = ResourceHelper.GetResourceAsString("/resources/cohortcharacterizations/sql/customPrevalence.sql");

    private final CohortCharacterization cohortCharacterization;
    private final String cohortTable;
    private final String cohortSchema;
    private final String sessionId;
    private final boolean includeAnnual;
    private final boolean includeTemporal;
    private String cdmSchema;
    private String resultsSchema;
    private String vocabularySchema;
    private String tempSchema;
    private Long jobId;

    private final CohortExpressionQueryBuilder queryBuilder;

    public CCQueryBuilder(String design, String cohortTable, String sessionId, String cdmSchema, String resultsSchema, String vocabularySchema, String tempSchema, long jobId) {
        this(Utils.deserialize(design, CohortCharacterizationImpl.class), cohortTable, sessionId, cdmSchema, resultsSchema, vocabularySchema, tempSchema, jobId, resultsSchema, false, false);
    }

    public CCQueryBuilder(CohortCharacterization design, String cohortTable, String sessionId, String cdmSchema, String resultsSchema, String vocabularySchema, String tempSchema, long jobId, boolean includeAnnual, boolean includeTemporal) {

        this(design, cohortTable, sessionId, cdmSchema, resultsSchema, vocabularySchema, tempSchema, jobId, resultsSchema, includeAnnual, includeTemporal);
    }

    public CCQueryBuilder(String design, String cohortTable, String sessionId, String cdmSchema, String resultsSchema, String vocabularySchema, String tempSchema, long jobId, String cohortSchema) {
        this(Utils.deserialize(design, CohortCharacterizationImpl.class), cohortTable, sessionId, cdmSchema, resultsSchema, vocabularySchema, tempSchema, jobId, cohortSchema, false, false);
    }

    public CCQueryBuilder(CohortCharacterization cohortCharacterization, String cohortTable, String sessionId, String cdmSchema, String resultsSchema, String vocabularySchema, String tempSchema, Long jobId, String cohortSchema, boolean includeAnnual, boolean includeTemporal) {
        this.cohortCharacterization = cohortCharacterization;
        this.cohortTable = cohortTable;
        this.cohortSchema = cohortSchema;
        this.sessionId = sessionId;
        this.cdmSchema = cdmSchema;
        this.resultsSchema = resultsSchema;
        this.vocabularySchema = vocabularySchema;
        this.tempSchema = tempSchema;
        this.jobId = jobId;
        this.queryBuilder = new CohortExpressionQueryBuilder();
        this.includeAnnual = includeAnnual;
        this.includeTemporal = includeTemporal;
    }

    public String build() {
        return cohortCharacterization.getCohorts()
                .stream()
                .map(def -> getAnalysisQueriesOnCohort(def.getId()))
                .flatMap(Collection::stream)
                .map(COMPLETE_DOTCOMMA)
                .collect(Collectors.joining("\n"));
    }

    private List<String> getAnalysisQueriesOnCohort(final Integer cohortDefinitionId) {

        final CohortExpressionQueryBuilder.BuildExpressionQueryOptions options = createDefaultOptions(cohortDefinitionId);
        List<String> queries = new LinkedList<>();
        queries.addAll(getSqlQueriesToRun(createFeJsonObject(options, cohortTable), cohortDefinitionId));
        if (includeTemporal) {
            JSONObject temporalJsonObject = createFeJsonObject(createDefaultOptions(cohortDefinitionId), cohortTable, includeTemporal, false);
            queries.addAll(getSqlQueriesToRun(temporalJsonObject, cohortDefinitionId));
        }
        if (includeAnnual) {
            JSONObject temporalJsonObject = createFeJsonObject(createDefaultOptions(cohortDefinitionId), cohortTable, false, includeAnnual);
            queries.addAll(getSqlQueriesToRun(temporalJsonObject, cohortDefinitionId));
        }
        return queries;
    }

    private String renderCustomAnalysisDesign(FeatureAnalysis fa, Integer cohortId, CohortCharacterizationStrata strata) {

        String cohortTable = Optional.ofNullable(strata).map(this::getStrataCohortTable).orElse(this.cohortTable);
        Map<String, String> params = cohortCharacterization.getParameters().stream().collect(Collectors.toMap(CohortCharacterizationParam::getName, v -> v.getValue().toString()));
        params.put("cdm_database_schema", cdmSchema);
        params.put("cohort_table", cohortTable);
        params.put("cohort_id", cohortId.toString());
        params.put("analysis_id", fa.getId().toString());

        return SqlRender.renderSql(
                (String) fa.getDesign(),
                params.keySet().toArray(new String[params.size()]),
                params.values().toArray(new String[params.size()])
        );
    }

    private List<String> getQueriesForCustomDistributionAnalyses(final Integer cohortId, CohortCharacterizationStrata strata) {

        Long strataId = Optional.ofNullable(strata).map(CohortCharacterizationStrata::getId).orElse(0L);
        String strataName = Optional.ofNullable(strata).map(CohortCharacterizationStrata::getName).orElse("");

        return cohortCharacterization.getFeatureAnalyses()
                .stream()
                .filter(fa -> fa.getType().equals(StandardFeatureAnalysisType.CUSTOM_FE))
                .filter(v -> Objects.equals(v.getStatType(), CcResultType.DISTRIBUTION))
                .flatMap(v -> prepareStatements(customDistributionQueryWrapper, sessionId,
                        ArrayUtils.addAll(CUSTOM_PARAMETERS, "strataId", "strataName"),
                        new String[]{String.valueOf(v.getId()), QuoteUtils.escapeSql(v.getName()),
                                String.valueOf(cohortId), String.valueOf(jobId), renderCustomAnalysisDesign(v, cohortId, strata),
                                String.valueOf(strataId), QuoteUtils.escapeSql(strataName)}).stream())
                .collect(Collectors.toList());
    }

    private List<String> getQueriesForCustomPrevalenceAnalyses(final Integer cohortId, CohortCharacterizationStrata strata) {

        Long strataId = Optional.ofNullable(strata).map(CohortCharacterizationStrata::getId).orElse(0L);
        String strataName = Optional.ofNullable(strata).map(CohortCharacterizationStrata::getName).orElse("");

        return cohortCharacterization.getFeatureAnalyses()
                .stream()
                .filter(v -> Objects.equals(v.getType(), StandardFeatureAnalysisType.CUSTOM_FE))
                .filter(v -> v.getStatType() == CcResultType.PREVALENCE)
                .flatMap(v -> prepareStatements(customPrevalenceQueryWrapper, sessionId,
                        ArrayUtils.addAll(CUSTOM_PARAMETERS, "strataId", "strataName"),
                        new String[]{String.valueOf(v.getId()), QuoteUtils.escapeSql(v.getName()), String.valueOf(cohortId),
                                String.valueOf(jobId), renderCustomAnalysisDesign(v, cohortId, strata), String.valueOf(strataId), QuoteUtils.escapeSql(strataName)}).stream())
                .collect(Collectors.toList());
    }

    private List<String> getQueriesForCriteriaAnalyses(Integer cohortDefinitionId, CohortCharacterizationStrata strata) {
        List<String> queries = new ArrayList<>();
        List<FeatureAnalysisWithCriteria<? extends BaseCriteriaFeature, Integer>> analysesWithCriteria = getFeAnalysesWithCriteria();
        if (!analysesWithCriteria.isEmpty()) {
            String cohortTable = Optional.ofNullable(strata).map(this::getStrataCohortTable).orElse(this.cohortTable);
            analysesWithCriteria.stream()
                    .map(analysis -> getCriteriaFeaturesQueries(cohortDefinitionId, analysis, cohortTable, strata))
                    .flatMap(Collection::stream)
                    .forEach(queries::add);
        }
        return queries;
    }

    private List<FeatureAnalysisWithCriteria<? extends BaseCriteriaFeature, Integer>> getFeAnalysesWithCriteria() {

        return cohortCharacterization.getFeatureAnalyses().stream()
                .filter(fa -> StandardFeatureAnalysisType.CRITERIA_SET.equals(fa.getType()))
                .map(fa -> (FeatureAnalysisWithCriteria<? extends BaseCriteriaFeature, Integer>) fa)
                .collect(Collectors.toList());
    }

    private List<String> getQueriesForPresetAnalyses(final JSONObject jsonObject, final Integer cohortId, final CohortCharacterizationStrata strata) {
        final String cohortWrapper = "select %1$d as %2$s from (%3$s) W";

        final String featureRefColumns = "cohort_definition_id, covariate_id, covariate_name, analysis_id, concept_id";
        final String featureRefs = String.format(cohortWrapper, cohortId, featureRefColumns,
                StringUtils.stripEnd(jsonObject.getString("sqlQueryFeatureRef"), ";"));

        final String analysisRefColumns = "cohort_definition_id, CAST(analysis_id AS INT) analysis_id, analysis_name, domain_id, CAST(is_binary AS CHAR(1)) is_binary,CAST(missing_means_zero AS CHAR(1)) missing_means_zero";
        final String analysisRefs = String.format(cohortWrapper, cohortId, analysisRefColumns,
                StringUtils.stripEnd(jsonObject.getString("sqlQueryAnalysisRef"), ";"));

        final List<String> queries = new ArrayList<>();

        Long strataId = Objects.nonNull(strata) ? strata.getId() : 0L;
        String strataName = Objects.nonNull(strata) ? strata.getName() : "";
        boolean temporal = isTemporal(jsonObject);
        boolean temporalAnnual = jsonObject.has("temporalPeriod") && jsonObject.getString("temporalPeriod").equals("annual");

        if (!temporal && !temporalAnnual && ccHasPresetDistributionAnalyses()) {
            final String distColumns = "cohort_definition_id, covariate_id, count_value, min_value, max_value, average_value, "
                    + "standard_deviation, median_value, p10_value, p25_value, p75_value, p90_value";
            if (jsonObject.has("sqlQueryContinuousFeatures")) {
                final String distFeatures = String.format(cohortWrapper, cohortId, distColumns,
                        StringUtils.stripEnd(jsonObject.getString("sqlQueryContinuousFeatures"), ";"));
                queries.addAll(prepareStatements(distributionRetrievingQuery, sessionId, ArrayUtils.addAll(RETRIEVING_PARAMETERS, "strataId", "strataName"),
                        new String[]{distFeatures, featureRefs, analysisRefs, String.valueOf(cohortId), String.valueOf(jobId), "cc_results",
                                String.valueOf(strataId), QuoteUtils.escapeSql(strataName)}));
            }
        }
        if (ccHasPresetPrevalenceAnalyses()) {
            final String featureColumns = String.join(",",
                    "cohort_definition_id",
                    "covariate_id",
                    "sum_value",
                    "average_value"
            );
            if (temporal) {
                final String features = String.format(cohortWrapper, cohortId, featureColumns + ", time_id",
                        StringUtils.stripEnd(jsonObject.getString("sqlQueryFeatures"), ";"));
                generateInsertResults(cohortId, temporal, false, features, featureRefs, analysisRefs, strataId, strataName, queries);
            }
            if (temporalAnnual && jsonObject.has("sqlQueryFeatures")) {
                final String features = String.format(cohortWrapper, cohortId, featureColumns + ", event_year",
                        StringUtils.stripEnd(jsonObject.getString("sqlQueryFeatures"), ";"));
                generateInsertResults(cohortId, false, temporalAnnual, features, featureRefs, analysisRefs, strataId, strataName, queries);
            }
            if (!temporal && !temporalAnnual) {
                final String features = String.format(cohortWrapper, cohortId, featureColumns,
                        StringUtils.stripEnd(jsonObject.getString("sqlQueryFeatures"), ";"));
                generateInsertResults(cohortId, false, false, features, featureRefs, analysisRefs, strataId, strataName, queries);
            }
        }

        return queries;
    }

    private void generateInsertResults(Integer cohortId, boolean temporal, boolean temporalAnnual, String features, String featureRefs, String analysisRefs, Long strataId, String strataName, List<String> queries) {
        String resultsTable = temporal ? "cc_temporal_results" : (temporalAnnual ? "cc_temporal_annual_results" : "cc_results");
        String[] paramValues = new String[]{features, featureRefs, analysisRefs, String.valueOf(cohortId), String.valueOf(jobId),
                resultsTable, String.valueOf(temporal), String.valueOf(temporalAnnual), String.valueOf(strataId), QuoteUtils.escapeSql(strataName)};
        String[] parameters = ArrayUtils.addAll(RETRIEVING_PARAMETERS, "temporal", "temporal_annual", "strataId", "strataName");
        queries.addAll(prepareStatements(prevalenceRetrievingQuery, sessionId, parameters, paramValues));
    }

    private boolean isTemporal(JSONObject jsonObject) {
        boolean result = false;
        if (jsonObject.has("tempTables")) {
            JSONObject tempTables = jsonObject.getJSONObject("tempTables");
            result = tempTables.has("#time_period");
        }
        return result;
    }

    private boolean ccHasPresetPrevalenceAnalyses() {
        return cohortCharacterization.getFeatureAnalyses()
                .stream()
                .anyMatch(analysis -> Objects.equals(analysis.getType(), StandardFeatureAnalysisType.PRESET) && analysis.getStatType() == CcResultType.PREVALENCE);
    }

    private boolean ccHasPresetDistributionAnalyses() {
        return cohortCharacterization.getFeatureAnalyses()
                .stream()
                .anyMatch(analysis -> Objects.equals(analysis.getType(), StandardFeatureAnalysisType.PRESET) && analysis.getStatType() == CcResultType.DISTRIBUTION);
    }

    private CohortExpressionQueryBuilder.BuildExpressionQueryOptions createDefaultOptions(final Integer id) {
        final CohortExpressionQueryBuilder.BuildExpressionQueryOptions options = new CohortExpressionQueryBuilder.BuildExpressionQueryOptions();
        options.cdmSchema = cdmSchema;
        options.cohortId = id;
        options.generateStats = false;
        return options;
    }

    private List<String> getCriteriaFeatureQuery(Integer cohortDefinitionId, FeatureAnalysis analysis, BaseCriteriaFeature feature, String targetTable, CohortCharacterizationStrata strata) {

        Long conceptId = 0L;
        String queryFile;
        String groupQuery = getCriteriaGroupQuery(analysis, feature, "qualified_events");
        String[] paramNames = CRITERIA_PARAM_NAMES.toArray(new String[0]);

        if (CcResultType.PREVALENCE.equals(analysis.getStatType())) {
            queryFile = COHORT_STATS_QUERY;
        } else if (CcResultType.DISTRIBUTION.equals(analysis.getStatType())) {
            queryFile = COHORT_DIST_QUERY;
        } else {
            throw new IllegalArgumentException(String.format("Stat type %s is not supported", analysis.getStatType()));
        }
        Long strataId = Objects.nonNull(strata) ? strata.getId() : 0L;
        String strataName = Objects.nonNull(strata) ? strata.getName() : "";
        Collection<String> paramValues = Lists.newArrayList(String.valueOf(cohortDefinitionId), String.valueOf(jobId), String.valueOf(analysis.getId()),
                QuoteUtils.escapeSql(analysis.getName()), QuoteUtils.escapeSql(feature.getName()), String.valueOf(conceptId),
                String.valueOf(((WithId) feature).getId()), String.valueOf(strataId), QuoteUtils.escapeSql(strataName),
                String.valueOf(getAggregateId(feature)),
                getAggregateName(feature),
                getMissingMeansZero(feature) ? "1" : "0");
        String aggregateJoinTable = getAggregateJoinTable(feature);
        String valueExpression = getValueExpression(feature);
        String[] criteriaValues = new String[]{groupQuery, targetTable, cohortTable, aggregateJoinTable, getAggregateJoin(feature), getAggregateCondition(feature), valueExpression,
                String.valueOf(useAggregatedValue(feature))};
        queryFile = StringUtils.replace(queryFile, "@missingMeansZeroQuery", getMissingMeansZero(feature) ? MISSING_MEANS_ZERO_QUERY : "");
        return Arrays.stream(SqlSplit.splitSql(queryFile))
                .map(COMPLETE_DOTCOMMA)
                .flatMap(sql -> prepareStatements(sql, sessionId, ArrayUtils.addAll(CRITERIA_REGEXES, paramNames),
                        ArrayUtils.addAll(criteriaValues, paramValues.toArray(new String[0]))).stream())
                .collect(Collectors.toList());
    }

    private String getAggregateCondition(BaseCriteriaFeature feature) {
        return SafeFeature.getAsString(feature, f -> StringUtils.defaultString(f.getJoinCondition()));
    }

    private String getAggregateJoin(BaseCriteriaFeature feature) {
        return SafeFeature.getAsString(feature, f -> Objects.nonNull(f.getJoinType()) ? f.getJoinType().getTerm() : "");
    }

    private Integer getAggregateId(BaseCriteriaFeature feature) {

        return SafeFeature.getAsInteger(feature, FeatureAnalysisAggregate::getId);
    }

    private String getAggregateName(BaseCriteriaFeature feature) {

        return Optional.ofNullable(feature.getAggregate())
                .map(FeatureAnalysisAggregate::getName)
                .map(QuoteUtils::escapeSql)
                .orElse("");
    }

    private boolean getMissingMeansZero(BaseCriteriaFeature feature) {
        return SafeFeature.getAsBoolean(feature, FeatureAnalysisAggregate::isMissingMeansZero);
    }

    private String getAggregateJoinTable(BaseCriteriaFeature feature) {

        return SafeFeature.getAsString(feature, f -> f.hasQuery() ? f.getJoinTable() : "");
    }

    private String getValueExpression(BaseCriteriaFeature feature) {

        String expr = COUNT_SQLFUNC;
        FeatureAnalysisAggregate aggregate = feature.getAggregate();
        if (Objects.nonNull(aggregate)) {
            Optional<AggregateFunction> aggregator = Optional.ofNullable(aggregate.getFunction());
            expr = aggregator.map(a -> a.getName() + "(").orElse("") +
                    (aggregate.getExpression() != null ? aggregate.getExpression() : "CAST(0 as BIGINT)") +
                    aggregator.map(a -> ")").orElse("");
        }
        return expr;
    }

    private List<CriteriaColumn> getAdditionalColumns(BaseCriteriaFeature feature) {

        return Optional.ofNullable(feature.getAggregate())
                .map(FeatureAnalysisAggregate::getAdditionalColumns)
                .orElse(Collections.emptyList());
    }

    private boolean useAggregatedValue(BaseCriteriaFeature feature) {

        FeatureAnalysisAggregate aggregate = feature.getAggregate();
        return Objects.isNull(aggregate) || Objects.nonNull(aggregate.getFunction());
    }

    private String getCriteriaGroupQuery(FeatureAnalysis analysis, BaseCriteriaFeature feature, String eventTable) {
        String groupQuery;
        if (CcResultType.PREVALENCE.equals(analysis.getStatType())) {
            groupQuery = queryBuilder.getCriteriaGroupQuery(((CriteriaFeature) feature).getExpression(), eventTable);
        } else if (CcResultType.DISTRIBUTION.equals(analysis.getStatType())) {
            if (feature instanceof WindowedCriteriaFeature) {
                WindowedCriteria criteria = ((WindowedCriteriaFeature) feature).getExpression();
                criteria.ignoreObservationPeriod = true;
                BuilderOptions options = new BuilderOptions();
                options.additionalColumns = getAdditionalColumns(feature);
                groupQuery = queryBuilder.getWindowedCriteriaQuery(criteria, eventTable, options);
            } else if (feature instanceof DemographicCriteriaFeature) {
                DemographicCriteria criteria = ((DemographicCriteriaFeature) feature).getExpression();
                groupQuery = queryBuilder.getDemographicCriteriaQuery(criteria, eventTable);
            } else {
                throw new IllegalArgumentException(String.format("Feature class %s is not supported", feature.getClass()));
            }
        } else {
            throw new IllegalArgumentException(String.format("Stat type %s is not supported", analysis.getStatType()));
        }
        return SqlRender.renderSql(groupQuery, ArrayUtils.addAll(DAIMONS, "indexId"),
                new String[]{resultsSchema, cdmSchema, tempSchema, vocabularySchema, "0"});
    }

    private List<String> getQueriesForStratifiedCriteriaAnalyses(Integer cohortDefinitionId) {

        List<String> queriesToRun = new ArrayList<>();
        List<String> strataCohortQueries = new ArrayList<>();
        strataCohortQueries.addAll(getCodesetQuery(cohortCharacterization.getStrataConceptSets()));

        //Generate stratified cohorts
        strataCohortQueries.addAll(cohortCharacterization.getStratas().stream()
                .flatMap(strata -> getStrataQuery(cohortDefinitionId, strata).stream())
                .collect(Collectors.toList()));

        strataCohortQueries.addAll(prepareStatements("TRUNCATE TABLE #Codesets;\n", sessionId));
        strataCohortQueries.addAll(prepareStatements("DROP TABLE #Codesets;\n", sessionId));
        queriesToRun.addAll(strataCohortQueries);

        //Extract features from stratified cohorts
        queriesToRun.addAll(cohortCharacterization.getStratas().stream()
                .flatMap(strata -> {
                    JSONObject jsonObject = createFeJsonObject(createDefaultOptions(cohortDefinitionId), getStrataCohortTable(strata));
                    List<String> queries = new ArrayList<>();
                    queries.addAll(getCreateQueries(jsonObject));
                    queries.addAll(getFeatureAnalysesQueries(jsonObject, cohortDefinitionId, strata));
                    queries.addAll(getCleanupQueries(jsonObject));
                    return queries.stream();
                })
                .collect(Collectors.toList()));

        //Cleanup stratified cohorts tables
        queriesToRun.addAll(cohortCharacterization.getStratas().stream()
                .flatMap(strata -> prepareStatements("DROP TABLE " + getStrataCohortTable(strata) + ";", sessionId).stream())
                .collect(Collectors.toList()));

        return queriesToRun;
    }

    private List<String> getStrataQuery(Integer cohortDefinitionId, CohortCharacterizationStrata strata) {
        List<String> queries = new ArrayList<>();
        String eventsTable = String.format("#qualified_events_%d", strata.getId());
        String strataQuery = queryBuilder.getCriteriaGroupQuery(strata.getCriteria(), eventsTable);
        strataQuery = SqlRender.renderSql(strataQuery, ArrayUtils.addAll(DAIMONS, "indexId"),
                new String[]{resultsSchema, cdmSchema, tempSchema, vocabularySchema, "0"});
        String[] paramNames = STRATA_PARAM_NAMES.toArray(new String[0]);
        String[] replacements = new String[]{strataQuery, cohortTable, getStrataCohortTable(strata), eventsTable};
        String[] paramValues = new String[]{String.valueOf(cohortDefinitionId), String.valueOf(strata.getId())};
        queries.addAll(prepareStatements("CREATE TABLE " + getStrataCohortTable(strata)
                + "(cohort_definition_id INTEGER, strata_id BIGINT, subject_id BIGINT, cohort_start_date DATE, cohort_end_date DATE);", sessionId));
        String[] statements = SqlSplit.splitSql(COHORT_STRATA_QUERY);
        queries.addAll(Arrays.stream(statements)
                .map(COMPLETE_DOTCOMMA)
                .flatMap(q -> prepareStatements(q, sessionId, ArrayUtils.addAll(STRATA_REGEXES, paramNames), ArrayUtils.addAll(replacements, paramValues)).stream())
                .collect(Collectors.toList())
        );
        return queries;
    }

    private String getStrataCohortTable(CohortCharacterizationStrata strata) {

        return String.format("%s.sc_%s_%d", tempSchema, sessionId, strata.getId());
    }

    private List<String> getCodesetQuery(Collection<ConceptSet> conceptSets) {

        String codesetQuery = queryBuilder.getCodesetQuery(conceptSets.toArray(new ConceptSet[0]));
        return new ArrayList<>(prepareStatements(codesetQuery, sessionId));
    }

    private List<String> getCriteriaFeaturesQueries(Integer cohortDefinitionId, FeatureAnalysisWithCriteria<? extends BaseCriteriaFeature, Integer> analysis, String targetTable, CohortCharacterizationStrata strata) {

        List<String> queriesToRun = new ArrayList<>();
        queriesToRun.addAll(getCodesetQuery(analysis.getConceptSets()));

        queriesToRun.addAll(analysis.getDesign().stream()
                .map(feature -> getCriteriaFeatureQuery(cohortDefinitionId, analysis, feature, targetTable, strata))
                .flatMap(Collection::stream)
                .collect(Collectors.toList())); // statistics queries
        queriesToRun.addAll(prepareStatements("DROP TABLE #Codesets;", sessionId));
        return queriesToRun;
    }

    private Collection<String> prepareStatements(String query, String sessionId, String[] regexes, String[] variables) {

        final String[] tmpRegexes = ArrayUtils.addAll(regexes, DAIMONS);
        final String[] tmpValues = ArrayUtils.addAll(variables, resultsSchema, cdmSchema, tempSchema, vocabularySchema);

        String sql = SqlRender.renderSql(query, tmpRegexes, tmpValues);
        return Arrays.asList(SqlSplit.splitSql(sql));
    }

    private Collection<String> prepareStatements(String query, String sessionId) {

        return prepareStatements(query, sessionId, new String[]{}, new String[]{});
    }

    private List<String> getSqlQueriesToRun(final JSONObject jsonObject, final Integer cohortDefinitionId) {
        List<String> queriesToRun = new LinkedList<>();

        if (!cohortCharacterization.getStrataOnly() || cohortCharacterization.getStratas().isEmpty()) {
            List<String> ccQueries = new LinkedList<>();
            ccQueries.addAll(getCreateQueries(jsonObject));
            ccQueries.addAll(getFeatureAnalysesQueries(jsonObject, cohortDefinitionId, null));
            ccQueries.addAll(getCleanupQueries(jsonObject));

            queriesToRun.addAll(ccQueries);
        }

        if (!cohortCharacterization.getStratas().isEmpty()) {
            queriesToRun.addAll(getQueriesForStratifiedCriteriaAnalyses(cohortDefinitionId));
        }

        return queriesToRun;
    }

    private List<String> getCreateQueries(final JSONObject jsonObject) {
        return Stream.concat(
                        getCreateTimePeriod(jsonObject),
                        Arrays.stream(SqlSplit.splitSql(jsonObject.getString("sqlConstruction")))
                )
                .map(COMPLETE_DOTCOMMA)
                .flatMap(sql -> prepareStatements(sql, sessionId).stream())
                .collect(Collectors.toList());
    }

    private Stream<String> getCreateTimePeriod(final JSONObject jsonObject) {
        if (jsonObject.has("tempTables")) {
            JSONObject tempTables = jsonObject.getJSONObject("tempTables");
            if (tempTables.has("#time_period")) {
                JSONObject timePeriod = tempTables.getJSONObject("#time_period");
                JSONArray timeId = timePeriod.getJSONArray("time_id");
                JSONArray startDate = timePeriod.getJSONArray("start_day");
                JSONArray endDate = timePeriod.getJSONArray("end_day");
                List<String> sqlValues = new ArrayList<>();
                for (int i = 0; i < timeId.length() - 1; i++) {
                    sqlValues.add(MessageFormat.format("({0}, {1}, {2})", timeId.get(i), startDate.get(i), endDate.get(i)));
                }
                return Arrays.stream(SqlSplit.splitSql(SqlRender.renderSql(TIME_PERIOD_QUERY, new String[]{"values"}, new String[]{String.join(", \n", sqlValues)})));
            }
        }
        return Stream.of();
    }

    private List<String> getCleanupQueries(final JSONObject jsonObject) {

        return Arrays.stream(SqlSplit.splitSql(jsonObject.getString("sqlCleanup")))
                .map(COMPLETE_DOTCOMMA)
                .flatMap(sql -> prepareStatements(sql, sessionId).stream())
                .collect(Collectors.toList());
    }

    private List<String> getFeatureAnalysesQueries(final JSONObject jsonObject, final Integer cohortDefinitionId, final CohortCharacterizationStrata strata) {

        List<String> queriesToRun = new ArrayList<>();
        queriesToRun.addAll(getQueriesForPresetAnalyses(jsonObject, cohortDefinitionId, strata));
        queriesToRun.addAll(getQueriesForCustomDistributionAnalyses(cohortDefinitionId, strata));
        queriesToRun.addAll(getQueriesForCustomPrevalenceAnalyses(cohortDefinitionId, strata));
        queriesToRun.addAll(getQueriesForCriteriaAnalyses(cohortDefinitionId, strata));
        return queriesToRun;
    }

    private JSONObject createFeJsonObject(final CohortExpressionQueryBuilder.BuildExpressionQueryOptions options, final String cohortTable) {
        return createFeJsonObject(options, cohortTable, false, false);
    }

    private JSONObject createFeJsonObject(final CohortExpressionQueryBuilder.BuildExpressionQueryOptions options, final String cohortTable, boolean temporal, boolean temporalAnnual) {
        FeatureExtraction.init(null);
        String settings = buildSettings(temporal, temporalAnnual);
        String sqlJson = FeatureExtraction.createSql(settings, true, tempSchema + "." + cohortTable,
                "subject_id", options.cohortId, options.cdmSchema);
        return new JSONObject(sqlJson);
    }

    private String buildSettings(boolean temporal, boolean temporalAnnual) {

        final JSONObject defaultSettings = new JSONObject(temporal ? FeatureExtraction.getDefaultPrespecTemporalAnalyses() : FeatureExtraction.getDefaultPrespecAnalyses());
        Map<String, FeatureExtraction.PrespecAnalysis> prespecAnalysisMap = FeatureExtraction.getNameToPrespecAnalysis();
        Map<String, FeatureExtraction.PrespecAnalysis> temporalAnalysisMap = FeatureExtraction.getNameToPrespecTemporalAnalysis();
        Map<String, FeatureExtraction.PrespecAnalysis> annualAnalysisMap = FeatureExtraction.getNameToPrespecTemporalAnnualAnalysis();
        Map<String, FeatureExtraction.PrespecAnalysis> analysisMap = temporal ? temporalAnalysisMap : (temporalAnnual ? annualAnalysisMap : prespecAnalysisMap);
        Stream.of(prespecAnalysisMap.keySet(), temporalAnalysisMap.keySet(), annualAnalysisMap.keySet()).flatMap(Set::stream).forEach(defaultSettings::remove);
        defaultSettings.put("temporal", temporal);
        defaultSettings.put("temporalSequence", false);
        defaultSettings.put("temporalAnnual", temporalAnnual);

        Function<FeatureAnalysis, Predicate<String>> designEquals = fa -> a -> Objects.equals(a, fa.getDesign());
        Function<? super FeatureAnalysis, Integer> toAnalysisId = temporal ?
                mapFeatureAnalysisId(temporalAnalysisMap, fa -> a -> fa.getDesign().toString().startsWith(a)) :
                (
                        temporalAnnual ? mapFeatureAnalysisId(annualAnalysisMap, designEquals) : mapFeatureAnalysisId(prespecAnalysisMap, designEquals)
                );

        Map<Integer, FeatureExtraction.PrespecAnalysis> analysisIdMap = analysisMap.values().stream()
                .collect(Collectors.toMap(fa -> fa.analysisId, Function.identity()));

        Predicate<? super FeatureAnalysis> predicate = temporal || temporalAnnual ?
                fa -> analysisMap.values().stream().anyMatch(a -> Objects.equals(a.analysisId, toAnalysisId.apply(fa))) :
                fa -> Objects.equals(fa.getType(), StandardFeatureAnalysisType.PRESET);
        Function<? super FeatureAnalysis, String> mapper = temporal || temporalAnnual ?
                fa -> Optional.ofNullable(analysisIdMap.get(toAnalysisId.apply(fa))).map(a -> a.analysisName)
                        .orElseThrow(() -> new RuntimeException(MessageFormat.format("Analysis [{0}] not found", fa.getDesign()))) :
                fa -> ((FeatureAnalysis<String, Integer>) fa).getDesign();
        cohortCharacterization.getParameters().forEach(param -> defaultSettings.put(param.getName(), param.getValue()));
        cohortCharacterization.getFeatureAnalyses()
                .stream()
                .filter(predicate)
                .map(mapper)
                .forEach(analysis -> defaultSettings.put(analysis, Boolean.TRUE));

        return defaultSettings.toString();
    }

    private Function<? super FeatureAnalysis, Integer> mapFeatureAnalysisId(Map<String, FeatureExtraction.PrespecAnalysis> analysisMap, Function<FeatureAnalysis, Predicate<String>> predicate) {
        return  fa -> analysisMap.keySet().stream()
                .filter(predicate.apply(fa))
                .findFirst()
                .map(a -> analysisMap.get(a).analysisId)
                .orElse(null);
    }

}
