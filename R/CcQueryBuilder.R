# @file CcQueryBuilder.R
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
#
#' Run CcQueryBuilder package
#'
#' @details
#' Run
#' 

#' @param cohortCharacterization  An object that represents CC design
#' @param cohortTable
#' @param sessionId
#' @param cdmSchema
#' @param resultsSchema
#' @param vocabularySchema
#' @param tempSchema
#' @param analysisId
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

