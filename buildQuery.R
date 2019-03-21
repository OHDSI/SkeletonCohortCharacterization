library(readr)
library(SkeletonCohortCharacterization)
library(DatabaseConnector)

design <- read_file('cc.json')

connectionDetails <- DatabaseConnector::createConnectionDetails(dbms = "postgresql",
      connectionString = "jdbc:postgresql://odysseusovh02.odysseusinc.com/cdm_v500_synpuf_v101_110k",
      user = "ohdsi",
      schema = "public",
      password = "ohdsi")

#runAnalysis(connectionDetails, design, cohortTable='cohort', sessionId='abc',  cdmSchema='public', resultsSchema='results', vocabularySchema='public', analysisId=3334)

#q <- buildQuery(design, cohortTable='cohort', sessionId='',  cdmSchema='public', resultsSchema='results', vocabularySchema='public', analysisId=3)
#print(paste('Query: ', q))

saveResults(connectionDetails, design, 3334, "results", "results")
