#!/usr/bin/env bash

source config.sh

# launch a vm instance
$DIR/launch-vm.sh || exit 1

# install software in the vm instance
$DIR/install-vm.sh || exit 1

# create AIM
aws ec2 create-image --instance-id $(cat instance.id) --name $CNV_IMAGE_NAME | jq -r .ImageId > image.id || exit 1
echo "New VM image with id $(cat image.id)."

# wait for image to become available.
echo "Waiting for image to be ready..."
aws ec2 wait image-available --filters Name=name,Values=$CNV_IMAGE_NAME || exit 1
echo "Waiting for image to be ready... done! \o/"

# terminate the vm instance
aws ec2 terminate-instances --instance-ids $(cat instance.id) || exit 1
