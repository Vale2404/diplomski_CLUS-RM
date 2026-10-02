DBLP skup za CLUS-RM (k-modes inicijalizacija)

Sadrzaj:
  W1.arff   - NEDOSTAJE: preimenuj profesorov input1DBLP.arff u W1.arff i stavi ga ovdje
  W2.arff   - profesorov input2DBLP.arff bez izmjena: 6455 autora, 304 konferencije {true,false}, bez nedostajucih vrijednosti
  preferences.txt, Settings_windows.set, Settings_linux_mac.set, out/

Pokretanje (isto kao clusrm_waveform):
  Main class:        redescriptionmining.SupplementingRandomForest
  Program arguments: Settings_windows.set   (ili Settings_linux_mac.set)
  Working directory: <putanja do ove mape>
  VM options:        -Xmx4g

Razlike u odnosu na profesorov SettingsDBLP.set:
  - relativne putanje (OutputFolder, Input1, Input2, preferenceFilePath), bez JavaPath i ClusPath
  - UKLONJEN initClusteringFileName: dok je zadan, CLUS-RM cita gotovu inicijalizaciju iz datoteke i preskace k-modes
  - uklonjeni WorkingRSSize, MaxRSSize, GeneratingModelType: ova verzija koda ih ne cita
  - numIterations = 5 za brzi prvi test (profesorova vrijednost je 40)
  - dodano numThreads = 4 i postavke za k-modes (InitializationMethod, NumOfClusters, ViewForClustering, KmodesInitMethod)
