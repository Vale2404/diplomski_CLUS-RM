package redescriptionmining;

import java.io.File;
import java.util.ArrayList;

/**
 * Kombinirana inicijalizacija algoritma CLUS-RM.
 *
 * Umjesto numRandomRestarts pokretanja iste inicijalizacije izvodi se zadani niz
 * pokretanja: nekoliko pokretanja s izvornom inicijalizacijom i po jedno pokretanje
 * za svaki navedeni algoritam klasteriranja i pogled. Redeskripcije iz svih
 * pokretanja odabiru se u isti skup redeskripcija, kao kod numRandomRestarts.
 *
 * Primjer u datoteci s postavkama:
 *   CombinedInitialization = default:10; hdbscan:W1,W2; spectral:W2; hierarchical-ward:W1
 *   ClusteringSettingsW1 = clustering_W1.set
 *   ClusteringSettingsW2 = clustering_W2.set
 *
 * Datoteke ClusteringSettingsW1 i ClusteringSettingsW2 sadrze parametre algoritama
 * klasteriranja za pojedini pogled (NumOfClusters, HdbscanMinPts, SpectralDistance, ...),
 * u istom obliku kao glavna datoteka s postavkama.
 */
public class CombinedInitialization {

    /** Jedno pokretanje: metoda, varijanta i pogled nad kojim se klasterira. */
    public static class Run {
        public final String method;  // default, kmeans, kmodes, kprototypes, dbscan, hdbscan, densitygrid, hierarchical, spectral
        public final String variant; // veza (hierarchical) ili inicijalizacija centara (kmeans, kmodes, kprototypes), "" ako nije zadana
        public final int view;       // 0 = W1, 1 = W2, -1 = izvorna inicijalizacija

        Run(String method, String variant, int view) {
            this.method = method;
            this.variant = variant;
            this.view = view;
        }

        @Override
        public String toString() {
            if (view < 0)
                return method;
            return method + (variant.isEmpty() ? "" : "-" + variant) + " nad W" + (view + 1);
        }
    }

    /** Parametri klasteriranja iz glavne datoteke s postavkama. */
    private static class Params {
        String kmeansInitMethod, kmodesInitMethod, kprototypesInitMethod;
        String hierarchicalLinkage, hierarchicalDistance;
        String dbscanDistance, hdbscanDistance, spectralDistance;
        double kprototypesGamma, dbscanEps, dbscanGamma, hdbscanGamma, spectralSigma, spectralGamma;
        int numOfClusters, dbscanMinPts, hdbscanMinPts, hdbscanMinClusterSize;

        void copyFrom(ApplicationSettings a) {
            kmeansInitMethod = a.kmeansInitMethod;
            kmodesInitMethod = a.kmodesInitMethod;
            kprototypesInitMethod = a.kprototypesInitMethod;
            hierarchicalLinkage = a.hierarchicalLinkage;
            hierarchicalDistance = a.hierarchicalDistance;
            dbscanDistance = a.dbscanDistance;
            hdbscanDistance = a.hdbscanDistance;
            spectralDistance = a.spectralDistance;
            kprototypesGamma = a.kprototypesGamma;
            dbscanEps = a.dbscanEps;
            dbscanGamma = a.dbscanGamma;
            hdbscanGamma = a.hdbscanGamma;
            spectralSigma = a.spectralSigma;
            spectralGamma = a.spectralGamma;
            numOfClusters = a.numOfClusters;
            dbscanMinPts = a.dbscanMinPts;
            hdbscanMinPts = a.hdbscanMinPts;
            hdbscanMinClusterSize = a.hdbscanMinClusterSize;
        }

        void copyTo(ApplicationSettings a) {
            a.kmeansInitMethod = kmeansInitMethod;
            a.kmodesInitMethod = kmodesInitMethod;
            a.kprototypesInitMethod = kprototypesInitMethod;
            a.hierarchicalLinkage = hierarchicalLinkage;
            a.hierarchicalDistance = hierarchicalDistance;
            a.dbscanDistance = dbscanDistance;
            a.hdbscanDistance = hdbscanDistance;
            a.spectralDistance = spectralDistance;
            a.kprototypesGamma = kprototypesGamma;
            a.dbscanEps = dbscanEps;
            a.dbscanGamma = dbscanGamma;
            a.hdbscanGamma = hdbscanGamma;
            a.spectralSigma = spectralSigma;
            a.spectralGamma = spectralGamma;
            a.numOfClusters = numOfClusters;
            a.dbscanMinPts = dbscanMinPts;
            a.hdbscanMinPts = hdbscanMinPts;
            a.hdbscanMinClusterSize = hdbscanMinClusterSize;
        }
    }

    private static final Params base = new Params();

    /** Iz zapisa CombinedInitialization gradi popis pokretanja, redom kojim su navedena. */
    public static ArrayList<Run> parse(String spec) {
        ArrayList<Run> runs = new ArrayList<>();

        for (String part : spec.split(";")) {
            part = part.trim();
            if (part.isEmpty())
                continue;

            String[] kv = part.split(":", 2);
            if (kv.length != 2)
                throw new IllegalArgumentException("Neispravan dio \"" + part
                        + "\" u CombinedInitialization, ocekuje se oblik metoda:vrijednost.");

            String name = kv[0].trim().toLowerCase();
            String value = kv[1].trim();

            // varijanta se zadaje iza crtice ili podvlake, npr. hierarchical-ward ili kmeans-kmeans++
            int sep = name.indexOf('-');
            if (sep < 0)
                sep = name.indexOf('_');
            String method = sep < 0 ? name : name.substring(0, sep);
            String variant = sep < 0 ? "" : name.substring(sep + 1);

            if (method.equals("default")) {
                if (!variant.isEmpty())
                    throw new IllegalArgumentException("Izvorna inicijalizacija nema varijantu: \"" + part + "\".");
                int count = Integer.parseInt(value);
                if (count < 0)
                    throw new IllegalArgumentException("Broj pokretanja izvorne inicijalizacije mora biti >= 0: \""
                            + part + "\".");
                for (int i = 0; i < count; i++)
                    runs.add(new Run("default", "", -1));
                continue;
            }

            check(method, variant, part);

            for (String v : value.split(",")) {
                v = v.trim();
                if (v.equalsIgnoreCase("W1"))
                    runs.add(new Run(method, variant, 0));
                else if (v.equalsIgnoreCase("W2"))
                    runs.add(new Run(method, variant, 1));
                else
                    throw new IllegalArgumentException("Nepoznat pogled \"" + v + "\" u CombinedInitialization: \""
                            + part + "\". Dozvoljeni: W1, W2.");
            }
        }

        if (runs.isEmpty())
            throw new IllegalArgumentException("CombinedInitialization ne sadrzi nijedno pokretanje.");

        return runs;
    }

    private static void check(String method, String variant, String part) {
        boolean ok;
        switch (method) {
            case "hierarchical":
                ok = variant.isEmpty() || variant.equals("single") || variant.equals("complete")
                        || variant.equals("average") || variant.equals("ward");
                break;
            case "kmeans":
                ok = variant.isEmpty() || variant.equals("forgy") || variant.equals("kmeans++");
                break;
            case "kmodes":
            case "kprototypes":
                ok = variant.isEmpty() || variant.equals("forgy") || variant.equals("kmeans++")
                        || variant.equals("cao");
                break;
            case "dbscan":
            case "hdbscan":
            case "densitygrid":
            case "spectral":
                ok = variant.isEmpty();
                break;
            default:
                throw new IllegalArgumentException("Nepoznata metoda \"" + method + "\" u CombinedInitialization: \""
                        + part + "\". Dozvoljene: default, kmeans, kmodes, kprototypes, dbscan, hdbscan,"
                        + " densitygrid, hierarchical, spectral.");
        }
        if (!ok)
            throw new IllegalArgumentException("Nepoznata varijanta \"" + variant + "\" za metodu " + method
                    + " u CombinedInitialization: \"" + part + "\".");
    }

    /** Pamti parametre klasteriranja iz glavne datoteke. Poziva se jednom, prije prvog pokretanja. */
    public static void saveBase(ApplicationSettings appset) {
        // datoteke za poglede provjeravamo odmah, a ne tek kod pokretanja koje ih treba
        checkFile(appset.clusteringSettingsW1, 1);
        checkFile(appset.clusteringSettingsW2, 2);
        base.copyFrom(appset);
    }

    private static void checkFile(String file, int view) {
        if (!file.isEmpty() && !new File(file).isFile())
            throw new IllegalArgumentException("Ne postoji datoteka s parametrima klasteriranja za W" + view
                    + ": " + new File(file).getAbsolutePath());
    }

    /** Postavlja appset za zadano pokretanje. */
    public static void apply(Run run, ApplicationSettings appset) {
        // parametri iz glavne datoteke, da ne ostanu vrijednosti iz prethodnog pokretanja
        base.copyTo(appset);
        // izvorna inicijalizacija ima jedan ciljni atribut, a klasteriranje ga ponovno postavlja na k
        appset.numInitTargets = 1;

        if (run.view >= 0) {
            String file = run.view == 0 ? appset.clusteringSettingsW1 : appset.clusteringSettingsW2;
            if (!file.isEmpty())
                appset.readSettings(new File(file));
            appset.viewForClustering = run.view;
            setVariant(run, appset);
        }

        appset.initializationMethod = run.method;
    }

    private static void setVariant(Run run, ApplicationSettings appset) {
        String v = run.variant;
        if (v.isEmpty())
            return;

        switch (run.method) {
            case "hierarchical":
                appset.hierarchicalLinkage = v.substring(0, 1).toUpperCase() + v.substring(1);
                break;
            case "kmeans":
                appset.kmeansInitMethod = initName(v);
                break;
            case "kmodes":
                appset.kmodesInitMethod = initName(v);
                break;
            case "kprototypes":
                appset.kprototypesInitMethod = initName(v);
                break;
        }
    }

    private static String initName(String v) {
        if (v.equals("forgy"))
            return "Forgy";
        if (v.equals("kmeans++"))
            return "Kmeans++";
        return "Cao";
    }
}