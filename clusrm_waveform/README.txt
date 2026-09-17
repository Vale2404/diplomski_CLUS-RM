1. Kopiraj ovu mapu (W1.arff, W2.arff, preferences.txt, Settings_*.set, out/) npr. u korijen projekta diplomski_CLUS-RM/clusrm_waveform
2. IntelliJ Run configuration:
   Main class:        redescriptionmining.SupplementingRandomForest
   Program arguments: Settings_windows.set   (ili Settings_linux_mac.set)
   Working directory: <putanja do ove mape>
   VM options:        -Xmx4g
3. Rezultat: out/redescriptions.rr1.rr i out/RuleData1.csv
Osnovna verzija: numSupplementTrees = 0, numThreads = 1
Rosandina verzija: numSupplementTrees = 8 (ili 16), numThreads = 4
