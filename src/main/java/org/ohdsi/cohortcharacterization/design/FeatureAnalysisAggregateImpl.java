package org.ohdsi.cohortcharacterization.design;

import org.apache.commons.lang3.StringUtils;
import org.ohdsi.analysis.cohortcharacterization.design.AggregateFunction;
import org.ohdsi.analysis.cohortcharacterization.design.FeatureAnalysisAggregate;
import org.ohdsi.analysis.cohortcharacterization.design.FeatureAnalysisDomain;

public class FeatureAnalysisAggregateImpl implements FeatureAnalysisAggregate {

    private String name;
    private FeatureAnalysisDomain domain;
    private AggregateFunction function;
    private String expression;
    private String query;

    @Override
    public String getName() {

        return name;
    }

    @Override
    public FeatureAnalysisDomain getDomain() {

        return domain;
    }

    @Override
    public AggregateFunction getFunction() {

        return function;
    }

    @Override
    public String getExpression() {

        return expression;
    }

    @Override
    public boolean hasQuery() {

        return StringUtils.isNotBlank(query);
    }

    @Override
    public String getQuery() {

        return query;
    }
}
