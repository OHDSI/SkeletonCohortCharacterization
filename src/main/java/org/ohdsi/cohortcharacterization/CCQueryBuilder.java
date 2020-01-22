package org.ohdsi.cohortcharacterization;

import static org.ohdsi.cohortcharacterization.Constants.Params.CDM_DATABASE_SCHEMA;
import static org.ohdsi.cohortcharacterization.Constants.Params.RESULTS_DATABASE_SCHEMA;
import static org.ohdsi.cohortcharacterization.Constants.Params.TEMP_DATABASE_SCHEMA;
import static org.ohdsi.cohortcharacterization.Constants.Params.VOCABULARY_DATABASE_SCHEMA;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.ohdsi.analysis.Utils;
import org.ohdsi.analysis.WithId;
import org.ohdsi.analysis.cohortcharacterization.design.BaseCriteriaFeature;
import org.ohdsi.analysis.cohortcharacterization.design.CcResultType;
import org.ohdsi.analysis.cohortcharacterization.design.CohortCharacterization;
import org.ohdsi.analysis.cohortcharacterization.design.CohortCharacterizationParam;
import org.ohdsi.analysis.cohortcharacterization.design.CohortCharacterizationStrata;
import org.ohdsi.analysis.cohortcharacterization.design.CriteriaFeature;
import org.ohdsi.analysis.cohortcharacterization.design.DemographicCriteriaFeature;
import org.ohdsi.analysis.cohortcharacterization.design.FeatureAnalysis;
import org.ohdsi.analysis.cohortcharacterization.design.FeatureAnalysisWithCriteria;
import org.ohdsi.analysis.cohortcharacterization.design.StandardFeatureAnalysisType;
import org.ohdsi.analysis.cohortcharacterization.design.WindowedCriteriaFeature;
import org.ohdsi.circe.cohortdefinition.CohortExpressionQueryBuilder;
import org.ohdsi.circe.cohortdefinition.ConceptSet;
import org.ohdsi.circe.cohortdefinition.DemographicCriteria;
import org.ohdsi.circe.cohortdefinition.WindowedCriteria;
import org.ohdsi.circe.helper.ResourceHelper;
import org.ohdsi.cohortcharacterization.design.CohortCharacterizationImpl;
import com.odysseusinc.arachne.commons.utils.QuoteUtils;
import org.ohdsi.featureExtraction.FeatureExtraction;
import org.ohdsi.sql.SqlRender;
import org.ohdsi.sql.SqlSplit;

public class CCQueryBuilder {

	private static final String[] CUSTOM_PARAMETERS = {"analysisId", "analysisName", "cohortId", "jobId", "design"};
	private static final String[] RETRIEVING_PARAMETERS = {"features", "featureRefs", "analysisRefs", "cohortId", "executionId"};
	private static final String[] DAIMONS = {RESULTS_DATABASE_SCHEMA, CDM_DATABASE_SCHEMA, TEMP_DATABASE_SCHEMA, VOCABULARY_DATABASE_SCHEMA};

	private static final String COHORT_STATS_QUERY = ResourceHelper.GetResourceAsString("/resources/cohortcharacterizations/sql/prevalenceWithCriteria.sql");
	private static final String COHORT_DIST_QUERY = ResourceHelper.GetResourceAsString("/resources/cohortcharacterizations/sql/distributionWithCriteria.sql");

	private static final String COHORT_STRATA_QUERY = ResourceHelper.GetResourceAsString("/resources/cohortcharacterizations/sql/strataWithCriteria.sql");

	private static final String[] CRITERIA_REGEXES = new String[] { "groupQuery", "targetTable", "totalsTable" };
	private static final String[] STRATA_REGEXES = new String[] { "strataQuery", "targetTable", "strataCohortTable", "eventsTable" };

	private static final Collection<String> CRITERIA_PARAM_NAMES = ImmutableList.<String>builder()
					.add("cohortId", "executionId", "analysisId", "analysisName", "covariateName", "conceptId", "covariateId", "strataId", "strataName")
					.build();

	private static final Collection<String> STRATA_PARAM_NAMES = ImmutableList.<String>builder()
					.add("cohortId")
					.add("strataId")
					.build();

	private static final Function<String, String> COMPLETE_DOTCOMMA = s -> s.trim().endsWith(";") ? s : s + ";";

	private final String prevalenceRetrievingQuery = ResourceHelper.GetResourceAsString("/resources/cohortcharacterizations/sql/prevalenceRetrieving.sql");

	private final String distributionRetrievingQuery = ResourceHelper.GetResourceAsString("/resources/cohortcharacterizations/sql/distributionRetrieving.sql");

	private final String customDistributionQueryWrapper = ResourceHelper.GetResourceAsString("/resources/cohortcharacterizations/sql/customDistribution.sql");

	private final String customPrevalenceQueryWrapper = ResourceHelper.GetResourceAsString("/resources/cohortcharacterizations/sql/customPrevalence.sql");

	private final CohortCharacterization cohortCharacterization;
	private final String cohortTable;
	private final String sessionId;
	private String cdmSchema;
	private String resultsSchema;
	private String vocabularySchema;
	private String tempSchema;
	private Long jobId;

	private final CohortExpressionQueryBuilder queryBuilder;

	public CCQueryBuilder(String design, String cohortTable, String sessionId, String cdmSchema, String resultsSchema, String vocabularySchema, String tempSchema, long jobId) {

		this(Utils.deserialize(design, CohortCharacterizationImpl.class), cohortTable, sessionId, cdmSchema, resultsSchema, vocabularySchema, tempSchema, jobId);
	}

	public CCQueryBuilder(CohortCharacterization cohortCharacterization, String cohortTable, String sessionId, String cdmSchema, String resultsSchema, String vocabularySchema, String tempSchema, Long jobId) {
		this.cohortCharacterization = cohortCharacterization;
		this.cohortTable = cohortTable;
		this.sessionId = sessionId;
		this.cdmSchema = cdmSchema;
		this.resultsSchema = resultsSchema;
		this.vocabularySchema = vocabularySchema;
		this.tempSchema = tempSchema;
		this.jobId = jobId;
		this.queryBuilder = new CohortExpressionQueryBuilder();
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
		return getSqlQueriesToRun(createFeJsonObject(options, options.resultSchema + "." + cohortTable), cohortDefinitionId);
	}

	private String renderCustomAnalysisDesign(FeatureAnalysis fa, Integer cohortId, CohortCharacterizationStrata strata) {

		String cohortTable = Objects.nonNull(strata) ? getStrataCohortTable(strata) : tempSchema + "." + this.cohortTable;
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

		Long strataId = Objects.nonNull(strata) ? strata.getId() : 0L;
		String strataName = Objects.nonNull(strata) ? strata.getName() : "";

		return cohortCharacterization.getFeatureAnalyses()
						.stream()
						.filter(fa -> fa.getType().equals(StandardFeatureAnalysisType.CUSTOM_FE))
						.filter(v -> Objects.equals(v.getStatType(), CcResultType.DISTRIBUTION))
						.flatMap(v -> prepareStatements(customDistributionQueryWrapper, sessionId,
										ArrayUtils.addAll(CUSTOM_PARAMETERS, "strataId", "strataName"),
										new String[] { String.valueOf(v.getId()), QuoteUtils.escapeSql(v.getName()),
												String.valueOf(cohortId), String.valueOf(jobId), renderCustomAnalysisDesign(v, cohortId, strata),
												String.valueOf(strataId), strataName }).stream())
						.collect(Collectors.toList());
	}

	private List<String> getQueriesForCustomPrevalenceAnalyses(final Integer cohortId, CohortCharacterizationStrata strata) {

		Long strataId = Objects.nonNull(strata) ? strata.getId() : 0L;
		String strataName = Objects.nonNull(strata) ? strata.getName() : "";

		return cohortCharacterization.getFeatureAnalyses()
						.stream()
						.filter(v -> Objects.equals(v.getType(), StandardFeatureAnalysisType.CUSTOM_FE))
						.filter(v -> v.getStatType() == CcResultType.PREVALENCE)
						.flatMap(v -> prepareStatements(customPrevalenceQueryWrapper, sessionId,
										ArrayUtils.addAll(CUSTOM_PARAMETERS, "strataId", "strataName"),
										new String[] { String.valueOf(v.getId()), QuoteUtils.escapeSql(v.getName()), String.valueOf(cohortId),
												String.valueOf(jobId), renderCustomAnalysisDesign(v, cohortId, strata), String.valueOf(strataId), strataName }).stream())
						.collect(Collectors.toList());
	}

	private List<String> getQueriesForCriteriaAnalyses(Integer cohortDefinitionId, CohortCharacterizationStrata strata) {
		List<String> queries = new ArrayList<>();
		List<FeatureAnalysisWithCriteria<? extends BaseCriteriaFeature, Integer>> analysesWithCriteria = getFeAnalysesWithCriteria();
		if (!analysesWithCriteria.isEmpty()) {
			String cohortTable = Objects.nonNull(strata) ? getStrataCohortTable(strata) : tempSchema + "." + this.cohortTable;
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
						.map(fa -> (FeatureAnalysisWithCriteria<? extends BaseCriteriaFeature, Integer>)fa)
						.collect(Collectors.toList());
	}

	private List<String> getQueriesForPresetAnalyses(final JSONObject jsonObject, final Integer cohortId, final CohortCharacterizationStrata strata) {
		final String cohortWrapper = "select %1$d as %2$s from (%3$s) W";

		final String featureRefColumns = "cohort_definition_id, covariate_id, covariate_name, analysis_id, concept_id";
		final String featureRefs = String.format(cohortWrapper, cohortId, featureRefColumns,
						StringUtils.stripEnd(jsonObject.getString("sqlQueryFeatureRef"), ";"));

		final String analysisRefColumns = "cohort_definition_id, CAST(analysis_id AS INT) analysis_id, analysis_name, domain_id, start_day, end_day, CAST(is_binary AS CHAR(1)) is_binary,CAST(missing_means_zero AS CHAR(1)) missing_means_zero";
		final String analysisRefs = String.format(cohortWrapper, cohortId, analysisRefColumns,
						StringUtils.stripEnd(jsonObject.getString("sqlQueryAnalysisRef"), ";"));

		final List<String> queries = new ArrayList<>();

		Long strataId = Objects.nonNull(strata) ? strata.getId() : 0L;
		String strataName = Objects.nonNull(strata) ? strata.getName() : "";

		if (ccHasPresetDistributionAnalyses()) {
			final String distColumns = "cohort_definition_id, covariate_id, count_value, min_value, max_value, average_value, "
							+ "standard_deviation, median_value, p10_value, p25_value, p75_value, p90_value";
			final String distFeatures = String.format(cohortWrapper, cohortId, distColumns,
							StringUtils.stripEnd(jsonObject.getString("sqlQueryContinuousFeatures"), ";"));
			queries.addAll(prepareStatements(distributionRetrievingQuery, sessionId, ArrayUtils.addAll(RETRIEVING_PARAMETERS, "strataId", "strataName"),
							new String[] { distFeatures, featureRefs, analysisRefs, String.valueOf(cohortId), String.valueOf(jobId), String.valueOf(strataId), strataName }));
		}
		if (ccHasPresetPrevalenceAnalyses()) {
			final String featureColumns = "cohort_definition_id, covariate_id, sum_value, average_value";
			final String features = String.format(cohortWrapper, cohortId, featureColumns,
							StringUtils.stripEnd(jsonObject.getString("sqlQueryFeatures"), ";"));
			String[] paramValues = new String[]{ features, featureRefs, analysisRefs, String.valueOf(cohortId), String.valueOf(jobId), String.valueOf(strataId), strataName };
			queries.addAll(prepareStatements(prevalenceRetrievingQuery, sessionId, ArrayUtils.addAll(RETRIEVING_PARAMETERS, "strataId", "strataName"), paramValues));
		}

		return queries;
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
		// Target schema
		options.resultSchema = tempSchema;
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
						analysis.getName(), feature.getName(), String.valueOf(conceptId),
						String.valueOf(((WithId)feature).getId()), String.valueOf(strataId), strataName);
		String[] criteriaValues = new String[]{ groupQuery, targetTable, cohortTable };

		return Arrays.stream(SqlSplit.splitSql(queryFile))
						.map(COMPLETE_DOTCOMMA)
						.flatMap(sql -> prepareStatements(sql, sessionId, ArrayUtils.addAll(CRITERIA_REGEXES, paramNames),
										ArrayUtils.addAll(criteriaValues, paramValues.toArray(new String[0]))).stream())
						.collect(Collectors.toList());
	}

	private String getCriteriaGroupQuery(FeatureAnalysis analysis, BaseCriteriaFeature feature, String eventTable) {
		String groupQuery;
		if (CcResultType.PREVALENCE.equals(analysis.getStatType())) {
			groupQuery = queryBuilder.getCriteriaGroupQuery(((CriteriaFeature)feature).getExpression(), eventTable);
		} else if (CcResultType.DISTRIBUTION.equals(analysis.getStatType())) {
			if (feature instanceof WindowedCriteriaFeature) {
				WindowedCriteria criteria = ((WindowedCriteriaFeature) feature).getExpression();
				criteria.ignoreObservationPeriod = true;
				groupQuery = queryBuilder.getWindowedCriteriaQuery(criteria, eventTable);
			} else if (feature instanceof DemographicCriteriaFeature) {
				DemographicCriteria criteria = ((DemographicCriteriaFeature)feature).getExpression();
				groupQuery = queryBuilder.getDemographicCriteriaQuery(criteria, eventTable);
			} else {
				throw new IllegalArgumentException(String.format("Feature class %s is not supported", feature.getClass()));
			}
		} else {
			throw new IllegalArgumentException(String.format("Stat type %s is not supported", analysis.getStatType()));
		}
		return SqlRender.renderSql(groupQuery, ArrayUtils.addAll(DAIMONS, "indexId"),
						new String[]{ resultsSchema, cdmSchema, tempSchema, vocabularySchema, "0" });
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
						new String[]{ resultsSchema, cdmSchema, tempSchema, vocabularySchema, "0" });
		String[] paramNames = STRATA_PARAM_NAMES.toArray(new String[0]);
		String[] replacements = new String[]{ strataQuery, cohortTable, getStrataCohortTable(strata), eventsTable };
		String[] paramValues = new String[]{ String.valueOf(cohortDefinitionId), String.valueOf(strata.getId()) };
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

		return Arrays.stream(SqlSplit.splitSql(jsonObject.getString("sqlConstruction")))
						.map(COMPLETE_DOTCOMMA)
						.flatMap(sql -> prepareStatements(sql, sessionId).stream())
						.collect(Collectors.toList());
	}

	private List<String> getCleanupQueries(final JSONObject jsonObject) {

		return Arrays.stream(SqlSplit.splitSql(jsonObject.getString("sqlCleanup")))
						.map(COMPLETE_DOTCOMMA)
						.flatMap(sql -> prepareStatements(sql, sessionId).stream())
						.collect(Collectors.toList());
	}

	private List<String> getFeatureAnalysesQueries(final JSONObject jsonObject, final Integer cohortDefinitionId, final CohortCharacterizationStrata strata) {

		List<String> queriesToRun = new ArrayList<>();
		queriesToRun.addAll(getQueriesForPresetAnalyses(jsonObject,cohortDefinitionId, strata));
		queriesToRun.addAll(getQueriesForCustomDistributionAnalyses(cohortDefinitionId, strata));
		queriesToRun.addAll(getQueriesForCustomPrevalenceAnalyses(cohortDefinitionId, strata));
		queriesToRun.addAll(getQueriesForCriteriaAnalyses(cohortDefinitionId, strata));
		return queriesToRun;
	}

	private JSONObject createFeJsonObject(final CohortExpressionQueryBuilder.BuildExpressionQueryOptions options, final String cohortTable) {
		FeatureExtraction.init(null);
		String settings = buildSettings();
		String sqlJson = FeatureExtraction.createSql(settings, true, cohortTable,
						"subject_id", options.cohortId, options.cdmSchema);
		return new JSONObject(sqlJson);
	}

	private String buildSettings() {

		final JSONObject defaultSettings = new JSONObject(FeatureExtraction.getDefaultPrespecAnalyses());
		FeatureExtraction.getNameToPrespecAnalysis().keySet().forEach(defaultSettings::remove);

		cohortCharacterization.getParameters().forEach(param -> defaultSettings.put(param.getName(), param.getValue()));
		cohortCharacterization.getFeatureAnalyses()
						.stream()
						.filter(fa -> Objects.equals(fa.getType(), StandardFeatureAnalysisType.PRESET))
						.forEach(analysis -> defaultSettings.put(((FeatureAnalysis<String, Integer>)analysis).getDesign(), Boolean.TRUE));

		return defaultSettings.toString();
	}

}
