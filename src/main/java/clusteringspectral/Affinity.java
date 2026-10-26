package clusteringspectral;

import java.util.Arrays;
import java.util.Objects;

import smile.math.matrix.Matrix;

// Matrica slicnosti W za spektralno klasteriranje (tezine grafa slicnosti):
//   W[i][j] = exp(-s(x_i, x_j) / (2 sigma^2))  za i != j,
//   W[i][i] = 0.
//
// s je udaljenost na "kvadratnoj" skali: SQ_EUCLIDEAN za numericke
// podatke, HAMMING za kategoricke i mixed (k-prototypes) za mijesane.
// Hamming se ne kvadrira jer se vec ponasa kao kvadrat euklidske
// udaljenosti (kod one-hot kodiranja svako neslaganje doprinosi 2
// kvadratu euklidske udaljenosti). Za SQ_EUCLIDEAN je ovo tocno
// Gaussova jezgra koju koristi Smile u fit(double[][], k, sigma).
//
// Dijagonala mora ostati 0: Smile u fit(Matrix W, k) normalizira samo
// elemente izvan dijagonale, pa bi W[i][i] != 0 ostao nenormaliziran
// i pokvario rezultat.
final class Affinity {
    private Affinity() {}

    static Matrix gaussian(double[][] X, Distance distance, double sigma) {
        Objects.requireNonNull(distance, "distance must not be null");
        // !(sigma > 0) hvata i NaN.
        if (!(sigma > 0) || !Double.isFinite(sigma))
            throw new IllegalArgumentException("sigma must be finite and > 0, got " + sigma);

        int n = X.length;
        // Isti izraz kao u Smileu, pa za SQ_EUCLIDEAN dobijemo
        // bit po bit istu matricu kao Smile.
        double gamma = -0.5 / (sigma * sigma);

        // Nova matrica je puna nula, pa je dijagonala vec 0.
        Matrix W = new Matrix(n, n);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < i; j++) {
                double w = Math.exp(gamma * distance.dist(X[i], X[j]));
                W.set(i, j, w);
                W.set(j, i, w);
            }
        }
        return W;
    }

    // Zadani sigma: korijen medijana od s po svim parovima i < j.
    // Za numericke podatke (s = SQ_EUCLIDEAN) to je medijan euklidskih
    // udaljenosti, pa par s medijanskom udaljenoscu dobije tezinu exp(-1/2).
    // Memorija je n(n-1)/2 doubleova, npr. oko 100 MB za n = 5000.
    static double medianSigma(double[][] X, Distance distance) {
        Objects.requireNonNull(distance, "distance must not be null");
        int n = X.length;
        if (n < 2)
            throw new IllegalArgumentException("need at least 2 points, got " + n);
        long m = (long) n * (n - 1) / 2;
        if (m > Integer.MAX_VALUE - 8)
            throw new IllegalArgumentException("too many points for median sigma: n = " + n);

        double[] s = new double[(int) m];
        int p = 0;
        for (int i = 0; i < n; i++)
            for (int j = 0; j < i; j++)
                s[p++] = distance.dist(X[i], X[j]);

        Arrays.sort(s);
        int h = s.length / 2;
        double med = (s.length % 2 == 1) ? s[h] : 0.5 * (s[h - 1] + s[h]);

        // Npr. kategoricki podaci u kojima je vise od pola parova identicno.
        if (!(med > 0))
            throw new IllegalArgumentException(
                    "median distance is 0 (more than half of the pairs are identical), set sigma manually");
        return Math.sqrt(med);
    }
}