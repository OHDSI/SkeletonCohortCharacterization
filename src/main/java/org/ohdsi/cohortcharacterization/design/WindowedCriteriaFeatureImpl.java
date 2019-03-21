package org.ohdsi.cohortcharacterization.design;

import org.ohdsi.analysis.cohortcharacterization.design.WindowedCriteriaFeature;
import org.ohdsi.circe.cohortdefinition.WindowedCriteria;

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
