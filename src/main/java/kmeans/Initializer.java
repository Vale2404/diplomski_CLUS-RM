package kmeans;

import java.util.Random;

public interface Initializer {
    double[][] initialCentroids(double[][] X, int k, Distance distance, Random rnd);
    String name();
}