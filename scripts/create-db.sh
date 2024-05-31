#!/usr/bin/env bash

# Delete table if already exists
./delete-db.sh

echo "Creating the table"
aws dynamodb create-table \
  --region $AWS_REGION \
  --table-name $DYNAMO_DB_TABLE_NAME \
  --attribute-definition \
    AttributeName=ReplicaID,AttributeType=S \
    AttributeName=SeqNb,AttributeType=N \
  --key-schema \
    AttributeName=ReplicaID,KeyType=HASH \
    AttributeName=SeqNb,KeyType=RANGE \
  --provisioned-throughput \
    ReadCapacityUnits=5,WriteCapacityUnits=1000 \
  --table-class STANDARD 2>&1 | jq .

echo "Waiting until table is available"
aws dynamodb wait table-exists \
  --region $AWS_REGION \
  --table-name $DYNAMO_DB_TABLE_NAME

echo "Table created successfully"
