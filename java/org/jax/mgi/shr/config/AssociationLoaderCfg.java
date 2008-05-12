package org.jax.mgi.shr.config;

/**
 * @is An object that is used to retrieve configuration parameters
 *     for the association loader.
 * @has
 *   <UL>
 *   <LI> A reference to a configuration manager
 *   </UL>
 * @does
 *   <UL>
 *   <LI> Provides methods for retrieving configuration parameters that
 *        are specific to the association loader.
 *   </UL>
 * @company The Jackson Laboratory
 * @author dbm
 * @version 1.0
 */

public class AssociationLoaderCfg extends Configurator
{
    /**
     * Constructs a AssociationLoaderCfg object.
     * @assumes Nothing
     * @effects Nothing
     * @param None
     * @throws ConfigException if a configuration manager cannot be obtained
     */
    public AssociationLoaderCfg()
        throws ConfigException
    {
    }

    /**
     * Get the type of MGI object that associations will be made to.
     * @assumes Nothing
     * @effects Nothing
     * @param None
     * @return The configuration value
     * @throws ConfigException if the value is not found
     */
    public String getTargetMGIType ()
        throws ConfigException
    {
        return getConfigStringNull("ASSOCLOAD_TARGET_MGI_TYPE");
    }

    /**
     * Get the value (true/false) that determines whether delete/reload option
     * has been chosen.
     * @assumes Nothing
     * @effects Nothing
     * @param None
     * @return The configuration value
     * @throws ConfigException if the value is not found
     */
    public Boolean getDeleteReload ()
        throws ConfigException
    {
        return getConfigBoolean("ASSOCLOAD_DELETE_RELOAD",new Boolean(false));
    }

    /**
     * Get the list of logical DBs that may only be associated with one object
     * in MGI.
     * @assumes Nothing
     * @effects Nothing
     * @param None
     * @return The configuration value
     * @throws ConfigException if the value is not found
     */
    public String[] getSingleDB ()
        throws ConfigException
    {
        return getConfigStringArrayNull("ASSOCLOAD_SINGLE_OBJECT_DB");
    }

    /**
     * Get the list of logical DBs that may be associated with multiple objects
     * in MGI.
     * @assumes Nothing
     * @effects Nothing
     * @param None
     * @return The configuration value
     * @throws ConfigException if the value is not found
     */
    public String[] getMultipleDB ()
        throws ConfigException
    {
        return getConfigStringArrayNull("ASSOCLOAD_MULTIPLE_OBJECT_DB");
    }

    /**
     * Get the value (true/false) that determines whether to load the
     * MGI_Association table from an input file.
     * @assumes Nothing
     * @effects Nothing
     * @param None
     * @return The configuration value
     * @throws ConfigException if the value is not found
     */
    public Boolean getLoadFromFile ()
        throws ConfigException
    {
        return getConfigBoolean("ASSOCLOAD_FROM_FILE",new Boolean(false));
    }

    /**
     * Get the value (true/false) that determines whether the load should
     * create private accession IDs.
     * @assumes Nothing
     * @effects Nothing
     * @param None
     * @return The configuration value
     * @throws ConfigException if the value is not found
     */
    public Boolean getPrivateAccID ()
        throws ConfigException
    {
        return getConfigBoolean("ASSOCLOAD_PRIVATE_ACCID",new Boolean(false));
    }
}
