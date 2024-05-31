package pt.ulisboa.tecnico.cnv.middleware.policies;

import com.amazonaws.services.ec2.model.Instance;

import pt.ulisboa.tecnico.cnv.middleware.Utils.Pair;
import pt.ulisboa.tecnico.cnv.middleware.Worker;
import pt.ulisboa.tecnico.cnv.middleware.metrics.AggregateWorkerMetrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 */
public class TimeInstRatioBasedScaling implements ASPolicy {
    // expected time/inst ratio
    private double expected;

    // margin for variance against expected
    private double MARGIN = 1;

    // number of slots we look back
    private int K = 10;

    private List<Double> ratioHistory = new ArrayList<>();

    private List<Double> cpuHistory = new ArrayList<>();

    private double lowThreshold;
    private double highThreshold;

    public TimeInstRatioBasedScaling(double lowThreshold, double highThreshold) {
        this.lowThreshold = lowThreshold;
        this.highThreshold = highThreshold;
    }

    // measures how much the ratio is increasing (by computing the slope of
    // the linear regression
    public double measureRatioIncrease(List<Double> history) {
        if (history.size() < K) return 0;

        int n = history.size();

        // sum of i
        double sumX = 0;
        // sum of ratio[i]
        double sumY = 0;
        // sum of i * ratio[i]
        double sumXY = 0;
        // sum of i * i
        double sumX2 = 0;

        for (int i = 0; i < n; i++) {
            double x = i;
            double y = history.get(i);

            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }

        double xMean = sumX / n;
        double yMean = sumY / n;

        double numerator = sumXY - (sumX * yMean);
        double denominator = sumX2 - (sumX * xMean);

        System.out.printf("Numerator: %f, denominator: %f\n", numerator, denominator);
         if (denominator == 0) {
            System.out.println("Denominator is zero, can't compute slope. Defaulting to 0");
            return 0;
        }

        return numerator/denominator;
    }

    // executed for a slot
    public ScalingDecision evaluate(Map<Worker, Optional<AggregateWorkerMetrics>> metrics) {
        long nonEmptyValues = metrics.entrySet().stream()
            .map(p -> p.getValue())
            .filter(p -> p.isPresent())
            .count();

        if (nonEmptyValues == 0) {
            System.out.printf("There's no information to do scaling\n");
            return ScalingDecision.DontChange;
        }

        // get cpu average
        double cpu = metrics.entrySet().stream()
            .map(p -> p.getValue())
            .filter(p -> p.isPresent())
            .mapToDouble(m -> m.get().getCpuUsage())
            .average().getAsDouble();

        System.out.printf("The average CPU ratio was %f\n", cpu);

        // get average time/instruction ratio
        // if there's nothing, the load is low and ratio is take to be 0
        double ratio = metrics.entrySet().stream()
            .map(p -> p.getValue())
            .filter(p -> p.isPresent())
            .flatMap(m -> m.get().getMetrics().stream())
            .mapToDouble(m -> ((double)m.getDuration()) / m.getRawData().get("ninsts"))
            .average().orElse(0.0);

        System.out.printf("The average time/insts ratio was %f\n", ratio);

        this.cpuHistory.add(cpu);
        this.ratioHistory.add(ratio);

        if (this.cpuHistory.size() > K) {
            this.cpuHistory = this.cpuHistory.subList(1, K+1);
            this.ratioHistory = this.ratioHistory.subList(1, K+1);
        }

        System.out.printf("History lists have %d elements\n", this.cpuHistory.size());

        // find ratio trend
        double ratioSlope = measureRatioIncrease(this.ratioHistory);
        System.out.printf("The slope found for ratios is %f\n", ratioSlope);

        // to increase, high cpu usage and increasing ratio
        // if cpu usage is high, but ratio is stable/decreasing, there's nothing to do
        
        // to decrease, low cpu usage

        if (average > this.highThreshold && ratioSlope > 0 ) {
            return ScalingDecision.Increase;
        }

        if (average < lowThreshold && metrics.size() > 1) {
            return ScalingDecision.Reduce;
        }

        return ScalingDecision.DontChange;
    }
}
