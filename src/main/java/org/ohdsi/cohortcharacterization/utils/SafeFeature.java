package org.ohdsi.cohortcharacterization.utils;

import org.ohdsi.analysis.cohortcharacterization.design.BaseCriteriaFeature;
import org.ohdsi.analysis.cohortcharacterization.design.FeatureAnalysisAggregate;

import java.util.Optional;
import java.util.function.Function;

public class SafeFeature {

    public static <T> T get(BaseCriteriaFeature feature, Function<? super FeatureAnalysisAggregate, T> mapFunc, T defaultValue) {

        return Optional.ofNullable(feature.getAggregate())
                .map(mapFunc::apply)
                .orElse(defaultValue);
    }

    public static Integer getAsInteger(BaseCriteriaFeature feature, Function<? super FeatureAnalysisAggregate, Integer> mapFunc) {

        return get(feature, mapFunc, 0);
    }

    public static String getAsString(BaseCriteriaFeature feature, Function<? super FeatureAnalysisAggregate, String> mapFunc) {

        return get(feature, mapFunc, "");
    }
}
