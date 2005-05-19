//  $Header$
//  $Name$

package org.jax.mgi.app.assocload;

import java.util.Vector;

import org.jax.mgi.dbs.rdr.dao.MGI_AssociationState;
import org.jax.mgi.dbs.mgd.lookup.LogicalDBLookup;
import org.jax.mgi.shr.exception.MGIException;
import org.jax.mgi.shr.ioutils.RecordDataInterpreter;
import org.jax.mgi.shr.ioutils.RecordFormatException;

/**
 * @is An object that knows how to create a DPAssociation object from an
 *     association input record.
 * @has
 *   <UL>
 *   <LI> A DPAssociation object that contains the attributes for creating
 *        records in the MGI_Association table.
 *   </UL>
 * @does
 *   <UL>
 *   <LI> Provides a method to interpret an input record and set the attributes
 *        of the DPAssociation object.
 *   <LI> Provides a method to determine if an input record is valid and needs
 *        to be processed.
 *   </UL>
 * @company The Jackson Laboratory
 * @author dbm
 */

public class DPAssociationInterpreter implements RecordDataInterpreter
{
    /////////////////
    //  Variables  //
    /////////////////

    private DPAssociation assoc;
    private String[] logicalDBs = null;
    private LogicalDBLookup lookup = null;


    /**
     * Constructs a DPAssociationInterpreter object.
     * @assumes Nothing
     * @effects Nothing
     * @param None
     * @throws MGIException If there is a problem configuring the lookup.
     */
    public DPAssociationInterpreter()
        throws MGIException
    {
        assoc = new DPAssociation();
        lookup = new LogicalDBLookup();

    }

    /**
     * Parses an input record to get the attributes needed to populate a
     * DPAssociation object.
     * @assumes Nothing
     * @effects Loads a DPAssociation object
     * @param rec A record from the input file
     * @return A DPAssociation object
     * @throws MGIException If the format of the input file is invalid.
     */
    public Object interpret(String rec)
        throws MGIException
    {
        int i;
        String s;
        Integer dbKey;
        MGI_AssociationState assocState = null;

        // If the logical DB array is empty, then this must be the header line
        // from the input file.
        if (logicalDBs == null)
        {
            // Get the tab-delimited logical DB names from the header record.
            //
            s = rec.replaceFirst(AssociationLoadConstants.CRT,"");
            logicalDBs = s.split(AssociationLoadConstants.TAB);

            // Throw an exception if the minimum number of fields is not found.
            //
            if (logicalDBs.length < AssociationLoadConstants.MIN_FIELD_COUNT)
            {
                RecordFormatException e = new RecordFormatException();
                e.bindRecord(rec);
                throw e;
            }

            // Lookup each logical DB name to make sure it exists.  This will
            // throw an exception if any of them cannot be found.
            //
            for (i=0; i<logicalDBs.length; i++)
            {
                dbKey = lookup.lookup(logicalDBs[i]);
            }

            // Return null to let the caller know that there was no
            // DPAssociation object created for this input record.
            //
            return null;
        }

        // Clear the attributes of the DPAssociation object so it can be
        // re-used for the next input record.
        //
        assoc.clear();

        // Remove the newline character from the input record and split the
        // input record into tab-delimited fields.
        //
        s = rec.replaceFirst(AssociationLoadConstants.CRT,"");
        String[] fields = s.split(AssociationLoadConstants.TAB);

        // Throw an exception if the input record does not have the required
        // number of fields.
        //
        if (fields.length != logicalDBs.length)
        {
            RecordFormatException e = new RecordFormatException();
            e.bindRecord(rec);
            throw e;
        }

        // Otherwise, use the fields from the input record to set the
        // attributes of the DP_MGC_ClonesState object.
        //
        else
        {
            // Process each field in the input record.
            //
            for (i=0; i<fields.length; i++)
            {
                // If the field is empty, skip to the next one.
                //
                if (fields[i].length() == 0)
                    continue;

                // Split the field into 1 or more comma-separated accession IDs.
                //
                String[] accIDs = fields[i].split(AssociationLoadConstants.COMMA);

                // Process each accession ID in the field.
                //
                for (int j=0; j<accIDs.length; j++)
                {
                    // Create a new MGI_AssociationState object and set its
                    // attributes.
                    //
                    assocState = new MGI_AssociationState();
                    assocState.setAccID(accIDs[j]);
                    assocState.setLogicalDB(logicalDBs[i]);

                    // Only the first field in the input file contains the target
                    // accession ID.
                    //
                    if (i == 0)
                        assocState.setTarget(new Boolean(true));
                    else
                        assocState.setTarget(new Boolean(false));

                    // Add the MGI_AssociationState object to the DPAssociation
                    // object.
                    //
                    assoc.addState(assocState);
                }
            }
        }

        return assoc;
    }

    /**
     * Determines if the given input record is a valid record. A comment
     * line is considered to be invalid.
     * @assumes Nothing
     * @effects Nothing
     * @param rec A record from the input file
     * @return Indicator of whether the input record is valid (true) or a
     *         comment line (false)
     * @throws Nothing
     */
    public boolean isValid (String rec)
    {
        // If the first character of the input record is a "#", it is a
        // comment and should be ignored.
        //
        if (rec.substring(0,1).equals("#"))
            return false;
        else
            return true;
    }
}


//  $Log$
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
