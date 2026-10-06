package clusteringhierarchical;

import java.util.Arrays;
import java.util.Objects;

public class AgglomerativeClustering {
    private final Distance distance;
    private final Linkage linkage;

    public AgglomerativeClustering(Distance distance, Linkage linkage) {
        this.distance = Objects.requireNonNull(distance, "distance must not be null");
        this.linkage = Objects.requireNonNull(linkage, "linkage must not be null");
    }

    // Tvornicke metode: udaljenost i veza uvijek idu u paru, pa ih je
    // sigurnije zadati na jednom mjestu nego pamtiti pri svakom pozivu.
    // Sve cetiri veze koriste euklidsku udaljenost. Kod Wardove veze to je
    // nuzno, jer se ona temelji na centroidima i SSE-u, a ostale tri koriste
    // euklidsku zbog usporedivosti s Wardom i s k-means.
    public static AgglomerativeClustering single() {
        return new AgglomerativeClustering(Distance.EUCLIDEAN, new Linkages.Single());
    }

    public static AgglomerativeClustering complete() {
        return new AgglomerativeClustering(Distance.EUCLIDEAN, new Linkages.Complete());
    }

    public static AgglomerativeClustering average() {
        return new AgglomerativeClustering(Distance.EUCLIDEAN, new Linkages.Average());
    }

    public static AgglomerativeClustering ward() {
        return new AgglomerativeClustering(Distance.EUCLIDEAN, new Linkages.Ward());
    }

    // Tvornice za kategoricke podatke, s Hammingovom udaljenoscu.
    // Jednostruka, potpuna i prosjecna veza rade s bilo kojom udaljenoscu,
    // jer koriste samo matricu udaljenosti, a ne same tocke. Wardova veza
    // ovdje nema smisla, jer kod kategorickih podataka nema centroida ni SSE-a,
    // pa za nju nema ni tvornice.
    public static AgglomerativeClustering singleHamming() {
        return new AgglomerativeClustering(Distance.HAMMING, new Linkages.Single());
    }

    public static AgglomerativeClustering completeHamming() {
        return new AgglomerativeClustering(Distance.HAMMING, new Linkages.Complete());
    }

    public static AgglomerativeClustering averageHamming() {
        return new AgglomerativeClustering(Distance.HAMMING, new Linkages.Average());
    }

    // Glavna metoda klase. Prima matricu X dimenzija n*d (n tocaka, svaka
    // ima d koordinata) i vraca niz od n - 1 spajanja (nakon n - 1 spajanja,
    // sve tocke su u jednom klasteru), redom kojim su se dogodila.
    // Ovo je naivna verzija: u svakom od n - 1 koraka pretrazujemo
    // cijelu matricu za najmanjom udaljenoscu, pa je slozenost O(n^3).
    public Merge[] fit(double[][] X) {

        // Provjera jesu li podaci u X ispravni.
        validateData(X);

        // Broj tocaka.
        int n = X.length;

        // Udaljenosti izmedu svih parova tocaka, na pocetku je svaka tocka jedan
        // klaster, pa su to ujedno i udaljenosti izmedu klastera.
        DistanceMatrix D = DistanceMatrix.compute(X, distance);

        // Klasteri zive u mjestima 0, ..., n - 1, tj. u retcima matrice D.
        // Kad spojimo klastere iz mjesta a i b, novi klaster preuzima mjesto a,
        // a mjesto b se gasi. Zato pamtimo:
        // active[s] - je li mjesto s jos zivo,
        // id[s]     - oznaka klastera koji trenutno zivi u mjestu s,
        // size[s]   - broj tocaka klastera koji trenutno zivi u mjestu s.
        boolean[] active = new boolean[n];
        Arrays.fill(active, true);
        int[] id = new int[n];
        for (int s = 0; s < n; s++)
            id[s] = s;
        int[] size = new int[n];
        Arrays.fill(size, 1);

        Merge[] merges = new Merge[n - 1];

        // Glavna petlja, u svakom koraku 1 spajanje, ukupno n - 1 spajanja.
        for (int t = 0; t < n - 1; t++) {

            // Trazimo par zivih mjesta s najmanjom udaljenoscu. Strogi <
            // znaci da kod jednakih udaljenosti ostaje prvi pronadeni par,
            // pa je rezultat deterministican. Zivih mjesta ima n - t >= 2,
            // pa ce par sigurno biti naden.
            int a = -1;
            int b = -1;
            double best = Double.POSITIVE_INFINITY;
            for (int i = 1; i < n; i++) {
                if (!active[i]) continue;
                for (int j = 0; j < i; j++) {
                    if (!active[j]) continue;
                    double d = D.get(i, j);
                    if (d < best) {
                        best = d;
                        a = j;
                        b = i;
                    }
                }
            }

            // Zapisemo spajanje, manja oznaka ide lijevo.
            merges[t] = new Merge(Math.min(id[a], id[b]), Math.max(id[a], id[b]), best, size[a] + size[b]);

            // Novi klaster zivi u mjestu a. Za svaki drugi zivi klaster k
            // racunamo udaljenost do novog klastera pomocu veze, samo iz
            // starih udaljenosti d(k, a) i d(k, b).
            for (int k = 0; k < n; k++) {
                if (!active[k] || k == a || k == b)
                    continue;
                double dNew = linkage.update(D.get(k, a), D.get(k, b), best, size[a], size[b], size[k]);
                D.set(k, a, dNew);
            }

            // Gasimo mjesto b i azuriramo podatke o mjestu a.
            active[b] = false;
            id[a] = n + t;
            size[a] += size[b];
        }
        return merges;
    }


    // Rezanje dendrograma: iz niza spajanja koji vraca fit dobivamo
    // podjelu tocaka u tocno k klastera. Svako spajanje smanji broj
    // klastera za 1, pa nakon prvih n - k spajanja ostaje tocno k klastera.
    // Vraca oznaku klastera (0, ..., k-1) za svaku tocku.
    // Spajanja su poredana po visini, jer kod jednostruke, potpune,
    // prosjecne i Wardove veze visine nikad ne padaju, pa je ovo isto sto i
    // vodoravni rez dendrograma na visini izmedu (n-k)-tog i (n-k+1)-og spajanja.
    public static int[] cut(Merge[] merges, int k) {
        Objects.requireNonNull(merges, "merges must not be null");

        // Broj tocaka. Za n tocaka ima n - 1 spajanja.
        int n = merges.length + 1;
        if (k <= 0 || k > n)
            throw new IllegalArgumentException("k must be in [1, n]");

        // Klastera ima ukupno 2n - 1: n pocetnih tocaka i n - 1 novih iz spajanja.
        // parent[c] je oznaka klastera u koji je klaster c spojen, ili -1
        // ako c nije spojen ni u jedan.
        int[] parent = new int[2 * n - 1];
        Arrays.fill(parent, -1);

        // Izvodimo samo prvih n - k spajanja. Spajanje t stvara klaster n + t.
        for (int t = 0; t < n - k; t++) {
            parent[merges[t].left] = n + t;
            parent[merges[t].right] = n + t;
        }

        // Za svaku tocku idemo prema gore po parent dok ne dodemo do klastera
        // koji nije nikamo spojen. To je klaster kojem tocka pripada nakon
        // n - k spajanja. Takvih klastera ima tocno k i prenumeriramo ih u
        // 0, ..., k-1 redom kojim ih nalazimo, pa tocka 0 uvijek ima oznaku 0.
        // labelOf[c] je nova oznaka klastera c, ili -1 ako ga jos nismo sreli.
        int[] labelOf = new int[2 * n - 1];
        Arrays.fill(labelOf, -1);
        int[] labels = new int[n];
        int next = 0;
        for (int i = 0; i < n; i++) {
            int c = i;
            while (parent[c] != -1)
                c = parent[c];
            if (labelOf[c] == -1)
                labelOf[c] = next++;
            labels[i] = labelOf[c];
        }
        return labels;
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
}