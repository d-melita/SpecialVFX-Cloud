#! /usr/bin/env bash

source ./config.sh

echo "Launching Load Balancer and Auto Scaler..."

# run new instance
aws ec2 run-instances \
	--image-id resolve:ssm:/aws/service/ami-amazon-linux-latest/amzn2-ami-hvm-x86_64-gp2 \
	--instance-type t2.micro \
	--key-name $AWS_KEYPAIR_NAME \
	--security-group-ids $AWS_SECURITY_GROUP \
	--tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=Coordinator}]" \
	--monitoring Enabled=true | jq -r ".Instances[0].InstanceId" > monitoring.id || exit 1

echo "New instance with id $(cat monitoring.id)."

# wait for instance to be running
aws ec2 wait instance-running --instance-ids $(cat monitoring.id)
echo "New instance with id $(cat monitoring.id) is now running."

# extract DNS name
aws ec2 describe-instances \
	--instance-ids $(cat monitoring.id) | jq -r ".Reservations[0].Instances[0].NetworkInterfaces[0].PrivateIpAddresses[0].Association.PublicDnsName" > monitoring.dns
echo "New instance with id $(cat monitoring.id) has address $(cat monitoring.dns)."

# wait for instance to have SSH ready.
while ! nc -z $(cat monitoring.dns) 22; do
	echo "Waiting for $(cat monitoring.dns):22 (SSH)..."
	sleep 0.5
done
echo "New instance with id $(cat monitoring.id) is ready for SSH access."

# install java
cmd="sudo yum update -y && sudo yum install java-11-amazon-corretto.x86_64 -y;"
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat monitoring.dns) $cmd || exit 1

pushd ..; mvn clean package || exit 1; popd

# copy files required for running
jar_file=middleware-1.0.0-SNAPSHOT-jar-with-dependencies.jar
scp -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ../middleware/target/$jar_file ec2-user@$(cat monitoring.dns):~
scp -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ../middleware/launchInstance.sh ec2-user@$(cat monitoring.dns):~
scp -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ../middleware/run ec2-user@$(cat monitoring.dns):~

# start load balances / auto scaler
cmd="cd ~ && AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID AWS_SECRET_KEY=$AWS_SECRET_ACCESS_KEY AWS_REGION=$AWS_REGION AWS_KEYPAIR_NAME=$AWS_KEYPAIR_NAME AWS_SECURITY_GROUP=$AWS_SECURITY_GROUP AWS_AMI_ID=$(cat image.id) DYNAMO_DB_TABLE_NAME=$DYNAMO_DB_TABLE_NAME ./run"
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat monitoring.dns) $cmd

./create-db.sh
./register-lambda.sh
