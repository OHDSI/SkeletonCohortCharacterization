package org.ohdsi.cohortcharacterization.design;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import org.ohdsi.analysis.cohortcharacterization.design.DemographicCriteriaFeature;
import org.ohdsi.cohortcharacterization.utils.FeatureAnalysisDeserializer;

@JsonDeserialize(using = FeatureAnalysisDeserializer.None.class)
public class FeatureAnalysisWithDemographicImpl extends FeatureAnalysisWithCriteriaImpl<DemographicCriteriaFeature> {

	@JsonDeserialize(as = DemographicCriteriaFeatureImpl.class)
	private List<DemographicCriteriaFeature> design;

	@Override
	public List<DemographicCriteriaFeature> getDesign() {

		return design;
	}

	public void setDesign(List<DemographicCriteriaFeature> design) {

		this.design = design;
	}
}
