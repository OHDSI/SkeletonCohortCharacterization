package org.ohdsi.cohortcharacterization.design;

import org.ohdsi.analysis.cohortcharacterization.design.BaseCriteriaFeature;

public abstract class BaseCriteriaFeatureImpl<T> implements BaseCriteriaFeature<T> {

	private String name;

	@Override
	public String getName() {

		return name;
	}

	public void setName(String name) {

		this.name = name;
	}
}
