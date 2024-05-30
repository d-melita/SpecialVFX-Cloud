package pt.ulisboa.tecnico.cnv.middleware.estimator;

import com.sun.net.httpserver.HttpExchange;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.LUDecomposition;
import java.net.URI;

/*
 * Estimator that uses only offline computed predictions for requests
 */
public class OnlineBasedEstimator implements Estimator {

    // slope for blur
    private double alpha = 0.0;

    // nominator blur
    private double nblur = 0.0;

    // denominator blur
    private double dblur = 0.0;

    // slope for enhance
    private double beta = 0.0;

    // nominator enhance
    private double nenhance = 0.0;

    // denominator enhance
    private double denhance = 0.0;

    // update rate
    private double XI = 0.9;

    // last K wcols
    private ArrayList<Double> wcolsList = new ArrayList<>();

    // last K wrows
    private ArrayList<Double> wrowsList = new ArrayList<>();

    // last K times for raytracer
    private ArrayList<Double> raytimes = new ArrayList<>();
    
    // coeficient for wcols
    private double gama1 = 0.0;

    // coeficient for wrows
    private double gama2 = 0.0;

    // number of records stored for raytracer multi-linear regression
    private int K = 100;

    public OnlineBasedEstimator() {
    }

    public long estimateBlur(HttpExchange exchange) {
        long bodySize = Long.parseLong(exchange.getRequestHeaders().getFirst("Content-Length"));
        System.out.println("Estimating Blur: BodySize = " + bodySize + ", Alpha = " + alpha + ", Estimate = " + this.alpha * bodySize);
        return (long) (this.alpha * bodySize);
    }

    public long estimateEnhance(HttpExchange exchange) {
        long bodySize = Long.parseLong(exchange.getRequestHeaders().getFirst("Content-Length"));
        System.out.println("Estimating Enhance: BodySize = " + bodySize + ", Beta = " + beta + ", Estimate = " + this.beta * bodySize);
        return (long) (this.beta * bodySize);
    }

    public long estimateRayTracer(HttpExchange exchange) {
        URI requestedUri = exchange.getRequestURI();
        String query = requestedUri.getRawQuery();
        Map<String, String> parameters = queryToMap(query);
        int wcols = Integer.parseInt(parameters.get("wcols"));
        int wrows = Integer.parseInt(parameters.get("wrows"));

        double estimate = wcols * gama1 + wrows * gama2;
        System.out.println("Estimating RayTracer with wcols: " + wcols + ", wrows: " + wrows + " -> estimate: " + estimate);
        return (long) estimate;
    }

    public void updateBlur(HttpExchange exchange, long time) {
        long bodySize = Long.parseLong(exchange.getRequestHeaders().getFirst("Content-Length"));
        this.nblur = this.nblur * XI + (1 - XI) * (time * bodySize);
        this.dblur = this.dblur * XI + (1 - XI) * (bodySize * bodySize);
        this.alpha = this.nblur / this.dblur;
        System.out.println("Updating Blur: BodySize = " + bodySize + ", Time = " + time + ", NBlur = " + nblur + ", DBlur = " + dblur + ", Alpha = " + alpha);
    }

    public void updateEnhance(HttpExchange exchange, long time) {
        long bodySize = Long.parseLong(exchange.getRequestHeaders().getFirst("Content-Length"));
        this.nenhance = this.nenhance * XI + (1 - XI) * (time * bodySize);
        this.denhance = this.denhance * XI + (1 - XI) * (bodySize * bodySize);
        this.beta = this.nenhance / this.denhance;
        System.out.println("Updating Enhance: BodySize = " + bodySize + ", Time = " + time + ", NEnhance = " + nenhance + ", DEnhance = " + denhance + ", Beta = " + beta);
    }

    public void updateGamas() {
        // let X = [ wrows wcols ], Y = [ raytimes ].
        // Compute (X^T * X)^-1 * X^T * Y
    
        // K by 2
        double[][] X = { this.wcolsList.stream().mapToDouble(Double::doubleValue).toArray(),
                         this.wrowsList.stream().mapToDouble(Double::doubleValue).toArray() };

        // K by 1
        double[][] Y = { this.raytimes.stream().mapToDouble(Double::doubleValue).toArray() };

        double[][] gamas = 
            multiply(
                    multiply(
                        inverse(multiply(transpose(X), X)),
                                transpose(X)),
                    Y);

        this.gama1 = gamas[0][0];
        this.gama2 = gamas[0][1];
        System.out.printf("gamas updated: gama1=%f, gama2=%f\n", gama1, gama2);
    }

    public void updateRayTracer(HttpExchange exchange, long time) {
        try {
            URI requestedUri = exchange.getRequestURI();
            String query = requestedUri.getRawQuery();
            Map<String, String> parameters = queryToMap(query);
            int wcols = Integer.parseInt(parameters.get("wcols"));
            int wrows = Integer.parseInt(parameters.get("wrows"));

            // add one, keep size up to K
            wcolsList.add((double) wcols);
            wrowsList.add((double) wrows);
            raytimes.add((double) time);
            System.out.printf("Updated lists: wcols=%d, wrows=%d, time=%d\n", wcols, wrows, time);

            if (wcolsList.size() > this.K) {
                this.wcolsList = (ArrayList<Double>) wcolsList.subList(1, this.K+1);
                this.wrowsList = (ArrayList<Double>) wrowsList.subList(1, this.K+1);
                this.raytimes = (ArrayList<Double>) raytimes.subList(1, this.K+1);
            }

            // TODO : to avoid overload, don't update all the time
            updateGamas();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }

        System.out.println("Updating RayTracer: No implementation");
    }

    public long estimate(HttpExchange exchange) {
        String action = exchange.getRequestURI().toString().split("\\?")[0].substring(1);
        System.out.println("Estimating action: " + action);
        if (action.equals("raytracer")) {
            return estimateRayTracer(exchange);  
        } else if (action.equals("blurimage")) {
            return estimateBlur(exchange);  
        } else if (action.equals("enhanceimage")) {
            return estimateEnhance(exchange);  
        } else {
            throw new RuntimeException("unknown action");
        }
    }

    public void updateInfo(HttpExchange exchange, long time) {
        String action = exchange.getRequestURI().toString().split("\\?")[0].substring(1);
        System.out.println("Updating info for action: " + action + " with time: " + time);
        if (action.equals("raytracer")) {
            updateRayTracer(exchange, time);  
        } else if (action.equals("blurimage")) {
            updateBlur(exchange, time);  
        } else if (action.equals("enhanceimage")) {
            updateEnhance(exchange, time);  
        } else {
            throw new RuntimeException("unknown action");
        }
    }

    // Method to calculate the transpose of a matrix
    public static double[][] transpose(double[][] matrix) {
        int rows = matrix.length;
        int cols = matrix[0].length;
        double[][] transposed = new double[cols][rows];
        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < rows; j++) {
                transposed[i][j] = matrix[j][i];
            }
        }
        return transposed;
    }

    // Method to calculate the product of two matrices
    public static double[][] multiply(double[][] firstMatrix, double[][] secondMatrix) {
        int firstRows = firstMatrix.length;
        int firstCols = firstMatrix[0].length;
        int secondCols = secondMatrix[0].length;

        double[][] product = new double[firstRows][secondCols];

        for (int i = 0; i < firstRows; i++) {
            for (int j = 0; j < secondCols; j++) {
                for (int k = 0; k < firstCols; k++) {
                    product[i][j] += firstMatrix[i][k] * secondMatrix[k][j];
                }
            }
        }

        return product;
    }

    public static double[][] inverse(double[][] data) {
        RealMatrix matrix = new Array2DRowRealMatrix(data);

        // Calculating the inverse using LU decomposition
        RealMatrix inverse = new LUDecomposition(matrix).getSolver().getInverse();
        return inverse.getData();
    }

    public Map<String, String> queryToMap(String query) {
        if (query == null) {
            return null;
        }
        Map<String, String> result = new HashMap<>();
        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            if (entry.length > 1) {
                result.put(entry[0], entry[1]);
            } else {
                result.put(entry[0], "");
            }
        }
        return result;
    }
}
