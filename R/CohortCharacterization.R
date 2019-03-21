#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

#' Run CohortCharacterization method
#' 
#' @details 
#' 
#' @param connectionDetails
#' @param cohortCharacterization
#' @param cohortTable
#' @param sessionId
#' @param cdmSchema
#' @param resultsSchema
#' @param vocabularySchema
#' @param tempSchema
#' @param analysisId
#' 
#' @export
runAnalysis <- function(connectionDetails,
                  cohortCharacterization,
                  cohortTable = "cohort",
                  sessionId,
                  cdmSchema,
                  resultsSchema,
                  vocabularySchema,
                  tempSchema = resultsSchema,
                  analysisId,
                  outputFolder = "SkeletonCohortCharacterizationStudy"
) {
  if (!file.exists(outputFolder))
    dir.create(outputFolder, recursive = TRUE)
  ParallelLogger::addDefaultFileLogger(file.path(outputFolder, "log.txt"))

  ParallelLogger::logInfo("Building Cohort Characterization queries to run")
  sql <- buildQuery(cohortCharacterization, cohortTable, sessionId, cdmSchema, resultsSchema, vocabularySchema, tempSchema, analysisId)
  dbms <- connectionDetails$dbms
  translatedSql <- SqlRender::translate(sql, dbms, tempSchema)

  ParallelLogger::logInfo("Running analysis")
  con <- DatabaseConnector::connect(connectionDetails)
  DatabaseConnector::executeSql(con, translatedSql)
  DatabaseConnector::disconnect(con)

  ParallelLogger::logInfo("Collecting results")
  saveResults(connectionDetails, cohortCharacterization, analysisId, resultsSchema, paste(outputFolder, "/results"))

  invisible(NULL)
}