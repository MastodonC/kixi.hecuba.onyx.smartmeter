#!/usr/bin/env bash

MASTER=$1
NUMBER=$2
# using deployment service sebastopol
EVENTLOGPORT=$3

sed -e "s/@@NUMBER@@/$NUMBER/g" -e "s/@@EVENTLOGPORT@@/$EVENTLOGPORT/" nginx.json.template > nginx-$NUMBER.json

# we want curl to output something we can use to indicate success/failure

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST http://$MASTER/marathon/v2/apps -H "Content-Type: application/json" --data-binary "@nginx-$NUMBER.json")
echo "HTTP code " $STATUS
if [ $STATUS == "201" ]
then exit 0
else exit 1
fi
