#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

#' Builds SQL to create cohort table
#' 
#' @param sqlPath Path to SQL template
#' @param resultsSchema The name of schema where cohort table will be placed
#' @param targetTable The name of cohort table
#' 
#' 
getCreateCohortTableSql <- function(sqlPath, resultSchema, targetTable) {
  createCohortTableSql <- SqlRender::readSql(sqlPath)
  createCohortTableSql <- SqlRender::render(createCohortTableSql, cohort_database_schema = resultSchema, cohort_table = targetTable)
  return(createCohortTableSql)
}

#' Builds SQLs to construct cohorts for analysis
#' 
#' @param studySpec Path to CC specification json
#' @param cdmSchema The name of schema containing data in CDM format
#' @param vocabularySchema The name of schema with vocabularies
#' @param resultsSchema The name of schema where cohorts will be placed
#' @param cohortTable The name of table where cohorts will be placed
#' 
getCohortSqls <- function(studySpec, cdmSchema, vocabularySchema, resultSchema, targetTable) {
  sqls <- c()
  
  cohortExpression <- new(J("org.ohdsi.circe.cohortdefinition.CohortExpression"))
  queryBuilder <- new(J("org.ohdsi.circe.cohortdefinition.CohortExpressionQueryBuilder"))
  
  cohorts <- fromJSON(studySpec)$cohorts
  
  dbOptions <- list(
    cdmSchema = cdmSchema,
    targetTable = paste(resultSchema, ".", targetTable, sep = ""),
    resultSchema = resultSchema,
    vocabularySchema = vocabularySchema,
    generateStats = FALSE
  )
  
  for (c in cohorts) {
    options <- list(dbOptions)
    
    options[[1]]$cohortId <- c$id
    
    optionsJSON <- toJSON(options, container = FALSE)[[1]]
    queryExpressionOptions <- queryBuilder$BuildExpressionQueryOptions$fromJson(optionsJSON)
    
    expressionJSON <- toJSON(c$expression)[[1]]
    expression <- cohortExpression$fromJson(expressionJSON)
    
    sqls <- c(sqls, queryBuilder$buildExpressionQuery(expression, queryExpressionOptions))
  }
  
  return(sqls)
}

#' Executes given SQLs to construct cohorts
#' 
#' @param cohortTableSql SQL to create cohort table
#' @param cohortSqls Cohort SQLs
#' @param connectionDetails An object of type \code{connectionDetails} as created using the
#'                             \code{\link[DatabaseConnector]{createConnectionDetails}} function in the
#'                             DatabaseConnector package.
#' 
constructCohorts <- function(cohortTableSql, cohortSqls, connectionDetails) {
  
  runSql <- function(sql) {
    con <- DatabaseConnector::connect(connectionDetails)
    DatabaseConnector::executeSql(con, sql, runAsBatch = TRUE)
    DatabaseConnector::disconnect(con)
  }
  
  cohortTableSql <- SqlRender::translate(cohortTableSql, connectionDetails$dbms)
  runSql(cohortTableSql)
  
  for (sql in cohortSqls) {
    renderedSql <- SqlRender::render(sql)
    translatedSql <- SqlRender::translate(renderedSql, connectionDetails$dbms)
    
    # TODO:
    # writeLines(sql, paste0("/tmp/sql-cohort-", dbms, "-", analysisId, ".sql"))
    
    ParallelLogger::logInfo("Building cohort")
    runSql(translatedSql)
  }
}

#' Constructs cohorts used in CC analysis
#' 
#' @param cohortTableSql SQL to create cohort table
#' @param cohortSqls Cohort SQLs
#' @param connectionDetails An object of type \code{connectionDetails} as created using the
#'                             \code{\link[DatabaseConnector]{createConnectionDetails}} function in the
#'                             DatabaseConnector package.
#' @export
createCohorts <- function(pckg, connectionDetails, cdmSchema, vocabularySchema, resultSchema) {
  tmpPostfix <- sample(1:10^8, 1)
  cohortTable <- paste("cohort_", tmpPostfix, sep = "")

  cohortTableSql <- getCreateCohortTableSql(
    sqlPath = system.file("sql", "sql_server", "CreateCohortTable.sql", package = pckg),
    resultSchema = resultSchema,
    targetTable = cohortTable
  )

  cohortSqls <- getCohortSqls(
    studySpec = system.file("settings", "StudySpecification.json", package = pckg),
    cdmSchema = cdmSchema,
    vocabularySchema = vocabularySchema,
    resultSchema = resultSchema,
    targetTable = cohortTable
  )

  constructCohorts(cohortTableSql, cohortSqls, connectionDetails)

  return(cohortTable)
}

#' Drops cohort table
#' 
#' @param connectionDetails An object of type \code{connectionDetails} as created using the
#'                             \code{\link[DatabaseConnector]{createConnectionDetails}} function in the
#'                             DatabaseConnector package.
#' @param resultsSchema The name of schema where cohort table will be placed
#' @param targetTable The name of cohort table
#' @export
cleanupCohortTable <- function(connectionDetails, resultSchema, cohortTable) {
  sql <- "IF OBJECT_ID('@cohort_database_schema.@cohort_table', 'U') IS NOT NULL DROP TABLE @cohort_database_schema.@cohort_table;"
  sql <- SqlRender::render(sql, cohort_database_schema = resultSchema, cohort_table = cohortTable)
  sql <- SqlRender::translate(sql, connectionDetails$dbms)
  con <- DatabaseConnector::connect(connectionDetails)
  DatabaseConnector::executeSql(con, sql, runAsBatch = TRUE)
  DatabaseConnector::disconnect(con)
}