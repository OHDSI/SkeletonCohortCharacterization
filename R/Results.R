# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

findAnalyses <- function(results) {
  analyses <- unique(results[c('ANALYSIS_ID', 'ANALYSIS_NAME', 'TYPE')])

  return(analyses)
}

findStratas <- function(results) {
  stratas <- unique(results[c('STRATA_ID', 'STRATA_NAME')])
  return(stratas)
}

getCohort <- function(id, cc) {
  cohorts <- cc$cohorts
  r <- NULL
  for(i in 1:length(cohorts)) {
    cohort = cohorts[[i]]
    if (cohort$id == id) {
      r = cohort
      break
    }
  }
  return(r)
}

findCohorts <- function(cc, results) {
  cohortIds <- unique(results[c('COHORT_DEFINITION_ID')])
  cohortNames <- ""[-1]

  for(i in 1:nrow(cohortIds)) {
    cohort <- getCohort(cohortIds[i,c('COHORT_DEFINITION_ID')], cc)
    if (!is.null(cohort)) {
      name = cohort$name
      cohortNames <- c(cohortNames, name)
    }
  }
  cohorts <- data.frame(id = cohortIds, name = cohortNames)
  return(cohorts)
}

getColumnNames <- function(type, results) {

  blacklist <- "cc_generation_id"
  distCols <- c('MIN_VALUE', 'P10_VALUE', 'P25_VALUE', 'MEDIAN_VALUE', 'P75_VALUE', 'P90_VALUE', 'MAX_VALUE', 'STDEV_VALUE')

  colNames <- colnames(results)
  colNames <- colNames[! colNames %in% blacklist]

  if (type != 'DISTRIBUTION') {
    colNames <- colNames[! colNames %in% distCols]
  }
  return(colNames)
}

trim <- function (x) gsub("^\\s+|\\s+$", "", x)

buildReports <- function(analysis, cohorts, stratas, results, outputFolder) {
  colNames <- getColumnNames(analysis[c('TYPE')], results)

  for(i in 1:length(cohorts)) {
    cohort <- cohorts[i,]
    analysisId <- trim(analysis[c('ANALYSIS_ID')])
    analysisName <- analysis[c('ANALYSIS_NAME')]
    cohortId <- trim(cohort$COHORT_DEFINITION_ID)
    ParallelLogger::logInfo(paste('Building report for analysis "', analysisName, ' ', analysisId, '" at cohort "', cohort$name, '"', sep = ''))
    reportData <- results[which(results$ANALYSIS_ID == analysisId & results$COHORT_DEFINITION_ID == cohortId), colNames]

    if (nrow(reportData) > 0) {
      reportData[, 'COHORT_NAME'] <- cohort$name
    }

    fileName <- paste(analysisName, '_', cohort$name, '.csv', sep = '')
    rows <- sum(complete.cases(reportData))
    ParallelLogger::logInfo(paste('Found ',rows,' rows', sep = ''))
    write.csv(reportData, file.path(outputFolder, fileName), row.names = TRUE)
  }
}

#' Save results of analyses from database to CSV files.
#'
#' @param connectionDetails An object of type \code{connectionDetails} as created using the
#'                             \code{\link[DatabaseConnector]{createConnectionDetails}} function in the
#'                             DatabaseConnector package.
#' @param cohortCharacterization A string object containing valid JSON that represents CC design
#' @param analysisId analysis identifier
#' @param resultsSchema the name of schema where results would be placed
#' @param outputFolder folder name where results would be saved
#' @param tresholdLevel treshold level is used for prevalence analyses to reduce meaningless records having percentage lower than 
#'                      treshold level. Default value is 0.01
#'
#' @export
saveResults <- function(connectionDetails, cohortCharacterization, analysisId, resultsSchema, outputFolder, tresholdLevel = 0.01) {

  library(jsonlite)
  if (!file.exists(outputFolder))
    dir.create(outputFolder, recursive = TRUE)

  con <- DatabaseConnector::connect(connectionDetails)

  ParallelLogger::logInfo("Gathering results from source")
  sql <- SqlRender::loadRenderTranslateSql(sqlFilename = "queryResults.sql",
    packageName = "SkeletonCohortCharacterization",
    dbms = attr(con, "dbms"),
    results_database_schema = resultsSchema,
    cohort_characterization_generation_id = analysisId,
    threshold_level = tresholdLevel)

  results <- DatabaseConnector::querySql(con, sql)

  if (nrow(results) > 0) {
    results[which(results$STRATA_ID == 0), 'STRATA_NAME'] <- 'All stratas'
  }

  fileName <- file.path(outputFolder, "raw_data.csv")
  ParallelLogger::logInfo(paste("Raw data is available at ", fileName))
  write.csv(results, fileName, row.names = TRUE)

  cc <- fromJSON(cohortCharacterization)

  analyses <- findAnalyses(results)
  cohorts <- findCohorts(cc, results)
  stratas <- findStratas(results)

  apply(analyses, 1, buildReports, cohorts, stratas, results, outputFolder)

  # for(analysis in analyses) {
  #   print(analysis)
  #   buildReports(analysis, cohorts, stratas, results, outputFolder)
  # }

  DatabaseConnector::disconnect(con)
}