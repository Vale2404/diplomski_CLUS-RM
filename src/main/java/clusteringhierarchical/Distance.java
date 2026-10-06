package clusteringhierarchical;

public interface Distance {
    double dist(double[] a, double[] b);

    Distance SQ_EUCLIDEAN = (a, b) -> {
        double s = 0.0;
        for (int i = 0; i < a.length; i++) {
            double d = a[i] - b[i];
            s += d * d;
        }
        return s;
    };

    Distance EUCLIDEAN = (a, b) -> Math.sqrt(SQ_EUCLIDEAN.dist(a, b));

    // Hammingova udaljenost za kategoricke podatke:
    // broj atributa u kojima se a i b razlikuju. Kategorije su kodirane
    // kao cijeli brojevi spremljeni u double, pa je usporedba s !=
    // egzaktna.
    Distance HAMMING = (a, b) -> {
        int s = 0;
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i])
                s++;
        }
        return s;
    };
}