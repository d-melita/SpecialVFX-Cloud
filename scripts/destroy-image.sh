#! /usr/bin/env bash

source ./config.sh

# delete the AMI and snapshot
aws ec2 deregister-image --image-id $(cat image.id) || exit 1
aws ec2 delete-snapshot --snapshot-id $(aws ec2 describe-snapshots --owner-ids self | jq -r ".Snapshots[0].SnapshotId") || exit 1
