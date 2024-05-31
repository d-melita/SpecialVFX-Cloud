# SpecialVFX@Cloud

This project contains five sub-projects:

- `common` - common classes
- `raytracer` - the Ray Tracing workload
- `imageproc` - the BlurImage and EnhanceImage workloads
- `webserver` - the web server exposing the functionality of the workloads
- `middleware` - bundles the AutoScaler and Load Balancer
- `javassist` - classes required for implementing Javassist

Refer to the `README.md` files of the sub-projects to get more details about each specific sub-project.

## How to build everything

> **NOTE**: it's assume that the repository is a directory called `project`.

1. Make sure your `JAVA_HOME` environment variable is set to Java 11+ distribution
2. Run `mvn clean package`

## How to run

To run the SpecialVFX@Cloud using AWS first it is necessary to set the AWS Environment Variables in the `scripts/config.sh` file.

On the `scripts` directory there are a lot of scripts that can be used to setup and run the system.
`cd` into the `scripts` directory to run these.

To setup the security groups run

```sh
./setup-security-group.sh
```

To create an AMI run
```sh
./create-image.sh
```

After the image is created, run the following to launch the deployment.

```sh
./launch-deployment.sh
```

This script launches the Auto Scaler and Load Balancer, which will spawn the required workers.


To free the resources, one can run the following.
```sh
./terminate-deployment.sh
```

To delete the AMI and associated snapshot, run the following

```sh
./destroy-image.sh
```

Finally, to delete the security group, use
```sh
./remove-security-group.sh
```

In the `scripts` directory there's also a `load.sh` script that can be used to load the system with requests (some example
loads are available in the `loads` folder).

## System Architecture

The AutoScaler and Load Balancer run side-by-side
as threads for the same process. In this process there are 3 main components:

- the **load balancer**, which redirects HTTP requests from clients to workers.
This decision is based on some load balancing policy (represented by the `LBPolicy`).
A simple implementation is provided. In the second sprint of the project, more
complex policies will be used.

- the **auto scaler**, which is responsible for spawning/stopping running instances.
This decision is based on some auto scaling policy (represented by the `ASPolicy`).
A simple implementation is provided. In the second sprint of the project, more
complex policies will be used.

- the **instance monitor**, which is responsible for retrieving metrics on the
workers. As it stands, the monitor reads in the CPU usage from AWS's Cloud Watch
and uses an HTTP request to the workers themselves to get more fine-grained
information

#### Local testing

To allow for local testing of the infrastructure (i.e. testing that doesn't require
the use of AWS), AWS's main functionalities were abstracted away using interfaces/
abstract classes. To use in AWS, the middleware should be set to run in "production"
(by changing a static variable in the middleware `WebServer`).
