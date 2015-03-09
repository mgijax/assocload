package org.jax.mgi.app.assocload;

import java.util.Iterator;
import java.util.Vector;

import org.jax.mgi.dbs.SchemaConstants;
import org.jax.mgi.shr.config.RADARCfg;
import org.jax.mgi.shr.dbutils.DBException;
import org.jax.mgi.shr.dbutils.MultiRowInterpreter;
import org.jax.mgi.shr.dbutils.MultiRowIterator;
import org.jax.mgi.shr.dbutils.ResultsNavigator;
import org.jax.mgi.shr.dbutils.RowReference;
import org.jax.mgi.shr.dbutils.SQLDataManager;
import org.jax.mgi.shr.dbutils.SQLDataManagerFactory;
import org.jax.mgi.shr.dla.log.DLALogger;
import org.jax.mgi.shr.exception.MGIException;

/**
 * @is An object that knows how to create MGIAssociation objects by comparing
 *     the accession ID/logical DB pairs in the MGI_Association table to what
 *     in already in MGI.
 * @has
 *   <UL>
 *   <LI> ResultsNavigator object
 *   </UL>
 * @does
 *   <UL>
 *   <LI> Creates a ResultsNavigator that will build MGIAssociation objects.
 *   <LI> Creates a MultiRowIterator that will step through the MGIAssociation
 *        objects.
 *   <LI> Provides a method to see if there are any more MGIAssociation objects.
 *   <LI> Provides a method to get the next MGIAssociation object.
 *   </UL>
 * @company The Jackson Laboratory
 * @author dbm
 * @version 1.0
 */

public class MGIAssociationGenerator
{
    /////////////////
    //  Variables  //
    /////////////////

    // A results navigator to step through the MGIAssociation objects.
    //
    private ResultsNavigator rn = null;

    // An iterator to step through the MGIAssociation objects.
    //
    private MultiRowIterator it = null;


    /**
     * Constructs a MGIAssociationGenerator object.
     * @assumes Nothing
     * @effects Nothing
     * @param logger The logger to write messages to.
     * @throws MGIException If there is a problem with configuration.
     */
    public MGIAssociationGenerator (DLALogger logger)
        throws MGIException
    {
        String sql = null;
        int rtn;

        // Create a configurator and get the job key for this run.
        //
        RADARCfg RDRCfg = new RADARCfg();
        int jobKey = RDRCfg.getJobKey().intValue();

        // Get a SQLDataManager for the RADAR database from the factory.
        //
        SQLDataManager sqlMgr =
            SQLDataManagerFactory.getShared(SchemaConstants.RADAR);

        // Get the name of the MGD database.
        //
        String mgdDB =
            SQLDataManagerFactory.getShared(SchemaConstants.MGD).getDatabase();

        // Create a ResultsNavigator to get all the associations that already
        // exist for the accession ID/logical DB pairs in the MGI_Association
        // table.
        //
        sql = "SELECT m._Record_key, " +
                     "m.accID, " +
                     "db._LogicalDB_key, " +
                     "m.target, " +
                     "null as 'MGI Type', " +
                     "null as 'Object Key' " +
              "FROM MGI_Association m, " +
                    mgdDB + ".ACC_LogicalDB db " +
              "WHERE m._JobStream_key = " + jobKey + " and " +
                    "m.logicalDB = db.name and " +
                    "not exists (SELECT 1 " +
                                "FROM " + mgdDB + ".ACC_Accession a, " +
                                      mgdDB + ".ACC_LogicalDB db2 " +
                                "WHERE m.accID = a.accID and " +
                                      "m.logicalDB = db2.name and " +
                                      "db2._LogicalDB_key = a._LogicalDB_key and " +
                                      "a._MGIType_key not in (21,25)) " +
              "UNION " +
              "SELECT m._Record_key, " +
                     "m.accID, " +
                     "db._LogicalDB_key, " +
                     "m.target, " +
                     "a._MGIType_key as 'MGI Type', " +
                     "a._Object_key as 'Object Key' " +
              "FROM MGI_Association m, " +
                    mgdDB + ".ACC_Accession a, " +
                    mgdDB + ".ACC_LogicalDB db " +
              "WHERE m._JobStream_key = " + jobKey + " and " +
                    "m.accID = a.accID and " +
                    "m.logicalDB = db.name and " +
                    "db._LogicalDB_key = a._LogicalDB_key and " +
                    "a._MGIType_key not in (21,25) " +
              "ORDER BY m._Record_key, m.accID, db._LogicalDB_key";
        logger.logdInfo("Execute Query: "+sql,true);
        rn = sqlMgr.executeQuery(sql);

        // Create a MultiRowIterator that uses an Interpreter to build and
        // return MGIAssociation objects from the ResultsNavigator.
        //
        it = new MultiRowIterator(rn, new Interpreter());
    }

    /**
     * Checks to see if there is another MGIAssociation object to be processed.
     * @assumes Nothing
     * @effects Nothing
     * @param None
     * @return True if there is another MGIAssociation object, otherwise false.
     * @throws Nothing
     */
    public boolean hasNext ()
    {
        return it.hasNext();
    }

    /**
     * Gets the next MGIAssociation object to be processed.
     * @assumes Nothing
     * @effects Nothing
     * @param None
     * @return The next MGIAssociation object.
     * @throws MGIException
     */
    public MGIAssociation next ()
        throws MGIException
    {
        return (MGIAssociation)it.next();
    }


    /**
     * @is An object that knows how to build MGIAssociation objects from
     *     multiple rows of a result set. All rows with the same record key
     *     belong to the same MGIAssociation object.
     * @has
     *   <UL>
     *   <LI> MGIAssociation object
     *   <LI> RowData inner class that holds the data from one row
     *   </UL>
     * @does
     *   <UL>
     *   <LI> Provides methods to implement the MultiRowInterpreter interface.
     *   </UL>
     * @company The Jackson Laboratory
     * @author dbm
     * @version 1.0
     */

    private class Interpreter implements MultiRowInterpreter
    {
        /////////////////
        //  Variables  //
        /////////////////

        // A MGIAssociation object.
        //
        private MGIAssociation assoc = null;


        /**
         * Constructs a Interpreter object.
         * @assumes Nothing
         * @effects Nothing
         * @param None
         * @throws MGIException
         */
        public Interpreter ()
            throws MGIException
        {
            assoc = new MGIAssociation();
        }

        /**
         * Build a RowData object from a given RowReference.
         * @assumes Nothing
         * @effects Nothing
         * @param row The current RowReference.
         * @return A RowData object.
         * @throws DBException
         */
        public Object interpret (RowReference row)
            throws DBException
        {
            // Create a new RowData object.
            //
            RowData rd = new RowData(row);

            return rd;
        }

        /**
         * Gets an object that represents the key to the given RowReference.
         * @assumes Nothing
         * @effects Nothing
         * @param row The current RowReference.
         * @return The key for the given RowReference.
         * @throws DBException
         */
        public Object interpretKey (RowReference row)
            throws DBException
        {
            return row.getInt(1);
        }

        /**
         * Build a MGIAssociation object from a vector of RowData objects.
         * @assumes Nothing
         * @effects Nothing
         * @param v The vector of RowData objects.
         * @return A MGIAssociation object.
         * @throws Nothing
         */
        public Object interpretRows (Vector v)
        {
            RowData rd = null;

            // Clear the MGIAssociation object to remove all attributes from the
            // previous association.
            //
            assoc.clear();

            // Iterate through the RowData objects to set the attributes of the
            // MGIAssociation object.
            //
            Iterator iter = v.iterator();
            while (iter.hasNext())
            {
                rd = (RowData)iter.next();

                assoc.addAccID(rd.accID);
                assoc.addLogicalDBKey(rd.logicalDBKey);
                assoc.addTarget(rd.target);
                assoc.addMGITypeKey(rd.mgiTypeKey);
                assoc.addObjectKey(rd.objectKey);
            }

            return assoc;
        }


        /**
         * @is An object that hold the attributes from one row of data.
         * @has
         *   <UL>
         *   <LI> Variables for each field of a RowReference.
         *   </UL>
         * @does Nothing
         * @company The Jackson Laboratory
         * @author dbm
         * @version 1.0
         */

        private class RowData
        {
            Integer recordKey;
            String accID;
            Integer logicalDBKey;
            Boolean target;
            Integer mgiTypeKey;
            Integer objectKey;

            /**
             * Constructs a RowData object.
             * @assumes Nothing
             * @effects Nothing
             * @param row The current RowReference.
             * @throws DBException
             */
            public RowData (RowReference row)
                throws DBException
            {
                // Set the attributes of the RowData object from the current row.
                //
                recordKey = row.getInt(1);
                accID = row.getString(2);
                logicalDBKey = row.getInt(3);
                target = row.getBoolean(4);
                mgiTypeKey = row.getInt(5);
                objectKey = row.getInt(6);
            }
        }
    }
}
