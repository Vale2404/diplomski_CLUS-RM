package clusteringevaluation;

import java.util.Objects;

final class Centroids {

    // centroid[c] je centroid klastera c, za c = 0..k-1 (indeksi iz Partition).
    final double[][] centroid;

    // Broj tocaka koje sudjeluju u izracunu (nisu NOISE nakon politike).
    final int n;

    // Dimenzija tocaka.
    final int d;

    // Konstruktor.
    private Centroids(double[][] centroid, int n, int d) {
        this.centroid = centroid;
        this.n = n;
        this.d = d;
    }

    static Centroids of(double[][] data, Partition p) {
        int d = validate(data, p);

        // centroid[c] najprije sluzi kao zbroj tocaka klastera c,
        // a nakon dijeljenja s velicinom klastera postaje centroid.
        double[][] centroid = new double[p.k][d];
        int n = 0;

        // Prolazimo kroz sve tocke i svaku dodajemo u
        // zbroj njena klastera.
        for (int i = 0; i < data.length; i++) {
            int c = p.cluster[i];
            if (c == NoisePolicy.NOISE_LABEL)
                continue;
            double[] point = data[i];
            double[] sum = centroid[c];
            for (int t = 0; t < d; t++)
                sum[t] += point[t];
            n++;
        }

        // Svaki zbroj podijelimo s velicinom klastera.
        for (int c = 0; c < p.k; c++) {
            double size = p.size[c];
            for (int t = 0; t < d; t++)
                centroid[c][t] /= size;
        }

        return new Centroids(centroid, n, d);
    }

    // Provjerava ulaz i vraca dimenziju d.
    // Provjeravaju se sve tocke, i one koje politika IGNORE iskljucuje:
    // neispravna vrijednost bilo gdje u podacima gotovo sigurno je bug kod pozivatelja.
    private static int validate(double[][] data, Partition p) {
        Objects.requireNonNull(data, "data cannot be null");

        if (data.length != p.cluster.length)
            throw new IllegalArgumentException("data has " + data.length + " points, but there are "
                    + p.cluster.length + " labels");

        if (data.length == 0)
            throw new IllegalArgumentException("data must contain at least one point");

        if (data[0] == null)
            throw new IllegalArgumentException("point 0 is null");

        int d = data[0].length;
        if (d == 0)
            throw new IllegalArgumentException("points must have dimension > 0");

        for (int i = 0; i < data.length; i++) {
            double[] point = data[i];
            if (point == null)
                throw new IllegalArgumentException("point " + i + " is null");

            if (point.length != d)
                throw new IllegalArgumentException("point " + i + " has length " + point.length + ", expected " + d);

            for (int t = 0; t < d; t++) {
                if (!Double.isFinite(point[t]))
                    throw new IllegalArgumentException("invalid value at point "
                            + i + ", coordinate " + t + ": " + point[t]);
            }
        }
        return d;
    }
}