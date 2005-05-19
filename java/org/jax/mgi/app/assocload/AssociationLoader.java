//  $Header$
//  $Name$

package org.jax.mgi.app.assocload;

import java.util.Vector;

import org.jax.mgi.dbs.SchemaConstants;
import org.jax.mgi.shr.config.AssociationLoaderCfg;
import org.jax.mgi.shr.config.BCPManagerCfg;
import org.jax.mgi.shr.dbutils.SQLDataManager;
import org.jax.mgi.shr.dbutils.SQLDataManagerFactory;
import org.jax.mgi.shr.dbutils.Table;
import org.jax.mgi.shr.dbutils.bcp.BCPManager;
import org.jax.mgi.shr.dbutils.dao.BCP_Stream;
import org.jax.mgi.shr.dbutils.dao.SQLStream;
import org.jax.mgi.shr.dla.loader.DLALoader;
import org.jax.mgi.shr.exception.MGIException;
import org.jax.mgi.shr.ioutils.InputDataFile;
import org.jax.mgi.shr.ioutils.RecordDataIterator;

/**
 * <pre>
 * Purpose: Associate accession IDs to MGI objects by using the associations
 *          defined in the MGI_Association table of the RADAR database to load
 *          new records into the ACC_Accesssion table of the MGD database.
 *
 * Usage:
 *
 *     ${JAVA} ${JAVARUNTIMEOPTS} -classpath ${CLASSPATH} \
 *             -DCONFIG=${COMMON_CONFIG},${DP_CONFIG},${ASSOCLOAD_CONFIG} \
 *             -DJOBKEY=${JOBKEY} ${DLA_START}
 *
 *     where
 *         JAVA is the path of the Java executable
 *         JAVARUNTIMEOPTS is any Java runtime options to be used
 *         CLASSPATH is the Java CLASSPATH to be used
 *         COMMON_CONFIG is the full path to the common configuration file
 *         DP_CONFIG is the full path to the data provider configuration file
 *         ASSOCLOAD_CONFIG is the full path to the association loader
 *                          configuration file
 *         JOBKEY is the job key created for this job stream
 *         DLA_START is the name of the DLAStart class
 *                   (e.g. org.jax.mgi.shr.dla.DLAStart)
 *
 * Env Vars:
 *
 *     DLA_LOADER must be set to the name of this class to allow the DLAStart
 *     class knows which class is a DLALoader and has the load() method to call
 *     (e.g. org.jax.mgi.app.assocload.AssociationLoader).
 *
 * System Properties:
 *
 *     CONFIG
 *     JOBKEY
 *
 * Inputs:
 *
 *     - classpath
 *     - Configuration files
 *     - Records in the RADAR database
 *
 * Outputs:
 *
 *     - bcp records to the following MGD database tables:
 *           ACC_Accession
 *           ACC_AccessionReference
 *           PRB_Reference (for clone associations only)
 *     - bcp records to the following RADAR database tables:
 *           MGI_Association (if ASSOCLOAD_FROM_FILE = true)
 *           QC_AssocLoad_Target_Discrep
 *           QC_AssocLoad_Assoc_Discrep
 *     - log files
 *
 * Exit Codes:
 *
 *     0 = Successful completion
 *     1 = Failure
 *
 * Assumes:
 *
 *     There are no other processes running at the same time that will be
 *     attempting to generate keys for the tables that it writes to.
 *
 * Implementation:
 *
 *     This class extends the DLALoader class and uses the initialize() and
 *     run() methods to perform its tasks.  The DLAStart class provides the
 *     main() method and will invoke the DLALoader.
 *
 *     If the environment variable ASSOCLOAD_FROM_FILE is true, this application
 *     will act as a data provider loader and load the MGI_Association table
 *     from an input file.  Otherwise, it assumes that the MGI_Association table
 *     has already been loaded by another application.
 *
 *     This application gets the records from the MGI_Association table for
 *     a given job key.  These records contain the accession IDs and logical DBs
 *     that define associations that are to be made.  All records with the same
 *     record key are processed together.  One of these records defines the
 *     target accession ID/logical DB that should uniquely identify a MGI
 *     object via an association in the ACC_Accession table.  The other records
 *     identify accession ID/logical DB pairs that are to be associated with
 *     this MGI object.
 *
 *     If the target is not associated with one (and only one) MGI object,
 *     an error is generated for the "Target Discrepancy" QC report.  The
 *     remaining accession IDs are analyzed based on whether their logical DBs
 *     are allowed to be association with single or multiple objects.  The
 *     logical DBs for each category are defined by the environment variables
 *     "ASSOCLOAD_SINGLE_OBJECT_DB" and "ASSOCLOAD_MULTIPLE_OBJECT_DB".  If
 *     any of these accession ID/logical DB pair does not follow the rules
 *     concerning the count/type of objects it can be associated with, an error
 *     is generated for the "Associate Discrepancy" QC report.  The algorithm
 *     for logical DBs that allow multiple associations does allow some
 *     associations to be made even if there is a discrepancy reported.
 *
 *     If an association is allowed to be made and doesn't already exist, a bcp
 *     record is written for the ACC_Accession and ACC_AccessionReference tables.
 *     For probe associations ONLY, a record is written for the PRB_Reference
 *     table if the load reference doesn't already exist for the probe.
 *
 *     All records written to database tables are done using the bcp utility.
 *     See the "Outputs" section for a list of the tables that are loaded.
 *
 *     There is a validation step that makes sure that all data written to
 *     the bcp file conforms to the constraints of the database table.
 *
 *     There is extensive logging.  See the DLA standards document for a
 *     description of the logs.
 *
 *     See the Association Loader Design Document for further details.
 *
 * Modules:
 *
 *     org.jax.mgi.dbs
 *     org.jax.mgi.dbs.mgd
 *     org.jax.mgi.shr.config
 *     org.jax.mgi.shr.dbutils
 *     org.jax.mgi.shr.dbutils.bcp
 *     org.jax.mgi.shr.dbutils.dao
 *     org.jax.mgi.shr.dla.loader
 *     org.jax.mgi.shr.exception
 *     org.jax.mgi.shr.ioutils
 *
 * Tools Used: None
 *
 * Exceptions:
 *
 *     MGIException - common object thrown for any exception that occurs
 *                    during the load.
 *
 * Company: The Jackson Laboratory
 * </pre>
 * @author dbm
 * @version 1.0
 */

public class AssociationLoader extends DLALoader
{
    /////////////////
    //  Variables  //
    /////////////////

    // A flag to indicate whether the MGI_Association table needs to be loaded
    // from an input file.
    //
    private boolean loadFromFile = false;

    // A stream for writing bcp records to the MGI_Association table if the load
    // is configured to perform this step.
    //
    private SQLStream radarStream = null;

    // An input data file object for the input file (if needed).
    //
    private InputDataFile inFile = null;

    // An interpreter for the input file (if needed).
    //
    private DPAssociationInterpreter interpreter = null;

    // An iterator that gets one DP_Association object at a time (if needed).
    //
    private RecordDataIterator iter = null;

    // An object that performs preprocessing steps.
    //
    private AssociationLoadPreprocessor assocPrep = null;

    // An object that generates the next MGIAssociation object to process.
    //
    private MGIAssociationGenerator assocGenerator = null;

    // An object that processes a MGIAssociation object.
    //
    private MGIAssociationProcessor assocProcessor = null;

    // An object that reports any discrepancy errors in a MGIAssociation object.
    //
    private AssociationLoadReporter assocRpt = null;


    /**
     * Initialize all the class variables.
     * @assumes Nothing
     * @effects Set all the class variables.
     * @param None
     * @return Nothing
     * @throws MGIException if any of the objects cannot be instantiated
     */
    protected void initialize ()
        throws MGIException
    {
        logger.logpInfo("Perform initialization",false);

        // Determine if the MGI_Association table is to be loaded from an
        // input file.
        AssociationLoaderCfg assocLoadCfg = new AssociationLoaderCfg();
        loadFromFile = assocLoadCfg.getLoadFromFile().booleanValue();

        if (loadFromFile)
        {
            // Create an input data file object for the input file.
            //
            inFile = new InputDataFile();

            // Create an interpreter for the input file.
            //
            interpreter = new DPAssociationInterpreter();

            // Create an iterator that gets one DP_Association object at a time.
            //
            iter = inFile.getIterator(interpreter);
        }

        // Create a AssociationLoadPreprocessor object for executing the
        // preprocessing steps.
        //
        assocPrep = new AssociationLoadPreprocessor(logger);

        // Create a AssociationLoadReporter object for reporting any discrepancy
        // errors in a MGIAssociation object.
        //
        assocRpt = new AssociationLoadReporter(qcStream, logger);
    }

    /**
     * Initializes BCP writers for tables that will be written to.
     * @assumes Nothing
     * @effects Nothing
     * @param None
     * @return Nothing
     * @throws MGIException if there is an error.
     */
    protected void preprocess ()
        throws MGIException
    {
        // Build a vector that contains a Table object for each table to be
        // written to in the "load" database.
        //
        Vector loadTables = new Vector();
        loadTables.add(Table.getInstance("ACC_Accession", loadDBMgr));
        loadTables.add(Table.getInstance("ACC_AccessionReference", loadDBMgr));
        loadTables.add(Table.getInstance("PRB_Reference", loadDBMgr));

        // Initialize writers for each table if a BCP stream if being used.
        //
        if (loadStream.isBCP())
            ((BCP_Stream)loadStream).initBCPWriters(loadTables);

        // Build a vector that contains a Table object for each table to be
        // written to in the QC database.
        //
        Vector qcTables = new Vector();
        qcTables.add(Table.getInstance("QC_AssocLoad_Target_Discrep", qcDBMgr));
        qcTables.add(Table.getInstance("QC_AssocLoad_Assoc_Discrep", qcDBMgr));

        // Initialize writers for each table if a BCP stream if being used.
        //
        if (qcStream.isBCP())
            ((BCP_Stream)qcStream).initBCPWriters(qcTables);

        // If the MGI_Association table is being loaded from a file by this load,
        // create an additional stream for writing bcp records to the RADAR
        // database.
        //
        if (loadFromFile)
        {
            // Get a SQLDataManager and a BCPManager and use them to create a
            // new stream.
            //
            SQLDataManager sqlMgr =
                SQLDataManagerFactory.getShared(SchemaConstants.RADAR);
            sqlMgr.setLogger(logger);
            BCPManager bcpMgr =
                new BCPManager(new BCPManagerCfg(SchemaConstants.RADAR));
            bcpMgr.setLogger(logger);
            radarStream = createSQLStream(dlaConfig.getLoadStreamName(),
                                          sqlMgr, bcpMgr);

            // Build a vector that contains a Table object for each table to be
            // written to in the RADAR database.
            //
            Vector radarTables = new Vector();
            radarTables.add(Table.getInstance("MGI_Association", sqlMgr));

            // Initialize writers for each table if a BCP stream if being used.
            //
            if (radarStream.isBCP())
                ((BCP_Stream)radarStream).initBCPWriters(radarTables);
        }

        // Delete any existing associations for the load reference if the load
        // has been configured to delete them.
        //
        assocPrep.deletePriorRecords();
    }

    /**
     * Executes the steps needed to load clones from the MGI format tables in
     * the RADAR database into the MGD database.
     * @assumes Nothing
     * @effects Nothing
     * @param None
     * @return Nothing
     * @throws MGIException if there is an error.
     */
    protected void run ()
        throws MGIException
    {
        int count = 0;
        DPAssociation dpAssoc = null;
        MGIAssociation mgiAssoc = null;

        // Write a heading to the data validation log.
        //
        logger.logvInfo("\nAssociation Loader Validation Errors",false);
        logger.logvInfo("------------------------------------",false);

        if (loadFromFile)
        {
            logger.logpInfo("Process the data provider input file",false);
            logger.logdInfo("Process the data provider input file",true);

            // Process each DP_Association object returned by the iterator.
            //
            while (iter.hasNext())
            {
                // Get the next DP_Association object from the iterator.
                //
                dpAssoc = (DPAssociation)iter.next();

                // If the DP_Association object is null, it means that the
                // interpreter processed the header record and did not create
                // a DP_Association object.  Skip to the next iteration.
                //
                if (dpAssoc == null)
                    continue;

                if (count > 0 && count%10000 == 0)
                    logger.logdInfo("Processed " + count + " input records",false);
                count++;

                // Send the DP_Association object to the stream.
                //
                dpAssoc.insert(radarStream);
            }

            logger.logdInfo("Processed " + count + " input records",false);

            // Load the bcp file for the MGIAssociation table.
            //
            logger.logpInfo("Load the bcp file for the MGIAssociation table",false);
            logger.logdInfo("Load the bcp file for the MGIAssociation table",true);
            radarStream.close();
        }

        // Create a MGIAssociationProcessor object for processing each
        // MGIAssociation object.
        //
        assocProcessor = new MGIAssociationProcessor(loadStream, logger, assocRpt);

        // Create a MGIAssociationGenerator object for getting the
        // MGIAssociation objects to be processed.
        //
        logger.logpInfo("Get all MGI Associations",false);
        logger.logdInfo("Get all MGI Associations",true);
        assocGenerator = new MGIAssociationGenerator(logger);

        // Process each MGIAssociation object returned by the
        // MGIAssociationGenerator.
        //
        logger.logpInfo("Process each MGI Association",false);
        logger.logdInfo("Process each MGI Association",true);
        count = 0;
        while (assocGenerator.hasNext())
        {
            if (count > 0 && count%10000 == 0)
                logger.logdInfo("Processed " + count + " MGI Associations",false);
            count++;

            // Get the next MGIAssociation object.
            //
            mgiAssoc = assocGenerator.next();

            if (logger.isDebug())
                mgiAssoc.print(logger);

            // Pass the MGIAssociation object to the MGIAssociationProcessor for
            // processing.
            //
            assocProcessor.process(mgiAssoc);
         }

        logger.logdInfo("Processed " + count + " MGI Associations",false);

        // Load the bcp files for tables in the MGD database.
        //
        logger.logpInfo("Load the bcp files for the ACC_Accession, " +
                        "ACC_AccessionReference and PRB_Reference tables",false);
        logger.logdInfo("Load the bcp files for the ACC_Accession, " +
                        "ACC_AccessionReference and PRB_Reference tables",true);
        loadStream.close();

        // Load the bcp files for the QC report tables.
        //
        logger.logpInfo("Load the bcp files for the " +
                        "QC_AssocLoad_Target_Discrep and " +
                        "QC_AssocLoad_Assoc_Discrep tables",false);
        logger.logdInfo("Load the bcp files for the " +
                        "QC_AssocLoad_Target_Discrep and " +
                        "QC_AssocLoad_Assoc_Discrep tables",true);
        qcStream.close();

        // Write the processing counts to the curator summary log.
        //
        logger.logcInfo("\nAssociation Loader Processing Counts",false);
        logger.logcInfo("------------------------------------",false);
        logger.logcInfo("Number of associations that already exist:       " +
                        assocProcessor.getExistCount(),false);
        logger.logcInfo("Number of associations skipped due to an error:  " +
                        assocProcessor.getSkipCount(),false);
        logger.logcInfo("Number of associations made:                     " +
                        assocProcessor.getAssocCount(),false);
        logger.logcInfo("Number of discrepancy errors reported:           " +
                        assocProcessor.getReportCount(),false);
    }

    /**
     * Provides an empty method required by the parent class.
     * @assumes Nothing
     * @effects Nothing
     * @param None
     * @return Nothing
     * @throws MGIException if there is an error.
     */
    protected void postprocess ()
        throws MGIException
    {
    }
}


//  $Log$
//  Revision 1.2  2005/02/17 19:03:08  dbm
//  Add PRB_Reference to table vector and fix curator summary log heading.
//
//  Revision 1.1  2005/01/24 17:19:15  dbm
//  New
//
//
/**************************************************************************
*
* Warranty Disclaimer and Copyright Notice
*
*  THE JACKSON LABORATORY MAKES NO REPRESENTATION ABOUT THE SUITABILITY OR
*  ACCURACY OF THIS SOFTWARE OR DATA FOR ANY PURPOSE, AND MAKES NO WARRANTIES,
*  EITHER EXPRESS OR IMPLIED, INCLUDING MERCHANTABILITY AND FITNESS FOR A
*  PARTICULAR PURPOSE OR THAT THE USE OF THIS SOFTWARE OR DATA WILL NOT
*  INFRINGE ANY THIRD PARTY PATENTS, COPYRIGHTS, TRADEMARKS, OR OTHER RIGHTS.
*  THE SOFTWARE AND DATA ARE PROVIDED "AS IS".
*
*  This software and data are provided to enhance knowledge and encourage
*  progress in the scientific community and are to be used only for research
*  and educational purposes.  Any reproduction or use for commercial purpose
*  is prohibited without the prior express written permission of The Jackson
*  Laboratory.
*
* Copyright \251 1996, 1999, 2002, 2005 by The Jackson Laboratory
*
* All Rights Reserved
*
**************************************************************************/
