#!/usr/bin/env bash

aws lambda delete-function --function-name raytracer-lambda
aws lambda delete-function --function-name enhanceimage-lambda
aws lambda delete-function --function-name blurimage-lambda

aws iam detach-role-policy \
	--role-name lambda-role \
	--policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole

aws iam delete-role --role-name lambda-role
