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

    // slope for raytracer
    private double gamma = 0.0;

    // nominator raytracer
    private double nraytracer = 0.0;

    // denominator raytracer
    private double draytracer = 0.0;

    // update rate
    private double XI = 0.9;

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
        int area = wcols * wrows;

        long estimate = (long) (this.gamma * area);
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

    public void updateRayTracer(HttpExchange exchange, long time) {
        try {
            URI requestedUri = exchange.getRequestURI();
            String query = requestedUri.getRawQuery();
            Map<String, String> parameters = queryToMap(query);
            int wcols = Integer.parseInt(parameters.get("wcols"));
            int wrows = Integer.parseInt(parameters.get("wrows"));
            long area = wcols * wrows;
            this.nraytracer = this.nraytracer * XI + (1 - XI) * (((double) time) * ((double) area));
            this.draytracer = this.draytracer * XI + (1 - XI) * (((double) area) * ((double) area));
            this.gamma = this.nraytracer / this.draytracer;
            System.out.println("Updating RayTracer: Area = " + area + ", Time = " + time + ", NRaytracer = " + nraytracer + ", DRaytracer = " + draytracer + ", Gamma = " + gamma);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
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
        int secondRows = secondMatrix.length;
        int secondCols = secondMatrix[0].length;

        System.out.printf("multipling A (%d x %d) by B (%d x %d)\n", firstRows, firstCols, secondRows, secondCols);

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
