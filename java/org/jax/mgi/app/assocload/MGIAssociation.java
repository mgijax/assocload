package org.jax.mgi.app.assocload;

import java.util.Iterator;
import java.util.Vector;

import org.jax.mgi.dbs.mgd.lookup.MGITypeLookup;
import org.jax.mgi.shr.config.AssociationLoaderCfg;
import org.jax.mgi.shr.dla.log.DLALogger;
import org.jax.mgi.shr.exception.MGIException;

/**
 * @is An object that holds the attributes necessary to associated one or more
 *     accession IDs to an MGI object.
 * @has
 *   <UL>
 *   <LI> The accession ID and logical DB that should identify a target MGI
 *        object that other accession ID will be associated with.
 *   <LI> The MGI type and object key for any objects that the target
 *        accession ID and logical DB are associated with.
 *   <LI> One or more additional accession ID/logical DB pairs to be associated
 *        with the target object.
 *   <LI> The MGI type and object key for any objects that the additional
 *        accession ID/logical DB pairs are associated with.
 *   </UL>
 * @does
 *   <UL>
 *   <LI> Provides methods to set/get the attributes.
 *   <LI> Provides a method to clear its attributes.
 *   </UL>
 * @company The Jackson Laboratory
 * @author dbm
 * @version 1.0
 */

public class MGIAssociation
{
    /////////////////
    //  Variables  //
    /////////////////

    // Single element arrays used with the "toArray()" method on a vector to
    // specify the array type to be returned.
    //
    private static String[] STRING_ARRAY = new String[0];
    private static Integer[] INTEGER_ARRAY = new Integer[0];
    private static Boolean[] BOOLEAN_ARRAY = new Boolean[0];

    // The name and key of the MGI type that the target accession ID/logical DB
    // shourld be associated with.
    //
    private String targetType = null;
    private int targetTypeKey;

    // Vectors to hold the association information.
    //
    private Vector vAccID = null;
    private Vector vLogicalDBKey = null;
    private Vector vTarget = null;
    private Vector vMGITypeKey = null;
    private Vector vObjectKey = null;


    /**
     * Constructs a MGIAssociation object.
     * @assumes Nothing
     * @effects Nothing
     * @param None
     * @throws MGIException If there is a problem with configuration or lookups.
     */
    public MGIAssociation ()
        throws MGIException
    {
        // Create a configurator and get the target MGI type.
        //
        AssociationLoaderCfg assocCfg = new AssociationLoaderCfg();
        targetType = assocCfg.getTargetMGIType();

        MGITypeLookup lookup = new MGITypeLookup();
        targetTypeKey = lookup.lookup(targetType).intValue();

        // Create vectors for the attributes.
        //
        vAccID = new Vector();
        vLogicalDBKey = new Vector();
        vTarget = new Vector();
        vMGITypeKey = new Vector();
        vObjectKey = new Vector();
    }

    /**
     * Get the target MGI type from this object.
     * @assumes Nothing
     * @effects Nothing
     * @param None
     * @return The target MGI type.
     * @throws Nothing
     */
    public String getTargetType ()
    {
        return targetType;
    }

    /**
     * Get the target MGI type key from this object.
     * @assumes Nothing
     * @effects Nothing
     * @param None
     * @return The target MGI type key.
     * @throws Nothing
     */
    public int getTargetTypeKey ()
    {
        return targetTypeKey;
    }

    /**
     * Add an accession ID to the vector.
     * @assumes Nothing
     * @effects Nothing
     * @param accID The accession ID to add.
     * @return Nothing
     * @throws Nothing
     */
    public void addAccID (String accID)
    {
        vAccID.add(accID);
    }

    /**
     * Get an array of values from the accession ID vector.
     * @assumes Nothing
     * @effects Nothing
     * @param None
     * @return An array of accession IDs.
     * @throws Nothing
     */
    public String[] getAccIDs()
    {
        return (String[])vAccID.toArray(STRING_ARRAY);
    }

    /**
     * Add a logical DB to the vector.
     * @assumes Nothing
     * @effects Nothing
     * @param logicalDB The logical DB to add.
     * @return Nothing
     * @throws Nothing
     */
    public void addLogicalDBKey (Integer logicalDBKey)
    {
        vLogicalDBKey.add(logicalDBKey);
    }

    /**
     * Get an array of values from the logical DB vector.
     * @assumes Nothing
     * @effects Nothing
     * @param None
     * @return An array of logical DB keys.
     * @throws Nothing
     */
    public Integer[] getLogicalDBKeys()
    {
        return (Integer[])vLogicalDBKey.toArray(INTEGER_ARRAY);
    }

    /**
     * Add a target indicator to the vector.
     * @assumes Nothing
     * @effects Nothing
     * @param target The target indicator to add.
     * @return Nothing
     * @throws Nothing
     */
    public void addTarget (Boolean target)
    {
        vTarget.add(target);
    }

    /**
     * Get an array of values from the target indicator vector.
     * @assumes Nothing
     * @effects Nothing
     * @param None
     * @return An array of target indicators.
     * @throws Nothing
     */
    public Boolean[] getTargets()
    {
        return (Boolean[])vTarget.toArray(BOOLEAN_ARRAY);
    }

    /**
     * Add a MGI type key to the vector.
     * @assumes Nothing
     * @effects Nothing
     * @param mgiTypeKey The MGI type key to add.
     * @return Nothing
     * @throws Nothing
     */
    public void addMGITypeKey (Integer mgiTypeKey)
    {
        vMGITypeKey.add(mgiTypeKey);
    }

    /**
     * Get an array of values from the MGI type key vector.
     * @assumes Nothing
     * @effects Nothing
     * @param None
     * @return An array of MGI type keys.
     * @throws Nothing
     */
    public Integer[] getMGITypeKeys()
    {
        return (Integer[])vMGITypeKey.toArray(INTEGER_ARRAY);
    }

    /**
     * Add an object key to the vector.
     * @assumes Nothing
     * @effects Nothing
     * @param objectKey The object key to add.
     * @return Nothing
     * @throws Nothing
     */
    public void addObjectKey (Integer objectKey)
    {
        vObjectKey.add(objectKey);
    }

    /**
     * Get an array of values from the object key vector.
     * @assumes Nothing
     * @effects Nothing
     * @param None
     * @return An array of object keys.
     * @throws Nothing
     */
    public Integer[] getObjectKeys()
    {
        return (Integer[])vObjectKey.toArray(INTEGER_ARRAY);
    }

    /**
     * Clear the attributes of this object.
     * @assumes Nothing
     * @effects Nothing
     * @param None
     * @return Nothing
     * @throws Nothing
     */
    public void clear ()
    {
        vAccID.clear();
        vLogicalDBKey.clear();
        vTarget.clear();
        vMGITypeKey.clear();
        vObjectKey.clear();
    }

    /**
     * Print the attributes of this object to the diagnostic log for debugging.
     * @assumes Nothing
     * @effects Nothing
     * @param logger The logger to write messages to.
     * @return Nothing
     * @throws Nothing
     */
    public void print(DLALogger logger)
    {
        logger.logdDebug("\ntargetType: " + targetType +
                         "  vAccID: " + vAccID.toString() +
                         "  vLogicalDBKey: " + vLogicalDBKey.toString() +
                         "  vTarget: " + vTarget.toString() +
                         "  vMGITypeKey: " + vMGITypeKey.toString() +
                         "  vObjectKey: " + vObjectKey.toString(), false);
    }
}
