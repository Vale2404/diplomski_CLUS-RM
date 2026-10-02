package clusteringpartitional;

// Sucelje za korak azuriranja u Lloydovoj iteraciji.
// Oznake su fiksne i nema praznih klastera,
// a centri C se zamjenjuju novima. Metoda mijenja C na mjestu.
// Za k-means centar je aritmeticka sredina, za k-modes mod.
public interface CenterUpdater {
    void update(double[][] X, double[][] C, int[] labels);

    // Poziva se jednom na pocetku fit-a, prije prve iteracije.
    // Sluzi za jednokratnu pripremu nad podacima (npr. provjeru
    // kodova i racunanje domena kod k-modesa). Zadano ne radi nista,
    // pa Mean ne mora nista mijenjati.
    default void prepare(double[][] X) {}
}