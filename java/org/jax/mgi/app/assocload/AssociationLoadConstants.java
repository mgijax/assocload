package org.jax.mgi.app.assocload;

/**
 * @is An object that contains constant definitions for the association load.
 * @has
 *   <UL>
 *   <LI> Constant definitions
 *   </UL>
 * @does Nothing
 * @company The Jackson Laboratory
 * @author dbm
 */

public class AssociationLoadConstants
{
    // String constants
    //
    public static final String CRT = "\n";
    public static final String TAB = "\t";
    public static final String COMMA = ",";

    // Association input file constants.
    //
    public static final int MIN_FIELD_COUNT = 2;

    // Error messages for target accession ID/logical DB discrepancies.
    //
    public static final String TARGET_DISCREP_A =
        "A: Same type (0), different type (0)";
    public static final String TARGET_DISCREP_B =
        "B: Same type (0), different type (1)";
    public static final String TARGET_DISCREP_C =
        "C: Same type (0), different type (>1)";
    public static final String TARGET_DISCREP_D =
        "D: Same type (1), different type (1)";
    public static final String TARGET_DISCREP_E =
        "E: Same type (1), different type (>1)";
    public static final String TARGET_DISCREP_F =
        "F: Same type (>1), different type (0)";
    public static final String TARGET_DISCREP_G =
        "G: Same type (>1), different type (1)";
    public static final String TARGET_DISCREP_H =
        "H: Same type (>1), different type (>1)";

    // Error messages for associate accession ID/logical DB discrepancies.
    //
    public static final String ASSOC_DISCREP_A =
        "A: Same type (0), different type (1)";
    public static final String ASSOC_DISCREP_B =
        "B: Same type (0), different type (>1)";
    public static final String ASSOC_DISCREP_C =
        "C: Same type (1), same object (1), different type (1)";
    public static final String ASSOC_DISCREP_D =
        "D: Same type (1), same object (1), different type (>1)";
    public static final String ASSOC_DISCREP_E =
        "E: Same type (1), same object (0), different type (0)";
    public static final String ASSOC_DISCREP_F =
        "F: Same type (1), same object (0), different type (1)";
    public static final String ASSOC_DISCREP_G =
        "G: Same type (1), same object (0), different type (>1)";
    public static final String ASSOC_DISCREP_H =
        "H: Same type (>1), same object (1), different type (0)";
    public static final String ASSOC_DISCREP_I =
        "I: Same type (>1), same object (1), different type (1)";
    public static final String ASSOC_DISCREP_J =
        "J: Same type (>1), same object (1), different type (>1)";
    public static final String ASSOC_DISCREP_K =
        "K: Same type (>1), same object (0), different type (0)";
    public static final String ASSOC_DISCREP_L =
        "L: Same type (>1), same object (0), different type (1)";
    public static final String ASSOC_DISCREP_M =
        "M: Same type (>1), same object (0), different type (>1)";

    // Possible actions to perform when processing the associations.
    //
    public static final int ACTION_SKIP = 1;
    public static final int ACTION_REPORT_SKIP = 2;
    public static final int ACTION_ASSOCIATE = 3;
    public static final int ACTION_REPORT_ASSOCIATE = 4;
}
