#! /usr/bin/env bash

source ./config.sh

# install java
cmd="sudo yum update -y; sudo yum install java-11-amazon-corretto.x86_64 -y;"
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat instance.dns) $cmd

# install maven (can't use yum, because the version is too old)
mvn_url="https://dlcdn.apache.org/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz"
cmd="wget $mvn_url && tar xfv apache-maven-3.9.6-bin.tar.gz && rm apache-maven-3.9.6-bin.tar.gz && echo 'PATH=\$PATH:\$HOME/apache-maven-3.9.6/bin' >> ~/.bashrc && echo 'Done installing maven'"
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat instance.dns) $cmd

# create dir for source code
TARGET_DIR='~/vfx-studio'
cmd="mkdir $TARGET_DIR"
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat instance.dns) $cmd

# copy source code
pushd ..; mvn clean; popd
echo "Copying files to virtual machine"
rsync -r -e "ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH" $DIR/../../project ec2-user@$(cat instance.dns):$TARGET_DIR || exit 1

# compile and install server
cmd="cd $TARGET_DIR/project && MAVEN_OPTS='-Xmx512m' mvn clean package"
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat instance.dns) $cmd || exit 1

# setup web server to start on instance launch
java_cmd="cd /home/ec2-user/vfx-studio/project/webserver; ./run"
cmd="echo \"$java_cmd &\" | sudo tee -a /etc/rc.local; sudo chmod +x /etc/rc.local"
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat instance.dns) $cmd

# setup systemd service
cmd="sudo systemctl enable --now rc-local"
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat instance.dns) $cmd
