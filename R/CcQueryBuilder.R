# Copyright 2019 Observational Health Data Sciences and Informatics
#
# This file is part of SkeletonComparativeEffectStudy
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
#

#' Builds SQL code to run analyses according given Cohort Characterization design
#'
#' @param cohortCharacterization  A string object containing valid JSON that represents CC design
#' @param cohortTable The name of table with cohorts
#' @param sessionId session identifier using to build temporary tables
#' @param cdmSchema the name of schema containing data in CDM format
#' @param resultsSchema the name of schema where results would be placed
#' @param vocabularySchema the name of schema with vocabularies
#' @param tempSchema the name of database temp schema
#' @param analysisId analysis identifier
#' @return SQL code in MS Sql Server dialect, if it's required to run analysis on another DBMS
#'         you have to use \code{\link[SqlRender]{translateSql}} function in the SqlRender package.
#' 
#' @export
buildQuery <- function(cohortCharacterization,
                        cohortTable = "cohort",
                        sessionId,
                        cdmSchema,
                        resultsSchema,
                        vocabularySchema = cdmSchema,
                        tempSchema = resultsSchema,
                        analysisId
) {

  id <- .jlong(analysisId)
  queryBuilder <- .jnew("org.ohdsi.cohortcharacterization.CCQueryBuilder", cohortCharacterization, cohortTable, sessionId, cdmSchema, resultsSchema, vocabularySchema, tempSchema, id)
  sql <- .jcall(queryBuilder, returnSig = "S", "build")
  return(sql)
}

