package clusteringdensity;

import java.util.Arrays;

// Pomocne metode za kategoricke podatke, iste kao u clusteringPartitional.
// Kategorije su kodirane kao nenegativni cijeli brojevi 0, 1, 2, ...
// spremljeni u double.
final class Categorical {
    private Categorical() {}


    // Verzija za mijesane podatke. categorical[t] == true znaci da je
    // stupac t kategoricki. Za kategoricke stupce provjerava da su sve
    // vrijednosti ispravni kodovi i vraca velicinu domene (najveci kod + 1).
    // Za numericke stupce vraca 0 i nista ne provjerava, jer konacnost
    // vrijednosti vec provjerava validateData u fit-u.
    // Maska se samo cita, pa je ne kloniramo.
    static int[] domains(double[][] X, boolean[] categorical) {
        int d = X[0].length;
        if (categorical == null)
            throw new IllegalArgumentException("categorical cannot be null");
        if (categorical.length != d)
            throw new IllegalArgumentException("categorical.length = " + categorical.length + " != d = " + d);

        int[] domain = new int[d];

        for (int t = 0; t < d; t++) {
            if (!categorical[t])
                continue;
            for (int i = 0; i < X.length; i++) {
                double v = X[i][t];
                if (!Double.isFinite(v) || v < 0 || v != Math.rint(v) || v >= Integer.MAX_VALUE)
                    throw new IllegalArgumentException("X[" + i + "][" + t + "] = " + v + " is not a valid category code");
                domain[t] = Math.max(domain[t], (int) v + 1);
            }
        }
        return domain;
    }

    // Ako su sve vrijednosti kategorijske. Samo poziva opcu verziju
    // s maskom u kojoj su sve vrijednosti true, pa je logika
    // provjere na jednom mjestu.
    static int[] domains(double[][] X) {
        boolean[] categorical = new boolean[X[0].length];
        Arrays.fill(categorical, true);
        return domains(X, categorical);
    }

    // Vraca indekse stupaca t za koje je mask[t] == value, uzlazno.
    // S value = true dobijemo kategoricke, a s value = false numericke stupce.
    static int[] indicesOf(boolean[] mask, boolean value) {
        int count = 0;
        for (boolean m : mask)
            if (m == value)
                count++;
        int[] idx = new int[count];
        int pos = 0;
        for (int t = 0; t < mask.length; t++)
            if (mask[t] == value)
                idx[pos++] = t;
        return idx;
    }
}