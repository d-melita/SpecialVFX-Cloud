#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

export AWS_REGION=eu-west-1
export AWS_DEFAULT_AZ=eu-west-1a
export AWS_ACCOUNT_ID=
export AWS_ACCESS_KEY_ID=
export AWS_SECRET_ACCESS_KEY=
export AWS_SECURITY_GROUP=cnv-group
export AWS_KEYPAIR_NAME=cnv-keypair
# TODO: think about a better way to wire the path
export AWS_EC2_SSH_KEYPAR_PATH=./$AWS_KEYPAIR_NAME.pem
export DYNAMO_DB_TABLE_NAME=cnvprojtable

export CNV_KEY_PAIR=$AWS_KEYPAIR_NAME
export CNV_SECURITY_GROUP=$AWS_SECURITY_GROUP
export CNV_IMAGE_NAME=cnv-proj-image
export CNV_LB_NAME=cnv-proj-lb
export CNV_LAUNCH_CONFIG_NAME=cnv-proj-launch-config
export CNV_AS_NAME=cnv-proj-autoscaling-group
export CNV_OUT_POLICY_NAME=cnv-proj-scale-out-policy
export CNV_HIGH_CPU_ALARM_NAME=cnv-proj-high-cpu-alarm
export CNV_IN_POLICY_NAME=cnv-proj-scale-in-policy
export CNV_LOW_CPU_ALARM_NAME=cnv-proj-low-cpu-alarm
