#!/usr/bin/env bash

source config.sh

# create load balancer and 
aws elb create-load-balancer \
	--load-balancer-name $CNV_LB_NAME \
	--listeners "Protocol=HTTP,LoadBalancerPort=80,InstanceProtocol=HTTP,InstancePort=8000" \
	--availability-zones $AWS_DEFAULT_AZ

# configure health check
aws elb configure-health-check \
	--load-balancer-name $CNV_LB_NAME \
	--health-check Target=HTTP:8000/test,Interval=30,UnhealthyThreshold=2,HealthyThreshold=10,Timeout=5 \
	--region $AWS_DEFAULT_REGION

# create launch configuration
aws autoscaling create-launch-configuration \
	--launch-configuration-name $CNV_LAUNCH_CONFIG_NAME \
	--image-id $(cat image.id) \
	--instance-type t2.micro \
	--security-groups $AWS_SECURITY_GROUP \
	--key-name $AWS_KEYPAIR_NAME \
	--instance-monitoring Enabled=true \
	--region $AWS_DEFAULT_REGION

# create auto scaling group
aws autoscaling create-auto-scaling-group \
	--auto-scaling-group-name $CNV_AS_NAME \
	--launch-configuration-name $CNV_LAUNCH_CONFIG_NAME \
	--load-balancer-names $CNV_LB_NAME \
	--availability-zones $AWS_DEFAULT_AZ \
	--health-check-type ELB \
	--health-check-grace-period 60 \
	--min-size 1 \
	--max-size 3 \
	--desired-capacity 1 \
	--region $AWS_DEFAULT_REGION

# create a policy for scale out
aws autoscaling put-scaling-policy \
	--auto-scaling-group-name $CNV_AS_NAME \
	--policy-name $CNV_OUT_POLICY_NAME \
	--policy-type StepScaling \
	--adjustment-type ChangeInCapacity \
	--metric-aggregation Average \
	--step-adjustments MetricIntervalLowerBound=0.0,ScalingAdjustment=1 \
	--region $AWS_DEFAULT_REGION | jq -r '.PolicyARN' > policy-out.arn

# create cloud watch alert for over utilization
aws cloudwatch put-metric-alarm \
	--alarm-name $CNV_HIGH_CPU_ALARM_NAME \
	--metric-name CPUUtilization \
	--namespace AWS/EC2 \
	--statistic Average \
	--period 60 \
	--evaluation-periods 2 \
	--threshold 70 \
	--comparison-operator GreaterThanThreshold \
	--dimensions "Name=AutoScalingGroupName,Value=$CNV_AS_NAME" \
	--alarm-actions $(cat policy-out.arn) \
	--region $AWS_DEFAULT_REGION

# create a policy for scale in
aws autoscaling put-scaling-policy \
	--auto-scaling-group-name $CNV_AS_NAME \
	--policy-name $CNV_IN_POLICY_NAME \
	--policy-type StepScaling \
	--adjustment-type ChangeInCapacity \
	--metric-aggregation Average \
	--step-adjustments MetricIntervalUpperBound=0.0,ScalingAdjustment=-1 \
	--region $AWS_DEFAULT_REGION | jq -r '.PolicyARN' > policy-in.arn

# create cloud watch alert for under utilization
aws cloudwatch put-metric-alarm \
	--alarm-name $CNV_LOW_CPU_ALARM_NAME \
	--metric-name CPUUtilization \
	--namespace AWS/EC2 \
	--statistic Average \
	--period 60 \
	--evaluation-periods 2 \
	--threshold 25 \
	--comparison-operator LessThanThreshold \
	--dimensions "Name=AutoScalingGroupName,Value=$CNV_AS_NAME" \
	--alarm-actions $(cat policy-in.arn) \
	--region $AWS_DEFAULT_REGION


