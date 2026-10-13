package clusteringevaluation;

import java.util.Objects;

// Davies-Bouldin indeks:
// DB = (1/k) * sum_q max_{r!=q} (S_q + S_r) / M_qr
// S_q = prosjecna euklidska udaljenost tocaka klastera q do njegova centroida,
// S_r = prosjecna euklidska udaljenost tocaka klastera r do njegova centroida,
// M_qr = euklidska udaljenost izmedu centroida klastera q i r.
// Manja vrijednost znaci bolje odvojene i kompaktnije klastere, minimum je 0.
// Kao i CH, vezan je uz centroide, pa trazi tocke u R^d.
// Za razliku od CH-a koristi obicnu udaljenost, a ne njen kvadrat.
public class DaviesBouldin {
    private DaviesBouldin() {}

    // Racuna DBI za particiju zadanu oznakama labels nad tockama data.
    // Tocke iskljucene politikom policy ne ulaze ni u S_q, ni u centroide, ni u n.
    // Baca IllegalArgumentException ako su podaci ili oznake neispravni,
    // ako particija nakon primjene politike
    // nema barem dva klastera, ako broj tocaka n nije veci od k, ili ako su
    // sve tocke identicne (DB tada nije definiran, svaki R_qr je 0/0).
    // Ako dva klastera imaju isti centroid (M_qr = 0), R_qr = +Infinity, pa je i
    // rezultat +Infinity: klasteri uopce nisu razdvojeni.
    public static double index(double[][] data, int[] labels, NoisePolicy policy) {
        Objects.requireNonNull(data, "data cannot be null");
        Objects.requireNonNull(labels, "labels cannot be null");
        Objects.requireNonNull(policy, "policy cannot be null");

        Partition p = Partition.of(labels, policy);
        Centroids cent = Centroids.of(data, p);

        // Provjere za k i n, rubni slucajevi.
        if (p.k < 2)
            throw new IllegalArgumentException("for computing Davies-Bouldin index k must be >= 2, got " + p.k);
        if (cent.n <= p.k)
            throw new IllegalArgumentException("for computing Davies-Bouldin index number of points must be > k, got "
                    + cent.n + " points and k = " + p.k);

        // Provjera na sirovim podacima, a ne preko S_q i M_qr: centroid m
        // identicnih tocaka x ne mora biti tocno x (npr. (0.1+0.1+0.1)/3 != 0.1),
        // pa S_q i M_qr mogu ispasti reda 1e-17 umjesto tocno 0.
        if (allPointsIdentical(data, p))
            throw new IllegalArgumentException(
                    "Davies-Bouldin index is undefined: all points are identical (S_q = M_qr = 0)");

        double[] s = scatter(data, p, cent);

        double sum = 0.0;
        for (int q = 0; q < p.k; q++) {
            // D_q = max_{r != q} R_qr. Svaki R_qr >= 0, pa je 0 sigurna pocetna vrijednost.
            double dq = 0.0;
            for (int r = 0; r < p.k; r++) {
                if (r == q)
                    continue;
                // M_qr se racuna dvaput (za (q, r) i (r, q)), ali to je O(k^2 * d),
                // zanemarivo u odnosu na prolaz kroz podatke, a kod ostaje jednostavan.
                double m = distance(cent.centroid[q], cent.centroid[r]);
                double rqr = (m == 0.0) ? Double.POSITIVE_INFINITY : (s[q] + s[r]) / m;
                dq = Math.max(dq, rqr);
            }
            sum += dq;
        }
        return sum / p.k;
    }


    // S_q: prosjecna udaljenost tocaka klastera q do njegova centroida.
    // Tocke iskljucene politikom suma se preskacu.
    static double[] scatter(double[][] data, Partition p, Centroids cent) {
        double[] s = new double[p.k];

        for (int i = 0; i < data.length; i++) {
            int c = p.cluster[i];
            if (c == NoisePolicy.NOISE_LABEL)
                continue;
            s[c] += distance(data[i], cent.centroid[c]);
        }

        // Partition jamci size[c] >= 1, pa je dijeljenje sigurno.
        for (int c = 0; c < p.k; c++)
            s[c] /= p.size[c];

        return s;
    }

    // Euklidska udaljenost (s korijenom, za razliku od CH-a).
    private static double distance(double[] a, double[] b) {
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            double diff = a[i] - b[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    // Jesu li sve tocke koje sudjeluju (nisu iskljucene politikom) identicne.
    // Usporedba s ==, a ne Arrays.equals: Arrays.equals smatra 0.0 i -0.0 razlicitima,
    // iako je udaljenost medu njima 0.
    private static boolean allPointsIdentical(double[][] data, Partition p) {
        double[] first = null;
        for (int i = 0; i < data.length; i++) {
            if (p.cluster[i] == NoisePolicy.NOISE_LABEL)
                continue;
            if (first == null) {
                first = data[i];
                continue;
            }
            for (int t = 0; t < first.length; t++) {
                if (data[i][t] != first[t])
                    return false;
            }
        }
        return true;
    }
}