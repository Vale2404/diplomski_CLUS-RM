package clusteringdensity;

import java.util.Arrays;
import java.util.Objects;

import elki.clustering.hierarchical.ClusterMergeHistory;
import elki.clustering.hierarchical.HDBSCANLinearMemory;
import elki.clustering.hierarchical.extraction.HDBSCANHierarchyExtraction;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.DendrogramModel;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.StaticArrayDatabase;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRange;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDVar;
import elki.database.relation.Relation;
import elki.datasource.ArrayAdapterDatabaseConnection;
import elki.utilities.datastructures.hierarchy.Hierarchy;
import elki.utilities.datastructures.iterator.It;

// HDBSCAN* (Campello, Moulavi, Sander 2013) preko biblioteke ELKI 0.8.0.
//
// Ulaz i izlaz su isti kao kod Dbscan: matrica n*d, tvornice numeric,
// categorical i mixed s istim udaljenostima, a rezultat je Dbscan.Result
// (oznake -1 = sum, inace 0, 1, 2, ...). Umjesto eps-a zadajemo:
//   minPts         - kao u DBSCAN-u: k-ta najbliza tocka (ukljucujuci samu
//                    tocku) odreduje jezgrenu udaljenost
//   minClusterSize - najmanji broj tocaka da bi skup bio klaster (a ne
//                    "otpadanje" tocaka iz roditeljskog klastera)
//
// ELKI radi: jezgrene udaljenosti, MST u grafu udaljenosti dosezljivosti
// (mutual reachability), hijerarhiju i odabir klastera po stabilnosti
// (excess of mass). Mi radimo samo prijevod ulaza i izlaza.
public class Hdbscan implements Densityclustering{
    private final int minPts;
    private final int minClusterSize;
    private final Distance distance;

    // Isto znacenje kao u Dbscan: samo provjera kategorickih kodova.
    private final boolean[] categorical;
    private final boolean allCategorical;

    public Hdbscan(int minPts, int minClusterSize, Distance distance) {
        this(minPts, minClusterSize, distance, null, false);
    }

    private Hdbscan(int minPts, int minClusterSize, Distance distance,
                    boolean[] categorical, boolean allCategorical) {
        if (minPts <= 0)
            throw new IllegalArgumentException("minPts must be > 0");
        // S minClusterSize = 1 svaka tocka moze biti klaster, pa sum ne
        // postoji. Campello i sur. zato traze barem 2.
        if (minClusterSize < 2)
            throw new IllegalArgumentException("minClusterSize must be >= 2");
        this.minPts = minPts;
        this.minClusterSize = minClusterSize;
        this.distance = Objects.requireNonNull(distance, "distance must not be null");
        this.categorical = categorical;
        this.allCategorical = allCategorical;
    }

    // Tvornice, iste udaljenosti kao u Dbscan.

    public static Hdbscan numeric(int minPts, int minClusterSize) {
        return new Hdbscan(minPts, minClusterSize, Distance.EUCLIDEAN);
    }

    public static Hdbscan categorical(int minPts, int minClusterSize) {
        return new Hdbscan(minPts, minClusterSize, Distance.HAMMING, null, true);
    }

    // Napomena: za razliku od DBSCAN-a, mixed sa svim numerickim stupcima
    // NIJE isto sto i numeric. Hijerarhija (MST) je ista jer je kvadriranje
    // monotono, ali stabilnost klastera racuna se iz 1 / udaljenost, pa
    // odabrani klasteri mogu biti drugaciji.
    public static Hdbscan mixed(int minPts, int minClusterSize, boolean[] categorical, double gamma) {
        if (categorical == null)
            throw new IllegalArgumentException("categorical cannot be null");
        return new Hdbscan(minPts, minClusterSize, Distance.mixed(categorical, gamma),
                categorical.clone(), false);
    }

    @Override
    public Dbscan.Result fit(double[][] X) {
        Elki e = runElki(X);
        int n = X.length;

        // hierarchical = true: ELKI vraca korijen hijerarhije kao klaster na
        // vrhu, a odabrane klastere kao njegovu djecu. Tocke koje su ostale
        // izravno u korijenu otpale su prije nego sto su usle u ijedan
        // odabrani klaster, tj. to je sum. Djeca su "spljostena" (svi
        // potomci su u njima), pa je dubina najvise 2.
        Clustering<DendrogramModel> c =
                new HDBSCANHierarchyExtraction(e.algorithm, minClusterSize, true).run(e.merges);
        Hierarchy<Cluster<DendrogramModel>> h = c.getClusterHierarchy();

        int[] labels = new int[n];
        Arrays.fill(labels, -1);
        int k = 0;
        for (It<Cluster<DendrogramModel>> top = c.iterToplevelClusters(); top.valid(); top.advance()) {
            Cluster<DendrogramModel> root = top.get();
            if (h.numChildren(root) == 0) {
                // Nijedno razdvajanje nije stabilnije od cijelog skupa: sve
                // je jedan klaster (kao allow_single_cluster u Pythonu).
                assign(root, k++, labels, e.ids);
                continue;
            }
            for (It<Cluster<DendrogramModel>> ch = h.iterChildren(root); ch.valid(); ch.advance())
                assign(ch.get(), k++, labels, e.ids);
        }

        int numNoise = 0;
        for (int l : labels)
            if (l < 0)
                numNoise++;
        return new Dbscan.Result(labels, k, numNoise);
    }

    // Hijerarhija bez ELKI tipova, za testove (usporedba s DBSCAN-om).
    // Spajanje j spaja a[j] i b[j] na visini height[j], uzlazno po visini.
    // Vrijednost x < n je redak X[x], a x = n + j je klaster nastao j-tim
    // spajanjem.
    static final class Merges {
        final int n;
        final int[] a, b;
        final double[] height;

        Merges(int n, int[] a, int[] b, double[] height) {
            this.n = n;
            this.a = a;
            this.b = b;
            this.height = height;
        }
    }

    Merges merges(double[][] X) {
        Elki e = runElki(X);
        int n = X.length;
        int m = e.merges.numMerges();
        int[] a = new int[m], b = new int[m];
        double[] height = new double[m];
        DBIDVar tmp = DBIDUtil.newVar();
        for (int j = 0; j < m; j++) {
            a[j] = toRow(e.merges.getMergeA(j), n, e, tmp);
            b[j] = toRow(e.merges.getMergeB(j), n, e, tmp);
            height[j] = e.merges.getMergeHeight(j);
        }
        return new Merges(n, a, b, height);
    }

    // ===================== ELKI dio =====================

    // Sve sto ELKI vrati, na jednom mjestu.
    private static final class Elki {
        HDBSCANLinearMemory<NumberVector> algorithm;
        ClusterMergeHistory merges;
        DBIDRange ids;
    }

    private Elki runElki(double[][] X) {
        Dbscan.validateData(X);
        if (minPts > X.length)
            throw new IllegalArgumentException("minPts = " + minPts + " > n = " + X.length);
        if (allCategorical)
            Categorical.domains(X);
        else if (categorical != null)
            Categorical.domains(X, categorical);

        // Baza iz double[][]: redak i dobiva DBID s pomakom (offset) i.
        Database db = new StaticArrayDatabase(new ArrayAdapterDatabaseConnection(X), null);
        db.initialize();
        Relation<NumberVector> rel = db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);

        Elki e = new Elki();
        e.ids = (DBIDRange) rel.getDBIDs();
        e.algorithm = new HDBSCANLinearMemory<>(new Elkidistance(distance), minPts);
        e.merges = e.algorithm.run(rel);
        return e;
    }

    // Sve tocke klastera dobivaju oznaku label.
    private static void assign(Cluster<DendrogramModel> clus, int label, int[] labels, DBIDRange ids) {
        for (DBIDIter it = clus.getIDs().iter(); it.valid(); it.advance())
            labels[ids.getOffset(it)] = label;
    }

    // Indeks iz ELKI-jeve hijerarhije u nas: tocke (x < n) u redak X,
    // klasteri (x >= n) ostaju isti.
    private static int toRow(int x, int n, Elki e, DBIDVar tmp) {
        return x < n ? e.ids.getOffset(e.merges.assignVar(x, tmp)) : x;
    }
}