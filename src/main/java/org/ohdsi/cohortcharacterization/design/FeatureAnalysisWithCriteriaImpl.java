package org.ohdsi.cohortcharacterization.design;

import java.util.List;
import org.ohdsi.analysis.cohortcharacterization.design.FeatureAnalysisAggregate;
import org.ohdsi.analysis.cohortcharacterization.design.FeatureAnalysisWithCriteria;
import org.ohdsi.circe.cohortdefinition.ConceptSet;

public abstract class FeatureAnalysisWithCriteriaImpl<T> extends FeatureAnalysisImpl<List<T>> implements FeatureAnalysisWithCriteria<T, Integer> {

	private List<T> design;
	private List<ConceptSet> conceptSets;
	private FeatureAnalysisAggregateImpl aggregate;

	@Override
	public List<T> getDesign() {

		return design;
	}

	public void setDesign(List<T> design) {

		this.design = design;
	}

	@Override
	public List<ConceptSet> getConceptSets() {

		return conceptSets;
	}

	public void setConceptSets(List<ConceptSet> conceptSets) {

		this.conceptSets = conceptSets;
	}

	@Override
	public FeatureAnalysisAggregate getAggregate() {

		return aggregate;
	}

	public void setAggregate(FeatureAnalysisAggregateImpl aggregate) {

		this.aggregate = aggregate;
	}
}
