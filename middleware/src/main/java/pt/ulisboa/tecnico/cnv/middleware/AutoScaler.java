package pt.ulisboa.tecnico.cnv.middleware;

import java.util.Map;

import pt.ulisboa.tecnico.cnv.middleware.ASPolicy;
import pt.ulisboa.tecnico.cnv.middleware.ScalingDecision;

import com.amazonaws.services.ec2.model.Instance;

public class AutoScaler {

    private static final String AWS_KEYPAIR_NAME = System.getenv("AWS_KEYPAIR_NAME");
    private static final String AWS_SECURITY_GROUP = System.getenv("AWS_SECURITY_GROUP");
    private static final String AWS_AMI_ID = System.getenv("AWS_AMI_ID");

    private ASPolicy policy;
    private AmazonEC2 ec2;

    AWSDashboard awsDashboard;
    Thread daemon;

    private static final int TIMER = 10000; // 10 seconds

    public AutoScaler(AWSDashboard awsDashboard, ASPolicy policy) {
        this.awsDashboard = awsDashboard;
        this.policy = policy;

        this.ec2 = AmazonEC2ClientBuilder.standard()
            .withCredentials(new EnvironmentVariableCredentialsProvider())
            .withRegion(AWS_REGION)
            .build();

        this.createInstance();
    }

    public void run() {
        while (true) {
            try {
                Thread.sleep(TIMER);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            this.update();
        }
    }

    private void update() {
        // get all cpu usage, if average is above 75% create a new instance if below 25% terminate one
        Map<Instance, InstanceMetrics> metrics = this.awsDashboard.getMetrics();
        switch (policy.evaluate(metrics)) {
            // TODO: fill this in
            case value:
                
                break;

            default:
                break;
        }

    }

    public void createInstance() {
        RunInstancesRequest runInstancesRequest = new RunInstancesRequest()
            .withImageId(AWS_AMI_ID)
            .withInstanceType("t2.micro")
            .withMinCount(1)
            .withMaxCount(1)
            .withKeyName(AWS_KEYPAIR_NAME)
            .withSecurityGroups(AWS_SECURITY_GROUP);

        RunInstancesResult runInstancesResult = this.ec2.runInstances(runInstancesRequest);
        String reservationId = runInstancesResult.getReservation().getReservationId();

        List<Instance> newInstances = runInstancesResult.getReservation().getInstances();

        if (newInstances.size() != 1) {
            throw new RuntimeException("Failed to create instances.");
        }

        Instance instance = newInstances.get(0);

        // wait until the instances are running
        DescriptInstancesRequest describeRequest = new DescribeInstancesRequest()
                            .withFilters(new Filter()
                                    .withName("reservation-id")
                                    .withValues(reservationId));

        Reservation reservation;
        while (!instance.getState().getName().equals("running")) {
            reservation = ec2.describeInstances(describeRequest)
                    .getthis.awsDashboard.getMetrics.iterator().next();Reservations().get(0);

            instance = reservation.getInstances().get(0);
            System.out.printf("The current state is %s\n", instance.getState().getName());

            System.out.printf("Waiting for instance to spawn for %d seconds\n",
                    QUERY_COOLDOWN / 1000);
            try {
                Thread.sleep(QUERY_COOLDOWN);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        this.awsDashboard.registerInstance(instance);
    }

    // terminate instance
    // FIXME: change the name to forceTerminateInstance
    public void terminateInstance() {
        // get the instance to terminate
        // FIXME: this might not work
        Optional<Instance> optInstance = this.awsDashboard.getMetrics().stream().findFirst();

        if (optInstance.isEmpty()) {
            throw new RuntimeException("No instances to terminate.");
        }

        Instance instance = optInstance.get();

        // FIXME: we shouldn't terminate when there's only one running
        if (instance == null) {
            throw new RuntimeException("No instances to terminate.");
        }

        // terminate the instance
        TerminateInstancesRequest termInstanceReq = new TerminateInstancesRequest();
        termInstanceReq.withInstanceIds(instance.getInstanceId());
        TerminateInstancesResult terminateResult = this.ec2.terminateInstances(termInstanceReq);
        // TODO: check result
        this.awsDashboard.unregisterInstance(instance);
    }

    public void start() {
        daemon = new Thread(this); 
        daemon.start();
    }

    public void waitFor() {
        // TODO
    }
}
