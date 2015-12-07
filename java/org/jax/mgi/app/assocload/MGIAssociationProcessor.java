package org.jax.mgi.app.assocload;

import java.util.Iterator;
import java.util.Vector;

import org.jax.mgi.dbs.mgd.AccessionLib;
import org.jax.mgi.dbs.mgd.LogicalDBConstants;
import org.jax.mgi.dbs.mgd.dao.ACC_AccessionDAO;
import org.jax.mgi.dbs.mgd.dao.ACC_AccessionState;
import org.jax.mgi.dbs.mgd.dao.ACC_AccessionReferenceDAO;
import org.jax.mgi.dbs.mgd.dao.ACC_AccessionReferenceState;
import org.jax.mgi.dbs.mgd.dao.PRB_ReferenceDAO;
import org.jax.mgi.dbs.mgd.dao.PRB_ReferenceState;
import org.jax.mgi.dbs.mgd.lookup.JNumberLookup;
import org.jax.mgi.dbs.mgd.lookup.LogicalDBLookup;
import org.jax.mgi.shr.config.AssociationLoaderCfg;
import org.jax.mgi.shr.config.RADARCfg;
import org.jax.mgi.shr.dbutils.dao.SQLStream;
import org.jax.mgi.shr.dla.log.DLALogger;
import org.jax.mgi.shr.exception.MGIException;

/**
 * @is An object that knows how to process a MGIAssociation object by determining
 *     whether each association should be skipped, reported and/or created.
 * @has
 *   <UL>
 *   <LI> A stream for handling DAO objects
 *   <LI> AssociationLoadReporter object
 *   <LI> ProbeRefLookup object
 *   <LI> Counter for each type of action taken
 *   </UL>
 * @does
 *   <UL>
 *   <LI> Provides a method to process a MGIAssociation object.
 *   <LI> Provides a method to get the count for each type of action taken.
 *   </UL>
 * @company The Jackson Laboratory
 * @author dbm
 * @version 1.0
 */

public class MGIAssociationProcessor
{
    /////////////////
    //  Variables  //
    /////////////////

    // Single element arrays used with the "toArray()" method on a vector to
    // specify the array type to be returned.
    //
    private static String[] STRING_ARRAY = new String[0];
    private static Integer[] INTEGER_ARRAY = new Integer[0];

    // A logger for logging messages.
    //
    private DLALogger logger = null;

    // A stream for handling DAO objects for the load database.
    //
    private SQLStream loadStream = null;

    // An object that reports any discrepancy errors in a MGIAssociation object.
    //
    private AssociationLoadReporter assocRpt = null;

    // An object for looking up whether a probe is already associated with a
    // reference.
    private ProbeRefLookup probeRefLookup = null;

    // The reference key used with each ACC_AccessionReference and PRB_Reference
    // record that is created.
    //
    private Integer refsKey = null;

    // Flag to indicate whether the PRB_Reference record has already been
    // created for the probe if the MGIAssociation object defines associations
    // to be made to a probe.
    //
    private boolean madeProbeRef;

    // Arrays to hold the keys for logical DBs that can be associated with
    // single or multiple MGi objects.
    //
    private Vector singleDB = null;
    private Vector multipleDB = null;

    // Flag to indicate whether new accession IDs should be private.
    private Boolean isPrivateAccID = null;

    // Counters to track the number of times each type of action is taken during
    // the processing of a MGIAssociation object.
    //
    private int existCount = 0;
    private int skipCount = 0;
    private int assocCount = 0;
    private int reportCount = 0;


    /**
     * Constructs a MGIAssociationProcessor object.
     * @assumes Nothing
     * @effects Nothing
     * @param pLoadStream The stream for the load database.
     * @param pLogger The logger to write messages to.
     * @param pAssocRpt The object used to report MGIAssociation discrepancies.
     * @throws MGIException If there is a problem with configuration or lookups.
     */
    public MGIAssociationProcessor (SQLStream pLoadStream, DLALogger pLogger,
                                    AssociationLoadReporter pAssocRpt)
        throws MGIException
    {
        int i;
        String[] list = null;

        loadStream = pLoadStream;
        logger = pLogger;
        assocRpt = pAssocRpt;

        // Create a configurator to get the J-Number.
        //
        RADARCfg radarCfg = new RADARCfg();

        // Create a J-Number lookup object and get the reference key for the
        // J-Number.
        //
        JNumberLookup jNumLookup = new JNumberLookup();
        refsKey = jNumLookup.lookup(radarCfg.getJNumber());

        // Create a logical DB lookup object.
        //
        LogicalDBLookup dbLookup = new LogicalDBLookup();

        // Create a configurator to get the logical DB lists.
        //
        AssociationLoaderCfg assocCfg = new AssociationLoaderCfg();

        // Determine whether new accession IDs should be private.
        isPrivateAccID = assocCfg.getPrivateAccID();

        // Get the list of logical DBs that may only be associated with one
        // object in MGI.
        //
        list = assocCfg.getSingleDB();

        // Look up each logical DB name and add the keys to the array.
        //
        singleDB = new Vector();
        for (i=0; i<list.length; i++) {
	    String ldb = list[i].trim();
            singleDB.add(dbLookup.lookup(ldb));
	}

        // Get the list of logical DBs that may only be associated with
        // multiple objects in MGI.
        //
        list = assocCfg.getMultipleDB();

        // Look up each logical DB name and add the keys to the array.
        //
        multipleDB = new Vector();
        for (i=0; i<list.length; i++) {
	    String ldb = list[i].trim();
            multipleDB.add(dbLookup.lookup(ldb));
	}

        logger.logdInfo("Logical DBs for single object associations: " +
                        singleDB.toString(),false);
        logger.logdInfo("Logical DBs for multiple object associations: " +
                        multipleDB.toString(),false);

        // Create a probe reference lookup object.
        //
        probeRefLookup = new ProbeRefLookup(refsKey.intValue());
    }

    /**
     * Process a MGIAssociation object.
     * @assumes Nothing
     * @effects Nothing
     * @param assoc The MGIAssociation object to process.
     * @return Nothing
     * @throws MGIException If there is a problem with DAOs or an undefined
     *         logical DB is encountered.
     */
    public void process (MGIAssociation assoc)
        throws MGIException
    {
        int i, j;

        // Flag that indicates that no associations should be made because a
        // discrepancy error was found.
        //
        boolean skipAssociation = false;

        // No probe reference association has been made yet.
        //
        madeProbeRef = false;

        // Attributes of a target accession ID/logical DB pair.
        //
        String targetAccID = null;
        int targetLogicalDBKey = 0;
        int targetMGITypeKey = 0;
        int targetObjectKey = 0;
        int targetSameType = 0;
        int targetDiffType = 0;
        String targetMsg = null;

        // Get the type of object that the target accession ID/logical DB should
        // be associated with.
        //
        String expTargetMGIType = assoc.getTargetType();
        int expTargetMGITypeKey = assoc.getTargetTypeKey();

        // Get arrays containing the attributes of the MGIAssociation object.
        //
        String[] accIDs = assoc.getAccIDs();
        Integer[] logicalDBKeys = assoc.getLogicalDBKeys();
        Boolean[] targets = assoc.getTargets();
        Integer[] mgiTypeKeys = assoc.getMGITypeKeys();
        Integer[] objectKeys = assoc.getObjectKeys();

        // Create arrays containing the accession IDs and corresponding
        // logical DBs for each distinct non-target accession ID/logicalDB from
        // the MGIAssociation object.
        //
        int idx1, idx2;
        String pair = null;
        Vector vAccIDs = new Vector();
        Vector vLogicalDBKeys = new Vector();
        Vector vPair = new Vector();

        for (i=0; i<accIDs.length; i++)
        {
            if (targets[i].booleanValue() == true)
                continue;

            // If the accession ID/logical DB pair is not already in the vectors,
            // add them.
            //
            pair = accIDs[i] + "," + logicalDBKeys[i].intValue();
            if (vPair.indexOf(pair) < 0)
            {
                vAccIDs.add(accIDs[i]);
                vLogicalDBKeys.add(logicalDBKeys[i]);
                vPair.add(pair);
            }
        }

        // Create arrays from the vectors.
        //
        String[] distinctAccIDs = (String[])vAccIDs.toArray(STRING_ARRAY);
        Integer[] distinctLogicalDBKeys =
            (Integer[])vLogicalDBKeys.toArray(INTEGER_ARRAY);

        // Initialize arrays that are used to determine how to handle each
        // distinct accession ID/logical DB pair.
        //
        int[] sameTypeCount = new int[distinctAccIDs.length];
        int[] diffTypeCount = new int[distinctAccIDs.length];
        int[] sameObjCount = new int[distinctAccIDs.length];
        int[] action = new int[distinctAccIDs.length];
        String[] msg = new String[distinctAccIDs.length];

        for (i=0; i<distinctAccIDs.length; i++)
        {
            sameTypeCount[i] = 0;
            diffTypeCount[i] = 0;
            sameObjCount[i] = 0;
            action[i] = 0;
            msg[i] = null;
        }

        // Check each target accession ID/logical DB to determine what type of
        // object they are associated with in MGI. Count how many associations
        // there are to objects that have the same/different type as the
        // expected target type.
        //
        for (i=0; i<accIDs.length; i++)
        {
            if (targets[i].booleanValue() == true)
            {
                // Save the target accession ID/logical DB for future use.
                //
                targetAccID = accIDs[i];
                targetLogicalDBKey = logicalDBKeys[i].intValue();

                // Skip this accession ID/logical DB if it does not exist in MGI.
                //
                if (mgiTypeKeys[i] == null)
                    continue;

                // Save the target type and object for future use.
                //
                targetMGITypeKey = mgiTypeKeys[i].intValue();
                targetObjectKey = objectKeys[i].intValue();

                // Count whether it is the same or different than the expected
                // target type.
                //
                if (targetMGITypeKey == expTargetMGITypeKey)
                    targetSameType++;
                else
                    targetDiffType++;
            }
        }

        // If the target accession ID is not associated with one object of the
        // target type or it is associated with an object of some other type,
        // report an error and do not process any of the accession IDs for the
        // current MGIAssociation object.
        //
        if (targetSameType == 0 && targetDiffType == 0)
            targetMsg = AssociationLoadConstants.TARGET_DISCREP_A;
        else if (targetSameType == 0 && targetDiffType == 1)
            targetMsg = AssociationLoadConstants.TARGET_DISCREP_B;
        else if (targetSameType == 0 && targetDiffType > 1)
            targetMsg = AssociationLoadConstants.TARGET_DISCREP_C;
        else if (targetSameType == 1 && targetDiffType == 1)
            targetMsg = AssociationLoadConstants.TARGET_DISCREP_D;
        else if (targetSameType == 1 && targetDiffType > 1)
            targetMsg = AssociationLoadConstants.TARGET_DISCREP_E;
        else if (targetSameType > 1 && targetDiffType == 0)
            targetMsg = AssociationLoadConstants.TARGET_DISCREP_F;
        else if (targetSameType > 1 && targetDiffType == 1)
            targetMsg = AssociationLoadConstants.TARGET_DISCREP_G;
        else if (targetSameType > 1 && targetDiffType > 1)
            targetMsg = AssociationLoadConstants.TARGET_DISCREP_H;

        // If there is a discrepancy with the target accession ID/logical DB,
        // report each discrepancy and do not process this MGIAssociation object
        // any further.
        //
        if (targetMsg != null)
        {
            for (i=0; i<accIDs.length; i++)
            {
                // Skip this accession ID/logical DB if it is not the target.
                //
                if (targets[i].booleanValue() == false)
                    continue;

                assocRpt.reportTargetDiscrepancy(accIDs[i], logicalDBKeys[i],
                                                 objectKeys[i], mgiTypeKeys[i],
                                                 expTargetMGIType, targetMsg);
                reportCount++;
            }

            // Increment the skip count by the number of accession IDs that
            // were supposed to be associated.
            //
            skipCount += distinctAccIDs.length;

            return;
        }

        // Check each distinct accession ID/logical DB to see how many
        // associations they have with MGI objects and what type of objects
        // they are. This is called the "current pair".
        //
        for (i=0; i<distinctAccIDs.length; i++)
        {
            // Check each accession ID/logical DB pair in the MGIAssociation
            // object for ones that are the same as the current pair.
            //
            for (j=0; j<accIDs.length; j++)
            {
                // Skip this accession ID/logical DB if it is not the same as
                // the current pair.
                //
                if ((! accIDs[j].equals(distinctAccIDs[i])) ||
                    logicalDBKeys[j].intValue() != distinctLogicalDBKeys[i].intValue())
                    continue;

                // Skip this accession ID/logical DB if it does not exist in MGI.
                //
                if (mgiTypeKeys[j] == null)
                    continue;

                // Count whether the accession ID/logical DB is associated with
                // an object that has the same or different object type as the
                // target accession ID/logical DB.
                //
                if (mgiTypeKeys[j].intValue() == targetMGITypeKey)
                {
                    sameTypeCount[i]++;
                    if (objectKeys[j].intValue() == targetObjectKey)
                        sameObjCount[i]++;
                }
                else
                    diffTypeCount[i]++;
            }
            logger.logdDebug("Counts: "+sameTypeCount[i]+"  "+
                             diffTypeCount[i]+"  "+
                             sameObjCount[i]+"  "+
                             distinctAccIDs[i]+"  "+
                             distinctLogicalDBKeys[i].intValue(),false);
        }

        // Determine the action for each non-target accession ID/logical DB pair
        // based on the counts.
        //
        for (i=0; i<distinctAccIDs.length; i++)
        {
            // Determine the action for a logical DB that is allowed to have
            // only one association.
            //
            if (singleDB.indexOf(distinctLogicalDBKeys[i]) >= 0)
            {
                if (sameTypeCount[i] == 0 && diffTypeCount[i] == 0)
                {
                    action[i] = AssociationLoadConstants.ACTION_ASSOCIATE;
                }
                else if (sameTypeCount[i] == 0 && diffTypeCount[i] == 1)
                {
                    action[i] = AssociationLoadConstants.ACTION_REPORT_SKIP;
                    msg[i] = AssociationLoadConstants.ASSOC_DISCREP_A;
                }
                else if (sameTypeCount[i] == 0 && diffTypeCount[i] > 1)
                {
                    action[i] = AssociationLoadConstants.ACTION_REPORT_SKIP;
                    msg[i] = AssociationLoadConstants.ASSOC_DISCREP_B;
                }
                else if (sameTypeCount[i] == 1 && sameObjCount[i] == 1 &&
                         diffTypeCount[i] == 0)
                {
                    action[i] = AssociationLoadConstants.ACTION_SKIP;
                }
                else if (sameTypeCount[i] == 1 && sameObjCount[i] == 1 &&
                         diffTypeCount[i] == 1)
                {
                    action[i] = AssociationLoadConstants.ACTION_REPORT_SKIP;
                    msg[i] = AssociationLoadConstants.ASSOC_DISCREP_C;
                }
                else if (sameTypeCount[i] == 1 && sameObjCount[i] == 1 &&
                         diffTypeCount[i] > 1)
                {
                    action[i] = AssociationLoadConstants.ACTION_REPORT_SKIP;
                    msg[i] = AssociationLoadConstants.ASSOC_DISCREP_D;
                }
                else if (sameTypeCount[i] == 1 && sameObjCount[i] == 0 &&
                         diffTypeCount[i] == 0)
                {
                    action[i] = AssociationLoadConstants.ACTION_REPORT_SKIP;
                    msg[i] = AssociationLoadConstants.ASSOC_DISCREP_E;
                }
                else if (sameTypeCount[i] == 1 && sameObjCount[i] == 0 &&
                         diffTypeCount[i] == 1)
                {
                    action[i] = AssociationLoadConstants.ACTION_REPORT_SKIP;
                    msg[i] = AssociationLoadConstants.ASSOC_DISCREP_F;
                }
                else if (sameTypeCount[i] == 1 && sameObjCount[i] == 0 &&
                         diffTypeCount[i] > 1)
                {
                    action[i] = AssociationLoadConstants.ACTION_REPORT_SKIP;
                    msg[i] = AssociationLoadConstants.ASSOC_DISCREP_G;
                }
                else if (sameTypeCount[i] > 1 && sameObjCount[i] == 1 &&
                         diffTypeCount[i] == 0)
                {
                    action[i] = AssociationLoadConstants.ACTION_REPORT_SKIP;
                    msg[i] = AssociationLoadConstants.ASSOC_DISCREP_H;
                }
                else if (sameTypeCount[i] > 1 && sameObjCount[i] == 1 &&
                         diffTypeCount[i] == 1)
                {
                    action[i] = AssociationLoadConstants.ACTION_REPORT_SKIP;
                    msg[i] = AssociationLoadConstants.ASSOC_DISCREP_I;
                }
                else if (sameTypeCount[i] > 1 && sameObjCount[i] == 1 &&
                         diffTypeCount[i] > 1)
                {
                    action[i] = AssociationLoadConstants.ACTION_REPORT_SKIP;
                    msg[i] = AssociationLoadConstants.ASSOC_DISCREP_J;
                }
                else if (sameTypeCount[i] > 1 && sameObjCount[i] == 0 &&
                         diffTypeCount[i] == 0)
                {
                    action[i] = AssociationLoadConstants.ACTION_REPORT_SKIP;
                    msg[i] = AssociationLoadConstants.ASSOC_DISCREP_K;
                }
                else if (sameTypeCount[i] > 1 && sameObjCount[i] == 0 &&
                         diffTypeCount[i] == 1)
                {
                    action[i] = AssociationLoadConstants.ACTION_REPORT_SKIP;
                    msg[i] = AssociationLoadConstants.ASSOC_DISCREP_L;
                }
                else if (sameTypeCount[i] > 1 && sameObjCount[i] == 0 &&
                         diffTypeCount[i] > 1)
                {
                    action[i] = AssociationLoadConstants.ACTION_REPORT_SKIP;
                    msg[i] = AssociationLoadConstants.ASSOC_DISCREP_M;
                }
            }

            // Determine the action for a logical DB that is allowed to have
            // multiple associations.
            //
            else if (multipleDB.indexOf(distinctLogicalDBKeys[i]) >= 0)
            {
                if (sameTypeCount[i] == 0)
                {
                    action[i] = AssociationLoadConstants.ACTION_ASSOCIATE;
                }
                else if (sameTypeCount[i] == 1 && sameObjCount[i] == 1)
                {
                    action[i] = AssociationLoadConstants.ACTION_SKIP;
                }
                else if (sameTypeCount[i] == 1 && sameObjCount[i] == 0 &&
                         diffTypeCount[i] == 0)
                {
                    action[i] = AssociationLoadConstants.ACTION_REPORT_ASSOCIATE;
                    msg[i] = AssociationLoadConstants.ASSOC_DISCREP_E;
                }
                else if (sameTypeCount[i] == 1 && sameObjCount[i] == 0 &&
                         diffTypeCount[i] == 1)
                {
                    action[i] = AssociationLoadConstants.ACTION_REPORT_ASSOCIATE;
                    msg[i] = AssociationLoadConstants.ASSOC_DISCREP_F;
                }
                else if (sameTypeCount[i] == 1 && sameObjCount[i] == 0 &&
                         diffTypeCount[i] > 1)
                {
                    action[i] = AssociationLoadConstants.ACTION_REPORT_ASSOCIATE;
                    msg[i] = AssociationLoadConstants.ASSOC_DISCREP_G;
                }
                else if (sameTypeCount[i] > 1 && sameObjCount[i] == 1 &&
                         diffTypeCount[i] == 0)
                {
                    action[i] = AssociationLoadConstants.ACTION_REPORT_SKIP;
                    msg[i] = AssociationLoadConstants.ASSOC_DISCREP_H;
                }
                else if (sameTypeCount[i] > 1 && sameObjCount[i] == 1 &&
                         diffTypeCount[i] == 1)
                {
                    action[i] = AssociationLoadConstants.ACTION_REPORT_SKIP;
                    msg[i] = AssociationLoadConstants.ASSOC_DISCREP_I;
                }
                else if (sameTypeCount[i] > 1 && sameObjCount[i] == 1 &&
                         diffTypeCount[i] > 1)
                {
                    action[i] = AssociationLoadConstants.ACTION_REPORT_SKIP;
                    msg[i] = AssociationLoadConstants.ASSOC_DISCREP_J;
                }
                else if (sameTypeCount[i] > 1 && sameObjCount[i] == 0 &&
                         diffTypeCount[i] == 0)
                {
                    action[i] = AssociationLoadConstants.ACTION_REPORT_ASSOCIATE;
                    msg[i] = AssociationLoadConstants.ASSOC_DISCREP_K;
                }
                else if (sameTypeCount[i] > 1 && sameObjCount[i] == 0 &&
                         diffTypeCount[i] == 1)
                {
                    action[i] = AssociationLoadConstants.ACTION_REPORT_ASSOCIATE;
                    msg[i] = AssociationLoadConstants.ASSOC_DISCREP_L;
                }
                else if (sameTypeCount[i] > 1 && sameObjCount[i] == 0 &&
                         diffTypeCount[i] > 1)
                {
                    action[i] = AssociationLoadConstants.ACTION_REPORT_ASSOCIATE;
                    msg[i] = AssociationLoadConstants.ASSOC_DISCREP_M;
                }
            }

            // Throw an exception if the logical DB if not defined in either
            // of the lists.
            //
            else
            {
                throw new MGIException("Logical DB (" +
                                       distinctLogicalDBKeys[i] + ") must be " +
                                       "configured to allow either single or " +
                                       "multiple associations.");
            }

            // If a "Report and Skip" discrepancy is found, do not allow any
            // associations to be made.
            //
            if (action[i] == AssociationLoadConstants.ACTION_REPORT_SKIP)
                skipAssociation = true;
        }

        // Use the action established for each distinct accession ID/logical DB
        // to see if it should be skipped, reported and/or associated.
        //
        for (i=0; i<distinctAccIDs.length; i++)
        {
            // Action: Do nothing (association already exists).
            //
            if (action[i] == AssociationLoadConstants.ACTION_SKIP)
            {
                logger.logdDebug("Exists: "+distinctAccIDs[i]+","+
                                 distinctLogicalDBKeys[i].intValue(),false);
                existCount++;
                continue;
            }

            // Action: Report a discrepancy.
            //
            if (action[i] == AssociationLoadConstants.ACTION_REPORT_SKIP ||
                action[i] == AssociationLoadConstants.ACTION_REPORT_ASSOCIATE)
            {
                for (j=0; j<accIDs.length; j++)
                {
                    // Skip this accession ID/logical DB if it is not the same as
                    // the current pair.
                    //
                    if ((!accIDs[j].equals(distinctAccIDs[i])) ||
                        logicalDBKeys[j].intValue() !=
                        distinctLogicalDBKeys[i].intValue())
                        continue;

                    assocRpt.reportAssocDiscrepancy(targetAccID, targetLogicalDBKey,
                                                    targetObjectKey, expTargetMGITypeKey,
                                                    accIDs[j], logicalDBKeys[j],
                                                    objectKeys[j], mgiTypeKeys[j],
                                                    msg[i]);
                    reportCount++;
                }
            }

            // Action: Make the association.
            //
            if (action[i] == AssociationLoadConstants.ACTION_ASSOCIATE ||
                action[i] == AssociationLoadConstants.ACTION_REPORT_ASSOCIATE)
            {
                // If any of the accession ID/logical DB pairs could not be
                // associated because of an error condition, do not make
                // the association.
                //
                if (skipAssociation)
                    continue;

                associate(distinctAccIDs[i],distinctLogicalDBKeys[i],
                          targetMGITypeKey, targetObjectKey);
                assocCount++;
            }
        }

        // If any of the accession ID/logical DB pairs could not be associated
        // because of an error condition, increment the skip count by the number
        // of accession IDs that were supposed to be associated.
        //
        if (skipAssociation)
            skipCount += distinctAccIDs.length;
    }

    /**
     * Associate an accession ID to a MGI object by creating DAOs for the
     * ACC_Accession and ACC_AccessionReference tables and passing them to the
     * stream to create bcp records. Do the same for the PRB_Reference table
     * for probe associations ONLY, if the reference doesn't exist already.
     * @assumes Nothing
     * @effects Nothing
     * @param accID The accession ID to associate to the MGI object.
     * @param logicalDBKey The logical DB for the accession ID.
     * @param mgiTypeKey The type of object to make the association to.
     * @param objectKey The object to make the association to.
     * @return Nothing
     * @throws MGIException If there is a problem using the DAOs.
     */
    public void associate (String accID, Integer logicalDBKey, int mgiTypeKey,
                           int objectKey)
        throws MGIException
    {
        Integer probeKey = new Integer(objectKey);

        logger.logdDebug("Make Association: "+accID+","+logicalDBKey+","+
                         objectKey+","+mgiTypeKey,false);

        Vector vParts = AccessionLib.splitAccID(accID);

        // Create an state object for the ACC_Accession table and set its
        // attributes.
        //
        ACC_AccessionState accState = new ACC_AccessionState();
        accState.setAccID(accID);
        accState.setPrefixPart((String)vParts.get(0));
        accState.setNumericPart((Integer)vParts.get(1));
        accState.setLogicalDBKey(logicalDBKey);
        accState.setMGITypeKey(new Integer(mgiTypeKey));
        accState.setObjectKey(probeKey);
        accState.setPrivateVal(isPrivateAccID);
        accState.setPreferred(new Boolean(true));

        // Create a DAO for the state object and pass it to the stream.
        //
        ACC_AccessionDAO accDAO = new ACC_AccessionDAO(accState);
        loadStream.insert(accDAO);

        // Create an state object for the ACC_AccessionReference table and set
        // its attributes.
        //
        ACC_AccessionReferenceState accRefState =
            new ACC_AccessionReferenceState();
        accRefState.setAccessionKey(accDAO.getKey().getKey());
        accRefState.setRefsKey(refsKey);

        // Create a DAO for the state object and pass it to the stream.
        //
        ACC_AccessionReferenceDAO accRefDAO =
                    new ACC_AccessionReferenceDAO(accRefState);
        loadStream.insert(accRefDAO);

        // Special processing for probe associations ONLY.
        //
        if (mgiTypeKey == 3 && madeProbeRef == false)
        {
            // Determine if the probe is already associated with the reference.
            //
            if (probeRefLookup.lookup(probeKey) == null)
            {
                // Create an state object for the PRB_Reference table and set
                // its attributes.
                //
                PRB_ReferenceState probeRefState = new PRB_ReferenceState();
                probeRefState.setProbeKey(probeKey);
                probeRefState.setRefsKey(refsKey);
                probeRefState.setHasrmap(new Boolean(false));
                probeRefState.setHassequence(new Boolean(false));

                // Create a DAO for the state object and pass it to the stream.
                //
                PRB_ReferenceDAO probeRefDAO = new PRB_ReferenceDAO(probeRefState);
                loadStream.insert(probeRefDAO);

                // Add the probe key to the lookup cache, so another bcp record
                // will not be created for it in the future.
                //
                probeRefLookup.addToCache(probeKey);
            }

            // The probe reference has been made.
            //
            madeProbeRef = true;
        }
    }

    /**
     * Get the number of associations that were skipped because they already
     * exist.
     * @assumes Nothing
     * @effects Nothing
     * @param None
     * @return The number of existing associations.
     * @throws Nothing
     */
    public int getExistCount ()
    {
        return existCount;
    }

    /**
     * Get the number of associations that were skipped due to an error.
     * @assumes Nothing
     * @effects Nothing
     * @param None
     * @return The number of skipped associations.
     * @throws Nothing
     */
    public int getSkipCount ()
    {
        return skipCount;
    }

    /**
     * Get the number of associations that were made.
     * @assumes Nothing
     * @effects Nothing
     * @param None
     * @return The number of associations made.
     * @throws Nothing
     */
    public int getAssocCount ()
    {
        return assocCount;
    }

    /**
     * Get the number of errors reported.
     * @assumes Nothing
     * @effects Nothing
     * @param None
     * @return The number of errors reported.
     * @throws Nothing
     */
    public int getReportCount ()
    {
        return reportCount;
    }
}
