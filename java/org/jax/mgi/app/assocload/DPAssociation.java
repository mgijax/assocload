//  $Header$
//  $Name$

package org.jax.mgi.app.assocload;

import java.util.Vector;

import org.jax.mgi.dbs.rdr.dao.MGI_AssociationDAO;
import org.jax.mgi.dbs.rdr.dao.MGI_AssociationState;
import org.jax.mgi.shr.dbutils.dao.SQLStream;
import org.jax.mgi.shr.exception.MGIException;

/**
 * @is An object that represents a set of accession ID/logical DB pairs taken
 *     from an input file that define associations to be made to a MGI object.
 * @has
 *   <UL>
 *   <LI> Vector of MGI_AssociationState objects to be loaded into the
 *        MGI_Association table.
 *   </UL>
 * @does
 *   <UL>
 *   <LI> Provides methods to set/get the attributes.
 *   <LI> Provides a method to clear the attributes.
 *   <LI> Provides a method create a DAO for the MGI_Association table and
 *        insert the DAO onto a stream to create a bcp record.
 *   </UL>
 * @company The Jackson Laboratory
 * @author dbm
 *
 */

public class DPAssociation
{
    /////////////////
    //  Variables  //
    /////////////////

    private Vector vMGIAssocState = null;

    private int recordNumber = 0;

    /**
     * Constructs a DPAssociation object.
     * @assumes Nothing
     * @effects Nothing
     * @param None
     * @throws Nothing
     */
    public DPAssociation()
    {
        vMGIAssocState = new Vector();
    }

    /**
     * Adds a MGI_AssociationState object to the vector.
     * @assumes Nothing
     * @effects Nothing
     * @param state The MGI_AssociationState object to add.
     * @return Nothing
     * @throws Nothing
     */
    public void addState(MGI_AssociationState state)
    {
        vMGIAssocState.add(state);
    }

    /**
     * Clears the vector of MGI_AssociationState objects.
     * @assumes Nothing
     * @effects Nothing
     * @param None
     * @return Nothing
     * @throws Nothing
     */
    public void clear()
    {
        vMGIAssocState.clear();
    }

    /**
     * Uses each MGI_Association object to create DAOs for the
     * MGI_Association table and insert them on the stream to create bcp
     * records.
     * @assumes Nothing
     * @effects Nothing
     * @param stream The bcp stream to write the bcp records to.
     * @return Nothing
     * @throws MGIException If there is a problem using the DAOs.
     */
      public void insert(SQLStream stream)
        throws MGIException
    {
        MGI_AssociationState state;
        recordNumber++;

        // Process each accession ID/logical DB pair in this object.
        //
        for (int i=0; i<vMGIAssocState.size(); i++)
        {
            state = (MGI_AssociationState)vMGIAssocState.get(i);

            // Set the attributes of the state object for the MGI_Association
            // table.
            //
            state.setRecordKey(new Integer(recordNumber));

            // Create a DAO from the state object and insert it on the stream
            // to create a bcp record.
            //
            MGI_AssociationDAO dao = new MGI_AssociationDAO(state);
            stream.insert(dao);
        }
    }
}


//  $Log$
//  Revision 1.1.2.1  2005/05/19 17:41:15  dbm
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
