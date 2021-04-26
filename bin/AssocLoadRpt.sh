#!/bin/sh
#
#  AssocLoadRpt.csh
###########################################################################
#
#  Purpose:  This script runs the Association loader QC reports.
#
#  Usage:
#
#      AssocLoadRpt.csh  OutputDir  Server  RADAR  MGD  JobKey
#
#      where
#
#          OutputDir is the directory where the report files are created.
#          Server is the database server to use.
#          RADAR is the name of the RADAR database to use.
#          MGD is the name of the MGD database to use.
#          JobKey is the value that identifies the records in the RADAR
#                 QC report tables that are to be processed.
#
#  Env Vars:
#
#      None
#
#  Inputs:
#
#      - Shell script arguments (See Usage)
#
#  Outputs:
#
#      - An output file created by each QC report.
#
#  Exit Codes:
#
#      0:  Successful completion
#      1:  Fatal error occurred
#      2:  Non-fatal error occurred
#
#  Assumes:  Nothing
#
#  Implementation:  Each python script in the directory is executed to
#                   produce the reports.
#
#  Notes:  None
#
###########################################################################

cd `dirname $0`
. ../AssocLoad.config

#
#  Verify the argument(s) to the shell script.
#
if  [ $# -ne 5 ]
then
    echo "Usage: $0  OutputDir  Server  RADAR  MGD  JobKey"
    exit 1
else
    OUTPUTDIR=$1
    SERVER=$2
    RADAR=$3
    MGD=$4
    JOBKEY=$5
fi

#
#  Run each Python report found in the directory.
#
cd `dirname $0`

for RPT in AssocDiscrepancyRpt.py TargetDiscrepancyRpt.py
do
    ${PYTHON} ${RPT} ${OUTPUTDIR} ${SERVER} ${RADAR} ${MGD} ${JOBKEY}
done

exit 0
