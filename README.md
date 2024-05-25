# SpecialVFX@Cloud

This project contains five sub-projects:

1. `raytracer` - the Ray Tracing workload
2. `imageproc` - the BlurImage and EnhanceImage workloads
3. `webserver` - the web server exposing the functionality of the workloads
4. `middleware` - bundles the AutoScaler and Load Balancer
4. `javassist` - classes required for implementing Javassist

Refer to the `README.md` files of the sub-projects to get more details about each specific sub-project.

## How to build everything

1. Make sure your `JAVA_HOME` environment variable is set to Java 11+ distribution
2. Run `mvn clean package`

## How to run

To run the SpecialVFX@Cloud using AWS first it is necessary to set the AWS Environment Variables in the `scripts/config.sh` file.

On the `scripts` directory there are a lot of scripts that can be used to setup and run the system.

To create an AMI run
```sh
./scripts/create-image.sh
```

After the image is created, run the following to launch the deployment.

```sh
./scripts/launch-deployment.sh
```

This script launches the Auto Scaler and Load Balancer, which will spawn the required workers.


To free the resources, one can run the following.
```sh
./scripts/terminate-deployment.sh
```

To delete the AMI and associated snapshot, run the following

```sh
./scripts/destroy-image.sh
```

In the `scripts` directory there's also a `load.sh` script that can be used to load the system with requests (some example
loads are available in the `loads` folder).

## System Architecture

There are two deployments that are provided:

1. One that uses a custom Java AS/LB implementation - this can be found in the `master` branch
2. One that uses AWS's AutoScaler and LB - this can be found in the `aws` branch

### Custom Java deployment

In the custom Java deployment, the AutoScaler and Load Balancer run side-by-side
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

### AWS's automated deployment

> **Note**: to test this version, run `git checkout aws`

The AWS's automated deployment is simpler and follows the structure discussed in class,
using AWS-provided infrastructure.

AWS was configured in the following way:

#### AutoScaler

The AutoScaler has been configured with the following specifications:

- **Health Check Grace Period**: 60 seconds
- **Minimum Size**: 1 instance
- **Maximum Size**: 3 instances

#### LoadBalancer

The LoadBalancer is configured with the following health check parameters:

- **Protocol**: HTTP
- **Port**: 8000
- **Path**: /test
- **Interval**: 30 seconds
- **Unhealthy Threshold**: 2
- **Healthy Threshold**: 10
- **Timeout**: 5 seconds

#### CloudWatch

Two policies and alarms were created for scaling operations: one for scaling out and another for scaling in.

##### Scale Out

**Policy**:
- **Step Adjustments**: Increase of one worker at a time (i.e. MetricIntervalLowerBound=0.0, ScalingAdjustment=1)
- **Metric Aggregation**: Average

**Alarm**:
- **Metric**: CPUUtilization
- **Statistic**: Average
- **Period**: 60 seconds
- **Evaluation Periods**: 2
- **Threshold**: > 70%

##### Scale In

**Policy**:
- **Step Adjustments**: Decrease of one worker at a time (i.e. MetricIntervalUpperBound=0.0, ScalingAdjustment=-1)
- **Metric Aggregation**: Average

**Alarm**:
- **Metric**: CPUUtilization
- **Statistic**: Average
- **Period**: 60 seconds
- **Evaluation Periods**: 2
- **Threshold**: < 25%
