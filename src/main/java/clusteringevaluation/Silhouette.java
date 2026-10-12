package clusteringevaluation;

import java.util.Arrays;
import java.util.Objects;

public final class Silhouette {

    private Silhouette() {}

    // Funkcija racuna s(i) za svaku tocku. Prima labels (oznake klastera),
    // distance (udaljenost izmedu tocaka zadanih indeksima 0, ..., labels.length - 1),
    // policy (sto napraviti s tockama suma).
    // Vraca polje duljine labels.length. Za tocke iskljucene politikom policy, vrijednost
    // je Double.NaN.
    // Baca IllegalArgumentException ako je neka oznaka negativna, a nije
    // NOISE_LABEL, ako particija (nakon primjene politike) nema
    // barem dva klastera, ili ako udaljenost vrati negativnu, NaN ili
    // beskonacnu vrijednost.
    public static double[] perPoint(int[] labels, PointDistance distance, NoisePolicy policy) {
        Objects.requireNonNull(labels, "labels cannot be null");
        Objects.requireNonNull(distance, "distance cannot be null");
        Objects.requireNonNull(policy, "policy cannot be null");

        Partition p = Partition.of(labels, policy);

        if (p.k < 2)
            throw new IllegalArgumentException("for computing silhouette coef. k must be >= 2");

        int n = labels.length;
        double[] s =  new double[n];

        // Jedno pomocno polje za cijelu petlju umjesto novog za svaku tocku:
        // sums[c] = zbroj udaljenosti od trenutne tocke do svih tocaka klastera c.
        double[] sums = new double[p.k];

        for (int i = 0; i < n; i++) {
            int ci = p.cluster[i];
            if (ci == NoisePolicy.NOISE_LABEL) {
                s[i] = Double.NaN;
                continue;
            }

            if (p.size[ci] == 1) {
                s[i] = 0.0;
                continue;
            }

            Arrays.fill(sums, 0.0);
            for (int j = 0; j < n; j++) {
                int cj = p.cluster[j];
                if (j == i || cj == NoisePolicy.NOISE_LABEL) {
                    continue;
                }
                sums[cj] += checkedDistance(distance, i, j);
            }

            double a = sums[ci] / (p.size[ci] - 1);
            double b = Double.POSITIVE_INFINITY;
            for (int c = 0; c < p.k; c++) {
                if (c != ci) {
                    b = Math.min(b, sums[c] / p.size[c]);
                }
            }

            double max = Math.max(a, b);
            s[i] = (max == 0.0) ? 0.0 : (b - a) / max;
        }
        return s;
    }

    // Prosjecna silueta po svim tockama koje nisu iskljucene politikom suma.
    // Ekvivalentno mean(perPoint(labels, distance, policy)).
    public static double mean(int[] labels, PointDistance distance, NoisePolicy policy) {
        return mean(perPoint(labels, distance, policy));
    }

    // Prosjek vec izracunatih vrijednosti s(i), uz preskakanje NaN vrijednosti
    // (tocke iskljucene politikom).
    // Zasto posebna metoda: ako trebas i vrijednosti po tockama (npr. za graf
    // siluete) i prosjek, izracunas perPoint jednom pa prosjek iz
    // rezultata, umjesto da O(n^2) posao radis dvaput.
    // throws IllegalArgumentException ako nema nijedne vrijednosti koja nije NaN.
    public static double mean(double[] silhouettes) {
        Objects.requireNonNull(silhouettes,  "silhouettes cannot be null");
        double sum = 0.0;
        int count = 0;
        for (double v : silhouettes) {
            if (!Double.isNaN(v)) {
                sum += v;
                count++;
            }
        }
        if (count == 0)
            throw new IllegalArgumentException("There are no rated points.");
        return sum / count;
    }

    // Poziva udaljenost i provjerava vrijednost. Negativna, NaN ili beskonacna
    // udaljenost gotovo uvijek znaci bug u lambdi pozivatelja, a bez provjere bi se
    // tiho prosirila u NaN siluetu. Provjera je jeftina u odnosu na sam izracun udaljenosti.
    private static double checkedDistance(PointDistance distance, int i, int j) {
        double v = distance.between(i, j);
        if (Double.isNaN(v) || Double.isInfinite(v) || v < 0.0) {
            throw new IllegalArgumentException(
                    "incorrect distance between points " + i + " and " + j + ": " + v);
        }
        return v;
    }
}