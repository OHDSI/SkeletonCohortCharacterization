package org.ohdsi.cohortcharacterization.design;

import org.apache.commons.lang3.StringUtils;
import org.ohdsi.analysis.TableJoin;
import org.ohdsi.analysis.cohortcharacterization.design.AggregateFunction;
import org.ohdsi.analysis.cohortcharacterization.design.FeatureAnalysisAggregate;
import org.ohdsi.analysis.cohortcharacterization.design.FeatureAnalysisDomain;
import org.ohdsi.circe.cohortdefinition.builders.CriteriaColumn;

import java.util.List;

public class FeatureAnalysisAggregateImpl implements FeatureAnalysisAggregate {

    private String name;
    private FeatureAnalysisDomain domain;
    private AggregateFunction function;
    private List<CriteriaColumn> additionalColumns;
    private String expression;
    private Integer id;
    private String joinTable;
    private String joinCondition;
    private TableJoin joinType;

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
    public List<CriteriaColumn> getAdditionalColumns() {

        return additionalColumns;
    }

    @Override
    public String getExpression() {

        return expression;
    }

    @Override
    public boolean hasQuery() {

        return StringUtils.isNotBlank(joinTable);
    }

    @Override
    public String getJoinTable() {
        return joinTable;
    }

    @Override
    public String getJoinCondition() {
        return joinCondition;
    }

    @Override
    public TableJoin getJoinType() {
        return joinType;
    }

    @Override
    public Integer getId() {
        return id;
    }
}
