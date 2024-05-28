#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

export AWS_DEFAULT_REGION=us-east-1
export AWS_DEFAULT_AZ=us-east-1a
export AWS_ACCOUNT_ID=
export AWS_ACCESS_KEY_ID=
export AWS_SECRET_ACCESS_KEY=
export AWS_EC2_SSH_KEYPAR_PATH=/home/dsa/work/ist/year4/cloud-computing-and-virtualization/labs/lab2-aws/dsa-lab2-keypair.pem
export AWS_SECURITY_GROUP=launch-wizard-1
export AWS_KEYPAIR_NAME=dsa-lab2-keypair
export DYNAMO_DB_TABLE_NAME=cnv-proj-table

export CNV_IMAGE_NAME=cnv-proj-image
export CNV_LB_NAME=cnv-proj-lb
export CNV_LAUNCH_CONFIG_NAME=cnv-proj-launch-config
export CNV_AS_NAME=cnv-proj-autoscaling-group
export CNV_OUT_POLICY_NAME=cnv-proj-scale-out-policy
export CNV_HIGH_CPU_ALARM_NAME=cnv-proj-high-cpu-alarm
export CNV_IN_POLICY_NAME=cnv-proj-scale-in-policy
export CNV_LOW_CPU_ALARM_NAME=cnv-proj-low-cpu-alarm
