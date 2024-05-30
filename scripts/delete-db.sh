#!/usr/bin/env bash

# Delete table if already exists
echo "Deleting any pre-existing table with the same name"
aws dynamodb delete-table \
  --region $AWS_DEFAULT_REGION \
  --table-name $DYNAMO_DB_TABLE_NAME 2>> /dev/null

echo "Waiting until table is deleted"
aws dynamodb wait table-not-exists \
  --region $AWS_DEFAULT_REGION \
  --table-name $DYNAMO_DB_TABLE_NAME

echo "Table deleted successfully"
