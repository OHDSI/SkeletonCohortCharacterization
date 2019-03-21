package org.ohdsi.cohortcharacterization.utils;

import static org.ohdsi.analysis.cohortcharacterization.design.StandardFeatureAnalysisType.CRITERIA_SET;
import static org.ohdsi.analysis.cohortcharacterization.design.StandardFeatureAnalysisType.CUSTOM_FE;
import static org.ohdsi.analysis.cohortcharacterization.design.StandardFeatureAnalysisType.PRESET;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.util.Objects;
import org.ohdsi.analysis.cohortcharacterization.design.CcResultType;
import org.ohdsi.cohortcharacterization.design.FeatureAnalysisImpl;
import org.ohdsi.cohortcharacterization.design.FeatureAnalysisWithDistributionImpl;
import org.ohdsi.cohortcharacterization.design.FeatureAnalysisWithPrevalenceImpl;
import org.ohdsi.cohortcharacterization.design.FeatureAnalysisWithStringImpl;

public class FeatureAnalysisDeserializer extends StdDeserializer<FeatureAnalysisImpl> {

	public FeatureAnalysisDeserializer() {

		super(FeatureAnalysisImpl.class);
	}

	@Override
	public FeatureAnalysisImpl deserialize(JsonParser parser, DeserializationContext dc) throws IOException, JsonProcessingException {

		JsonNode node = parser.readValueAsTree();

		Class<? extends FeatureAnalysisImpl> resultType = null;
		final String type = JsonUtils.getValueAsString(node, "type");
		if (Objects.equals(type, PRESET.name()) || Objects.equals(type, CUSTOM_FE.name())) {
			resultType = FeatureAnalysisWithStringImpl.class;
		} else if (Objects.equals(type, CRITERIA_SET.name())) {
			final String statType = JsonUtils.getValueAsString(node, "statType");
			if (Objects.equals(statType, CcResultType.PREVALENCE.name())) {
				resultType = FeatureAnalysisWithPrevalenceImpl.class;
			} else if (Objects.equals(statType, CcResultType.DISTRIBUTION.name())) {
				resultType = FeatureAnalysisWithDistributionImpl.class;
			}
		}

		return Objects.nonNull(resultType) ? parser.getCodec().treeToValue(node, resultType) : null;
	}
}
