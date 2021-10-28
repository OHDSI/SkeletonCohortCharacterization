package org.ohdsi.cohortcharacterization.design;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.ohdsi.analysis.Cohort;
import org.ohdsi.analysis.cohortcharacterization.design.CohortCharacterization;
import org.ohdsi.analysis.cohortcharacterization.design.CohortCharacterizationParam;
import org.ohdsi.analysis.cohortcharacterization.design.CohortCharacterizationStrata;
import org.ohdsi.analysis.cohortcharacterization.design.FeatureAnalysis;
import org.ohdsi.circe.cohortdefinition.ConceptSet;

public class CohortCharacterizationImpl implements CohortCharacterization {

	private Long id;
	private String name;
	private List<CohortImpl> cohorts = new ArrayList<>();
	private List<FeatureAnalysisImpl> featureAnalyses = new ArrayList<>();
	private List<CcParamImpl> parameters = new ArrayList<>();
	private List<CcStrataImpl> stratas = new ArrayList<>();
	private Boolean strataOnly;
	private List<ConceptSet> conceptSets = new ArrayList<>();

	@Override
	public Long getId() {

		return id;
	}

	@Override
	public String getName() {

		return name;
	}

	@Override
	public Collection<? extends Cohort> getCohorts() {

		return cohorts;
	}

	@Override
	public Collection<? extends FeatureAnalysis> getFeatureAnalyses() {

		return featureAnalyses;
	}

	@Override
	public Collection<? extends CohortCharacterizationParam> getParameters() {

		return parameters;
	}

	@Override
	public Collection<? extends CohortCharacterizationStrata> getStratas() {

		return stratas;
	}

	@Override
	public Boolean getStrataOnly() {

		return strataOnly;
	}

	@Override
	public Collection<ConceptSet> getStrataConceptSets() {

		return conceptSets;
	}

	public void setName(String name) {

		this.name = name;
	}

	public void setCohorts(List<CohortImpl> cohorts) {

		this.cohorts = cohorts;
	}

	public void setFeatureAnalyses(List<FeatureAnalysisImpl> featureAnalyses) {

		this.featureAnalyses = featureAnalyses;
	}

	public void setParameters(List<CcParamImpl> parameters) {

		this.parameters = parameters;
	}

	public void setStratas(List<CcStrataImpl> stratas) {

		this.stratas = stratas;
	}

	public void setStrataOnly(Boolean strataOnly) {

		this.strataOnly = strataOnly;
	}

	public void setConceptSets(List<ConceptSet> conceptSets) {

		this.conceptSets = conceptSets;
	}
}
