#! /usr/bin/env bash

source ./config.sh

# stop all instances
aws ec2 terminate-instances --instance-ids $(aws ec2 describe-instances | jq -r ".Reservations[].Instances[0].InstanceId") || exit 1

./deregister-lambda.sh
./delete-db.sh
