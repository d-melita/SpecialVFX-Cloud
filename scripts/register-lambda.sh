#!/usr/bin/env bash

# create new role in iam
aws iam create-role \
	--role-name lambda-role \
	--assume-role-policy-document '{"Version": "2012-10-17","Statement": [{ "Effect": "Allow", "Principal": {"Service": "lambda.amazonaws.com"}, "Action": "sts:AssumeRole"}]}'

# attach role to existing policy
aws iam attach-role-policy \
	--role-name lambda-role \
	--policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole

# register raytracer lambda
aws lambda create-function \
	--function-name raytracer-lambda \
	--zip-file fileb://../raytracer/target/raytracer-1.0.0-SNAPSHOT-jar-with-dependencies.jar \
	--handler pt.ulisboa.tecnico.cnv.raytracer.RaytracerHandler \
	--runtime java11 \
	--timeout 5 \
	--memory-size 256 \
	--role arn:aws:iam::$AWS_ACCOUNT_ID:role/lambda-role

# register enhance image lambda
aws lambda create-function \
	--function-name enhanceimage-lambda \
	--zip-file fileb://../imageproc/target/imageproc-1.0.0-SNAPSHOT-jar-with-dependencies.jar \
	--handler pt.ulisboa.tecnico.cnv.imageproc.EnhanceImageHandler \
	--runtime java11 \
	--timeout 5 \
	--memory-size 256 \
	--role arn:aws:iam::$AWS_ACCOUNT_ID:role/lambda-role

# register enhance image lambda
aws lambda create-function \
	--function-name blurimage-lambda \
	--zip-file fileb://../imageproc/target/imageproc-1.0.0-SNAPSHOT-jar-with-dependencies.jar \
	--handler pt.ulisboa.tecnico.cnv.imageproc.BlurImageHandler \
	--runtime java11 \
	--timeout 5 \
	--memory-size 256 \
	--role arn:aws:iam::$AWS_ACCOUNT_ID:role/lambda-role
