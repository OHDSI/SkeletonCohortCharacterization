package org.ohdsi.cohortcharacterization.design;

import org.ohdsi.analysis.cohortcharacterization.design.CohortCharacterizationParam;

public class CcParamImpl implements CohortCharacterizationParam {

	private String name;
	private Object value;

	@Override
	public String getName() {

		return name;
	}

	@Override
	public Object getValue() {

		return value;
	}

	public void setName(String name) {

		this.name = name;
	}

	public void setValue(Object value) {

		this.value = value;
	}
}
