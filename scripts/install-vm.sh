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
rsync -e "ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH" $DIR/../pom.xml ec2-user@$(cat instance.dns):$TARGET_DIR || exit 1
rsync -r -e "ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH" $DIR/../raytracer ec2-user@$(cat instance.dns):$TARGET_DIR || exit 1
rsync -r -e "ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH" $DIR/../imageproc ec2-user@$(cat instance.dns):$TARGET_DIR || exit 1
rsync -r -e "ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH" $DIR/../javassist ec2-user@$(cat instance.dns):$TARGET_DIR || exit 1
rsync -r -e "ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH" $DIR/../webserver ec2-user@$(cat instance.dns):$TARGET_DIR || exit 1

# compile and install server
cmd="cd $TARGET_DIR && mvn clean package"
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat instance.dns) $cmd || exit 1

# setup web server to start on instance launch
java_cmd="java -cp target/webserver-1.0.0-SNAPSHOT-jar-with-dependencies.jar pt.ulisboa.tecnico.cnv.webserver.WebServer"
cmd="echo \"$java_cmd &\" | sudo tee -a /etc/rc.local; sudo chmod +x /etc/rc.local"
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat instance.dns) $cmd

# TODO: don't I have to enable the rc-local service ?
