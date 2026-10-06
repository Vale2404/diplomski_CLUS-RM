package clusteringhierarchical;

// Jedan korak aglomerativnog klasteriranja: spajanje dvaju klastera.
// Klastere oznacavamo: pocetne tocke su klasteri 0, ..., n-1,
// a klaster nastao u koraku t dobiva oznaku n + t. Tako se iz niza
// spajanja moze rekonstruirati cijeli dendrogram.
public final class Merge {

    // Oznake spojenih klastera, uvijek left < right.
    public final int left;
    public final int right;

    // Udaljenost izmedu ta dva klastera u trenutku spajanja.
    public final double height;

    // Broj tocaka u novom klasteru.
    public final int size;

    Merge(int left, int right, double height, int size) {
        this.left = left;
        this.right = right;
        this.height = height;
        this.size = size;
    }

    @Override
    public String toString() {
        return String.format("spoji %d i %d na visini %.4f, velicina %d",
                left, right, height, size);
    }
}