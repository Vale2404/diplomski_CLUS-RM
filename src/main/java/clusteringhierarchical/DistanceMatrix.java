package clusteringhierarchical;

import java.util.Objects;

// Matrica udaljenosti izmedu klastera za aglomerativno klasteriranje.
// Matrica je simetricna, d(i, j) = d(j, i), a dijagonala je uvijek 0
// i algoritmu ne treba. Zato pamtimo samo strogo donji trokut, tj.
// elemente (i, j) za i > j, sto je n(n-1)/2 umjesto n*n brojeva.
// Trokut spremamo redak po redak u jedno polje.
// Redak i ima tocno i elemenata, pa ispred njega u polju stoji
// (i-1) + (i-2) + ... + 2 + 1 = i(i-1)/2 elemenata. Element (i, j) je
// zato na indeksu i(i-1)/2 + j.
final class DistanceMatrix {

    // Broj redaka i stupaca matrice.
    private final int n;

    // Strogo donji trokut spremljen redak po
    // redak u 1D polje.
    private final double[] values;

    DistanceMatrix(int n) {
        if (n <= 0) throw new IllegalArgumentException("n must be > 0");

        // Broj elemenata matrice (strogo donjeg trokuta), racunamo ga u longu
        // jer n^2 brzo prelazi najveci int.
        long size = (long) n * (n - 1) / 2;

        // Polje u Javi ne moze biti vece od priblizno Integer.MAX_VALUE,
        // pa n smije biti najvise 65536.
        if (size > Integer.MAX_VALUE)
            throw new IllegalArgumentException("n = " + n + " is too large for a distance matrix");

        this.n = n;
        this.values = new double[(int) size];
    }

    // Racuna udaljenosti izmedu svih parova tocaka iz X.
    // Matrica je simetricna, pa svaki par racunamo samo jednom.
    // Ispravnost podataka (iste dimenzije, konacne vrijednosti)
    // provjeravat ce fit, ovdje samo osnovno.
    static DistanceMatrix compute(double[][] X, Distance distance) {
        Objects.requireNonNull(distance, "distance must not be null");
        if (X == null || X.length == 0)
            throw new IllegalArgumentException("X must not be null or empty");

        int n = X.length;
        DistanceMatrix D = new DistanceMatrix(n);

        // Petlje idu redom kojim su elementi spremljeni u polju,
        // pa se values popunjava od pocetka do kraja.
        for (int i = 1; i < n; i++) {
            for (int j = 0; j < i; j++) {
                D.set(i, j, distance.dist(X[i], X[j]));
            }
        }
        return D;
    }

    // Broj tocaka.
    int size() {
        return n;
    }

    // Getter za element (i, j)
    double get(int i, int j) {
        return values[index(i, j)];
    }

    // Setter za element (i, j)
    void set(int i, int j, double v) {
        values[index(i, j)] = v;
    }

    // Pretvara par (i, j) u indeks u polju values.
    // Matrica je simetricna pa poredak nije bitan, (i, j) je
    // isto sto i (j, i), ali ako je i < j, obrnemo t.d. vrijedi i > j.
    // Dijagonala se ne pamti pa trazenje el. (i, i) baca iznimku,
    // umjesto da potiho vratimo 0.
    private int index(int i, int j) {
        if (i < 0 || i >= n || j < 0 || j >= n)
            throw new IndexOutOfBoundsException("(" + i + ", " + j + ") out of range for n = " + n);

        if (i == j)
            throw new IllegalArgumentException("diagonal element (" + i + ", " + i + ") is not stored");

        if (i < j) {
            int temp = i;
            i = j;
            j = temp;
        }

        // i(i-1) racunamo u long-u jer za vece i umnozak vise ne stane u int.
        // Konacni rezultat je manji od values.length, pa ga smijemo vratiti kao int.
        return (int) ((long) i * (i - 1) / 2 + j);
    }
}