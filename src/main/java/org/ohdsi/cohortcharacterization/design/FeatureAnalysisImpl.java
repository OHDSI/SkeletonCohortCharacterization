package org.ohdsi.cohortcharacterization.design;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.ohdsi.analysis.cohortcharacterization.design.CcResultType;
import org.ohdsi.analysis.cohortcharacterization.design.FeatureAnalysis;
import org.ohdsi.analysis.cohortcharacterization.design.FeatureAnalysisDomain;
import org.ohdsi.analysis.cohortcharacterization.design.FeatureAnalysisType;
import org.ohdsi.analysis.cohortcharacterization.design.StandardFeatureAnalysisDomain;
import org.ohdsi.analysis.cohortcharacterization.design.StandardFeatureAnalysisType;
import org.ohdsi.cohortcharacterization.utils.FeatureAnalysisDeserializer;

@JsonDeserialize(using = FeatureAnalysisDeserializer.class)
public abstract class FeatureAnalysisImpl<T> implements FeatureAnalysis<T, Integer> {

	private Integer id;
	@JsonDeserialize(as = StandardFeatureAnalysisType.class)
	private FeatureAnalysisType type;
	private String name;
	@JsonDeserialize(as = StandardFeatureAnalysisDomain.class)
	private FeatureAnalysisDomain domain;
	private String descr;
	private CcResultType statType;

	@Override
	public FeatureAnalysisType getType() {

		return type;
	}

	@Override
	public String getName() {

		return name;
	}

	@Override
	public FeatureAnalysisDomain getDomain() {

		return domain;
	}

	@Override
	public String getDescr() {

		return descr;
	}

	@Override
	public CcResultType getStatType() {

		return statType;
	}

	@Override
	public Integer getId() {

		return id;
	}

	public void setId(Integer id) {

		this.id = id;
	}
}
