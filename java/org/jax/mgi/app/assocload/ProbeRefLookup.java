//  $Header$
//  $Name$

package org.jax.mgi.app.assocload;

import org.jax.mgi.dbs.SchemaConstants;
import org.jax.mgi.shr.cache.CacheException;
import org.jax.mgi.shr.cache.FullCachedLookup;
import org.jax.mgi.shr.cache.KeyValue;
import org.jax.mgi.shr.config.ConfigException;
import org.jax.mgi.shr.dbutils.DBException;
import org.jax.mgi.shr.dbutils.RowDataInterpreter;
import org.jax.mgi.shr.dbutils.RowReference;
import org.jax.mgi.shr.dbutils.SQLDataManagerFactory;

/**
 * @is An object that knows how to look up a probe key to determine if it is
 *     already associated with the given reference.
 * @has Nothing
 * @does
 *   <UL>
 *   <LI> Provides a method to look up a given probe key to verify that it
 *        already is associated with the reference.
 *   </UL>
 * @company The Jackson Laboratory
 * @author dbm
 */

public class ProbeRefLookup extends FullCachedLookup
{
    // The reference key for the load reference.
    //
    private int refsKey;

    /**
     * Constructs a ProbeRefLookup object.
     * @assumes Nothing
     * @effects Nothing
     * @param refsKey The reference key to get the probe keys for.
     * @throws CacheException
     * @throws ConfigException
     * @throws DBException
     */
    public ProbeRefLookup(int pRefsKey)
        throws CacheException, ConfigException, DBException
    {
        super(SQLDataManagerFactory.getShared(SchemaConstants.MGD));

        refsKey = pRefsKey;
    }

    /**
     * Looks up a probe key to make sure it exists in the cache.
     * @assumes Nothing
     * @effects Nothing
     * @param probeKey The key for the probe to look up.
     * @return The same key that was used for the lookup if it was found.
     *         Otherwise a null is returned.
     * @throws DBException
     * @throws CacheException
     */
    public Integer lookup(Integer probeKey)
        throws DBException, CacheException
    {
            return (Integer)super.lookupNullsOk(probeKey);
    }

    /**
     * Get the query to fully initialize the cache with the keys for all probes
     * that are associated with the given reference.
     * @assumes Nothing
     * @effects Nothing
     * @param None
     * @return The query to fully initialize the cache.
     * @throws Nothing
     */
    public String getFullInitQuery ()
    {
        return new String("SELECT _Probe_key FROM PRB_Reference " +
                          "WHERE _Refs_key = " + refsKey);
    }

    /**
     * Add a new probe key to the cache.
     * @assumes Nothing
     * @effects Nothing
     * @param probeKey The probe key to add.
     * @return Nothing
     * @throws CacheException
     * @throws DBException
     */
    protected void addToCache(Integer probeKey)
    throws CacheException, DBException
    {
        super.cache.put(probeKey, probeKey);
    }

    /**
     * Get a RowDataInterpreter for creating a KeyValue object from a database
     * used for creating a new cache entry.
     * @assumes nothing
     * @effects nothing
     * @param None
     * @return The RowDataInterpreter object.
     * @throws Nothing
     */
    public RowDataInterpreter getRowDataInterpreter()
    {
        class Interpreter implements RowDataInterpreter
        {
            public Object interpret (RowReference row)
                throws DBException
            {
                return new KeyValue(row.getInt(1), row.getInt(1));
            }
        }
        return new Interpreter();
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
