package org.ohdsi.cohortcharacterization.design;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.ohdsi.cohortcharacterization.utils.FeatureAnalysisDeserializer;

@JsonDeserialize(using = FeatureAnalysisDeserializer.None.class)
public class FeatureAnalysisWithDistributionImpl extends FeatureAnalysisWithCriteriaImpl<DistributionFeatureImpl> {
}
