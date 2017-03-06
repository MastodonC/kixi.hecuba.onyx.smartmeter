#!/usr/bin/env bash

MASTER=$1
# using deployment service sebastopol
TAG=$2
EVENTLOGPORT=$3
NUMBER=00

sed -e "s/@@TAG@@/$TAG/" -e "s/@@NUMBER@@/$NUMBER/" -e "s/@@EVENTLOGPORT@@/$EVENTLOGPORT/" kixi-eventlog.json.template > kixi-travelport-eventlog-$NUMBER.json

# we want curl to output something we can use to indicate success/failure

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST http://$MASTER/marathon/v2/apps -H "Content-Type: application/json" --data-binary "@kixi-travelport-eventlog-$NUMBER.json")
echo "HTTP code " $STATUS
if [ $STATUS == "201" ]
then exit 0
else exit 1
fi
