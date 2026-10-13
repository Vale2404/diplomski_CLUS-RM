package clusteringevaluation;

import java.util.Objects;

// Calinski-Harabasz indeks (variance ratio criterion):
// CH(k) = [SSB / (k - 1)] / [SSW / (n - k)]
// SSW = zbroj kvadrata udaljenosti tocaka do centroida svog klastera,
// SSB = zbroj |c_q| * ||c_q - c||^2 po klasterima, gdje je c centroid svih tocaka.
// Veca vrijednost znaci bolje odvojene i kompaktnije klastere.
// Zasto vektori, a ne PointDistance kao kod siluete: CH je po definiciji
// vezan uz centroide i euklidsku udaljenost, pa trazi tocke u R^d.
public final class CalinskiHarabasz {

    private CalinskiHarabasz() {}

    // Racuna CH za particiju zadanu oznakama labels nad tockama data.
    // Tocke iskljucene politikom policy ne ulaze ni u SSW, ni u SSB, ni u n.
    // Baca IllegalArgumentException ako su podaci ili oznake neispravni
    // (vidi Partition i Centroids), ako particija nakon primjene politike
    // nema barem dva klastera, ili ako broj tocaka n nije veci od k.
    // Vraca +Infinity kad su unutar svakog klastera sve tocke identicne,
    // a baca iznimku kad su sve tocke identicne.
    public static double index(double[][] data, int[] labels, NoisePolicy policy) {
        Objects.requireNonNull(data, "data cannot be null");
        Objects.requireNonNull(labels, "labels cannot be null");
        Objects.requireNonNull(policy, "policy cannot be null");

        Partition p = Partition.of(labels, policy);
        Centroids cent = Centroids.of(data, p);

        if (p.k < 2)
            throw new IllegalArgumentException("for computing Calinski-Harabasz index k must be >= 2, got " + p.k);

        if (cent.n <= p.k)
            throw new IllegalArgumentException("for computing Calinski-Harabasz index number of points must be > k, got "
                    + cent.n + " points and k = " + p.k);

        // Obje odluke donosimo na sirovim podacima, a ne preko w i b: centroid
        // m identicnih tocaka x ne mora biti tocno x (npr. (0.1+0.1+0.1)/3 != 0.1),
        // pa w i b mogu ispasti reda 1e-33 umjesto tocno 0.
        // Sve tocke identicne: SSW = SSB = 0 i CH nije definiran (0/0).
        if (allPointsIdentical(data, p))
            throw new IllegalArgumentException(
                    "Calinski-Harabasz index is undefined: all points are identical (SSW = SSB = 0)");

        // U svakom klasteru sve su tocke identicne, a klasteri se razlikuju:
        // SSW = 0 i SSB > 0, klasteri su savrseno kompaktni i razdvojeni, CH tezi u
        // beskonacnost, pa vracamo +Infinity (sklearn ovdje vraca 1.0, sto bi
        // savrsene klastere ocijenilo losije od gotovo svake stvarne particije).
        if (everyClusterConstant(data, p))
            return Double.POSITIVE_INFINITY;

        double w = ssw(data, p, cent);
        double b = ssb(p, cent);

        return (b / (p.k - 1)) / (w / (cent.n - p.k));
    }


    // SSW: zbroj kvadrata udaljenosti tocaka do centroida svog klastera.
    // Tocke iskljucene politikom suma se preskacu.
    static double ssw(double[][] data, Partition p, Centroids cent) {
        double w = 0.0;
        for (int i = 0; i < data.length; i++) {
            int c = p.cluster[i];
            if (c == NoisePolicy.NOISE_LABEL)
                continue;
            w += squaredDistance(data[i], cent.centroid[c]);
        }
        return w;
    }

    // SSB: zbroj kvadrata udaljenosti centroida klastera do globalnog
    // centroida i svaki sumand pomnozen s velicinom klastera.
    static double ssb(Partition p, Centroids cent) {
        double[] globalC = globalCentroid(p, cent);
        double b = 0.0;
        for (int c = 0; c < p.k; c++)
            b += p.size[c] * squaredDistance(cent.centroid[c], globalC);
        return b;
    }

    // Kvadrat euklidske udaljenosti.
    private static double squaredDistance(double[] a, double[] b) {
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            double diff = a[i] - b[i];
            sum += diff * diff;
        }
        return sum;
    }

    // Centroid svih tocaka koje sudjeluju, izracunat kao tezinski prosjek
    // centroida klastera: |c| * mu_c je zbroj tocaka klastera c, pa je ovo
    // zbroj svih tocaka podijeljen s n, bez novog prolaza kroz podatke.
    private static double[] globalCentroid(Partition p, Centroids cent) {
        double[] globalC = new double[cent.d];
        for (int c = 0; c < p.k; c++) {
            double[] mc = cent.centroid[c];
            for (int t = 0; t < cent.d; t++) {
                globalC[t] += p.size[c] * mc[t];
            }
        }
        for (int t = 0; t < cent.d; t++) {
            globalC[t] /= cent.n;
        }
        return globalC;
    }

    // Jesu li sve tocke koje sudjeluju identicne.
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

    // Jesu li unutar svakog klastera sve tocke identicne.
    private static boolean everyClusterConstant(double[][] data, Partition p) {
        double[][] first = new double[p.k][];
        for (int i = 0; i < data.length; i++) {
            int c  = p.cluster[i];
            if (c == NoisePolicy.NOISE_LABEL)
                continue;
            if (first[c] == null) {
                first[c] = data[i];
                continue;
            }
            for (int t = 0; t < first[c].length; t++) {
                if (data[i][t] != first[c][t])
                    return false;
            }
        }
        return true;
    }
}