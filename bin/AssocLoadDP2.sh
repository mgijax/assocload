#!/bin/sh
#
#  AssocLoadDP2.sh
###########################################################################
#
#  Purpose:  This script is a wrapper that is used when the association
#            loader needs to handle the loading of the MGI_Association
#            table from an association input file as the first step in
#            its processing.
#
#  Usage:
#
#      AssocLoadDP2.sh config file [config_file2 ... config_fileN]
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
LOG=`pwd`/AssocLoad.log
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
# Verify and source the common configuration file.
#

COMMON_CONFIG=`pwd`/common.config.sh

if [ ! -r ${COMMON_CONFIG} ]
then
    echo "Cannot read configuration file: ${COMMON_CONFIG}" | tee -a ${LOG}
    exit 1
fi

. ${COMMON_CONFIG}

#
# Verify and source the command line config files.
#

config_files="${COMMON_CONFIG}"
for config in $@
do
    if [ ! -r ${config} ]
    then
        echo "Cannot read configuration file: ${config}" | tee -a ${LOG}
        exit 1
    fi
    config_files="${config_files},${config}"
    . ${config}
done

#
# Verify and source the Association Loader config file.
#

ASSOCLOAD_CONFIG=`pwd`/AssocLoad.config

if [ ! -r ${ASSOCLOAD_CONFIG} ]
then
    echo "Cannot read configuration file: ${ASSOCLOAD_CONFIG}" | tee -a ${LOG}
    exit 1
fi

. ${ASSOCLOAD_CONFIG}

config_files="${config_files},${ASSOCLOAD_CONFIG}"
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
#  Write the configuration information to the diagnostic log.
#
getConfigEnv -e >> ${LOG_DIAG}

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
#  Perform post-load tasks.
#
postload

exit 0

###########################################################################
#
# Warranty Disclaimer and Copyright Notice
#
#  THE JACKSON LABORATORY MAKES NO REPRESENTATION ABOUT THE SUITABILITY OR
#  ACCURACY OF THIS SOFTWARE OR DATA FOR ANY PURPOSE, AND MAKES NO WARRANTIES,
#  EITHER EXPRESS OR IMPLIED, INCLUDING MERCHANTABILITY AND FITNESS FOR A
#  PARTICULAR PURPOSE OR THAT THE USE OF THIS SOFTWARE OR DATA WILL NOT
#  INFRINGE ANY THIRD PARTY PATENTS, COPYRIGHTS, TRADEMARKS, OR OTHER RIGHTS.
#  THE SOFTWARE AND DATA ARE PROVIDED "AS IS".
#
#  This software and data are provided to enhance knowledge and encourage
#  progress in the scientific community and are to be used only for research
#  and educational purposes.  Any reproduction or use for commercial purpose
#  is prohibited without the prior express written permission of The Jackson
#  Laboratory.
#
# Copyright \251 1996, 1999, 2002, 2005 by The Jackson Laboratory
#
# All Rights Reserved
#
###########################################################################
