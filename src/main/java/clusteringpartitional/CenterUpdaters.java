package clusteringpartitional;

public final class CenterUpdaters {
    private CenterUpdaters() {}


    // Klasicno k-means azuriranje: za svaki klaster izracunamo
    // aritmeticku sredinu svih tocaka tog klastera, i tu tocku
    // postavimo za novi centroid tog klastera.
    public static final class Mean implements CenterUpdater {

        // Korak azuriranja. Oznake su fiksne, a pomicu se centroidi.
        @Override
        public void update(double[][] X, double[][] C, int[] labels) {

            // Broj tocaka.
            int n = X.length;

            // Dimenzija tocaka.
            int d = X[0].length;

            // Broj klastera tj. centroida.
            int k = C.length;

            // sums je vektor zbroja svih tocaka za svaki klaster,
            // npr. sums[i][j] = zbroj j-te komponente svih tocaka
            // iz klastera i.
            // counts je vektor koji kaze koliko je tocaka u pojedinom
            // klasteru, npr. counts[i] = broj tocaka u klasteru i.
            // Kada imamo te dvije stvari lako se izracuna aritm. sredina.
            double[][] sums = new double[k][d];
            int[] counts = new int[k];
            for (int i = 0; i < n; i++) {
                int c = labels[i];
                counts[c]++;
                double[] s = sums[c];
                for (int j = 0; j < d; j++) {
                    s[j] += X[i][j];
                }
            }

            // fit prije poziva azuriranja popravlja prazne klastere,
            // pa ovdje prazan klaster znaci gresku.
            for (int j = 0; j < k; j++) {
                if (counts[j] == 0)
                    throw new IllegalStateException("empty cluster " + j);
            }

            // Za svaki klaster racunamo aritm. sredinu svih tocka
            // po koordinatama i tu novu tocku postavljamo kao novi centroid
            // tog klastera.
            for (int j = 0; j < k; j++) {
                double[] newC = new double[d];
                for (int t = 0; t < d; t++) {
                    newC[t] = sums[j][t] / counts[j];
                }
                C[j] = newC;
            }
        }
    }


    // k-modes azuriranje za kategoricke podatke: centroid klastera
    // ne postaje aritmeticka sredina tocaka unutar tog klastera,
    // vec za svaki atribut svih tocaka u klasteru odredimo vrijednost
    // koja se najcesce pojavljuje i tu vrijednost stavimo za vrijednost
    // tog atributa u centroidu.
    // Kategorije moraju biti kodirane kao nenegativni cijeli brojevi
    // 0, 1, 2, ... spremljeni u double.
    // Mod je slozen po atributima, pa ne mora biti jednak nijednom
    // retku iz X, sto je standardno ponasanje k-modesa.
    public static final class Mode implements CenterUpdater {

        // Velicina domene svakog atributa (najveci kod + 1). Update
        // to treba, racuna se jednom u prepare, a ne u svakoj iteraciji,
        // jer se podaci X tijekom fit-a ne mijenjaju. Zbog ovog polja Mode ima stanje,
        // pa jednu instancu ne treba dijeliti izmedu paralelnih fit-ova.
        private int[] domain;

        @Override
        public void prepare(double[][] X) {
            domain = Categorical.domains(X);
        }

        // Korak azuriranja centroida.
        @Override
        public void update(double[][] X, double[][] C, int[] labels) {

            // Broj tocaka.
            int n = X.length;

            // Broj atributa svake tocke.
            int d = X[0].length;

            // Broj klastera.
            int k = C.length;

            if (domain == null || domain.length != d)
                throw new IllegalStateException("prepare(X) must be called before update");

            // Polje koje za svaki klaster drzi broj njegovih tocaka,
            // npr. counts[j] je koliko tocaka je u klasteru j.
            // To polje sluzi samo za sljedecu provjeru da nema
            // praznog klastera. Nigdje vise nije potrebno.
            int[] counts = new int[k];
            for (int i = 0; i < n; i++)
                counts[labels[i]]++;

            // fit prije poziva popravlja prazne klastere,
            // pa ovdje prazan klaster znaci gresku.
            for (int j = 0; j < k; j++) {
                if (counts[j] == 0)
                    throw new IllegalStateException("empty cluster " + j);
            }

            // Novi centroidi.
            double[][] newC = new double[k][d];

            // Vanjska petlja po atributima, za svaki atribut nezavisno napravimo
            // tablicu frekvencija freq[klaster][kategorija].
            // freq[j][v] = broj tocaka u klasteru j koje imaju vrijednost
            // promatranog atributa jednaku v.
            for (int t = 0; t < d; t++) {

                // Za svaki atribut posebno!
                int[][] freq = new int[k][domain[t]];

                // Za svaku tocku i gledamo njen klaster i
                // vrijednost njenog atributa t.
                for (int i = 0; i < n; i++)
                    freq[labels[i]][(int) X[i][t]]++;

                // Najcesca vrijednost atributa t u svakom klasteru j.
                // Kada ju nademo onda postavljamo vrijednost
                // atributa t novog centroida klastera j na tu najcescu.
                // Strogi > znaci da kod izjednacenja ostaje manji kod,
                // pa je rezultat deterministicki.
                for (int j = 0; j < k; j++) {
                    int best = 0;
                    for (int v = 1; v < domain[t]; v++) {
                        if (freq[j][v] > freq[j][best])
                            best = v;
                    }
                    newC[j][t] = best;
                }
            }
            System.arraycopy(newC, 0, C, 0, k);
        }
    }


    // k-prototypes azuriranje za mijesane podatke. Centroid se racuna
    // stupac po stupac: za numericki stupac uzimamo aritmeticku sredinu
    // tocaka klastera (kao Mean), a za kategoricki stupac najcescu
    // vrijednost (kao Mode). To je ispravno jer se funkcija cilja rastavlja
    // na clanove od kojih svaki ovisi o jednoj komponenti centroida,
    // pa svaku komponentu mozemo optimizirati zasebno.
    // categorical[t] == true znaci da je stupac t kategoricki.
    // Kao i Mode, klasa ima stanje (domain), pa jednu instancu ne treba
    // dijeliti izmedu paralelnih fit-ova.
    public static final class Prototype implements CenterUpdater {

        // Maska tipova stupaca. Kopija, pa promjena maske kod pozivatelja
        // ne utjece na updater
        private final boolean[] categorical;

        // Indeksi numerickih i kategorickih stupaca, racunaju se jednom.
        private final int[] num;
        private final int[] cat;

        // Velicina domene svakog stupca, racuna se u prepare.
        // Za numericke stupce je 0 i ne koristi se.
        private int[] domain;

        public Prototype(boolean[] categorical) {
            if (categorical == null)
                throw new IllegalArgumentException("categorical cannot be null");
            this.categorical = categorical.clone();
            this.num = Categorical.indicesOf(this.categorical, false);
            this.cat = Categorical.indicesOf(this.categorical, true);
        }


        // Provjerava da maska ima duljinu d i da su kategoricki stupci
        // ispravni kodovi, te racuna njihove domene. Poziva se u fit-u prije
        // inicijalizatora, pa je duljina maske provjerena prije prvog
        // poziva udaljenosti.
        @Override
        public void prepare(double[][] X) {
            domain = Categorical.domains(X, categorical);
        }

        @Override
        public void update(double[][] X, double[][] C, int[] labels) {

            int n =  X.length;
            int d = X[0].length;
            int k = C.length;

            if (domain == null || domain.length != d)
                throw new IllegalStateException("prepare(X) must be called before update");

            // counts[j] = broj tocaka u klasteru j. Treba za provjeru
            // praznih klastera i za racunanje sredine.
            int[] counts = new int[k];
            for (int i = 0; i < n; i++)
                counts[labels[i]]++;

            // fit prije poziva popravlja prazne klastere,
            // pa ovdje prazan klaster znaci gresku.
            for (int j = 0; j < k; j++) {
                if (counts[j] == 0)
                    throw new IllegalStateException("empty cluster " + j);
            }

            // Novi centroidi. Svaki stupac je ili u num ili u cat,
            // pa ce svaka komponenta biti postavljena.
            double[][] newC = new double[k][d];

            // Numericki dio, isto kao Mean.
            // sums[j][s] = zbroj vrijednosti stupca num[s] po tockama
            // klastera j. Zbrajamo redom po i, pa dijelimo s counts, tocno
            // kao Mean, da uz masku bez kategorickih stupaca rezultat bude
            // bit po bit jednak k-meansu.
            double[][] sums = new double[k][num.length];
            for (int i = 0; i < n; i++) {
                double[] s = sums[labels[i]];
                for (int p = 0; p < num.length; p++)
                    s[p] += X[i][num[p]];
            }
            for (int j = 0; j < k; j++)
                for (int p = 0; p < num.length; p++)
                    newC[j][num[p]] = sums[j][p] / counts[j];


            // Kategoricki dio, isto kao Mode.
            // Za svaki kategoricki stupac t tablica frekvencija
            // freq[j][v] = broj tocaka klastera j cija je vrijednost
            // stupca t jednaka v.
            for (int t : cat) {
                int[][] freq = new int[k][domain[t]];
                for (int i = 0; i < n; i++)
                    freq[labels[i]][(int) X[i][t]]++;

                // Najcesca vrijednost u svakom klasteru. Strogi > znaci da kod
                // izjednacenja ostaje manji kod, kao u Mode, pa je rezultat
                // deterministicki i uz masku sa svim true jednak k-modesu.
                for (int j = 0; j < k; j++) {
                    int best = 0;
                    for (int v = 1; v < domain[t]; v++) {
                        if (freq[j][v] > freq[j][best])
                            best = v;
                    }
                    newC[j][t] = best;
                }
            }
            System.arraycopy(newC, 0, C, 0, k);
        }
    }
}