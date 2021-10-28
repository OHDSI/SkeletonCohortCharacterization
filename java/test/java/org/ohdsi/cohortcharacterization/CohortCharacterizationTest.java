package org.ohdsi.cohortcharacterization;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.text.IsEmptyString.emptyString;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.ohdsi.analysis.Utils;
import org.ohdsi.analysis.cohortcharacterization.design.CohortCharacterization;
import org.ohdsi.circe.helper.ResourceHelper;
import org.ohdsi.cohortcharacterization.design.CohortCharacterizationImpl;

public class CohortCharacterizationTest {

	private static final String CC = ResourceHelper.GetResourceAsString("/cohortcharacterization/CohortCharacterization.json");

	@Test
	public void testCohortCharacterization() {

		CohortCharacterization cc = Utils.deserialize(CC, CohortCharacterizationImpl.class);
		assertThat(cc, is(notNullValue()));
	}

	@Test
	public void testBuildQuery() {

		CCQueryBuilder builder = new CCQueryBuilder(CC, "cohort", "123456", "public", "results", "public", "results", 148);
		String sql = builder.build();
		assertThat(sql, is(notNullValue()));
		assertThat(sql, not(emptyString()));
	}
	
	@Test
	public void mixedAnalysisTypeTest() {

		CCQueryBuilder builder = new CCQueryBuilder(ResourceHelper.GetResourceAsString("/cohortcharacterization/mixedAnalysisType.json"), "cohort", "123456", "public", "results", "public", "results", 148);
		String sql = builder.build();
		assertThat(sql, is(notNullValue()));
		assertThat(sql, not(emptyString()));
	}	
}
