package clusteringhierarchical;

public final class Linkages {
    private Linkages() {}

    // Jednostruka veza: udaljenost dvaju klastera je udaljenost
    // njihovih najblizih tocaka, d(k, i u j) = min(d(k, i), d(k, j)).
    public static final class Single implements Linkage {
        @Override
        public double update(double dKI, double dKJ, double dIJ, int nI, int nJ, int nK) {
            return Math.min(dKI, dKJ);
        }
        @Override
        public String name() {
            return "Single";
        }
    }

    // Potpuna veza: udaljenost dvaju klastera je udaljenost
    // njihovih najdaljih tocaka, d(k, i u j) = max(d(k, i), d(k, j)).
    public static final class Complete implements Linkage {
        @Override
        public double update(double dKI, double dKJ, double dIJ, int nI, int nJ, int nK) {
            return Math.max(dKI, dKJ);
        }
        @Override
        public String name() {
            return "Complete";
        }
    }

    // Prosjecna veza: udaljenost dvaju klastera je prosjek udaljenosti
    // svih parova tocaka, jedna iz jednog, druga iz drugog klastera.
    // d(k, i) je prosjek nK*nI parova, pa je zbroj tih udaljenosti
    // nK*nI*d(k, i). Parovi izmedu k i i U j su upravo parovi izmedu
    // k i i zajedno s parovima izmedu k i j, pa je
    // d(k, i U j) = (nK*nI*d(k, i) + nK*nJ*d(k, j)) / (nK*(nI + nJ))
    //             = (nI*d(k, i) + nJ*d(k, j)) / (nI + nJ).
    // Novi prosjek je tezinska sredina
    // starih, a vecem klasteru pripada veca tezina.
    public static final class Average implements Linkage {
        @Override
        public double update(double dKI, double dKJ, double dIJ, int nI, int nJ, int nK) {
            return (nI * dKI + nJ * dKJ) / (nI + nJ);
        }
        @Override
        public String name() {
            return "Average";
        }
    }

    // Wardova veza: spajamo dva klastera cije spajanje najmanje poveca
    // sumu kvadrata unutar klastera (SSE), istu velicinu koju minimizira k-means.
    // To povecanje za klastere A i B s centroidima cA i cB je
    // Delta(A, B) = |A|*|B| / (|A| + |B|) * ||cA - cB||^2,
    // pa se skupo spajaju klasteri s dalekim centroidima i dva velika klastera.
    // Kao scipy, u matrici ne pamtimo Delta, nego sqrt(2 * Delta). Za dvije
    // pojedinacne tocke to je upravo njihova euklidska udaljenost, pa fit
    // pocinje s Distance.EUCLIDEAN kao i ostale veze, a visine spajanja su u
    // istim jedinicama. Lance-Williamsova formula vrijedi za kvadrate tih
    // udaljenosti, pa ih u update kvadriramo, primijenimo formulu
    // d(k, i U j)^2 = ((nI + nK)*d(k, i)^2 + (nJ + nK)*d(k, j)^2 - nK*d(i, j)^2)
    //                   / (nI + nJ + nK)
    // i na kraju vratimo korijen.
    //
    // Ward ima smisla samo uz euklidsku udaljenost, jer cijela ideja pociva
    // na centroidima i SSE-u. S npr. Hammingovom udaljenoscu brojevi bi bili
    // bez znacenja.
    public static final class Ward implements Linkage {
        @Override
        public double update(double dKI, double dKJ, double dIJ, int nI, int nJ, int nK) {
            double sq = ((nI + nK) * dKI * dKI
                    + (nJ + nK) * dKJ * dKJ
                    - nK * dIJ * dIJ) / (nI + nJ + nK);

            // U egzaktnoj aritmetici je sq >= 0, ali zbog zaokruzivanja moze
            // ispasti nesto poput -1e-15 (npr. kod duplikata tocaka), a tada
            // bi Math.sqrt vratio NaN. Zato odrezemo na 0.
            return Math.sqrt(Math.max(sq, 0.0));
        }
        @Override
        public String name() {
            return "Ward";
        }
    }
}