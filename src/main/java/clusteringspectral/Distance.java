package clusteringspectral;

// Udaljenosti, iste kao u clusteringdensity i clusteringPartitional.
// Vrijednosti dist su bit po bit iste kao u paketu
// clusteringPartitional, pa iste podatke i isti parametar gamma mozemo
// koristiti i za k-means/k-modes/k-prototypes i za DBSCAN.
//
// Razlika u odnosu na clusteringPartitional je metoda within. DBSCAN nikad
// ne treba samu udaljenost, nego samo odgovor na pitanje "je li dist(a, b)
// <= eps". To mozemo odgovoriti brze od racunanja cijele udaljenosti: cim
// djelomicni zbroj prijede eps, ostatak koordinata vise ne treba gledati.
// Kod puno atributa vecina parova otpada nakon nekoliko koordinata.
public interface Distance {
    double dist(double[] a, double[] b);

    // Je li dist(a, b) <= eps. Zadana verzija racuna cijelu udaljenost.
    // Udaljenosti ispod je nadjacavaju verzijom s ranim izlazom.
    default boolean within(double[] a, double[] b, double eps) {
        return dist(a, b) <= eps;
    }

    // Kvadrirana euklidska udaljenost.
    Distance SQ_EUCLIDEAN = new Distance() {
        @Override
        public double dist(double[] a, double[] b) {
            double s = 0.0;
            for (int i = 0; i < a.length; i++) {
                double d = a[i] - b[i];
                s += d * d;
            }
            return s;
        }

        // Zbroj kvadrata samo raste, pa mozemo stati cim prijede eps.
        // Zbrajamo istim redom kao dist, pa je rezultat tocno dist <= eps.
        @Override
        public boolean within(double[] a, double[] b, double eps) {
            double s = 0.0;
            for (int i = 0; i < a.length; i++) {
                double d = a[i] - b[i];
                s += d * d;
                if (s > eps)
                    return false;
            }
            return true;
        }
    };

    // Euklidska udaljenost.
    Distance EUCLIDEAN = new Distance() {
        @Override
        public double dist(double[] a, double[] b) {
            return Math.sqrt(SQ_EUCLIDEAN.dist(a, b));
        }

        // Umjesto sqrt(s) <= eps usporedujemo s <= eps * eps, pa korijen
        // ne treba racunati. Matematicki je to isto, a u aritmetici s
        // pomicnim zarezom moze se razlikovati samo za par tocno na rubu
        // (udaljenost jednaka eps do na zadnji bit).
        @Override
        public boolean within(double[] a, double[] b, double eps) {
            return SQ_EUCLIDEAN.within(a, b, eps * eps);
        }
    };

    // Hammingova udaljenost za kategoricke podatke:
    // broj atributa u kojima se a i b razlikuju. Kategorije su kodirane
    // kao cijeli brojevi spremljeni u double, pa je usporedba s !=
    // egzaktna.
    Distance HAMMING = new Distance() {
        @Override
        public double dist(double[] a, double[] b) {
            int s = 0;
            for (int i = 0; i < a.length; i++) {
                if (a[i] != b[i])
                    s++;
            }
            return s;
        }

        // Broj razlika samo raste, pa stajemo cim prijede eps.
        @Override
        public boolean within(double[] a, double[] b, double eps) {
            int s = 0;
            for (int i = 0; i < a.length; i++) {
                if (a[i] != b[i]) {
                    s++;
                    if (s > eps)
                        return false;
                }
            }
            return true;
        }
    };

    // Mijesana udaljenost, ista kao za k-prototypes:
    // d(a, b) = zbroj kvadrata razlika po numerickim stupcima
    //           + gamma * broj kategorickih stupaca u kojima se a i b razlikuju.
    // categorical[t] == true znaci da je stupac t kategoricki.
    // gamma odreduje koliko vrijedi jedno kategoricko neslaganje u odnosu
    // na numericki dio. gamma = 0 znaci da se kategoricki stupci ignoriraju.
    // Specijalni slucajevi: maska bez ijednog true daje SQ_EUCLIDEAN, a
    // maska sa svim true i gamma = 1 daje HAMMING.
    // Numericki dio je KVADRAT euklidske udaljenosti, pa je i eps za ovu
    // udaljenost na toj skali (vidi Dbscan.mixed).
    static Distance mixed(boolean[] categorical, double gamma) {
        if (categorical == null)
            throw new IllegalArgumentException("categorical cannot be null");
        if (!Double.isFinite(gamma) || gamma < 0)
            throw new IllegalArgumentException("gamma must be finite and >= 0, got " + gamma);

        // Indekse racunamo jednom ovdje, a ne u svakom pozivu dist.
        // Objekt pamti ova polja, a ne masku, pa kasnija promjena
        // maske kod pozivatelja ne utjece na udaljenost.
        int[] num = Categorical.indicesOf(categorical, false);
        int[] cat = Categorical.indicesOf(categorical, true);

        return new Distance() {
            @Override
            public double dist(double[] a, double[] b) {
                // Numericki dio, kvadrirana euklidska udaljenost.
                double sNum = 0.0;
                for (int t : num) {
                    double diff = a[t] - b[t];
                    sNum += diff * diff;
                }

                // Kategoricki dio: Hamming.
                int sCat = 0;
                for (int t : cat) {
                    if (a[t] != b[t])
                        sCat++;
                }
                return sNum + gamma * sCat;
            }

            // Oba dijela samo rastu, pa mozemo stati cim zbroj prijede eps.
            // Kategoricki dio ne zbrajamo postupno (s += gamma), nego svaki put
            // racunamo sNum + gamma * sCat, istim izrazom kao dist. Tako je
            // zadnja usporedena vrijednost tocno dist(a, b), pa within vraca
            // tocno dist(a, b) <= eps, bez razlika u zaokruzivanju.
            @Override
            public boolean within(double[] a, double[] b, double eps) {
                double sNum = 0.0;
                for (int t : num) {
                    double diff = a[t] - b[t];
                    sNum += diff * diff;
                    if (sNum > eps)
                        return false;
                }
                int sCat = 0;
                for (int t : cat) {
                    if (a[t] != b[t]) {
                        sCat++;
                        if (sNum + gamma * sCat > eps)
                            return false;
                    }
                }
                return true;
            }
        };
    }
}