package clusteringpartitional;

// Priprema podataka prije klasteriranja. Algoritmi u ovom paketu sami
// ne mijenjaju podatke, pa se standardizacija radi ovdje, izvan fit-a,
// i pozivatelj bira kada je primijeniti.
public final class Preprocessing {
    private  Preprocessing() {}

    // Z-standardizacija numerickih stupaca: od svake vrijednosti oduzme se
    // sredina stupca i podijeli se standardnom devijacijom stupca, pa svaki
    // numericki stupac ima sredinu 0 i devijaciju 1.
    // Kategoricki stupci (categorical[t] == true) samo se prepisuju.
    // Konstantan stupac ne nosi informaciju, pa ga postavljamo na 0
    // (samo centriramo) umjesto da dijelimo s nulom.
    // Vraca novu matricu, a X ostaje nepromijenjen.
    public static double[][] standardize(double[][] X, boolean[] categorical) {
        if (X == null || X.length == 0 || X[0] == null || X[0].length == 0)
            throw new IllegalArgumentException("X cannot be null or empty");
        int n = X.length;
        int d = X[0].length;

        // Svi retci moraju imati d stupaca, a sve vrijednosti biti konacne.
        // Jedan NaN bi inace pokvario sredinu cijelog stupca.
        for (int i = 0; i < n; i++) {
            if (X[i] == null || X[i].length != d)
                throw new IllegalArgumentException("X[" + i + "].length != d");
            for (int t = 0; t < d; t++) {
                if (!Double.isFinite(X[i][t]))
                    throw new IllegalArgumentException("X[" + i + "][" + t + "] is not finite");
            }
        }

        // Provjerava da maska nije null, da ima duljinu d i da su
        // kategoricki stupci ispravni kodovi. Domene nam ovdje ne trebaju.
        Categorical.domains(X, categorical);

        // Kopija podataka. Kategoricki stupci ostaju kakvi jesu,
        // a numericke prepisujemo standardiziranim vrijednostima.
        double[][] Z = new double[n][];
        for (int i = 0; i < n; i++)
            Z[i] = X[i].clone();

        for (int t : Categorical.indicesOf(categorical, false)) {
            double mean = 0.0;
            for (int i = 0; i < n; i++)
                mean += X[i][t];
            mean /= n;

            double var = 0.0;
            for (int i = 0; i < n; i++) {
                double diff = X[i][t] - mean;
                var += diff * diff;
            }
            double sd = Math.sqrt(var / n);

            for (int i = 0; i < n; i++)
                Z[i][t] = sd > 0.0 ? (X[i][t] - mean) / sd : 0.0;
        }
        return Z;
    }
}