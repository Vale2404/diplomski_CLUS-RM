package clusteringevaluation;


import java.util.HashMap;
import java.util.Map;

// Particija s oznakama preslikanima u 0..k-1.
// Zasto normalizacija: algoritmi daju oznake na razlicite nacine
// (hijerarhijsko npr. ID-jeve cvorova, k-modes nakon praznog klastera
// oznake s "rupama"). Indeksi interno trebaju gusta polja indeksirana
// klasterom, a ovako adapteri ne moraju o tome brinuti. Usput nestaju i
// prazni klasteri: dobivaju indeks samo oznake koje se stvarno pojavljuju.
final class Partition {

    // Normalizirana oznaka za svaku tocku, ili NOISE_LABEL ako se tocka ignorira.
    final int[] cluster;

    // Broj tocaka u svakom klasteru, svaki je >= 1.
    final int[] size;

    // Broj klastera nakon normalizacije i primjene politike suma.
    final int k;

    private Partition(int[] cluster, int[] size, int k) {
        this.cluster = cluster;
        this.size = size;
        this.k = k;
    }

    static Partition of(int[] labels, NoisePolicy policy) {
        int n = labels.length;
        int[] cluster = new int[n];
        Map<Integer, Integer> ids = new HashMap<Integer, Integer>();
        int next = 0;

        for (int i = 0; i < n; i++) {
            int label = labels[i];

            if (label == NoisePolicy.NOISE_LABEL) {
                switch (policy) {
                    case IGNORE:
                        cluster[i] = NoisePolicy.NOISE_LABEL;
                        break;
                    case SINGLETON:
                        cluster[i] = next++;
                        break;
                    default:
                        throw new IllegalStateException("Unknown policy: " + policy);
                }
            } else if (label < 0) {
                throw new IllegalArgumentException(
                        "labels must be >= 0 or NOISE = -1: " + label);
            } else {
                Integer id = ids.get(label);
                if (id == null) {
                    id = next++;
                    ids.put(label, id);
                }
                cluster[i] = id;
            }
        }

        int[] size = new int[next];
        for (int c : cluster) {
            if (c != NoisePolicy.NOISE_LABEL) {
                size[c]++;
            }
        }

        return new Partition(cluster, size, next);
    }
}