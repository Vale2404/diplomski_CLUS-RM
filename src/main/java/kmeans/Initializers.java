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

    public static final class KmeansPlusPlus implements Initializer {
        @Override
        public double[][] initialCentroids(double[][] X, int k, Distance distance, Random rnd) {
            int n = X.length;
            if (k > n)
                throw new IllegalArgumentException("k > n");
            double[][] C = new double[k][];

            // 1. centroid biramo nasumicno.
            int firstIdx = rnd.nextInt(n);
            C[0] =  X[firstIdx].clone();

            // minDist[j] je udaljenost tocke j do najblizeg
            // dosad odabranog centroida. Nakon 1. centroida,
            // to je tivijalno, za svaku tocku je to udaljenost
            // bas do tog jedinog centroida.
            double[] minDist = new double[n];
            for (int i = 0; i < n; i++) {
                minDist[i] = distance.dist(X[i], C[0]);
            }

            // Glavna petlja, biramo sljedeci centroid.
            for (int j = 1; j < k; j++) {

                // Zbrajamo sve minDist vrijednosti, to je ukupna
                // "tezina" svih tocaka, treba nam kao normalizacijska
                // konstanta za vjerojatnost.
                double sum = 0.0;
                for (int i = 0; i < n; i++) {
                    sum += minDist[i];
                }

                int chosen;

                // Degenerirani slucaj.
                if (sum <= 0.0) {
                    chosen = rnd.nextInt(n);
                } else {
                    // Tocka s vecim minDist (dalje od svih
                    // do sad odabranih centroida ima vecu
                    // sansu da bude odabrana kao sljedeci.
                    double r = rnd.nextDouble() * sum;
                    double cum = 0.0;
                    chosen = n - 1;
                    for (int i = 0; i < n; i++) {
                        cum += minDist[i];
                        if (cum >= r) {
                            chosen = i;
                            break;
                        }
                    }
                }
                // Odabrana tocka postaje novi centroid.
                C[j] = X[chosen].clone();

                // Azuriramo minDist novim centroidom.
                for (int i = 0; i < n; i++) {
                    double d = distance.dist(X[i], C[j]);
                    if (d < minDist[i]) {
                        minDist[i] = d;
                    }
                }
            }
            return C;
        }

        @Override
        public String name() {
            return "Kmeans++";
        }
    }
}