package kmeans;

import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

public class Kmeans {
    private final int k;
    private final int maxIter;
    private final double tol;
    private final Distance distance;
    private final Initializer initializer;

    public Kmeans(int k, int maxIter, double tol, Distance distance, Initializer initializer) {
        if (k <= 0)
            throw new IllegalArgumentException("k must be > 0");
        if (maxIter <= 0)
            throw new IllegalArgumentException("maxIter must be > 0");
        if (tol < 0)
            throw new IllegalArgumentException("tol must be >= 0");
        this.k = k;
        this.maxIter = maxIter;
        this.tol = tol;
        this.distance = distance;
        this.initializer = initializer;
    }

    public Kmeans(int k, Initializer initializer) {
        this(k, 300, 1e-8, Distance.SQ_EUCLIDEAN, initializer);
    }

    // Glavna metoda klase, ona pokrece algoritam i vraca
    // objekt Result u kojem je sve sto je izracunala.
    // Metoda prima matricu X dimenzija n*d (n tocaka, svaka
    // ima d koordinata), generator slucajnih brojeva koji
    // se prosljeduje inicijalizatoru. Ako zadamo isti seed,
    // npr. new Random(42) dobit cemo iste pocetne centroide
    // i isti rezultat.
    public Result fit(double[][] X, Random rnd) {
        // 1. Provjera jesu li podaci ispravni,
        // svi retci iste dimenzije i k <= n.
        Objects.requireNonNull(rnd, "rnd must not be null");
        int d = validateData(X);
        int n = X.length;
        if (k > n)
            throw new IllegalArgumentException("k must be <= n");

        // 2. Zovemo inicijalizator (npr. Forgy) da odabere k pocetnih centroida.
        double[][] C = copyCentroids(initializer.initialCentroids(X, k, distance, rnd), d);

        // Niz koji za svaku tocku iz X pamti kojem klasteru pripada
        int[] labels = new int[n];
        Arrays.fill(labels, -1);

        // 3. U petlji ponavljamo Lloydove korake: pridruzivanje pa
        // azuriranje dok ne konvergira ili ne dosegne maxIter.
        int iter = 0;
        boolean converged = false;
        while (iter < maxIter) {
            iter++;
            boolean changed = assign(X, C, labels);
            double maxShift = update(X, C, labels);
            if (!changed || maxShift <= tol) {
                converged = true;
                break;
            }
        }

        // Zavrsno pridruzivanje.
        assign(X, C, labels);

        return new Result(C, labels, sse(X, C, labels), iter, converged, initializer.name());
    }

    // Korak azuriranja. Oznake su fiksne, a pomicu se centroidi:
    // svaki centroid postaje aritmeticka sredina tocaka svog klastera.
    // Funkcija usput izracuna i maxShift, maksimum pomaka svih centroida,
    // sto fit koristi za odluku o zaustavljanju.
    private double update(double[][] X, double[][] C, int[] labels) {
        int n = X.length;
        int d = X[0].length;

        // Vektor zbroja za svaki klaster i brojac tocaka
        // u svakom klasteru. Kada to imamo lako cemo kasnije
        // izracunati sredinu kao sums[j] / counts[j].
        double[][] sums = new double[k][d];
        int[] counts = new int[k];

        for (int i = 0; i < n; i++) {
            int c = labels[i];
            counts[c]++;
            double[] s = sums[c];
            for (int j = 0; j < d; j++) {
                s[j] += X[i][j];
            }
        }

        // Ako je klaster j prazan, counts[j] == 0, pa bi
        // sums[j] / counts[j] dalo 0.0 / 0 = NaN. Takav centroid
        // bi pokvario sve daljnje udaljenosti. Zato prazan klaster
        // treba dobiti barem jednu tocku.
        for (int j = 0; j < k; j++) {
            if (counts[j] > 0)
                continue;

            // Trazimo tocku koja je najudaljenija od svog trenutnog
            // centroida, ali samo medu tockama iz klastera s barem
            // dvije tocke. Taj uvjet je vazan jer da uzmemo jedinu
            // tocku nekog klastera, samo bismo premjestili problem
            // jer bi taj klaster ostao prazan.
            //Ovdje se koriste stari centroidi C, jer novi jos nisu
            // izracunati. Tocka daleko od svog centroida je "najlosije
            // objasnjena" tocka, pa je razumno od nje napraviti novi klaster.
            // Budući da vrijedi k ≤ n, ako je neki klaster prazan, po Dirichletovom
            // principu mora postojati klaster s barem dvije tocke, pa far nikad ne ostane -1.
            int far = -1;
            double farDist = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < n; i++) {
                int c =  labels[i];
                if (counts[c] <= 1)
                    continue;
                double dist = distance.dist(X[i], C[c]);
                if (dist > farDist) {
                    farDist = dist;
                    far = i;
                }
            }

            // Sada tu tocku premjestimo: iz klastera donora je oduzmemo
            // (smanjimo brojac i oduzmemo njezine koordinate iz zbroja),
            // promijenimo joj oznaku i postavimo je kao jedinu tocku klastera j.
            // Zbroj klastera j je tada upravo ta tocka, pa ce novi centroid biti
            // tocno na njoj. Ako je praznih klastera vise, isti postupak se
            // ponavlja. Ista tocka ne moze biti odabrana dvaput, jer njezin novi
            // klaster ima counts[j] == 1, pa je iskljucena uvjetom counts[c] <= 1.
            int donor = labels[far];
            counts[donor]--;
            for (int i = 0; i < d; i++) {
                sums[donor][i] -= X[far][i];
            }
            labels[far] = j;
            counts[j] = 1;
            System.arraycopy(X[far], 0, sums[j], 0, d);
        }

        // Sada vise nema praznih klastera, sada za svaki klaster
        // izracunamo sredinu po koordinatama. Prije nego stari
        // centroid zamijenimo novim, izmjerimo koliko se pomaknuo
        // i pamtimo najveci takav pomak. Na kraju C[j] pokazuje na
        // novi centroid, a funkcija vraca maxShift.
        double maxShift = 0.0;
        for (int j = 0; j < k; j++) {
            double[] newC = new double[d];
            for (int t = 0; t < d; t++) {
                newC[t] = sums[j][t] / counts[j];
            }
            maxShift = Math.max(maxShift, Distance.SQ_EUCLIDEAN.dist(C[j], newC));
            C[j] = newC;
        }
        return maxShift;
    }

    // Korak pridruzivanja. Vraca true ako se ijedna oznaka promijenila.
    private boolean assign(double[][] X, double[][] C, int[] labels) {
        boolean changed = false;

        // Vanjska petlja kroz sve tocke, jednu po jednu neovisno,
        // i za svaku trazi najblizi centroid.
        for (int i = 0; i < X.length; i++) {
            int best = 0;
            double bestDist = distance.dist(X[i], C[0]);
            for (int j = 1; j < C.length; j++) {
                double dist = distance.dist(X[i], C[j]);
                if (dist < bestDist) {
                    best = j;
                    bestDist = dist;
                }
            }
            if (labels[i] != best) {
                labels[i] = best;
                changed = true;
            }
        }
        return changed;
    }

    // Metoda racuna sumu kvadrata euklidskih udaljenosti
    // tocaka od njihovih centroida.
    private static double sse(double[][] X, double[][] C, int[] labels) {
        double s = 0.0;
        for (int i = 0; i < X.length; i++) {
            s += Distance.SQ_EUCLIDEAN.dist(X[i], C[labels[i]]);
        }
        return s;
    }

    // Metoda koja provjerava jesu li podaci u X ispravni.
    private static int validateData(double[][] X) {
        if (X == null || X.length == 0)
            throw new IllegalArgumentException("X must not be null or empty");
        if (X[0] == null || X[0].length == 0)
            throw new IllegalArgumentException("X[0] must not be null or empty");
        int d = X[0].length;
        for (int i = 1; i < X.length; i++) {
            if (X[i] == null || X[i].length != d)
                throw new IllegalArgumentException("X[" + i + "].length != d");
        }
        return d;
    }

    // Metoda koja provjerava sto je inicijalizator vratio
    // i kopira te centroide u novi niz.
    private double[][] copyCentroids(double[][] C0, int d) {
        if (C0 == null || C0.length != k)
            throw new IllegalArgumentException("initializer must return k centroids");
        double[][] C = new double[k][];
        for (int j = 0; j < k; j++) {
            if (C0[j] == null ||  C0[j].length != d)
                throw new IllegalArgumentException("centroid " + j + " dimension must be d");
            C[j] = C0[j].clone();
        }
        return C;
    }

    public static final class Result {
        public final double[][] centroids;
        public final int[] labels;
        public final double sse;
        public final int iterations;
        public final boolean converged;
        public final String initializerName;

        Result(double[][] centroids, int[] labels, double sse,
               int iterations, boolean converged, String initializerName) {
            this.centroids = centroids;
            this.labels = labels;
            this.sse = sse;
            this.iterations = iterations;
            this.converged = converged;
            this.initializerName = initializerName;
        }
        @Override
        public String toString() {
            return String.format("%s: SSE=%.6f, iteracije=%d, konvergirao=%b",
                    initializerName, sse,  iterations, converged);
        }
    }
}