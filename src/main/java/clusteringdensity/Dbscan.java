package clusteringdensity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

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
//
// Algoritam ne ovisi o vrsti podataka: treba mu samo odgovor na pitanje
// "jesu li dvije tocke unutar eps", tj. udaljenost i prag. Zato ista
// implementacija radi i za kategoricke (Hamming) i za mijesane podatke
// (udaljenost iz k-prototypesa). To je ideja generaliziranog DBSCAN-a
// (GDBSCAN, Sander i sur. 1998). Tvornicke metode numeric, categorical
// i mixed zadaju udaljenost i provjeru podataka koje idu zajedno, kao
// kMeans, kModes i kPrototypes u LloydClustering.


public class Dbscan {
    private final double eps;
    private final int minPts;
    private final Distance distance;

    // Koje stupce fit provjerava kao kategoricke kodove (nenegativni cijeli
    // brojevi). allCategorical = true znaci da su svi stupci kategoricki
    // (broj stupaca saznamo tek u fit-u). categorical == null i
    // allCategorical = false znaci da nema kategorickih stupaca.
    // Sam algoritam ovo ne koristi, provjera samo ranije javlja gresku
    // ako podaci ne odgovaraju udaljenosti (npr. 1.5 kao kategorija).
    private final boolean[] categorical;
    private final boolean allCategorical;

    // Interne oznake dok algoritam radi. NOISE i UNCLASSIFIED se koriste
    // samo tijekom izvodenja; u konacnom Result.labels ostaje samo NOISE
    // (-1) ili indeks klastera (0, 1, 2, ...). UNCLASSIFIED se nigdje ne
    // vraca korisniku jer na kraju svaka tocka mora biti ili obradjena ili sum.
    private static final int UNCLASSIFIED = -2;
    private static final int NOISE = -1;

    // Opci konstruktor s proizvoljnom udaljenoscu, bez provjere
    // kategorickih kodova. Tvornice ispod pozivaju privatni konstruktor.
    public Dbscan(double eps, int minPts, Distance distance) {
        this(eps, minPts, distance, null, false);
    }

    // Ostaje radi kompatibilnosti s postojecim kodom, isto kao numeric.
    public Dbscan(double eps, int minPts) {
        this(eps, minPts, Distance.EUCLIDEAN);
    }

    private Dbscan(double eps, int minPts, Distance distance,
                   boolean[] categorical, boolean allCategorical) {
        // !(eps > 0) hvata i NaN, za koji je svaka usporedba false.
        if (!(eps > 0) || !Double.isFinite(eps))
            throw new IllegalArgumentException("eps must be finite and > 0, got " + eps);
        if (minPts <= 0)
            throw new IllegalArgumentException("minPts must be > 0");
        this.eps = eps;
        this.minPts = minPts;
        this.distance = Objects.requireNonNull(distance, "distance must not be null");
        this.categorical = categorical;
        this.allCategorical = allCategorical;
    }

    // Tvornicke metode: udaljenost i provjera podataka uvijek idu u paru,
    // pa ih je sigurnije zadati na jednom mjestu.

    // Numericki podaci, euklidska udaljenost. eps je polumjer okoline u
    // istim jedinicama kao podaci (kao u originalnom DBSCAN-u).
    public static Dbscan numeric(double eps, int minPts) {
        return new Dbscan(eps, minPts, Distance.EUCLIDEAN);
    }

    // Kategoricki podaci, Hammingova udaljenost. eps je najveci broj
    // atributa u kojima se dvije tocke smiju razlikovati da bi bile
    // susjedi. Hamming daje cijele brojeve, pa su eps = 2 i eps = 2.5
    // isti prag.
    public static Dbscan categorical(double eps, int minPts) {
        return new Dbscan(eps, minPts, Distance.HAMMING, null, true);
    }

    // Mijesani podaci, ista udaljenost kao k-prototypes:
    // zbroj kvadrata razlika po numerickim stupcima + gamma * broj
    // kategorickih neslaganja. Numericki dio je KVADRAT euklidske
    // udaljenosti, pa je i eps na toj skali: za masku bez kategorickih
    // stupaca mixed(eps * eps, ...) daje isto sto i numeric(eps, ...).
    // Numericke stupce prije toga treba standardizirati
    // (Preprocessing.standardize), inace stupac s najvecim rasponom
    // odredi sve susjedstvo.
    // Maska se kopira, pa kasnija promjena kod pozivatelja ne utjece na objekt.
    public static Dbscan mixed(double eps, int minPts, boolean[] categorical, double gamma) {
        if (categorical == null)
            throw new IllegalArgumentException("categorical cannot be null");
        return new Dbscan(eps, minPts, Distance.mixed(categorical, gamma),
                categorical.clone(), false);
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

        // Provjera kategorickih kodova (i duljine maske) prije prvog
        // racunanja udaljenosti. Domene same ne trebaju, samo provjera.
        if (allCategorical)
            Categorical.domains(X);
        else if (categorical != null)
            Categorical.domains(X, categorical);

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


    // Sirenje klastera clusterId iz pocetnog skupa susjeda seeds (eps-okolina
    // jezgrene tocke koja je zapocela klaster). Iterativni BFS preko reda
    // umjesto rekurzije, da se kod velikih/gustih klastera izbjegne
    // prelijevanje stoga (stack overflow).
    // Tocka dobiva oznaku
    // klastera u trenutku kad ULAZI u red, a ne kad iz njega izlazi. Time
    // svaka tocka moze uci u red najvise jednom, pa je red velicine O(n)
    // umjesto O(n^2) u najgorem slucaju (kad su sve tocke medjusobno unutar
    // eps, svaka jezgrena tocka bi inace ubacila sve ostale).
    private void expandCluster(double[][] X, int[] labels, boolean[] core,
                               List<Integer> seeds, int clusterId) {

        Deque<Integer> queue = new ArrayDeque<>();

        // Pocetni susjedi jezgrene tocke koja je otvorila klaster. Sama ta
        // tocka je vec oznacena u fit (labels[i] = clusterId), pa je enqueue
        // preskace i ne ubacuje je ponovno.
        enqueue(seeds, labels, queue, clusterId);

        while (!queue.isEmpty()) {
            int q = queue.poll();

            // q je vec oznacena kao dio klastera (u enqueue). Ovdje samo
            // provjeravamo je li jezgrena, tj. siri li se klaster dalje kroz nju.
            List<Integer> qn = regionQuery(X, q);
            if (qn.size() >= minPts) {
                // q je jezgrena: klaster se siri kroz sve tocke u njezinoj
                // eps-okolini koje jos nisu u nekom klasteru.
                core[q] = true;
                enqueue(qn, labels, queue, clusterId);
            }
            // Inace je q rubna tocka: pripada klasteru, ali se preko nje
            // klaster dalje ne siri (njezini susjedi se ne dodaju u red).
        }
    }


    // Pomocna metoda koja prolazi kroz listu susjeda nb i one koji jos ne
    // pripadaju nijednom klasteru pridruzuje klasteru clusterId.
    // Svaka tocka se ovdje obradi najvise jednom po cijelom izvodenju, jer
    // nakon dodjele oznake vise nije ni UNCLASSIFIED ni NOISE, pa sve iduce
    // pozive za nju preskacemo.
    private static void enqueue(List<Integer> nb, int[] labels, Deque<Integer> queue, int clusterId) {
        for (int r : nb) {
            if (labels[r] == UNCLASSIFIED) {
                // Tocka jos nije posjecena: oznacavamo je odmah (da je nitko
                // drugi ne ubaci ponovno) i stavljamo u red da kasnije
                // provjerimo je li jezgrena.
                labels[r] = clusterId;
                queue.add(r);
            } else if (labels[r] == NOISE)
                // Tocka je u glavnoj petlji fit vec proglasena sumom, sto znaci
                // da je za nju vec napravljen regionQuery i da NIJE jezgrena.
                // Sada je dosegnuta iz jezgrene tocke ovog klastera, pa postaje
                // rubna tocka ovog klastera. U red je ne stavljamo jer bi
                // regionQuery za nju bio uzaludan (znamo da se kroz nju klaster
                // ne siri).
                labels[r] = clusterId;

            // Ostali slucajevi (labels[r] >= 0): tocka je vec u nekom klasteru.
            // Ako je jezgrena, to je nuzno ovaj klaster (dvije gusto povezane
            // jezgrene tocke ne mogu zavrsiti u razlicitim klasterima). Ako je
            // rubna, moze pripadati i ranije pronadjenom klasteru - tada ostaje
            // tamo, tj. u prvom klasteru koji ju je dosegao. Dodjela rubnih
            // tocaka zato ovisi o redoslijedu obilaska, kao i u originalnom DBSCAN-u.
        }
    }


    // Vraca indekse svih tocaka (ukljucujuci samu idx) cija je udaljenost
    // od X[idx] manja ili jednaka eps. Naivna implementacija: O(n) po
    // pozivu, O(n^2) ukupno za cijeli fit - dovoljno za manje i srednje
    // skupove podataka.
    private List<Integer> regionQuery(double[][] X, int idx) {
        List<Integer> neighbors = new ArrayList<>();
        for (int i = 0; i < X.length; i++) {
            if (distance.within(X[idx], X[i], eps))
                neighbors.add(i);
        }
        return neighbors;
    }


    // Za svaku tocku vraca udaljenost do njezinog minPts-tog najblizeg
    // susjeda, pri cemu se sama tocka broji kao prvi susjed (udaljenost 0),
    // isto kao u regionQuery. Zato vrijedi: tocka i je jezgrena za dani eps
    // tocno onda kada je kDist[i] <= eps. Koristi se za odabir eps-a
    // (k-distance graf): ako eps postavimo na q-kvantil ovih vrijednosti,
    // jezgreno je priblizno q udio tocaka.
    public static double[] kDistances(double[][] X, int minPts, Distance distance) {
        int n = X.length;
        if (minPts <= 0 || minPts > n)
            throw new IllegalArgumentException("minPts mora biti iz [1, n]");

        double[] kDist = new double[n];
        double[] dists = new double[n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++)
                dists[j] = distance.dist(X[i], X[j]);
            java.util.Arrays.sort(dists);
            kDist[i] = dists[minPts - 1];
        }
        return kDist;
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
        // Konacnost provjeravamo kao u LloydClustering. Ovdje je to jos
        // vaznije: s NaN je svaka usporedba false, pa bi "s > eps" bio false
        // i tocka s NaN bi bila susjed svim tockama.
        for (int i = 0; i < X.length; i++) {
            if (X[i] == null || X[i].length != d)
                throw new IllegalArgumentException("X[" + i + "].length != d");
            for (int t = 0; t < d; t++)
                if (!Double.isFinite(X[i][t]))
                    throw new IllegalArgumentException("X[" + i + "][" + t + "] is not finite");
        }
    }
}