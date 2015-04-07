#!/bin/sh
#
#  AssocLoad.sh
###########################################################################
#
#  Purpose:  This script controls the execution of the association loader.
#
#  Usage:
#
#      AssocLoad.sh  ConfigFile  JobKey  [SystemProps]
#
#      where
#
#          ConfigFile is the path name of the configuration file for the
#                     data provider loader.
#          JobKey is the value that identifies the records in the RADAR
#                 database that are to be processed.
#          SystemProps is a list of optional system properties to be passed
#                      to the Java application.  For example:
#
#                          -DSYSPROP1=abc -DSYSPROP2=123
#
#  Env Vars:
#
#      See the configuration file
#
#  Inputs:
#
#      - Association loader configuration file (AssocLoad.config)
#      - Data provider loader configuration file (first argument)
#      - Additional arguments (see Usage)
#
#  Outputs:
#
#      - Log files defined by the environment variables ${LOG_PROC},
#        ${LOG_DIAG}, ${LOG_CUR} and ${LOG_VAL}
#      - BCP files for each database table to be loaded
#      - Records written to the database tables
#      - Exceptions written to standard error
#      - Configuration and initialization errors are written to a log file
#        for the shell script
#
#  Exit Codes:
#
#      0:  Successful completion
#      1:  Fatal error occurred
#      2:  Non-fatal error occurred
#
#  Assumes:  Nothing
#
#  Implementation:
#
#      This script performs the following steps:
#
#      1) Calls the Association Load application to:
#
#         a) Use the records in the MGI_Association table in the RADAR
#            database to make associations in the MGD database.
#         b) Load QC report tables in the RADAR database with any
#            discrepancies in the data.
#
#      2) Calls the QC Report product to generate the QC reports using the
#         data in the QC report tables.
#
#  Notes:  None
#
###########################################################################

#
#  Set up a log file for the shell script in case there is an error
#  during configuration and initialization.
#
cd `dirname $0`/..
LOG=`pwd`/AssocLoad.log
rm -f ${LOG}

#
#  Verify the argument(s) to the shell script.
#
if [ $# -lt 2 ]
then
    echo "Usage: $0  ConfigFile  JobKey  [SystemProps]" | tee -a ${LOG}
    exit 1
else
    DP_CONFIG=$1
    shift
    JOBKEY=$1
    shift
    SYSPROPS=$*
fi

#
#  Verify and Source the data provider configuration file.
#
if [ ! -r ${DP_CONFIG} ]
then
    echo "Cannot read configuration file: ${DP_CONFIG}" | tee -a ${LOG}
    exit 1
fi
. ${DP_CONFIG}

#
#  Establish the configuration file name.
#
ASSOCLOAD_CONFIG=`pwd`/AssocLoad.config

#
#  Verify and Source the association load configuration file.
#  Must be sourced *after* any configuration file that sets CLASSPATH.
#
if [ ! -r ${ASSOCLOAD_CONFIG} ]
then
    echo "Cannot read configuration file: ${ASSOCLOAD_CONFIG}" | tee -a ${LOG}
    exit 1
fi
. ${ASSOCLOAD_CONFIG}

#
#  Source the common DLA functions script.
#
if [ "${DLAJOBSTREAMFUNC}" != "" ]
then
    if [ -r ${DLAJOBSTREAMFUNC} ]
    then
        . ${DLAJOBSTREAMFUNC}
    else
        echo "Cannot source DLA functions script: ${DLAJOBSTREAMFUNC}" | tee -a ${LOG}
        exit 1
    fi
else
    echo "Environment variable DLAJOBSTREAMFUNC has not been defined." | tee -a ${LOG}
    exit 1
fi

#
# Set and verify the master configuration file name
#
CONFIG_MASTER=${MGICONFIG}/master.config.sh
if [ ! -r ${CONFIG_MASTER} ]
then
    echo "Cannot read configuration file: ${CONFIG_MASTER}" | tee -a ${LOG}
    exit 1
fi

#
#  Write the configuration information to the diagnostic log.
#
getConfigEnv -e >> ${LOG_DIAG}

#
#  Run the association loader.
#
echo ""
echo "`date`" >> ${LOG_PROC}
echo "Run the association loader application" >> ${LOG_PROC}
${JAVA} ${JAVARUNTIMEOPTS} -classpath ${CLASSPATH} \
        -DCONFIG=${CONFIG_MASTER},${DP_CONFIG},${ASSOCLOAD_CONFIG} \
        -DJOBKEY=${JOBKEY} ${SYSPROPS} ${DLA_START}
STAT=$?
if [ ${STAT} -ne 0 ]
then
    echo "Association loader application failed.  Return status: ${STAT}" >> ${LOG_PROC}
    exit 1
fi
echo "Association loader application completed successfully" >> ${LOG_PROC}

#
#  Generate the association loader QC reports.
#
echo "${ASSOCLOADER_QCRPT} ${RPTDIR} ${RADAR_DBSERVER} ${RADAR_DBNAME} ${MGD_DBNAME} ${JOBKEY}"
echo ""
echo "`date`" >> ${LOG_PROC}
echo "Generate the association loader QC reports" >> ${LOG_PROC}
${ASSOCLOADER_QCRPT} ${RPTDIR} ${RADAR_DBSERVER} radar ${MGD_DBNAME} ${JOBKEY} >> ${LOG_DIAG}
STAT=$?
if [ ${STAT} -ne 0 ]
then
    echo "QC reports failed.  Return status: ${STAT}" >> ${LOG_PROC}
    exit 1
fi
echo "QC reports completed successfully" >> ${LOG_PROC}

exit 0

