#!/usr/bin/env bash

aws lambda delete-function --function-name raytracer
aws lambda delete-function --function-name enhanceimage 
aws lambda delete-function --function-name blurimage

aws iam detach-role-policy \
	--role-name lambda-role \
	--policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole

aws iam delete-role --role-name lambda-role
