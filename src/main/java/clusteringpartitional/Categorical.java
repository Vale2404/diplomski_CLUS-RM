package clusteringpartitional;

// Pomocne metode za kategoricke podatke. Kategorije su kodirane kao
// nenegativni cijeli brojevi 0, 1, 2, ... spremljeni u double.
final class Categorical {
    private Categorical() {}


    // Provjerava da su sve vrijednosti u X ispravni kodovi i vraca
    // velicinu domene svakog atributa (najveci kod + 1).
    static int[] domains(double[][] X) {
        int d = X[0].length;
        int[] domain = new int[d];
        for (int i = 0; i < X.length; i++) {
            for (int t = 0; t < d; t++) {
                double v = X[i][t];
                if (!Double.isFinite(v) || v < 0 || v != Math.rint(v) || v >= Integer.MAX_VALUE)
                    throw new IllegalArgumentException("X[" + i + "][" + t + "] = " + v + " is not a valid category code");
                domain[t] = Math.max(domain[t], (int) v + 1);
            }
        }
        return domain;
    }
}