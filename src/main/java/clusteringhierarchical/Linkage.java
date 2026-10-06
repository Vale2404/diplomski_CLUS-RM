package clusteringhierarchical;

// Sucelje za vezu (linkage) u aglomerativnom klasteriranju.
// Kad spojimo klastere i i j, za svaki drugi klaster k treba
// izracunati udaljenost d(k, i U j). Veza odreduje kako se
// ta udaljenost dobiva iz starih udaljenosti d(k, i) i d(k, j),
// pa algoritam nakon pocetka vise ne mora gledati same tocke.
// nI je velicina klastera i, a nJ velicina klastera j.
// dIJ je udaljenost spojenih klastera, a nK je velicina klastera k.
public interface Linkage {
    double update(double dKI, double dKJ, double dIJ, int nI, int nJ, int nK);
    String name();
}
