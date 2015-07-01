package org.jax.mgi.app.assocload;

import org.jax.mgi.dbs.rdr.dao.QC_AssocLoad_Target_DiscrepDAO;
import org.jax.mgi.dbs.rdr.dao.QC_AssocLoad_Target_DiscrepState;
import org.jax.mgi.dbs.rdr.dao.QC_AssocLoad_Assoc_DiscrepDAO;
import org.jax.mgi.dbs.rdr.dao.QC_AssocLoad_Assoc_DiscrepState;
import org.jax.mgi.shr.dbutils.dao.SQLStream;
import org.jax.mgi.shr.dla.log.DLALogger;
import org.jax.mgi.shr.exception.MGIException;

/**
 * @is An object that knows how to create a DAO objects for QC report tables
 *     and send them to a stream to create a bcp record.
 * @has
 *   <UL>
 *   <LI> A stream for writing DAO objects to.
 *   </UL>
 * @does
 *   <UL>
 *   <LI> Provides methods that report discrepancy errors found in a
 *        MGIAssociation object by creating DAO objects for the corresponding
 *        QC report tables.
 *   </UL>
 * @company The Jackson Laboratory
 * @author dbm
 */

public class AssociationLoadReporter
{
    /////////////////
    //  Variables  //
    /////////////////

    // A stream for handling DAO objects.
    //
    private SQLStream stream;

    // A logger for logging messages.
    //
    private DLALogger logger;


    /**
     * Constructs a AssociationLoadReporter object.
     * @assumes Nothing
     * @effects Nothing
     * @param pStream The stream to send DAO object to.
     * @param pLogger The logger to write messages to.
     * @throws Nothing
     */
    public AssociationLoadReporter(SQLStream pStream, DLALogger pLogger)
    {
        stream = pStream;
        logger = pLogger;
    }

    /**
     * Create a DAO object for the QC_AssocLoad_Target_Discrep table and send
     * it to the stream.
     * @assumes Nothing
     * @effects Nothing
     * @param accID The target accession ID.
     * @param logicalDBKey The target logical DB key.
     * @param objectKey The target object key.
     * @param mgiTypeKey The target MGI type key.
     * @param expMGIType The expected MGI type name.
     * @param msg The error message.
     * @return Nothing
     * @throws MGIException If there is a problem using the DAOs.
     */
    public void reportTargetDiscrepancy(String accID, Integer logicalDBKey,
                                        Integer objectKey, Integer mgiTypeKey,
                                        String expMGIType, String msg)
        throws MGIException
    {
        if (objectKey != null)
            logger.logdDebug("Target Discrepancy: "+
                             accID+","+logicalDBKey.intValue()+","+
                             objectKey.intValue()+","+mgiTypeKey.intValue()+","+
                             expMGIType+","+msg,false);
        else
            logger.logdDebug("Target Discrepancy: "+
                             accID+","+logicalDBKey.intValue()+",0,0,"+
                             expMGIType+","+msg,false);

        // Create a state object and set the attributes.
        //
        QC_AssocLoad_Target_DiscrepState qcState =
            new QC_AssocLoad_Target_DiscrepState();
        qcState.setAccID(accID);
        qcState.setLogicalDBKey(logicalDBKey);
        if (objectKey != null)
            qcState.setObjectKey(objectKey);
        if (mgiTypeKey != null)
            qcState.setMGITypeKey(mgiTypeKey);
        qcState.setExpectedtype(expMGIType);
        qcState.setMessage(msg);

        // Create a DAO object from the state object and pass the DAO object
        // to the stream to create a bcp record.
        //
        QC_AssocLoad_Target_DiscrepDAO qcDAO =
            new QC_AssocLoad_Target_DiscrepDAO(qcState);
        stream.insert(qcDAO);
    }

    /**
     * Create a DAO object for the QC_AssocLoad_Assoc_Discrep table and send
     * it to the stream.
     * @assumes Nothing
     * @effects Nothing
     * @param tgtAccID The target accession ID.
     * @param tgtLogicalDBKey The target logical DB key.
     * @param tgtObjectKey The target object key.
     * @param tgtMGITypeKey The target MGI type key.
     * @param accID The accession ID that could not be associated.
     * @param logicalDBKey The logical DB key for the accession ID.
     * @param objectKey The object key for the accession ID.
     * @param mgiTypeKey The MGI type key for the accession ID.
     * @param msg The error message.
     * @return Nothing
     * @throws MGIException If there is a problem using the DAOs.
     */
    public void reportAssocDiscrepancy(String tgtAccID, int tgtLogicalDBKey,
                                       int tgtObjectKey, int tgtMGITypeKey,
                                       String accID, Integer logicalDBKey,
                                       Integer objectKey, Integer mgiTypeKey,
                                       String msg)
        throws MGIException
    {
        logger.logdDebug("Associate Discrepancy: "+
                         tgtAccID+","+tgtLogicalDBKey+","+
                         tgtObjectKey+","+tgtMGITypeKey+","+
                         accID+","+logicalDBKey+","+
                         objectKey+","+mgiTypeKey+","+
                         msg,false);

        // Create a state object and set the attributes.
        //
        QC_AssocLoad_Assoc_DiscrepState qcState =
            new QC_AssocLoad_Assoc_DiscrepState();
        qcState.setTgtaccID(tgtAccID);
        qcState.setTgtlogicalDBKey(new Integer(tgtLogicalDBKey));
        qcState.setTgtobjectKey(new Integer(tgtObjectKey));
        qcState.setTgtMGITypeKey(new Integer(tgtMGITypeKey));
        qcState.setAccID(accID);
        qcState.setLogicalDBKey(logicalDBKey);
        qcState.setObjectKey(objectKey);
        qcState.setMGITypeKey(mgiTypeKey);
        qcState.setMessage(msg);

        // Create a DAO object from the state object and pass the DAO object
        // to the stream to create a bcp record.
        //
        QC_AssocLoad_Assoc_DiscrepDAO qcDAO =
            new QC_AssocLoad_Assoc_DiscrepDAO(qcState);
        stream.insert(qcDAO);
    }
}
