package org.ohdsi.cohortcharacterization;

import com.github.mjeanroy.dbunit.core.dataset.DataSetFactory;
import org.dbunit.Assertion;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.CompositeDataSet;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.operation.DatabaseOperation;
import org.dbunit.util.TableFormatter;
import org.junit.BeforeClass;
import org.junit.Test;
import org.ohdsi.circe.helper.ResourceHelper;
import org.ohdsi.sql.SqlSplit;
import org.ohdsi.sql.SqlTranslate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;


// Note: to verify the test results, we must directly query the database
// via createQueryTable(), because loading the result schema tables via
// getTables() fails because the results schema isn't seen by the existing connection.

public class Characterization_5_0_Test extends AbstractDatabaseTest {
  private final static Logger log = LoggerFactory.getLogger(Characterization_5_0_Test.class);
  private static final String CDM_DDL_PATH = "/ddl/cdm_v5.0.sql";
  private static final String RESULTS_DDL_PATH = "/ddl/resultsSchema.sql";
  private static final String CDM_SCHEMA = "cdm";
  
  @BeforeClass
  public static void beforeClass() {
    jdbcTemplate = new JdbcTemplate(getDataSource());
    prepareSchema(CDM_SCHEMA, CDM_DDL_PATH);
  }
  
  @Test
  public void criteriaPrevalenceTest() throws Exception {
    final String RESULTS_SCHEMA = "criteria_prevalence"; // this must be all lower case for DBUnit to work
    final String[] testDataSetsPrep = new String[] { 
      "/datasets/vocabulary.json",
      "/cohortcharacterization/criteriaPrevalenceTest_PREP.json" 
    };

    // prepare results schema for the specified results schema
    prepareSchema(RESULTS_SCHEMA, RESULTS_DDL_PATH);

    final IDatabaseConnection dbUnitCon = getConnection();

    // load test data into DB.
    final IDataSet dsPrep = DataSetFactory.createDataSet(testDataSetsPrep);
    DatabaseOperation.CLEAN_INSERT.execute(dbUnitCon, dsPrep); // clean load of the DB. Careful, clean means "delete the old stuff"

    // load the default expression: 
    // condition occurrence persistend for 180 days, censor window between 2000-04-01 to 2000-09-01
    // cohort 1 will use the default expression from JSON.
    String characterizationDesign = ResourceHelper.GetResourceAsString("/cohortcharacterization/criteriaPrevalenceTest.json");
    CCQueryBuilder builder = new CCQueryBuilder(characterizationDesign, "cohort", "SID", CDM_SCHEMA, RESULTS_SCHEMA, CDM_SCHEMA, RESULTS_SCHEMA, 1);
    String ccSql = SqlTranslate.translateSql(builder.build(), "postgresql");

    // execute on database, expect no errors
    jdbcTemplate.batchUpdate(SqlSplit.splitSql(ccSql));

    // Validate results
    // Load actual records from cohort table
    final ITable ccResultsTable = dbUnitCon.createQueryTable(RESULTS_SCHEMA + ".cc_results", 
            String.format("SELECT * from %s where cc_generation_id = %d ORDER BY analysis_id, concept_id ", 
                    RESULTS_SCHEMA + ".cc_results",
                    1));
    TableFormatter f = new TableFormatter();
    String ccResultsText = f.format(ccResultsTable);
    final IDataSet actualDataSet = new CompositeDataSet(new ITable[] {ccResultsTable});

    // Load expected data from an XML dataset
    final String[] testDataSetsVerify = new String[] {"/cohortcharacterization/criteriaPrevalenceTest_VERIFY.json"};
    final IDataSet expectedDataSet = DataSetFactory.createDataSet(testDataSetsVerify);

    // Assert actual database table match expected table
    Assertion.assertEquals(expectedDataSet, actualDataSet);     

  }
  
  @Test
  public void criteriaDistributionTest() throws Exception {
    final String RESULTS_SCHEMA = "criteria_dist"; // this must be all lower case for DBUnit to work
    final String[] testDataSetsPrep = new String[] { 
      "/datasets/vocabulary.json",
      "/cohortcharacterization/criteriaDistributionTest_PREP.json" 
    };

    // prepare results schema for the specified results schema
    prepareSchema(RESULTS_SCHEMA, RESULTS_DDL_PATH);

    final IDatabaseConnection dbUnitCon = getConnection();

    // load test data into DB.
    final IDataSet dsPrep = DataSetFactory.createDataSet(testDataSetsPrep);
    DatabaseOperation.CLEAN_INSERT.execute(dbUnitCon, dsPrep); // clean load of the DB. Careful, clean means "delete the old stuff"

    // load the default expression: 
    // condition occurrence persistend for 180 days, censor window between 2000-04-01 to 2000-09-01
    // cohort 1 will use the default expression from JSON.
    String characterizationDesign = ResourceHelper.GetResourceAsString("/cohortcharacterization/criteriaDistributionTest.json");
    CCQueryBuilder builder = new CCQueryBuilder(characterizationDesign, "cohort", "SID", CDM_SCHEMA, RESULTS_SCHEMA, CDM_SCHEMA, RESULTS_SCHEMA, 1);
    String ccSql = SqlTranslate.translateSql(builder.build(), "postgresql");

    // execute on database, expect no errors
    jdbcTemplate.batchUpdate(SqlSplit.splitSql(ccSql));

    // Validate results
    // Load actual records from cohort table
    final ITable ccResultsTable = dbUnitCon.createQueryTable(RESULTS_SCHEMA + ".cc_results", 
            String.format("SELECT * from %s where cc_generation_id = %d ORDER BY analysis_id, concept_id ", 
                    RESULTS_SCHEMA + ".cc_results",
                    1));
    TableFormatter f = new TableFormatter();
    String ccResultsText = f.format(ccResultsTable);
    final IDataSet actualDataSet = new CompositeDataSet(new ITable[] {ccResultsTable});

    // Load expected data from an XML dataset
    final String[] testDataSetsVerify = new String[] {"/cohortcharacterization/criteriaDistributionTest_VERIFY.json"};
    final IDataSet expectedDataSet = DataSetFactory.createDataSet(testDataSetsVerify);

    // Assert actual database table match expected table
    Assertion.assertEquals(expectedDataSet, actualDataSet);     

  }
}
