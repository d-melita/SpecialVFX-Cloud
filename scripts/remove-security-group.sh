#! /usr/bin/env bash

source ./config.sh

# Define the security group name
GROUP_NAME=$CNV_SECURITY_GROUP

# Retrieve the security group ID based on the name
SECURITY_GROUP_ID=$(aws ec2 describe-security-groups --filters Name=group-name,Values="$GROUP_NAME" --query 'SecurityGroups[0].GroupId' --output text)

# Check if the security group ID was retrieved
if [ -z "$SECURITY_GROUP_ID" ]; then
  echo "Security group '$GROUP_NAME' not found. Exiting..."
  exit 1
fi

echo "Deleting security group '$GROUP_NAME' with ID: $SECURITY_GROUP_ID"

# Attempt to delete the security group
DELETE_STATUS=$(aws ec2 delete-security-group --group-id "$SECURITY_GROUP_ID")

# Check the status of the delete operation
if [ -z "$DELETE_STATUS" ]; then
  echo "Security group '$GROUP_NAME' deleted successfully."
else
  echo "Failed to delete security group '$GROUP_NAME'. Error: $DELETE_STATUS"
fi

# Remove keypair
echo "Deleting existing key pair $CNV_KEY_PAIR..."
aws ec2 delete-key-pair --key-name "$CNV_KEY_PAIR"
rm -f "${CNV_KEY_PAIR}.pem"
