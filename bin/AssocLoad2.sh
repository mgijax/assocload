#!/bin/sh
#
#  AssocLoad2.sh
###########################################################################
#
#  Purpose:  This script controls the execution of the association loader
#	     and accepts mulitple configuration files.
#
#	     It should be used if you are using the association loader to 
#	     load data from the input file defined by the configuration 
#	     variable INFILE_NAME.
#
#  Usage:
#
#      AssocLoad2.sh config file [config_file2 ... config_fileN]
#
#  Env Vars:
#
#      See the configuration file
#
#  Inputs:
#
#      - Common configuration file (common.config.sh)
#      - Data provider configuration files
#      - Association input file
#
#  Outputs:
#
#      - An archive file
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
#      1) Source configuration files to establish the environment.
#
#      2) Perform preload functions.
#
#      3) Calls the Association Loader.
#
#      4) Perform postload functions.
#
#  Notes:  None
#
###########################################################################

#
#  Set up a log file for the shell script in case there is an error
#  during configuration and initialization.
#
cd `dirname $0`/..
LOG=${ASSOCLOAD}/AssocLoad.log
rm -f ${LOG}

#
#  Verify the argument(s) to the shell script.
#
if [ $# -lt 1 ]
then
    echo "Usage: $0 config file [config_file2 ... config_fileN]" | tee -a ${LOG}
    exit 1
fi

#
# Verify and source the Association Loader config file.
# This should be the last config file sent to the association loader.
#

ASSOCLOAD_CONFIG=${ASSOCLOAD}/AssocLoad.config

if [ ! -r ${ASSOCLOAD_CONFIG} ]
then
    echo "Cannot read configuration file: ${ASSOCLOAD_CONFIG}" | tee -a ${LOG}
    exit 1
fi

. ${ASSOCLOAD_CONFIG}

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
# Verify and source the command line config files.
#

config_files=""
for config in $@
do
    if [ ! -r ${config} ]
    then
        echo "Cannot read configuration file: ${config}" | tee -a ${LOG}
        exit 1
    fi
    config_files="${config_files}${config},"
    . ${config}
done

config_files="${config_files}${CONFIG_MASTER},${ASSOCLOAD_CONFIG}"
echo "config_files:${config_files}"

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
#  Perform pre-load tasks.
#
preload

#
#  Run the association loader.
#
echo "\n`date`" >> ${LOG_PROC}
echo "Run the association loader application" >> ${LOG_PROC}
${JAVA} ${JAVARUNTIMEOPTS} -classpath ${CLASSPATH} \
        -DCONFIG=${config_files} \
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
echo "\n`date`" >> ${LOG_PROC}
echo "Generate the association loader QC reports" >> ${LOG_PROC}
${ASSOCLOADER_QCRPT} ${RPTDIR} ${RADAR_DBSERVER} ${RADAR_DBNAME} ${MGD_DBNAME} ${JOBKEY} >> ${LOG_DIAG}
STAT=$?
if [ ${STAT} -ne 0 ]
then
    echo "QC reports failed.  Return status: ${STAT}" >> ${LOG_PROC}
    exit 1
fi
echo "QC reports completed successfully" >> ${LOG_PROC}

#
#  Perform post-load tasks.
#
postload

exit 0

