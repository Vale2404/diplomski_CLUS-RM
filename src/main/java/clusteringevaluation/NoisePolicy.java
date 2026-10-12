package clusteringevaluation;

// Nacin postupanja s tockama oznacenima kao sum,
// npr. u rezultatu DBSCAN-a.
// Zasto je ovo eksplicitan parametar: sum nije klaster, pa definicija siluete
// ne kaze sto s njim. Razliciti izbori daju razlicite brojeve, a particije su
// medusobno usporedive samo ako je izbor isti. Zato ga pozivatelj mora navesti
// svjesno, umjesto da se skriva u nekoj zadanoj vrijednosti.
// Ako particija nema suma, obje politike daju identican rezultat.

// Sto se tice CH-a:
//IGNORE: tocke suma ne ulaze u SSW, SSB ni n.
//SINGLETON: svaka tocka suma je jednoclani klaster. Doprinosi 0 u SSW, ali ulazi u SSB i povecava k i n.
//Ovo je vazno jer SINGLETON djeluje razlicito na ta dva indeksa.

public enum NoisePolicy {

    // Tocke suma potpuno se uklanjaju iz izracuna: za njih se ne racuna s(i)
    // i ne ulaze ni u a(i) ni u b(i) ostalih tocaka.
    // Mjeri kvalitetu samo onoga sto je algoritam proglasio klasterima.
    // Nedostatak: algoritam koji "teske" tocke proglasi sumom moze dobiti
    // umjetno visoku siluetu.
    IGNORE,


    // Svaka tocka suma postaje zaseban jednoclani klaster. Po dogovoru za
    // jednoclane klastere s(i) = 0, a drugim tockama moze biti
    // susjedni klaster pri racunanju b(i).
    // Stroze prema algoritmima koji oznacavaju puno suma: svaka takva tocka
    // povlaci prosjek prema 0.
    SINGLETON;

    // Dogovorena oznaka suma. Sve ostale oznake moraju biti >= 0.
    public static final int NOISE_LABEL = -1;
}