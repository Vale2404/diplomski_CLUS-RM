package clusteringevaluation;

// Nacin postupanja s tockama oznacenima kao sum,
// npr. u rezultatu DBSCAN-a.
// Zasto je ovo eksplicitan parametar: sum nije klaster, pa definicija siluete
// ne kaze sto s njim. Razliciti izbori daju razlicite brojeve, a particije su
// medusobno usporedive samo ako je izbor isti. Zato ga pozivatelj mora navesti
// svjesno, umjesto da se skriva u nekoj zadanoj vrijednosti.
// Ako particija nema suma, obje politike daju identican rezultat.

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
    SINGLETON
}