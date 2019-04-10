package org.ohdsi.cohortcharacterization.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.util.Objects;
import org.ohdsi.cohortcharacterization.design.CriteriaGroupFeatureImpl;
import org.ohdsi.cohortcharacterization.design.DemographicCriteriaFeatureImpl;
import org.ohdsi.cohortcharacterization.design.DistributionFeatureImpl;
import org.ohdsi.cohortcharacterization.design.FeatureAnalysisWithStringImpl;
import org.ohdsi.cohortcharacterization.design.WindowedCriteriaFeatureImpl;

public class DistributionFeatureDeserializer extends StdDeserializer<DistributionFeatureImpl> {

	protected DistributionFeatureDeserializer() {

		super(DistributionFeatureImpl.class);
	}

	@Override
	public DistributionFeatureImpl deserialize(JsonParser parser, DeserializationContext dc) throws IOException, JsonProcessingException {

		JsonNode node = parser.readValueAsTree();

		String type = JsonUtils.getValueAsString(node, "criteriaType");
		Class<? extends DistributionFeatureImpl> resultType = null;
		if (Objects.equals(type, "WindowedCriteria")) {
			resultType = WindowedCriteriaFeatureImpl.class;
		} else if (Objects.equals(type, "DemographicCriteria")) {
			resultType = DemographicCriteriaFeatureImpl.class;
		}

		return Objects.nonNull(resultType) ? parser.getCodec().treeToValue(node, resultType) : null;
	}
}
