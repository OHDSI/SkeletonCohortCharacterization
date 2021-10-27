package org.ohdsi.cohortcharacterization.design;

import org.ohdsi.analysis.cohortcharacterization.design.CohortCharacterizationStrata;
import org.ohdsi.circe.cohortdefinition.CriteriaGroup;

public class CcStrataImpl implements CohortCharacterizationStrata {

	private String name;
	private CriteriaGroup criteria = new CriteriaGroup();
	private Long id;

	@Override
	public String getName() {

		return name;
	}

	@Override
	public CriteriaGroup getCriteria() {
		return criteria;
	}

	@Override
	public Long getId() {

		return id;
	}

	public void setName(String name) {

		this.name = name;
	}

	public void setCriteria(CriteriaGroup criteria) {

		this.criteria = criteria;
	}

	public void setId(Long id) {

		this.id = id;
	}
}
