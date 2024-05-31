#! /usr/bin/env bash

source ./config.sh

# Define variables
GROUP_NAME=$CNV_SECURITY_GROUP
DESCRIPTION="Security Group for inbound TCP on port 8000"
VPC_ID=$(aws ec2 describe-vpcs --query 'Vpcs[0].VpcId' --output text)

# Check if the VPC ID was retrieved successfully
if [ -z "$VPC_ID" ]; then
  echo "No VPC found. Exiting..."
  exit 1
fi

# Create the security group
SECURITY_GROUP_ID=$(aws ec2 create-security-group --group-name "$GROUP_NAME" --description "$DESCRIPTION" --vpc-id "$VPC_ID" --query 'GroupId' --output text)

# Check if the security group was created successfully
if [ -z "$SECURITY_GROUP_ID" ]; then
  echo "Failed to create security group. Exiting..."
  exit 1
fi

echo "$SECURITY_GROUP_ID" > security-group.id

echo "Security group '$GROUP_NAME' created with ID: $SECURITY_GROUP_ID"

# Add inbound rule to allow TCP on port 8000 from anywhere
aws ec2 authorize-security-group-ingress --group-id "$SECURITY_GROUP_ID" --protocol tcp --port 8000 --cidr 0.0.0.0/0
echo "Added TCP port 8000 rule."

# Add inbound rule to allow SSH access
aws ec2 authorize-security-group-ingress --group-id "$SECURITY_GROUP_ID" --protocol tcp --port 22 --cidr 0.0.0.0/0
echo "Added SSH access rule."

# Add inbound rule to allow HTTP access
aws ec2 authorize-security-group-ingress --group-id "$SECURITY_GROUP_ID" --protocol tcp --port 80 --cidr 0.0.0.0/0
echo "Added HTTP access rule."

# Add inbound rule to allow HTTPS access
aws ec2 authorize-security-group-ingress --group-id "$SECURITY_GROUP_ID" --protocol tcp --port 443 --cidr 0.0.0.0/0
echo "Added HTTPS access rule."
echo "Inbound rules added to security group '$GROUP_NAME'."

# Create and register key pair
KEY_PAIR_NAME=$CNV_KEY_PAIR

# Check if the key pair already exists
EXISTING_KEY_PAIR=$(aws ec2 describe-key-pairs --key-names "$KEY_PAIR_NAME" --query 'KeyPairs[0].KeyName' --output text 2> /dev/null)

if [ "$EXISTING_KEY_PAIR" == "$KEY_PAIR_NAME" ]; then
  # If key pair exists, delete it
  echo "Deleting existing key pair $KEY_PAIR_NAME..."
  aws ec2 delete-key-pair --key-name "$KEY_PAIR_NAME"
fi

# Create a new key pair and save the private key
echo "Creating new key pair $KEY_PAIR_NAME..."
rm -f "${KEY_PAIR_NAME}.pem" || true
aws ec2 create-key-pair --key-name "$KEY_PAIR_NAME" --query 'KeyMaterial' --output text > "${KEY_PAIR_NAME}.pem"

# Set the correct permissions for the private key file
chmod 400 "${KEY_PAIR_NAME}.pem"

echo "Key pair created and saved as ${KEY_PAIR_NAME}.pem"
