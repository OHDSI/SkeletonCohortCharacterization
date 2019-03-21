package org.ohdsi.cohortcharacterization.design;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import org.ohdsi.analysis.cohortcharacterization.design.WindowedCriteriaFeature;
import org.ohdsi.cohortcharacterization.utils.FeatureAnalysisDeserializer;

@JsonDeserialize(using = FeatureAnalysisDeserializer.None.class)
public class FeatureAnalysisWithWindowedCriteriaImpl extends FeatureAnalysisWithCriteriaImpl<WindowedCriteriaFeature> {

	@JsonDeserialize(as = WindowedCriteriaFeatureImpl.class)
	private List<WindowedCriteriaFeature> design;

	@Override
	public List<WindowedCriteriaFeature> getDesign() {

		return design;
	}
}
