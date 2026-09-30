package dbscan;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

// DBSCAN (Density-Based Spatial Clustering of Applications with Noise).

// Za razliku od kmeans-a, DBSCAN ne trazi unaprijed zadani broj klastera k,
// nego klastere gradi iz gustoce tocaka: klaster je maksimalan skup
// "gusto povezanih" tocaka, a tocke koje ne pripadaju nijednom takvom
// skupu proglasavaju se sumom (noise, oznaka -1).

// Dva parametra definiraju gustocu:
//   eps    - polumjer okoline oko tocke (koliko "blizu" moraju biti dvije
//            tocke da bi se smatrale susjedima)
//   minPts - najmanji broj tocaka (ukljucujuci samu tocku) koje moraju
//            biti u toj okolini da bi tocka bila "jezgrena" (core point)
//
// Vrste tocaka nakon zavrsetka algoritma:
//   jezgrena (core)   - ima barem minPts tocaka u svojoj eps-okolini
//   rubna (border)    - nije jezgrena, ali je u eps-okolini neke jezgrene
//                        tocke pa pripada njezinom klasteru
//   sum (noise)       - nije ni jezgrena ni rubna, ne pripada nijednom klasteru
//
// Implementacija je namjerno jednostavna i citljiva (kao i kmeans): regionQuery
// je naivna, O(n) po upitu, bez prostorne strukture (npr. KD-stabla). Za velike
// skupove podataka to je sporije, ali je lakse za razumjeti i provjeriti tocnost,
// sto je ovdje prioritet.


public class Dbscan {
    private final double eps;
    private final int minPts;
    private final Distance distance;

    // Interne oznake dok algoritam radi. NOISE i UNCLASSIFIED se koriste
    // samo tijekom izvodenja; u konacnom Result.labels ostaje samo NOISE
    // (-1) ili indeks klastera (0, 1, 2, ...). UNCLASSIFIED se nigdje ne
    // vraca korisniku jer na kraju svaka tocka mora biti ili obradjena ili sum.
    private static final int UNCLASSIFIED = -2;
    private static final int NOISE = -1;

    public Dbscan(double eps, int minPts, Distance distance) {
        if (eps <= 0)
            throw new IllegalArgumentException("eps must be > 0");
        if (minPts <= 0)
            throw new IllegalArgumentException("minPts must be > 0");
        this.eps = eps;
        this.minPts = minPts;
        this.distance = distance;
    }

    public Dbscan(double eps, int minPts) {
        this(eps, minPts, Distance.EUCLIDEAN);
    }

    // Glavna metoda. Prima matricu X dimenzija n*d (n tocaka, svaka d
    // koordinata) i vraca Result sa oznakama klastera za svaku tocku.
    // Za razliku od Kmeans.fit, ovdje nema potrebe za generatorom
    // slucajnih brojeva - DBSCAN je deterministican s obzirom na ulazne
    // podatke i parametre (jedina "nasumicnost" je redoslijed obilaska
    // tocaka, koji ovdje uvijek ide 0..n-1, pa je rezultat u potpunosti
    // reproducibilan).
    public Result fit(double[][] X) {
        validateData(X);
        int n = X.length;

        int[] labels = new int[n];
        java.util.Arrays.fill(labels, UNCLASSIFIED);
        boolean[] core = new boolean[n];

        int clusterId = 0;

        // Prolazimo kroz sve tocke redom. Svaka tocka koja jos nije
        // obradjena (UNCLASSIFIED) postaje kandidat za pocetak novog
        // klastera.
        for (int i = 0; i < n; i++) {
            if (labels[i] != UNCLASSIFIED)
                continue;

            List<Integer> neighbors = regionQuery(X, i);

            // Tocka i nema dovoljno susjeda da bude jezgrena. Privremeno
            // je oznacavamo kao sum - "privremeno" jer kasnije, dok
            // sirimo neki drugi klaster, ova tocka moze ispasti u
            // eps-okolini neke jezgrene tocke i tada postaje rubna
            // tocka tog klastera (vidi expandCluster).
            if (neighbors.size() < minPts) {
                labels[i] = NOISE;
                continue;
            }

            // Tocka i ima >= minPts susjeda (ukljucujuci samu sebe, jer
            // regionQuery vraca i tocku i), dakle jezgrena je i pocinje
            // novi klaster koji zatim sirimo kroz gusto povezane susjede.
            core[i] = true;
            labels[i] = clusterId;
            expandCluster(X, labels, core, neighbors, clusterId);
            clusterId++;
        }

        int numNoise = 0;
        for (int label : labels) {
            if (label == NOISE)
                numNoise++;
        }

        return new Result(labels, clusterId, numNoise);
    }



    // Sirenje klastera clusterId iz pocetnog skupa susjeda seedNeighbors
    // (susjeda jezgrene tocke koja je zapocela klaster). Koristimo red
    // (BFS) umjesto rekurzije jer se za velike/guste klastere rekurzija
    // moze prevaliti (stack overflow) - ovo je iterativna
    // varijanta istog postupka.
    private void expandCluster(double[][] X, int[] labels, boolean[] core,
                               List<Integer> seedNeighbors, int clusterId) {

        Deque<Integer> queue = new ArrayDeque<>(seedNeighbors);

        while (!queue.isEmpty()) {
            int q = queue.poll();
            if (labels[q] == NOISE) {
                // q je ranije (u glavnoj petlji fit) proglasena sumom jer
                // sama nije imala dovoljno susjeda, ali je sada dosegnuta
                // iz jezgrene tocke ovog klastera - dakle rubna je tocka
                // ovog klastera, ne sum.
                labels[q] = clusterId;
            }
            if (labels[q] != UNCLASSIFIED) {
                // q je vec dio nekog klastera (ovog ili je vec obradjena
                // gore preko NOISE grane) - preskacemo je. Ovo takoder
                // sprjecava da istu tocku dvaput sirimo dalje.
                continue;
            }
            labels[q] = clusterId;

            List<Integer> qNeighbors = regionQuery(X, q);
            if (qNeighbors.size() >= minPts) {
                // I q je jezgrena tocka pa se klaster siri i kroz njezine
                // susjede - dodajemo ih u red da ih obradimo. Tocke koje
                // su vec klasificirane (bilo u ovaj bilo bi teoretski u
                // neki drugi klaster - sto se ne moze dogoditi jer bi to
                // znacilo da su dva klastera gusto povezana, a tada bi to
                // po definiciji bio jedan klaster) jednostavno ce biti
                // preskocene kad dodju na red gore.
                core[q] = true;
                for (int r : qNeighbors) {
                    if (labels[r] == UNCLASSIFIED || labels[r] == NOISE) {
                        queue.add(r);
                    }
                }
            }
            // Ako q nije jezgrena, ona je samo rubna tocka - pripada
            // klasteru, ali se preko nje dalje ne siri (njezini susjedi
            // se ne dodaju u red).
        }
    }

    // Vraca indekse svih tocaka (ukljucujuci samu idx) cija je udaljenost
    // od X[idx] manja ili jednaka eps. Naivna implementacija: O(n) po
    // pozivu, O(n^2) ukupno za cijeli fit - dovoljno za manje i srednje
    // skupove podataka.
    private List<Integer> regionQuery(double[][] X, int idx) {
        List<Integer> neighbors = new ArrayList<>();
        for (int i = 0; i < X.length; i++) {
            if (distance.dist(X[idx], X[i]) <= eps) {
                neighbors.add(i);
            }
        }
        return neighbors;
    }

    public static final class Result {
        // -1 = noise, inace indeks klastera (0, 1, 2, ...).
        public final int[] labels;
        public final int numClusters;
        public final int numNoise;


        Result(int[] labels,  int numClusters, int numNoise) {
            this.labels = labels;
            this.numClusters = numClusters;
            this.numNoise = numNoise;
        }
        @Override
        public String toString() {
            return String.format("DBSCAN: klastera=%d, sum (noise)=%d/%d tocaka",
                    numClusters, numNoise, labels.length);
        }
    }

    // Metoda koja provjerava jesu li podaci u X ispravni.
    private static void validateData(double[][] X) {
        if (X == null || X.length == 0)
            throw new IllegalArgumentException("X must not be null or empty");
        if (X[0] == null || X[0].length == 0)
            throw new IllegalArgumentException("X[0] must not be null or empty");
        int d = X[0].length;
        for (int i = 1; i < X.length; i++) {
            if (X[i] == null || X[i].length != d)
                throw new IllegalArgumentException("X[" + i + "].length != d");
        }
    }
}