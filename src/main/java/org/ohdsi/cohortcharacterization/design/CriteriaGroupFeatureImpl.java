package org.ohdsi.cohortcharacterization.design;

import org.ohdsi.analysis.WithId;
import org.ohdsi.analysis.cohortcharacterization.design.CriteriaFeature;
import org.ohdsi.circe.cohortdefinition.CriteriaGroup;

public class CriteriaGroupFeatureImpl extends BaseCriteriaFeatureImpl<CriteriaGroup> implements CriteriaFeature, WithId<Long> {

	private CriteriaGroup expression;
	private Long id;

	@Override
	public CriteriaGroup getExpression() {

		return expression;
	}

	public void setExpression(CriteriaGroup expression) {

		this.expression = expression;
	}

	@Override
	public Long getId() {

		return id;
	}

	public void setId(Long id) {

		this.id = id;
	}
}
