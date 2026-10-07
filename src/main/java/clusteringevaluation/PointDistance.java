package clusteringevaluation;

// Udaljenost izmedu dviju tocaka skupa podataka, zadanih svojim indeksima.
// Zasto indeksi, a ne vektori?
// Paketi s algoritmima klasteriranja podatke drze na razlicite
// nacine (double[] za k-means, kategoricke vrijednosti za k-modes, CLUS-RM-ove
// vlastite strukture...). Kad silueta trazi samo "udaljenost izmedu tocke i i
// tocke j", svaki pozivatelj preda lambdu koja zna dohvatiti svoje dvije tocke,
// a ovaj paket ne mora znati nista o reprezentaciji podataka.
// Ocekuje se da je udaljenost simetricna, nenegativna i konacna te da je
// d(i, i) = 0, kao kod svake metrike.

@FunctionalInterface
public interface PointDistance {

    // i indeks prve tocke,
    // j indeks druge tocke,
    // vraca udaljenost izmedu tocaka i, j.
    double between(int i, int j);
}