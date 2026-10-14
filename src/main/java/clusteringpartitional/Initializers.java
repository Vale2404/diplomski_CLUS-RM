package clusteringpartitional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public final class Initializers {
    private Initializers() {}


    // Ako imamo k klastera, ova inicijalizacija pocetnih centroida bira
    // nasumicno bilo kojih k tocaka bez ponavljanja istih
    // iz podataka kao pocetne centroide.
    public static final class Forgy implements Initializer {
        @Override
        public double[][] initialCentroids(double[][] X, int k, Distance distance, Random rnd) {

            // Broj tocaka.
            int n = X.length;

            // Provjera da u skupu X postoji barem k razlicitih tocaka.
            // Bez toga Forgy ne moze vratiti k razlicitih centroida.
            requireDistinctRows(X, k);

            // Skup odabranih tocaka.
            Set<RowKey> chosenRows = new HashSet<>();

            // Skup centroida koji ce ova metoda vratiti, za svaki od k klastera,
            // jedan vektor.
            double[][] C = new double[k][];

            // Za svaki klaster nasumicno odaberemo centroid kao tocku
            // koja nije vec odabrana kao centroid u nekoj prethodnoj iteraciji.
            for (int j = 0; j < k; j++) {
                int idx;

                // Novi indeks prihvacamo samo ako vrijednost
                // tocke na tom indeksu jos nije odabrana.
                // Ako podaci nemaju duplikata uvjet je uvijek ispunjen.
                do {
                    idx = rnd.nextInt(n);
                } while (!chosenRows.add(new RowKey(X[idx])));

                // Pridruzimo tu tocku kao centroid j-tog klastera.
                C[j] = X[idx].clone();
            }
            return C;
        }
        @Override
        public String name() {
            return "Forgy";
        }
    }


    // Centroide biramo jedan po jedan, i to tako da tocke daleko od vec odabranih
    // centroida imaju vecu sansu postati sljedeci centroid. Vjerojatnost odabira tocke
    // proporcionalna je kvadratu udaljenosti do najblizeg vec odabranog centroida (ako
    // se koristi SQ_EUCLIDEAN), a proporcionalna samoj udaljenosti uz HAMMING.
    // Tako se centri prirodno rasire po podacima, a Forgy to ne radi, on lako odabere
    // dva centroida u istom "oblaku" podataka.
    public static final class KmeansPlusPlus implements Initializer {
        @Override
        public double[][] initialCentroids(double[][] X, int k, Distance distance, Random rnd) {

            // Broj tocaka.
            int n = X.length;

            // Jamci da postoji barem k razlicitih tocaka, bez
            // toga ne mozemo dobiti k razlicitih centroida.
            requireDistinctRows(X, k);

            // Centroidi koje popunjavamo jedan po jedan i na kraju vratimo.
            double[][] C = new double[k][];

            // Prvi centroid biramo nasumicno.
            int firstIdx = rnd.nextInt(n);
            C[0] = X[firstIdx].clone();

            // minDist[i] je udaljenost tocke i do najblizeg dosad odabranog
            // centroida. Dok imamo samo 1 centroid, to je trivijalno, za svaku
            // tocku je to udaljenost bas do tog jedinog centroida. Uz SQ_EUCLIDEAN
            // ovo vec je kvadrat udaljenosti.
            double[] minDist = new double[n];
            for (int i = 0; i < n; i++)
                minDist[i] = distance.dist(X[i], C[0]);

            // Glavna petlja, biramo sljedece centroide.
            for (int j = 1; j < k; j++) {

                // U svakom prolazu biramo centroid C[j]. Prolaz
                // ima 3 koraka: zbroj tezina, izvlacenje, azuriranje minDist.
                // Zbrajamo sve minDist vrijednosti, to je ukupna
                // "tezina" svih tocaka, treba nam kao normalizacijska
                // konstanta za vjerojatnost. Tocka i biti ce odabrana kao novi
                // centroid s vjerojatnoscu minDist[i] / sum.
                // Uz metriku (SQ_EUCLIDEAN, HAMMING) sigurno je sum > 0 jer je
                // odabrano j < k centroida, a razlicitih
                // tocaka ima barem k, zato postoji tocka koja nije jednaka niti
                // jednom centroidu, pa je njen minDist strogo pozitivan.
                double sum = 0.0;
                for (int i = 0; i < n; i++)
                    sum += minDist[i];

                // Mijesana udaljenost uz gamma = 0 nije metrika: dvije tocke koje
                // se razlikuju samo u kategorickim stupcima imaju udaljenost 0.
                // Tada sve preostale tocke mogu biti na udaljenosti 0 od nekog
                // centra, pa nema tocke koju bismo mogli izvuci.
                if (sum <= 0.0)
                    throw new IllegalStateException("all points are at distance 0 from chosen centers after choosing "
                            + j + " centers (with k-prototypes this can happen when gamma = 0)");

                // Tocka s vecim minDist (dalje od svih do sad odabranih
                // centroida) ima vecu sansu da bude odabrana kao sljedeci.
                // Ovo funkcionira kao kotac ruleta:
                // Zamisli interval [0, sum> podijeljen na n uzastopnih komada, gdje
                // komad tocke i ima duljinu minDist[i]:
                // |--0--|-1-|------------2------------|--3--| ... |
                // 0                                              sum
                // r je nasumican broj u tom intervalu. Petlja zbraja duljine komada (cum)
                // i staje kod prvog komada ciji desni rub prelazi r. To je komad u koji je r pao.
                // Vjerojatnost da r padne u komad i jednaka je njegovoj duljini podijeljenoj s ukupnom
                // duljinom, tj. minDist[i]/sum. Tocno ono sto zelimo.
                // Uvjet je strogi > zato da komadi duljine 0 (tocke koje su vec centri) ne mogu biti
                // odabrani. Kod njih cum ne raste, pa ako cum > r nije vrijedilo prije njih, ne vrijedi ni kod njih.
                // chosen = n - 1 je samo pocetna vrijednost koju Java trazi.
                double r = rnd.nextDouble() * sum;
                double cum = 0.0;
                int chosen = n - 1;
                for (int i = 0; i < n; i++) {
                    cum += minDist[i];
                    if (cum > r) {
                        chosen = i;
                        break;
                    }
                }

                // Odabrana tocka postaje novi centroid.
                C[j] = X[chosen].clone();

                // Azuriramo minDist novim centroidom.
                // Najblizi od j+1 centara je ili stari najblizi ili novi
                // centar, pa je dovoljno zadrzati manju od dvije udaljenosti.
                for (int i = 0; i < n; i++) {
                    double dist = distance.dist(X[i], C[j]);
                    if (dist < minDist[i])
                        minDist[i] = dist;
                }
            }
            return C;
        }

        @Override
        public String name() {
            return "Kmeans++";
        }
    }


    // Caova inicijalizacija za kategoricke i mijesane podatke.
    // Dobar pocetni centar mora biti tipican (lezati u "srcu" neke grupe)
    // i daleko od vec odabranih centara. Tipicnost mjerimo gustocom:
    // koliko cesto se vrijednosti atributa tocke pojavljuju u cijelom skupu.
    // Prvi centar je tocka najvece gustoce, a svaki sljedeci je tocka s
    // najvecim umnoskom gustoce i udaljenosti do najblizeg vec odabranog
    // centra. Sama udaljenost bi birala outliere, a sama gustoca bi sve
    // centre stavila u najveci klaster, pa umnozak balansira ta dva zahtjeva.
    // Za mijesane podatke (prosirenje izvorne metode) gustocu racunamo samo po
    // kategorickim stupcima, jer je frekvencija vrijednosti definirana samo za
    // njih, a udaljenost je ona koju koristi algoritam (kod k-prototypesa
    // mijesana udaljenost), pa numericki dio utjece na to koliko je tocka
    // daleko od centara. Bez maske su svi stupci kategoricki i metoda je
    // tocno izvorni Cao.
    // Metoda je deterministicka, za iste podatke uvijek daje iste centre.
    public static final class Cao implements Initializer {
        // categorical[t] == true znaci da je stupac t kategoricki.
        // null znaci da su svi stupci kategoricki (izvorni Cao).
        private final boolean[] categorical;

        // Izvorni Cao za cisto kategoricke podatke.
        public Cao() {
            this.categorical = null;
        }

        // Cao za mijesane podatke. Maska mora biti ista kao ona
        // predana kPrototypes, a kopira se, pa kasnija promjena
        // kod pozivatelja ne utjece na inicijalizator.
        // Bez ijednog kategorickog stupca sve gustoce bi bile 0,
        // pa nijedna tocka ne bi mogla biti odabrana. Zato to
        // odbijamo odmah, a ne tek u initialCentroids.
        public Cao(boolean[] categorical) {
            if (categorical == null)
                throw new IllegalArgumentException("categorical cannot be null");
            boolean anyCat = false;
            for (boolean c : categorical)
                anyCat |= c;
            if (!anyCat)
                throw new IllegalArgumentException("Cao requires at least one categorical column");
            this.categorical = categorical.clone();
        }

        @Override
        public double[][] initialCentroids(double[][] X, int k, Distance distance, Random rnd) {
            // rnd se ne koristi jer metoda nema slucajnosti,
            // ali ga sucelje Initializer trazi.

            // Broj tocaka.
            int n = X.length;

            // Broj atributa svake tocke.
            int d = X[0].length;

            // Jamci da postoji barem k razlicitih tocaka. Bez toga ne
            // mozemo dobiti k razlicitih centara.
            requireDistinctRows(X, k);

            // Maska koju koristimo: zadana, ili "sve kategoricko".
            boolean[] mask = categorical;
            if (mask == null) {
                mask = new boolean[d];
                Arrays.fill(mask, true);
            }

            // Korak 2: gustoca
            // Velicina domene svakog stupca. Za kategoricke stupce ujedno
            // provjerava da su vrijednosti ispravni kodovi, a provjerava i da
            // maska ima duljinu d. Numericki stupci imaju domain 0.
            int[] domain = Categorical.domains(X, mask);

            // Indeksi kategorickih stupaca. Samo oni ulaze u gustocu.
            int[] cat = Categorical.indicesOf(mask, true);

            // Tablica frekvencija za cijeli skup podataka:
            // freq[t][v] = broj tocaka cija je vrijednost stupca t jednaka v.
            // Retci za numericke stupce ostaju null i nikad se ne koriste.
            int[][] freq = new int[d][];
            for (int t : cat)
                freq[t] = new int[domain[t]];

            // Jedan prolaz kroz podatke: za svaku tocku i svaki kategoricki
            // stupac povecamo brojac vrijednosti koju ta tocka ima u tom stupcu.
            for (int i = 0; i < n; i++)
                for (int t : cat)
                    freq[t][(int) X[i][t]]++;

            // Gustoca tocke: zbroj frekvencija njezinih vrijednosti po
            // kategorickim stupcima. Prava gustoca iz rada je
            // dens[i] / (n * broj kategorickih stupaca), ali dijeljenje istom
            // konstantom ne mijenja koja tocka ima najveci score, pa ga
            // preskacemo i ostajemo u cijelim brojevima.
            // Svaka tocka ima dens >= broj kategorickih stupaca >= 1, jer
            // njezina vlastita vrijednost ulazi u frekvenciju.
            int[] dens = new int[n];
            for (int i = 0; i < n; i++)
                for (int t : cat)
                    dens[i] += freq[t][(int) X[i][t]];

            // korak 3: biranje centroida
            // Centroidi koje popunjavamo jedan po jedan i na kraju vratimo.
            double[][] C = new double[k][];

            // Prvi centroid je tocka najvece gustoce. Uvjet je strogi >, pa
            // kod izjednacenja ostaje tocka s najmanjim indeksom, sto metodu
            // cini deterministickom.
            int first = 0;
            for (int i = 1; i < n; i++) {
                if (dens[i] > dens[first])
                    first = i;
            }
            C[0] = X[first].clone();

            // minDist[i] je udaljenost tocke i do najblizeg dosad odabranog
            // centroida, mjerena udaljenoscu koju koristi algoritam.
            double[] minDist = new double[n];
            for (int i = 0; i < n; i++)
                minDist[i] = distance.dist(X[i], C[0]);

            // Glavna petlja, biramo sljedece centroide.
            for (int j = 1; j < k; j++) {

                // Score tocke je umnozak gustoce i udaljenosti do najblizeg
                // vec odabranog centroida. Za novi centroid uzimamo tocku s
                // najvecim scoreom, dakle tocku koja je istovremeno tipicna
                // i daleko od postojecih centroida.
                // bestScore krece od 0, a uvjet je strogi >, pa tocka sa
                // scoreom 0 nikad ne moze biti odabrana. To su upravo vec
                // odabrani centroidi i njihovi duplikati (uz mijesanu udaljenost
                // to su sve tocke na udaljenosti 0 od nekog centra), jer im je minDist 0.
                // Strogi > ujedno znaci da kod izjednacenja ostaje manji indeks.
                int best = -1;
                double bestScore = 0.0;
                for (int i = 0; i < n; i++) {
                    double score = dens[i] * minDist[i];
                    if (score > bestScore) {
                        bestScore = score;
                        best = i;
                    }
                }

                // Uz metriku (npr. Hamming) zbog requireDistinctRows uvijek
                // postoji tocka s minDist > 0, pa i scoreom > 0. Mijesana
                // udaljenost uz gamma = 0 nije metrika: dvije tocke koje se
                // razlikuju samo u kategorickim stupcima imaju udaljenost 0.
                // Tada se moze dogoditi da su sve preostale tocke na udaljenosti
                // 0 od nekog centra, pa ne mozemo odabrati k centara.
                if (best == -1)
                    throw new IllegalStateException("no point has positive score after choosing "
                            + j + " centers (all remaining points are at distance 0 from a chosen center; "
                            + "with k-prototypes this can happen when gamma = 0)");

                // Odabrana tocka postaje novi centroid.
                C[j] = X[best].clone();

                // Azuriramo minDist novim centroidom.
                // Najblizi od j+1 centara je ili stari najblizi ili novi
                // centar, pa je dovoljno zadrzati manju od dvije udaljenosti.
                for (int i = 0; i < n; i++) {
                    double dist = distance.dist(X[i], C[j]);
                    if (dist < minDist[i])
                        minDist[i] = dist;
                }
            }
            return C;
        }

        @Override
        public String name() {
            return "Cao";
        }
    }


    // Provjerava da X ima barem k medusobno razlicitih redaka. Bez toga
    // nijedan inicijalizator ne moze vratiti k razlicitih centara.
    // Staje cim nade k razlicitih, pa je obicno vrlo brza.
    private static void requireDistinctRows(double[][] X, int k) {
        if (k > X.length)
            throw new IllegalArgumentException("k > n");
        Set<RowKey> distinct = new HashSet<>();
        for (double[] row : X) {
            distinct.add(new RowKey(row));
            if (distinct.size() >= k)
                return;
        }
        throw new IllegalArgumentException("X has fewer than k distinct rows");
    }


    // Omotac oko tocke iz X koji omogucuje spremanje tocke u HashSet
    // i usporedbu po vrijednosti (double[] sam po sebi usporeduje
    // samo reference). Tocka se kopira i -0.0 se pretvara u 0.0,
    // jer Arrays.equals razlikuje te dvije vrijednosti, a za
    // udaljenost su one ista tocka.
    private static final class RowKey {
        private final double[] point;
        private final int hash;

        RowKey(double[] point) {
            double[] p = point.clone();
            for (int t = 0; t < p.length; t++) {
                if (p[t] == 0.0)  // vrijedi i za -0.0
                    p[t] = 0.0;
            }
            this.point = p;
            this.hash = Arrays.hashCode(p);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof RowKey && Arrays.equals(point, ((RowKey) o).point);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }
}