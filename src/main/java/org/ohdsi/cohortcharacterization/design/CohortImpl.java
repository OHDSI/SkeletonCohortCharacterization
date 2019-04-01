package org.ohdsi.cohortcharacterization.design;

import org.ohdsi.analysis.Cohort;
import org.ohdsi.circe.cohortdefinition.CohortExpression;

public class CohortImpl implements Cohort {

	private Integer id;
	private String name;
	private String description;
	private CohortExpression expression = new CohortExpression();

	@Override
	public CohortExpression getExpression() {

		return expression;
	}

	@Override
	public Integer getId() {

		return id;
	}

	@Override
	public String getName() {

		return name;
	}

	@Override
	public String getDescription() {

		return description;
	}

	public void setId(Integer id) {

		this.id = id;
	}

	public void setName(String name) {

		this.name = name;
	}

	public void setDescription(String description) {

		this.description = description;
	}

	public void setExpression(CohortExpression expression) {

		this.expression = expression;
	}
}
