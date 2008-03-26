package org.jax.mgi.app.assocload;

import org.jax.mgi.dbs.SchemaConstants;
import org.jax.mgi.dbs.mgd.LogicalDBConstants;
import org.jax.mgi.dbs.mgd.MGITypeConstants;
import org.jax.mgi.shr.config.AssociationLoaderCfg;
import org.jax.mgi.shr.config.RADARCfg;
import org.jax.mgi.shr.dbutils.SQLDataManager;
import org.jax.mgi.shr.dbutils.SQLDataManagerFactory;
import org.jax.mgi.shr.dla.log.DLALogger;
import org.jax.mgi.shr.exception.MGIException;

/**
 * @is An object that knows how to prepare the database for loading the
 *     associations.
 * @has Nothing
 * @does
 *   <UL>
 *   <LI> Provides a method to delete any associations and reference records
 *        that were created by the prior run of the current job stream.
 *   </UL>
 * @company The Jackson Laboratory
 * @author dbm
 * @version 1.0
 */

public class AssociationLoadPreprocessor
{
    /////////////////
    //  Variables  //
    /////////////////

    // A logger for logging messages.
    //
    private DLALogger logger = null;


    /**
     * Constructs a AssociationLoadPreprocessor object.
     * @assumes Nothing
     * @effects Nothing
     * @param pLogger The logger to write messages to.
     * @throws Nothing
     */
    public AssociationLoadPreprocessor (DLALogger pLogger)
    {
        logger = pLogger;
    }

    /**
     * Delete all associations the prior run of the current job stream.
     * @assumes Nothing
     * @effects Nothing
     * @param None
     * @return Nothing
     * @throws MGIException If there is a problem with configuration or lookups.
     */
    public void deletePriorRecords ()
        throws MGIException
    {
        String jobStreamName = null;
        String sql = null;
        int rtn;

        // Create a configurator and get the delete/reload option.
        //
        AssociationLoaderCfg assocCfg = new AssociationLoaderCfg();
        Boolean deleteReload = assocCfg.getDeleteReload();

        // If the delete/reload options is not set, skip the delete.
        //
        if (deleteReload.booleanValue() == false)
            return;

        // Create a configurator and get the job stream name that is used to
        // identify the records to delete.
        //
        RADARCfg radarCfg = new RADARCfg();
        jobStreamName = radarCfg.getJobStreamName();

        logger.logpInfo("Delete current associations and reference records " +
                        "created by: " + jobStreamName,false);
        logger.logdInfo("Delete current associations and reference records " +
                        "created by: " + jobStreamName,true);

        // Get an SQLDataManager for the MGD database from the factory.
        //
        SQLDataManager sqlMgr = SQLDataManagerFactory.getShared(SchemaConstants.MGD);

        // Load a temp table with a list of all keys for associations that
        // were made by the prior run of this job stream.
        //
        sql = "SELECT _Accession_key " +
              "INTO #Keys " +
              "FROM ACC_AccessionReference r, " +
                   "MGI_User u " +
              "WHERE r._CreatedBy_key = u._User_key and " +
                     "u.login = '" + jobStreamName + "'";
        logger.logdInfo("Execute SQL: "+sql,true);
        rtn = sqlMgr.executeUpdate(sql);
        logger.logdInfo("Rows affected: "+rtn,false);

        // Delete all the ACC_AccessionReference records for the list of keys.
        // This has better performance than letting the trigger on the
        // ACC_Accession table do the delete.
        //
        sql = "DELETE ACC_AccessionReference " +
              "FROM ACC_AccessionReference r, " +
                   "#Keys k " +
              "WHERE r._Accession_key = k._Accession_key";
        logger.logdInfo("Execute SQL: "+sql,true);
        rtn = sqlMgr.executeUpdate(sql);
        logger.logdInfo("Rows affected: "+rtn,false);

        // Delete all the ACC_Accession records for the list of keys.
        //
        sql = "DELETE ACC_Accession " +
              "FROM ACC_Accession a, " +
                   "#Keys k " +
              "WHERE a._Accession_key = k._Accession_key";
        logger.logdInfo("Execute SQL: "+sql,true);
        rtn = sqlMgr.executeUpdate(sql);
        logger.logdInfo("Rows affected: "+rtn,false);

        // Drop the temp table that was created.
        //
        sql = "DROP TABLE #Keys";
        logger.logdInfo("Execute SQL: "+sql,true);
        rtn = sqlMgr.executeUpdate(sql);

        // Delete all records from the PRB_Reference table that were created by
        // the prior run of this job stream.
        //
        sql = "DELETE PRB_Reference " +
              "FROM PRB_Reference r, " +
                   "MGI_User u " +
              "WHERE r._CreatedBy_key = u._User_key and " +
                     "u.login = '" + jobStreamName + "'";
        logger.logdInfo("Execute SQL: "+sql,true);
        rtn = sqlMgr.executeUpdate(sql);
        logger.logdInfo("Rows affected: "+rtn,false);
    }
}
