package clusteringpartitional;

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

    // Mijesana udaljenost za k-prototypes:
    // d(a, b) = zbroj kvadrata razlika po numerickim stupcima
    //           + gamma * broj kategorickih stupaca u kojima se a i b razlikuju.
    // categorical[t] == true znaci da je stupac t kategoricki.
    // gamma odreduje koliko vrijedi jedno kategoricko neslaganje u odnosu
    // na numericki dio. gamma = 0 znaci da se kategoricki stupci ignoriraju.
    // Specijalni slucajevi: maska bez ijednog true daje SQ_EUCLIDEAN, a
    // maska sa svim true i gamma = 1 daje HAMMING.
    static Distance mixed(boolean[] categorical, double gamma) {
        if (categorical == null)
            throw new IllegalArgumentException("categorical cannot be null");
        if (!Double.isFinite(gamma) || gamma < 0)
            throw new IllegalArgumentException("gamma must be finite and >= 0, got " + gamma);

        // Indekse racunamo jednom ovdje, a ne u svakom pozivu dist.
        // Lambda pamti ova polja, a ne masku, pa kasnija promjena
        // maske kod pozivatelja ne utjece na udaljenost.
        int[] num = Categorical.indicesOf(categorical, false);
        int[] cat = Categorical.indicesOf(categorical, true);

        return (a, b) -> {
            // Numericki dio, kvadrirana euklidska udaljenost.
            double sNum = 0.0;
            for (int t : num) {
                double diff = a[t] - b[t];
                sNum += diff * diff;
            }

            // Kategoricki dio: Hamming. Kodovi su cijeli brojevi
            // spremljeni u double, pa je usporedba s != egzaktna.
            int sCat = 0;
            for (int t : cat) {
                if (a[t] != b[t])
                    sCat++;
            }
            return sNum + gamma * sCat;
        };
    }
}