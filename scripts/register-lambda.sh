#!/usr/bin/env bash

source ./config.sh

# create new role in iam
aws iam create-role \
	--role-name lambda-role \
	--assume-role-policy-document '{"Version": "2012-10-17","Statement": [{ "Effect": "Allow", "Principal": {"Service": "lambda.amazonaws.com"}, "Action": "sts:AssumeRole"}]}'

# attach role to existing policy
aws iam attach-role-policy \
	--role-name lambda-role \
	--policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole

sleep 3

# register raytracer lambda
aws lambda create-function \
	--function-name raytracer \
	--zip-file fileb://../middleware/target/middleware-1.0.0-SNAPSHOT-jar-with-dependencies.jar \
	--handler pt.ulisboa.tecnico.cnv.middleware.RayTracerLambda \
	--runtime java11 \
	--timeout 90 \
	--memory-size 512 \
	--role arn:aws:iam::$AWS_ACCOUNT_ID:role/lambda-role

# register enhance image lambda
aws lambda create-function \
	--function-name enhanceimage \
	--zip-file fileb://../middleware/target/middleware-1.0.0-SNAPSHOT-jar-with-dependencies.jar \
	--handler pt.ulisboa.tecnico.cnv.middleware.EnhanceLambda \
	--runtime java11 \
	--timeout 30 \
	--memory-size 256 \
	--role arn:aws:iam::$AWS_ACCOUNT_ID:role/lambda-role

# register enhance image lambda
aws lambda create-function \
	--function-name blurimage \
	--zip-file fileb://../middleware/target/middleware-1.0.0-SNAPSHOT-jar-with-dependencies.jar \
	--handler pt.ulisboa.tecnico.cnv.middleware.BlurLambda \
	--runtime java11 \
	--timeout 30 \
	--memory-size 256 \
	--role arn:aws:iam::$AWS_ACCOUNT_ID:role/lambda-role
