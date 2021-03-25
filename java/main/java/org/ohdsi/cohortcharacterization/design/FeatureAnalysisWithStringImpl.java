package org.ohdsi.cohortcharacterization.design;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.ohdsi.cohortcharacterization.utils.FeatureAnalysisDeserializer;

@JsonDeserialize(using = FeatureAnalysisDeserializer.None.class)
public class FeatureAnalysisWithStringImpl extends FeatureAnalysisImpl<String> {

	private String design;

	@Override
	public String getDesign() {

		return design;
	}

}
