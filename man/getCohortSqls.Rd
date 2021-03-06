% Generated by roxygen2: do not edit by hand
% Please edit documentation in R/CreateCohorts.R
\name{getCohortSqls}
\alias{getCohortSqls}
\title{Builds SQLs to construct cohorts for analysis}
\usage{
getCohortSqls(studySpec, cdmSchema, vocabularySchema, resultSchema,
  targetTable)
}
\arguments{
\item{studySpec}{Path to CC specification json}

\item{cdmSchema}{The name of schema containing data in CDM format}

\item{vocabularySchema}{The name of schema with vocabularies}

\item{resultsSchema}{The name of schema where cohorts will be placed}

\item{cohortTable}{The name of table where cohorts will be placed}
}
\description{
Builds SQLs to construct cohorts for analysis
}
