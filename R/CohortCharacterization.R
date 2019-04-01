#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

#' Builds query, run analyses and save results to the result folder.
#' 
#' @param connectionDetails An object of type \code{connectionDetails} as created using the
#'                             \code{\link[DatabaseConnector]{createConnectionDetails}} function in the
#'                             DatabaseConnector package.
#' @param cohortTable The name of table with cohorts
#' @param sessionId session identifier using to build temporary tables
#' @param cdmSchema the name of schema containing data in CDM format
#' @param resultsSchema the name of schema where results would be placed
#' @param vocabularySchema the name of schema with vocabularies
#' @param tempSchema the name of database temp schema
#' @param analysisId analysis identifier
#' 
#' @export
runAnalysis <- function(connectionDetails,
                  cohortTable = "cohort",
                  sessionId,
                  cdmSchema,
                  resultsSchema,
                  vocabularySchema,
                  tempSchema = resultsSchema,
                  analysisId,
                  outputFolder = "SkeletonCohortCharacterization"
) {
  if (!file.exists(outputFolder))
    dir.create(outputFolder, recursive = TRUE)
  ParallelLogger::addDefaultFileLogger(file.path(outputFolder, "log.txt"))

  filename <- system.file("settings", "StudySpecification.json", package = "SkeletonCohortCharacterization")
  cohortCharacterization <- read_file(filename)

  ParallelLogger::logInfo("Building Cohort Characterization queries to run")
  sql <- buildQuery(cohortCharacterization, cohortTable, sessionId, cdmSchema, resultsSchema, vocabularySchema, tempSchema, analysisId)
  dbms <- connectionDetails$dbms
  ParallelLogger::logInfo(paste("Translate SQL for", dbms))
  translatedSql <- SqlRender::translate(sql, dbms, tempSchema)

  writeLines(sql, paste0("/tmp/sql-cc-", dbms, "-", analysisId, ".sql"))

  ParallelLogger::logInfo("Running analysis")
  con <- DatabaseConnector::connect(connectionDetails)
  DatabaseConnector::batchUpdate(con, translatedSql)
  DatabaseConnector::disconnect(con)

  ParallelLogger::logInfo("Collecting results")
  saveResults(connectionDetails, cohortCharacterization, analysisId, resultsSchema, paste(outputFolder, "/results"))

  invisible(NULL)
}