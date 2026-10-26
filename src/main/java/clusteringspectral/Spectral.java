package clusteringspectral;

import java.util.Objects;

import smile.clustering.SpectralClustering;
import smile.math.MathEx;
import smile.math.matrix.Matrix;

// Spektralno klasteriranje (Ng, Jordan, Weiss 2001) preko biblioteke Smile 2.6.0.
//
// Mi gradimo samo matricu slicnosti W (Affinity), a Smile radi ostalo:
// normalizaciju D^(-1/2) W D^(-1/2), k vodecih svojstvenih vektora
// (ARPACK), normalizaciju redaka na jedinicnu duljinu i k-means na retcima.
//
// Za razliku od DBSCAN-a i HDBSCAN-a nema suma: svaka tocka dobije oznaku
// 0..k-1, a broj klastera k zadajemo unaprijed.
//
// Tvornice numeric, categorical i mixed zadaju udaljenost na kvadratnoj
// skali koja ulazi u Gaussovu jezgru (vidi Affinity) i provjeru podataka,
// kao kMeans, kModes i kPrototypes u LloydClustering.
//
// sigma > 0 je sirina Gaussove jezgre. sigma = 0 znaci da se racuna
// automatski kao korijen medijana udaljenosti (Affinity.medianSigma).
public class Spectral {
    private final int k;
    private final double sigma;
    private final Distance distance;

    // Isto znacenje kao u Dbscan: samo provjera kategorickih kodova.
    private final boolean[] categorical;
    private final boolean allCategorical;

    // k-means u spektralnom prostoru, iste vrijednosti kao Smileove zadane.
    private static final int MAX_ITER = 100;
    private static final double TOL = 1E-4;

    public Spectral(int k, double sigma, Distance distance) {
        this(k, sigma, distance, null, false);
    }

    private Spectral(int k, double sigma, Distance distance,
                     boolean[] categorical, boolean allCategorical) {
        if (k < 2)
            throw new IllegalArgumentException("k must be >= 2, got " + k);
        // !(sigma >= 0) hvata i NaN.
        if (!(sigma >= 0) || !Double.isFinite(sigma))
            throw new IllegalArgumentException("sigma must be finite and >= 0 (0 = median), got " + sigma);
        this.k = k;
        this.sigma = sigma;
        this.distance = Objects.requireNonNull(distance, "distance must not be null");
        this.categorical = categorical;
        this.allCategorical = allCategorical;
    }

    // Numericki podaci. U jezgru ulazi KVADRAT euklidske udaljenosti,
    // pa je ovo tocno Gaussova jezgra iz Smilea.
    public static Spectral numeric(int k, double sigma) {
        return new Spectral(k, sigma, Distance.SQ_EUCLIDEAN);
    }

    // Kategoricki podaci, Hammingova udaljenost (bez kvadriranja).
    public static Spectral categorical(int k, double sigma) {
        return new Spectral(k, sigma, Distance.HAMMING, null, true);
    }

    // Mijesani podaci, ista udaljenost kao k-prototypes. Numericke stupce
    // prije toga treba standardizirati. Maska se kopira.
    public static Spectral mixed(int k, double sigma, boolean[] categorical, double gamma) {
        if (categorical == null)
            throw new IllegalArgumentException("categorical cannot be null");
        return new Spectral(k, sigma, Distance.mixed(categorical, gamma),
                categorical.clone(), false);
    }

    // Bez seeda koristimo fiksni seed 1, da je rezultat uvijek ponovljiv.
    public Result fit(double[][] X) {
        return fit(X, 1L);
    }

    public Result fit(double[][] X, long seed) {
        validateData(X);
        int n = X.length;
        // ARPACK trazi strogo manje svojstvenih vektora od dimenzije matrice.
        if (k >= n)
            throw new IllegalArgumentException("k = " + k + " must be < n = " + n);

        if (allCategorical)
            Categorical.domains(X);
        else if (categorical != null)
            Categorical.domains(X, categorical);

        double s = sigma > 0 ? sigma : Affinity.medianSigma(X, distance);
        Matrix W = Affinity.gaussian(X, distance, s);

        // Smileov k-means nasumicno bira pocetne centre (k-means++),
        // pa seed postavljamo radi ponovljivosti.
        MathEx.setSeed(seed);
        SpectralClustering model = SpectralClustering.fit(W, k, MAX_ITER, TOL);
        return new Result(model.y, k, s, model.distortion);
    }

    public static final class Result {
        // Oznake 0..numClusters-1, nema suma.
        public final int[] labels;
        public final int numClusters;
        // Stvarno koristeni sigma (zadani ili izracunati medijan).
        public final double sigma;
        // Distorzija k-meansa u spektralnom prostoru.
        public final double distortion;

        Result(int[] labels, int numClusters, double sigma, double distortion) {
            this.labels = labels;
            this.numClusters = numClusters;
            this.sigma = sigma;
            this.distortion = distortion;
        }

        @Override
        public String toString() {
            return String.format("Spektralno: klastera=%d, sigma=%.4f, distorzija=%.4f",
                    numClusters, sigma, distortion);
        }
    }

    // Ista provjera kao Dbscan.validateData.
    static void validateData(double[][] X) {
        if (X == null || X.length == 0)
            throw new IllegalArgumentException("X must not be null or empty");
        if (X[0] == null || X[0].length == 0)
            throw new IllegalArgumentException("X[0] must not be null or empty");
        int d = X[0].length;
        for (int i = 0; i < X.length; i++) {
            if (X[i] == null || X[i].length != d)
                throw new IllegalArgumentException("X[" + i + "].length != d");
            for (int t = 0; t < d; t++)
                if (!Double.isFinite(X[i][t]))
                    throw new IllegalArgumentException("X[" + i + "][" + t + "] is not finite");
        }
    }
}