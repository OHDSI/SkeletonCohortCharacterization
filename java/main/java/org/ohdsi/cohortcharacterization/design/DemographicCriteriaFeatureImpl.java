package org.ohdsi.cohortcharacterization.design;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.ohdsi.analysis.cohortcharacterization.design.DemographicCriteriaFeature;
import org.ohdsi.circe.cohortdefinition.DemographicCriteria;
import org.ohdsi.cohortcharacterization.utils.FeatureAnalysisDeserializer;

@JsonDeserialize(using = FeatureAnalysisDeserializer.None.class)
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
