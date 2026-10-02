package clusteringpartitional;

import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

public class LloydClustering {
    private final int k;
    private final int maxIter;
    private final Distance distance;
    private final Initializer initializer;
    private final CenterUpdater updater;

    // Opci konstruktor s 5 parametara kojeg pozivaju tvornice.
    public LloydClustering(int k, int maxIter, Distance distance,
                           Initializer initializer, CenterUpdater updater) {
        if (k <= 0)
            throw new IllegalArgumentException("k must be > 0");
        if (maxIter <= 0)
            throw new IllegalArgumentException("maxIter must be > 0");
        this.k = k;
        this.maxIter = maxIter;
        this.distance = Objects.requireNonNull(distance, "distance must not be null");
        this.initializer = Objects.requireNonNull(initializer, "initializer must not be null");
        this.updater = Objects.requireNonNull(updater, "updater must not be null");
    }

    // Tvornicke metode: udaljenost i updater uvijek idu u paru, pa ih je
    // sigurnije zadati na jednom mjestu nego pamtiti pri svakom pozivu.
    public static LloydClustering kMeans(int k, Initializer initializer) {
        return new LloydClustering(k, 300, Distance.SQ_EUCLIDEAN, initializer, new CenterUpdaters.Mean());
    }

    // Svaki poziv stvara novi Mode, pa se stanje updatera
    // ne dijeli izmedu LloydClustering objekata.
    public static LloydClustering kModes(int k, Initializer initializer) {
        return new LloydClustering(k, 300, Distance.HAMMING, initializer, new CenterUpdaters.Mode());
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

        // Priprema updatera (za k-modes) provjera kodova i domene,
        // za k-means prazna funkcija.
        updater.prepare(X);

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

            // Za fiksne centroide C svakoj tocki pridruzujemo
            // najblizi centroid i stavljamo ju u njegov klaster.
            // Vracamo true ako se ijedna oznaka promijenila.
            boolean changed = assign(X, C, labels);

            // Ako se nista nije promijenilo, algoritam je konvergirao.
            if (!changed) {
                converged = true;
                break;
            }

            // Ako je pridruzivanje nekom klasteru ostavilo 0 tocaka,
            // onda medu tockama iz svih klastera s barem 2 tocke uzmemo
            // onu koja je najudaljenija od centroida svog klastera i
            // premjestimo je u prazni klaster.
            fixEmptyClusters(X, C, labels);

            // Azuriramo centroide.
            updater.update(X, C, labels);
        }
        return new Result(C, labels, cost(X, C, labels), iter, converged, initializer.name());
    }

    // Korak pridruzivanja. Vraca true ako se ijedna oznaka promijenila,
    // tj. ako je za barem jednu tocku naden centroid koji je manje
    // udaljen od trenutnog centroida cijem klasteru je ta tocka pridruzena.
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

            // Ako je za tu tocku najblizi centroid koji smo upravo nasli
            // razlicit od stvarnog centroida kojem je ona pridruzena,
            // pridruzimo tu tocku tom centroidu.
            // U slucaju da je pronadeni najbolji centroid jednako
            // udaljen od starog centroida, stari ostaje.
            // U 1. iteraciji tocke jos nemaju klaster pa bi
            // C[-1] bacio iznimku.
            if (labels[i] != -1 && labels[i] != best
                    && distance.dist(X[i], C[labels[i]]) == bestDist) {
                best = labels[i];
            }
            if (labels[i] != best) {
                labels[i] = best;
                changed = true;
            }
        }
        return changed;
    }

    // Funkcija cilja: suma udaljenosti tocaka do centara njihovih klastera,
    // mjerena zadanom udaljenoscu. Za k-means sa SQ_EUCLIDEAN to je SSE,
    // a za k-modes s HAMMING ukupni broj neslaganja u atributima.
    private double cost(double[][] X, double[][] C, int[] labels) {
        double s = 0.0;
        for (int i = 0; i < X.length; i++) {
            s += distance.dist(X[i], C[labels[i]]);
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
        for (int i = 0; i < X.length; i++) {
            if (X[i] == null || X[i].length != d)
                throw new IllegalArgumentException("X[" + i + "].length != d");
            for (int t = 0; t < d; t++)
                if (!Double.isFinite(X[i][t]))
                    throw new IllegalArgumentException("X[" + i + "][" + t + "] is not finite");
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

    // Popravak praznih klastera. Poziva se nakon pridruzivanja, a prije
    // azuriranja centara, tako da updater uvijek dobije oznake u kojima
    // nijedan klaster nije prazan. Radi samo nad oznakama i udaljenostima,
    // pa je isti za k-means i k-modes.
    private void fixEmptyClusters(double[][] X, double[][] C, int[] labels) {
        int n = X.length;
        int[] counts = new int[k];
        for (int i = 0; i < n; i++)
            counts[labels[i]]++;

        for (int j = 0; j < k; j++) {
            if (counts[j] != 0)
                continue;

            // Trazimo tocku koja je najudaljenija od svog trenutnog
            // centroida, ali samo medu tockama iz klastera s barem
            // dvije tocke. Taj uvjet je vazan jer da uzmemo jedinu
            // tocku nekog klastera, samo bismo premjestili problem
            // jer bi taj klaster ostao prazan.
            // Ovdje se koriste stari centroidi C, jer novi jos nisu
            // izracunati. Tocka daleko od svog centroida je "najlosije
            // objasnjena" tocka, pa je razumno od nje napraviti novi klaster.
            // Buduci da vrijedi k <= n, ako je neki klaster prazan, po Dirichletovom
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

            // Tocku premjestimo u klaster j. Updater ce iz oznaka izracunati
            // novi centar, a centar klastera s jednom tockom je upravo ta tocka.
            // Ista tocka ne moze biti odabrana dvaput, jer njezin novi klaster
            // ima counts[j] == 1, pa je iskljucena uvjetom counts[c] <= 1.
            counts[labels[far]]--;
            labels[far] = j;
            counts[j] = 1;
        }
    }

    public static final class Result {
        private final double[][] centroids;
        private final int[] labels;
        public final double cost;
        public final int iterations;
        public final boolean converged;
        public final String initializerName;

        Result(double[][] centroids, int[] labels, double cost,
               int iterations, boolean converged, String initializerName) {
            this.centroids = centroids;
            this.labels = labels;
            this.cost = cost;
            this.iterations = iterations;
            this.converged = converged;
            this.initializerName = initializerName;
        }

        // Vracamo kopije, pa pozivatelj ne moze promijeniti stanje rezultata.
        public int[] labels() {
            return labels.clone();
        }

        // Za 2D niz treba duboka kopija.
        public double[][] centroids() {
            double[][] copy = new double[centroids.length][];
            for (int j = 0; j < centroids.length; j++)
                copy[j] = centroids[j].clone();
            return copy;
        }

        @Override
        public String toString() {
            return String.format("%s: cost=%.6f, iteracije=%d, konvergirao=%b",
                    initializerName, cost,  iterations, converged);
        }
    }
}