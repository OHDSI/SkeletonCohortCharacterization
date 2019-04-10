package org.ohdsi.cohortcharacterization.design;

import org.ohdsi.analysis.WithId;
import org.ohdsi.analysis.cohortcharacterization.design.BaseCriteriaFeature;

public abstract class BaseCriteriaFeatureImpl<T> implements BaseCriteriaFeature<T>, WithId<Long> {

	private String name;
	private Long id;

	@Override
	public String getName() {

		return name;
	}

	public void setName(String name) {

		this.name = name;
	}

	@Override
	public Long getId() {

		return id;
	}

	public void setId(Long id) {

		this.id = id;
	}
}
