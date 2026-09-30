/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package redescriptionmining;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author matej
 */
public class ApplicationSettings {
    final static Charset ENCODING = StandardCharsets.UTF_8;

    public String initializationMethod; // default ili kmeans

    // stvarni broj ciljnih atributa koje je dodala inicijalizacija klasteriranjem
    // (postavlja se u kodu, ne cita se iz datoteke)
    public int numInitTargets;

    public String kmeansInitMethod;  // default je forgy, ili kmeans++
    public int numOfClusters; // broj klastera
    public int viewForClustering; // 0 - klasteriram pogled V1, 1 - klasteriram pogled V2, -1 - oba

    public double dbscanEps; // polumjer okoline za DBSCAN, mora se zadati za InitializationMethod = dbscan
    public int dbscanMinPts; // minPts za DBSCAN; <= 0 znaci heuristiku 2*d


    public int minSupport, maxSupport, numIterations, numnewRAttr, numRetRed, numTreesinForest, aTreeDepth, numTargets, clusteringMemory, Bandwith, maxDistance, numRandomRestarts, attributeImportance, containsView, numSupplementTrees, jsType, numInitial, redesSetSizeType, legacy/*, typeOfLSTrees, typeOfRSTrees*/, optimizationType, missingValueJSType, exhaustiveTesting, ruleSizeNormalization, numThreads;
    public boolean minimizeRules, unguidedExpansion, allowSERed, useJoin, leftNegation, rightNegation, leftDisjunction, rightDisjunction, computeDMfromRules, networkInit, useNetworkAsBackground, useSplitTesting, removePValBeforeJoin;
    public double minJS, minAddRedJS, maxPval/*, JSImpWeight, PValImpWeight, AttDivImpWeight, ElemDivImpWeight, RuleSizeImpWeight*/, Alpha, percentageForTrain, RedStabilityWeight;
    public String javaPath, clusPath, outFolderPath, outputName, initClusteringFileName, preferenceFilePath, system, trainFileName, testFileName;
    public ArrayList<String> viewInputPaths, distanceFilePaths, spatialMatrix, spatialMeasures;
    public  ArrayList<ArrayList<ArrayList<String>>> importantAttributes;
    public HashMap<String, Integer> preferenceHeader;
    public ArrayList<Integer> attributeImportanceGen = new ArrayList<>();
    public ArrayList<Integer> treeTypes;
    public ArrayList<Boolean> useNC;
    public ArrayList<double[]> preferences;
    public ArrayList<ArrayList<Double>> parameters;
    public String dataFilePath;
    
    private static ApplicationSettings appset = new ApplicationSettings();
    
    public static ApplicationSettings getInstance() {
    	return appset;
    }
    
    private ApplicationSettings() {
        system = "windows";
        parameters = new ArrayList<>();
        legacy = 0;
        missingValueJSType = 0;
        exhaustiveTesting = 0;
        redesSetSizeType = 0; //0-flexible, 1-exact
        RedStabilityWeight = 0.0;
        ruleSizeNormalization = 20;
        importantAttributes = new ArrayList<ArrayList<ArrayList<String>>>();
        attributeImportanceGen = new ArrayList<Integer>();
        optimizationType = 0;//0-by exchange, 1-by extraction
        minSupport = 1;
        numIterations = 1;
        numnewRAttr = 1;
        numRetRed = Integer.MAX_VALUE;
        numTreesinForest = 50;
        aTreeDepth = Integer.MAX_VALUE;
        numTargets = 1600;
        clusteringMemory = 2000;
        numRandomRestarts = 1;
        numSupplementTrees = 0;
        useJoin = true;
        minimizeRules = true;
        unguidedExpansion = false;
        allowSERed = false;
        attributeImportance = 0;
        jsType = 0; //0 -qnm, 1- pess, 2-opt, 3-rejective
        minJS = 0.6;
        minAddRedJS = 0.1;
        maxPval = 0.02; /*JSImpWeight=0.2; PValImpWeight=0.2; AttDivImpWeight=0.2; ElemDivImpWeight=0.2; RuleSizeImpWeight=0.2;*/
        numThreads = 1;
        javaPath = "java";
        clusPath = "clus.jar";
        outFolderPath = "";
        outputName = "Redescriptions.rr";
        initClusteringFileName = "";
        trainFileName = "";
        testFileName = "";
        viewInputPaths = new ArrayList<>();
        preferenceFilePath = "preference.txt";
        viewInputPaths.add("input1.arff");
        viewInputPaths.add("input2.arff");
        maxSupport = Integer.MAX_VALUE;
        leftNegation = false;
        rightNegation = false;
        leftDisjunction = false;
        rightDisjunction = false;
        treeTypes = new ArrayList<>();
        treeTypes.add(1);
        treeTypes.add(1);
        useNC = new ArrayList<>();
        useNC.add(false);
        useNC.add(false);
        distanceFilePaths = new ArrayList<>();
        spatialMatrix = new ArrayList<>();
        spatialMeasures = new ArrayList<>();
        Bandwith = 100;
        Alpha = 0.5;
        computeDMfromRules = false;
        maxDistance = 100;
        networkInit = false;
        useNetworkAsBackground = false;
        useSplitTesting = false;
        removePValBeforeJoin = true;
        percentageForTrain = 0.8;
        importantAttributes = new ArrayList<>();
        preferenceHeader = new HashMap<>();
        preferences = new ArrayList<>();
        containsView = 0; // 0 - no attribute constraints, 1 attr constr for W1, 2 attr constr for W2, 3 attr constr for both
        //typeOfLSTrees=1; //1 regresion, 0 classification, 2 network etc...
        //typeOfRSTrees=1; //1 regresion, 0 classification, 2 network etc...
        initializationMethod = "default";
        numInitTargets = 1;
        numOfClusters = 3;
        viewForClustering = -1;
        dbscanEps = -1;
        dbscanMinPts = -1;
    }

    public void printSettings(ApplicationSettings appset) {
		System.out.println("Num targets: " + appset.numTargets);
		System.out.println("Num trees in RS: " + appset.numTreesinForest);
		System.out.println("Average tree depth in RS: " + appset.aTreeDepth);
		System.out.println("Allow left side rule negation: " + appset.leftNegation);
		System.out.println("Allow right side rule negation: " + appset.rightNegation);
		System.out.println("Allow left side rule disjunction: " + appset.leftDisjunction);
		System.out.println("Allow right side rule disjunction: " + appset.rightDisjunction);
		System.out.println("Types of LSTrees: " + appset.treeTypes.get(0));
		System.out.println("Types of RSTrees: " + appset.treeTypes.get(1));
		System.out.println("Use Network information: " + appset.useNC.toString());
		System.out.println("Spatial matrix: " + appset.spatialMatrix.toString());
		System.out.println("Spatial measure: " + appset.spatialMeasures.toString());

		System.out.println("Attribute importance gen: ");
		for (int i = 0; i < appset.attributeImportanceGen.size(); i++)
			System.out.print(appset.attributeImportanceGen.get(i) + " ");
		System.out.println();

		System.out.println("Important attributes: ");
		for (int i = 0; i < appset.importantAttributes.size(); i++) {
			for (int j = 0; j < appset.importantAttributes.get(i).size(); j++) {
				for (int k = 0; k < appset.importantAttributes.get(i).get(j).size(); k++) {
					if (k < appset.importantAttributes.get(i).get(j).size())
						System.out.print(appset.importantAttributes.get(i).get(j).get(k) + " , ");
				}
				System.out.print(" + ");
			}
			System.out.println();
		}
    }
    
    public void readPreference() {
        File input = new File(preferenceFilePath);

        BufferedReader reader;

        try {
            Path path = Paths.get(preferenceFilePath);
            reader = Files.newBufferedReader(path, ENCODING);
            String line = "";
            int count = 0;

            while ((line = reader.readLine()) != null) {
                ++count;
                String tmp[] = line.split(" ");

                if (count > 1) {
                    double tmpPref[] = new double[tmp.length];

                    for (int i = 0; i < tmp.length; i++)
                        tmpPref[i] = Double.parseDouble(tmp[i]);

                    preferences.add(tmpPref);
                } else {
                    for (int i = 0; i < tmp.length; i++)
                        preferenceHeader.put(tmp[i].trim(), i);
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (preferences.isEmpty()) {
            System.out.println("Adding default preference parameters...");
            System.out.println("In order to control redescription stability and get information about different JS measures RedStability parameter preference must be >0");
            double tmp[] = {0.2, 0.2, 0.2, 0.18, 0.01, 0.01};
            preferences.add(tmp);
            if (!preferenceHeader.isEmpty())
                preferenceHeader.clear();

            if (useNetworkAsBackground == false) {
                for (int k = 0; k < tmp.length - 1; k++)
                    tmp[k] = 0.2;
                tmp[tmp.length - 1] = 0.0001;
                tmp[tmp.length - 2] = 0.2 - 0.0001;
            }

            preferenceHeader.put("JSImp", 0);
            preferenceHeader.put("PValImp", 1);
            preferenceHeader.put("AttDivImp", 2);
            preferenceHeader.put("ElemDivImp", 3);
            if (this.optimizationType == 1) {
                preferenceHeader.put("RuleSizeImp", 4);
                preferenceHeader.put("RedStability", 5);
            } else {
                preferenceHeader.put("ECoverage", 4);
                if (this.useNetworkAsBackground == true)
                    preferenceHeader.put("NetImportance", 5);
            }
        } else if (preferenceHeader.keySet().size() < 5 || this.useNetworkAsBackground == true && preferenceHeader.keySet().size() < 6) {
            int num = preferenceHeader.keySet().size();
            if (!preferenceHeader.keySet().contains("JSImp"))
                preferenceHeader.put("JSImp", num++);
            if (!preferenceHeader.keySet().contains("PValImp"))
                preferenceHeader.put("PValImp", num++);
            if (!preferenceHeader.keySet().contains("AttDivImp"))
                preferenceHeader.put("AttDivImp", num++);
            if (!preferenceHeader.keySet().contains("ElemDivImp"))
                preferenceHeader.put("ElemDivImp", num++);
            if (this.optimizationType == 1) {
                if (!preferenceHeader.keySet().contains("RuleSizeImp"))
                    preferenceHeader.put("RuleSizeImp", num++);
                if (!preferenceHeader.keySet().contains("RedStability"))
                    preferenceHeader.put("RedStability", num++);
            } else {
                if (!preferenceHeader.keySet().contains("ECoverage"))
                    preferenceHeader.put("ECoverage", num++);
                if (!preferenceHeader.keySet().contains("NetImportance") && this.useNetworkAsBackground == true)
                    preferenceHeader.put("NetImportance", num++);
            }

            for (int i = 0; i < preferences.size(); i++) {

                double row[] = preferences.get(i);
                double newT[] = new double[num];
                double sum = 0.0;

                for (int j = 0; j < row.length; j++) {
                    newT[j] = row[j];
                    sum += row[j];
                }

                double rem = 1.0 - sum;

                for (int j = row.length; j < num; j++)
                    newT[j] = rem / (num - j);

                preferences.set(i, newT);
            }
        }
    }

    void readSettings(File settings) {

        BufferedReader reader;

        try {
            Path path = Paths.get(settings.getAbsolutePath());
            System.out.println("Path: " + settings.getAbsolutePath());
            reader = Files.newBufferedReader(path, ENCODING);
            String line = null;
            int inputCount = 0, numST = 0, numNAC = 0;
            while ((line = reader.readLine()) != null) {
                if (line.contains("JavaPath")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    javaPath = tmp[1];
                } else if (line.contains("ClusPath")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    clusPath = tmp[1];
                } else if (line.contains("OutputFolder")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    outFolderPath = tmp[1];
                } else if (line.contains("Input")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    if (inputCount < 2)
                        viewInputPaths.set(inputCount, tmp[1]);
                    else {
                        viewInputPaths.add(tmp[1]);
                    }
                    inputCount++;
                    System.out.println("Input: " + viewInputPaths);
                } else if (line.contains("MinSupport")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    minSupport = Integer.parseInt(tmp[1]);
                } else if (line.contains("numIterations")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    numIterations = Integer.parseInt(tmp[1]);
                } else if (line.contains("System")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    system = tmp[1];
                } else if (line.contains("numNewAttr")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    numnewRAttr = Integer.parseInt(tmp[1]);
                } else if (line.contains("numRetRed")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    if (tmp[1].contentEquals("all"))
                        numRetRed = Integer.MAX_VALUE;
                    else
                        numRetRed = Integer.parseInt(tmp[1]);
                } else if (line.contains("ruleSizeNormalization")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    ruleSizeNormalization = Integer.parseInt(tmp[1]);
                } else if (line.contains("numThreads")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    numThreads = Integer.parseInt(tmp[1]);
                } else if (line.contains("minimizeRules")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    if (tmp[1].contentEquals("yes"))
                        minimizeRules = true;
                    else minimizeRules = false;
                } else if (line.contains("legacy")) {
                	boolean t = line.contains("legacy");
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    if (tmp[1].contentEquals("true"))
                        legacy = 1;
                    else legacy = 0;
                } else if (line.contains("unguidedExpansion")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    if (tmp[1].contentEquals("yes"))
                        unguidedExpansion = true;
                    else unguidedExpansion = false;
                } else if (line.contains("joiningProcedure")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    if (tmp[1].contentEquals("yes"))
                        useJoin = true;
                    else useJoin = false;
                } else if (line.contains("allowSERed")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    if (tmp[1].contentEquals("yes"))
                        allowSERed = true;
                    else allowSERed = false;
                } else if (line.contains("minJS")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    minJS = Double.parseDouble(tmp[1]);
                } else if (line.contains("minAddRedJS")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    minAddRedJS = Double.parseDouble(tmp[1]);
                } else if (line.contains("maxPval")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    maxPval = Double.parseDouble(tmp[1]);
                } else if (line.contains("numTrees")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    numTreesinForest = Integer.parseInt(tmp[1]);
                } else if (line.contains("numSupplementTrees")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    numSupplementTrees = Integer.parseInt(tmp[1]);
                } else if (line.contains("OutputFileName")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    outputName = tmp[1];
                } else if (line.contains("ATreeDepth")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    if (tmp[1].contains("inf"))
                        aTreeDepth = Integer.MAX_VALUE;
                    else aTreeDepth = Integer.parseInt(tmp[1]);
                } else if (line.contains("preferenceFilePath")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    preferenceFilePath = tmp[1];
                } else if (line.contains("NumTarget")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    numTargets = Integer.parseInt(tmp[1]);
                } else if (line.contains("clusteringMemory")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    clusteringMemory = Integer.parseInt(tmp[1]);
                }
                        /*else if(line.contains("JSImp")){
                            String tmp[]=line.split("=");
                            tmp[1]=tmp[1].trim();
                            JSImpWeight=Double.parseDouble(tmp[1]);
                        }
                        else if(line.contains("PValImp")){
                            String tmp[]=line.split("=");
                            tmp[1]=tmp[1].trim();
                            PValImpWeight=Double.parseDouble(tmp[1]);
                        }
                        else if(line.contains("AttDivImp")){
                            String tmp[]=line.split("=");
                            tmp[1]=tmp[1].trim();
                            AttDivImpWeight=Double.parseDouble(tmp[1]);
                        }
                        else if(line.contains("ElemDivImp")){
                            String tmp[]=line.split("=");
                            tmp[1]=tmp[1].trim();
                            ElemDivImpWeight=Double.parseDouble(tmp[1]);
                        }
                        else if(line.contains("RuleSizeImp")){
                            String tmp[]=line.split("=");
                            tmp[1]=tmp[1].trim();
                            RuleSizeImpWeight=Double.parseDouble(tmp[1]);
                        }*/
                else if (line.contains("MaxSupport")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    if (tmp[1].contains("inf"))
                        maxSupport = Integer.MAX_VALUE;
                    else
                        maxSupport = Integer.parseInt(tmp[1]);
                } else if (line.contains("allowLeftNeg")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    if (tmp[1].contains("true"))
                        leftNegation = true;
                    else
                        leftNegation = false;
                } else if (line.contains("allowRightNeg")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    if (tmp[1].contains("true"))
                        rightNegation = true;
                    else
                        rightNegation = false;
                } else if (line.contains("allowLeftDisj")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    if (tmp[1].contains("true"))
                        leftDisjunction = true;
                    else
                        leftDisjunction = false;
                } else if (line.contains("allowRightDisj")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    if (tmp[1].contains("true"))
                        rightDisjunction = true;
                    else
                        rightDisjunction = false;
                } else if (line.contains("removePValBeforeJoin")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    removePValBeforeJoin = tmp[1].contains("true");
                } else if (line.contains("jsType")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    if (tmp[1].toLowerCase().contains("pessimistic"))
                        jsType = 1;
                    else if (tmp[1].toLowerCase().contains("query non missing"))
                        jsType = 0;
                } else if (line.contains("SideTrees")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    if (tmp[1].contains("regression")) {
                        if (numST < 2)
                            treeTypes.set(numST, 1);
                        else treeTypes.add(1);
                    } else {
                        if (numST < 2)
                            treeTypes.set(numST, 0);
                        else treeTypes.add(0);
                    }
                    numST++;
                } else if (line.contains("useNetworkAC")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    if (numNAC < 2) {
                        if (tmp[1].contains("true"))
                            useNC.set(numNAC, true);
                        else
                            useNC.set(numNAC, false);
                    } else {
                        if (tmp[1].contains("true"))
                            useNC.add(numNAC, true);
                        else
                            useNC.add(numNAC, false);
                    }
                    numNAC++;
                } else if (line.contains("distanceFile")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    distanceFilePaths.add(tmp[1]);
                } else if (line.contains("spatialMatrix")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    spatialMatrix.add(tmp[1]);
                } else if (line.contains("spatialMeasure")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    spatialMeasures.add(tmp[1]);
                } else if (line.contains("Alpha")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    Alpha = Double.parseDouble(tmp[1]);
                } else if (line.contains("Bandwith")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    Bandwith = Integer.parseInt(tmp[1]);
                } else if (line.contains("computeDistanceMatrixFromRules")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    if (tmp[1].contains("true"))
                        computeDMfromRules = true;
                    else
                        computeDMfromRules = false;
                } else if (line.contains("maxDistance")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    maxDistance = Integer.parseInt(tmp[1]);
                } else if (line.contains("useNetworkforInitialization")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    if (tmp[1].contains("true"))
                        networkInit = true;
                } else if (line.contains("useNetworkAsBackground")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    if (tmp[1].contains("true"))
                        useNetworkAsBackground = true;
                } else if (line.contains("useSplitTesting")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    if (tmp[1].contains("true"))
                        useSplitTesting = true;
                } else if (line.contains("initClusteringFileName")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    initClusteringFileName = tmp[1];
                } else if (line.contains("InitializationMethod")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    initializationMethod = tmp[1];
                    if (!initializationMethod.equalsIgnoreCase("default")
                            && !initializationMethod.equalsIgnoreCase("kmeans")
                            && !initializationMethod.equalsIgnoreCase("dbscan"))
                        throw new IllegalArgumentException("Illegal arg. for InitializationMethod: \""
                                + tmp[1] + "\". allowed: default, kmeans, dbscan.");
                } else if (line.contains("NumOfClusters")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    numOfClusters = Integer.parseInt(tmp[1]);
                } else if (line.contains("ViewForClustering")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    if (tmp[1].equalsIgnoreCase("W1"))
                        viewForClustering = 0;
                    else if (tmp[1].equalsIgnoreCase("W2"))
                        viewForClustering = 1;
                    else if (tmp[1].equalsIgnoreCase("both"))
                        viewForClustering = -1;
                    else
                        throw new IllegalArgumentException("Illegal arg. for ViewForClustering: \"" + tmp[1] + "\". allowed: W1, W2, both.");
                } else if (line.contains("KmeansInitMethod")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    if (tmp[1].equalsIgnoreCase("KMeans++") || tmp[1].equalsIgnoreCase("KMeansPlusPlus"))
                        kmeansInitMethod = "Kmeans++";
                    else if (tmp[1].equalsIgnoreCase("Forgy"))
                        kmeansInitMethod = "Forgy";
                    else
                        throw new IllegalArgumentException("Illegal arg. for KmeansInitMethod: \"" + tmp[1] + "\". allowed: Forgy, KMeans++.");
                } else if (line.contains("DbscanEps")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    dbscanEps = Double.parseDouble(tmp[1]);
                    if (dbscanEps <= 0)
                        throw new IllegalArgumentException("Illegal arg. for DbscanEps: \"" + tmp[1] + "\". must be > 0.");
                }
                else if (line.contains("DbscanMinPts")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    dbscanMinPts = Integer.parseInt(tmp[1]);
                    if (dbscanMinPts <= 0)
                        throw new IllegalArgumentException("Illegal arg. for DbscanMinPts: \"" + tmp[1] + "\". must be > 0.");
                } else if (line.contains("trainFileName")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    trainFileName = tmp[1];
                } else if (line.contains("testFileName")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    testFileName = tmp[1];
                } else if (line.contains("percentageForTrain")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    double perc = Double.parseDouble(tmp[1]);
                    if (perc > 0.0 && perc <= 1.0)
                        percentageForTrain = perc;
                } else if (line.contains("redesSetSizeType")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    if (tmp[1].toLowerCase().contains("exact"))
                        redesSetSizeType = 1;
                    else redesSetSizeType = 0;
                } else if (line.contains("importantAttributes")) {
                    String tmp[] = line.split("=");
                    System.out.println(tmp);
                    tmp[1] = tmp[1].trim();
                    String attrs[] = null;

                    if (tmp[1].contains(","))
                        attrs = tmp[1].split(",");
                    else if (tmp[1].contains(" "))
                        attrs = tmp[1].split(" ");
                    else if (tmp[1].contains("\t"))
                        attrs = tmp[1].split("\t");
                    else {
                        attrs = new String[1];
                        attrs[0] = tmp[1];
                    }

                    int vn = -1;

                    if (line.contains("W")) {
                        String tw[] = tmp[0].split("W");
                        vn = Integer.parseInt(tw[1].trim());

                        while (importantAttributes.size() < vn - 1)
                            importantAttributes.add(null);

                        importantAttributes.add(vn - 1, new ArrayList<ArrayList<String>>());

                    } else vn = importantAttributes.size();


                    for (int i = 0; i < attrs.length; i++) {

                        importantAttributes.get(vn - 1).add(new ArrayList<String>());

                        String tattrs = attrs[i];
                        tattrs = tattrs.replace("{", "");
                        tattrs = tattrs.replace("}", "");
                        attrs[i] = tattrs;
                        String attrsC[] = attrs[i].split(";");

                        for (int j = 0; j < attrsC.length; j++)
                            importantAttributes.get(vn - 1).get(i).add(attrsC[j]);
                    }
                } else if (line.contains("attributeImportance")) {
                    attributeImportance = 1;
                    String attributeImportanceTmp = line.split("=")[1].trim();
                    String atName = line.split("=")[0].trim();
                    int vn = 0;

                    if (atName.contains("W")) {

                        String tw[] = atName.split("W");
                        vn = Integer.parseInt(tw[1].trim());

                        while (attributeImportanceGen.size() < vn - 1)
                            attributeImportanceGen.add(null);
                    } else vn = attributeImportanceGen.size();

                    attributeImportanceGen.add(vn - 1, 0);

                    if (!attributeImportanceTmp.equals("none") && !attributeImportanceTmp.equals("hard") && !attributeImportanceTmp.equals("soft"))
                        attributeImportanceGen.set(vn - 1, 0);
                    else if (attributeImportanceTmp.equals("none"))
                        attributeImportanceGen.set(vn - 1, 0);
                    else if (attributeImportanceTmp.equals("soft"))
                        attributeImportanceGen.set(vn - 1, 1);
                    else if (attributeImportanceTmp.equals("hard"))
                        attributeImportanceGen.set(vn - 1, 2);
                    else if (attributeImportanceTmp.equals("suggested"))
                        attributeImportanceGen.set(vn - 1, 3);

                }
                        /*else if(line.contains("attributeImportanceW1")){//allowed options, none, hard, soft
                            String attributeImportanceTmp=line.split("=")[1].trim();
                            
                            if(!attributeImportanceTmp.equals("none") && !attributeImportanceTmp.equals("hard") && !attributeImportanceTmp.equals("soft"))
                                    attributeImportance=0;
                            else if(attributeImportanceTmp.equals("none"))
                                attributeImportance=0;
                            else if(attributeImportanceTmp.equals("soft"))
                                attributeImportance=1;
                            else if(attributeImportanceTmp.equals("hard"))
                                attributeImportance=2;
                            attributeImportance+=1;
                            
                        }
                        else if(line.contains("attributeImportanceW2")){//allowed options, none, hard, soft
                            String attributeImportanceTmp=line.split("=")[1].trim();
                            
                            if(!attributeImportanceTmp.equals("none") && !attributeImportanceTmp.equals("hard") && !attributeImportanceTmp.equals("soft"))
                                    attributeImportance=0;
                            else if(attributeImportanceTmp.equals("none"))
                                attributeImportance=0;
                            else if(attributeImportanceTmp.equals("soft"))
                                attributeImportance=1;
                            else if(attributeImportanceTmp.equals("hard"))
                                attributeImportance=2;
                            attributeImportance+=2;
                            
                        }*/
                else if (line.contains("numRandomRestarts")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    numRandomRestarts = Integer.parseInt(tmp[1]);
                } else if (line.contains("RedStability")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    RedStabilityWeight = Double.parseDouble(tmp[1]);
                }
                        
                         /*else if(line.contains("leftSideTrees")){
                             String tmp[]=line.split("=");
                             tmp[1]=tmp[1].trim();
                             if(tmp[1].contains("regression")){
                                 typeOfLSTrees=1;
                             }
                             else if(tmp[1].contains("classification")){
                                 typeOfLSTrees=0;
                             }
                         }
                        else if(line.contains("rightSideTrees")){
                             String tmp[]=line.split("=");
                             tmp[1]=tmp[1].trim();
                             if(tmp[1].contains("regression")){
                                 typeOfRSTrees=1;
                             }
                             else if(tmp[1].contains("classification")){
                                 typeOfRSTrees=0;
                             }
                         }*/
                else if (line.contains("optimizationType")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    if (tmp[1].equals("exchange"))
                        optimizationType = 0;
                    else if (tmp[1].equals("extraction"))
                        optimizationType = 1;
                    else optimizationType = 0;
                } else if (line.contains("missingValsJSType")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    int type = Integer.parseInt(tmp[1]);
                    if (type < 4)
                        missingValueJSType = type;
                    else type = 0;
                } else if (line.contains("parameters")) {
                    String tmp[] = line.split("=");
                    tmp[1] = tmp[1].trim();
                    if (tmp[1].contains("(")) {
                        exhaustiveTesting = 0;
                        tmp[1] = tmp[1].replaceAll("\\(", "");
                        tmp[1] = tmp[1].replaceAll("\\)", "");
                        String tmp2[] = tmp[1].split(",");
                        ArrayList<Double> pars = null;

                        for (int i = 0; i < tmp2.length; i++) {
                            if (i % 3 == 0) {
                                pars = new ArrayList<>();
                            }

                            pars.add(Double.parseDouble(tmp2[i]));

                            if (pars.size() == 3)
                                parameters.add(pars);
                        }
                    } else if (tmp[1].contains("-")) {
                        exhaustiveTesting = 1;
                        String tmp2[] = tmp[1].split(",");

                        for (int i = 0; i < tmp2.length; i++) {
                            ArrayList<Double> pars = new ArrayList<>();

                            String tmp3[] = tmp2[i].split("-");
                            tmp3[0] = tmp3[0].trim();
                            tmp3[1] = tmp3[1].trim();
                            pars.add(Double.parseDouble(tmp3[0]));
                            String tmp4[] = tmp3[1].split(";");
                            tmp4[0] = tmp4[0].trim();
                            if (tmp4.length == 2)
                                tmp4[1] = tmp4[1].trim();
                            pars.add(Double.parseDouble(tmp4[0]));

                            if (tmp4.length == 2) pars.add(Double.parseDouble(tmp4[1]));
                            parameters.add(pars);
                        }
                    }
                }
            }
            reader.close();
        } catch (IOException io) {
            io.printStackTrace();
        }
    }
}
