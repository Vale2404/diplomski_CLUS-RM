package kmeans;

public interface Distance {
    double dist(double[] a, double[] b);

    Distance SQ_EUCLIDEAN = (a, b) -> {
        double s = 0.0;
        for (int i = 0; i < a.length; i++) {
            double d = a[i] - b[i];
            s += d * d;
        }
        return s;
    };

    Distance EUCLIDEAN = (a, b) -> Math.sqrt(SQ_EUCLIDEAN.dist(a, b));
}