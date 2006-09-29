#!/bin/sh
#
#  AssocLoadDP.sh
###########################################################################
#
#  Purpose:  This script is a wrapper that is used when the association
#            loader needs to handle the loading of the MGI_Association
#            table from an association input file as the first step in
#            its processing.
#
#  Usage:
#
#      AssocLoadDP.sh  DP.config
#
#  Env Vars:
#
#      See the configuration file
#
#  Inputs:
#
#      - Common configuration file (common.config.sh)
#      - Data provider configuration file (DP.config.[data provider name])
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
#      3) Calls the Association Loader shell script.
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
LOG=`pwd`/AssocLoad.log
rm -f ${LOG}

#
#  Verify the argument(s) to the shell script.
#
if [ $# -ne 1 ]
then
    echo "Usage: $0  DP.config" | tee -a ${LOG}
    exit 1
fi

#
#  Establish the configuration file names.
#
CONFIG_MASTER=${MGICONFIG}/master.config.sh
DP_CONFIG=$1

#
#  Make sure the configuration files are readable.
#
if [ ! -r ${CONFIG_MASTER} ]
then
    echo "Cannot read configuration file: ${CONFIG_MASTER}" | tee -a ${LOG}
    exit 1
fi
if [ ! -r ${DP_CONFIG} ]
then
    echo "Cannot read configuration file: ${DP_CONFIG}" | tee -a ${LOG}
    exit 1
fi

#
#  Source the common configuration file.
#
. ${CONFIG_MASTER}

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
#  Source the data provider configuration file.
#
. ${DP_CONFIG}

#
#  Create any required directories that don't already exist.
#
for i in ${FILEDIR} ${ARCHIVEDIR} ${LOGDIR} ${RPTDIR} ${OUTPUTDIR}
do
    if [ ! -d ${i} ]
    then
        mkdir -p ${i}
        if [ $? -ne 0 ]
        then
              echo "Cannot create directory: ${i}" | tee -a ${LOG}
              exit 1
        fi

        chmod -f 755 ${i}
    fi
done

#
#  Perform pre-load tasks.
#
preload

#
#  Call the association loader wrapper.
#
echo "\n`date`" >> ${LOG_PROC}
echo "Call the association loader wrapper" >> ${LOG_PROC}
${ASSOCLOADER_SH} ${DP_CONFIG} ${JOBKEY} >> ${LOG_DIAG}
STAT=$?
if [ ${STAT} -ne 0 ]
then
    postload
    exit 1
fi

#
#  Perform post-load tasks.
#
postload

exit 0

