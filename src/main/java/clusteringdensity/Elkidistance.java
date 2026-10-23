package clusteringdensity;

import elki.data.NumberVector;
import elki.data.type.SimpleTypeInformation;
import elki.distance.PrimitiveDistance;

// Adapter: nasa udaljenost (Distance) u obliku koji ELKI razumije.
// ELKI tocke predaje kao NumberVector, a nase udaljenosti rade na double[],
// pa vektor samo prepisemo u polje. Vrijednosti su zato bit po bit iste
// kao Distance.dist, sto je bitno za usporedbu s DBSCAN-om.
//
// Paziti na imena: elki.distance.Distance i clusteringdensity.Distance su
// razlicita sucelja. Ovdje "Distance" uvijek znaci nase (isti paket),
// a ELKI-jevo se ne uvozi.
public class Elkidistance implements PrimitiveDistance<NumberVector> {
    private final Distance distance;

    Elkidistance(Distance distance) {
        this.distance = distance;
    }

    @Override
    public double distance(NumberVector a, NumberVector b) {
        return distance.dist(a.toArray(), b.toArray());
    }

    @Override
    public SimpleTypeInformation<? super NumberVector> getInputTypeRestriction() {
        return NumberVector.FIELD;
    }
}