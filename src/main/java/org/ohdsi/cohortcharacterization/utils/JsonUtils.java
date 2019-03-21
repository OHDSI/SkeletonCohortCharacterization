package org.ohdsi.cohortcharacterization.utils;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;

public class JsonUtils {

	static public String getValueAsString(JsonNode node, String field) {
		JsonNode fieldNode = node.get(field);
		return Objects.nonNull(fieldNode) ? fieldNode.asText() : null;
	}

}
