#! /usr/bin/env bash

source ./config.sh

# delete scaling policies
echo "Deleting scale out policy"
aws autoscaling delete-policy \
	--auto-scaling-group-name $CNV_AS_NAME \
	--policy-name $CNV_OUT_POLICY_NAME || exit 1

echo "Deleting scale in policy"
aws autoscaling delete-policy \
	--auto-scaling-group-name $CNV_AS_NAME \
	--policy-name $CNV_IN_POLICY_NAME || exit 1

# delete auto scaling group
echo "Deleting auto-scaling group"
aws autoscaling delete-auto-scaling-group \
	--auto-scaling-group-name $CNV_AS_NAME \
	--force-delete || exit 1

# delete launch configuration
echo "Deleting launch configuration"
aws autoscaling delete-launch-configuration \
	--launch-configuration-name $CNV_LAUNCH_CONFIG_NAME || exit 1

# delete load balancer
echo "Deleting load balancer"
aws elb delete-load-balancer \
	--load-balancer-name $CNV_LB_NAME || exit 1

# delete cloudwatch
echo "Deleting high cpu usage alarm"
aws cloudwatch delete-alarms \
	--alarm-name $CNV_HIGH_CPU_ALARM_NAME || exit 1

echo "Deleting low cpu usage alarm"
aws cloudwatch delete-alarms \
	--alarm-name $CNV_LOW_CPU_ALARM_NAME || exit 1
