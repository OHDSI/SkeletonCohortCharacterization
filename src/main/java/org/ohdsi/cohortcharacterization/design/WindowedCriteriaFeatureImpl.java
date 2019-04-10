package org.ohdsi.cohortcharacterization.design;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.ohdsi.analysis.cohortcharacterization.design.WindowedCriteriaFeature;
import org.ohdsi.circe.cohortdefinition.WindowedCriteria;
import org.ohdsi.cohortcharacterization.utils.FeatureAnalysisDeserializer;

@JsonDeserialize(using = FeatureAnalysisDeserializer.None.class)
public class WindowedCriteriaFeatureImpl extends DistributionFeatureImpl<WindowedCriteria> implements WindowedCriteriaFeature {

	private WindowedCriteria expression;

	@Override
	public WindowedCriteria getExpression() {

		return expression;
	}

	public void setExpression(WindowedCriteria expression) {

		this.expression = expression;
	}
}
