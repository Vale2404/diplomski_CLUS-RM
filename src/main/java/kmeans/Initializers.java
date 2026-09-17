package kmeans;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public final class Initializers {
    private Initializers() {}

    public static final class Forgy implements Initializer {
        @Override
        public double[][] initialCentroids(double[][] X, int k, Distance distance, Random rnd) {
            int n = X.length;
            if (k > n)
                throw new IllegalArgumentException("k > n");
            Set<Integer> chosen = new HashSet<>();
            double[][] C = new double[k][];
            for (int j = 0; j < k; j++) {
                int idx;
                do {
                    idx = rnd.nextInt(n);
                } while (!chosen.add(idx));
                C[j] = X[idx].clone();
            }
            return C;
        }
        @Override
        public String name() {
            return "Forgy";
        }
    }
}