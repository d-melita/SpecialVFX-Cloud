#!/usrbin/env bash

source config.sh

# Terminate worker instance
aws ec2 terminate-instances --instance-ids $(cat monitoring.id)

