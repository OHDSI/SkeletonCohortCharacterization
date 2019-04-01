package org.ohdsi.cohortcharacterization.design;

import org.ohdsi.analysis.cohortcharacterization.design.DemographicCriteriaFeature;
import org.ohdsi.circe.cohortdefinition.DemographicCriteria;

public class DemographicCriteriaFeatureImpl extends DistributionFeatureImpl<DemographicCriteria> implements DemographicCriteriaFeature {

	private DemographicCriteria expression;

	@Override
	public DemographicCriteria getExpression() {

		return expression;
	}

	public void setExpression(DemographicCriteria expression) {

		this.expression = expression;
	}
}
