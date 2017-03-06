#!/usr/bin/env bash

MASTER=$1
NUMBER=$2
TAG=$3

sed -e "s/@@TAG@@/$TAG/" -e "s/@@AWS_ACCESS_KEY_ID@@/$AWS_ACCESS_KEY_ID/" -e "s/@@AWS_SECRET_KEY@@/$AWS_SECRET_KEY/" -e "s/@@NUMBER@@/$NUMBER/" travelport.json.template > travelport-config-$NUMBER.json

# we want curl to output something we can use to indicate success/failure

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST http://$MASTER/marathon/v2/apps -H "Content-Type: application/json" --data-binary "@travelport-config-$NUMBER.json")
echo "HTTP code " $STATUS
if [ $STATUS == "201" ]
then exit 0
else exit 1
fi
