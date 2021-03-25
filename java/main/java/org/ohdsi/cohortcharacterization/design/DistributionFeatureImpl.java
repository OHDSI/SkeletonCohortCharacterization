package org.ohdsi.cohortcharacterization.design;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.ohdsi.cohortcharacterization.utils.DistributionFeatureDeserializer;

@JsonDeserialize(using = DistributionFeatureDeserializer.class)
public abstract class DistributionFeatureImpl<T> extends BaseCriteriaFeatureImpl<T> {
}
