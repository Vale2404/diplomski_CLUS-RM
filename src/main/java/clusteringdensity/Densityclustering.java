package clusteringdensity;

// Zajednicko sucelje za DBSCAN i HDBSCAN. Oba primaju matricu n*d i
// vracaju isti Result (oznake -1 = sum, inace 0, 1, 2, ...), pa kod koji
// koristi rezultat (npr. CLUS-RM) ne mora znati koji je algoritam radio.
public interface Densityclustering {
    Dbscan.Result fit(double[][] X);
}