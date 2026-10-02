/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package redescriptionmining;

import si.ijs.kt.clus.main.settings.Settings;
import si.ijs.kt.clus.util.exception.ClusException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import static redescriptionmining.DataSetCreator.writeArff;

/**
 * @author matej
 */
public class SupplementingRandomForest {
	
	private static DataSetCreator datJ;
	
	private static int numIter;
	
	private static Mappings fid;
	
	private static int leftSide;
	private static int leftSide1;
	private static int rightSide;
	private static int rightSide1;
	private static int initSetCapacity;
	private static int numExamplesSquared;

	public static int getLeftSide() {
		return leftSide;
	}

	public static int getLeftSide1() {
		return leftSide1;
	}

	public static int getRightSide() {
		return rightSide;
	}

	public static int getRightSide1() {
		return rightSide1;
	}

	public static Mappings getFid() {
		return fid;
	}
	
	public static DataSetCreator getDatJ() {
		return datJ;
	}
	
	public static int getNumIter() {
		return numIter;
	}

	static int numSupplmentingForestRules = 0;
	
	public static void main(String[] args) throws FileNotFoundException, ClusException {
//		System.setOut(new PrintStream(new FileOutputStream("C:\\Users\\ijukic_apis\\Desktop\\attachments\\output.txt")));
		// 1. compute pareto optimal front of rules
		// 2. visualizations
		// 3. add several initial clustering methods (using target-descriptive and
		// both), future work
		// 4. add settings element for network approach, future work...
		// 5. if return redescription = all return all redescriptions and do not compute
		// rule score (perhaps do not compute vizaulization set also?)
		// 6. multi-view approach

		long startTime = System.currentTimeMillis();

		ApplicationSettings appset = ApplicationSettings.getInstance();
		appset.readSettings(new File(args[0]));
		appset.readPreference();
		appset.printSettings(appset);
		// appset.importantAttributes

		fid = new Mappings();
		Mappings fidFull = new Mappings();
		Mappings fidTest = new Mappings();

		// TODO pozvati konstruktor koji nema outFolderPath
		datJ = new DataSetCreator(appset.viewInputPaths, appset.outFolderPath, appset);
		// DataSetCreator datJ = new DataSetCreator(appset.viewInputPaths, appset.outFolderPath, appset);
		
		DataSetCreator datJFull = null;
		DataSetCreator datJTest = null;

		fid.createIndex(datJ);
		// TODO koristiti programski createIndex
		/*if (appset.system.equals("windows"))
			fid.createIndex(appset.outFolderPath + "\\Jinput.arff");
		else
			fid.createIndex(appset.outFolderPath + "/Jinput.arff");*/

		// TODO koristiti programski createIndex za fidFull i fidTest
		if (appset.useSplitTesting) {
			fidFull.createIndex(datJ);
			fidTest.createIndex(datJ);

			/*if (appset.system.equals("windows")) {
				fidFull.createIndex(appset.outFolderPath + "\\Jinput.arff");
				fidTest.createIndex(appset.outFolderPath + "\\Jinput.arff");
			} else {
				fidFull.createIndex(appset.outFolderPath + "/Jinput.arff");
				fidTest.createIndex(appset.outFolderPath + "/Jinput.arff");
			}*/
		}

		// TODO split testing kasnije
		if (appset.useSplitTesting) {
			ArrayList<DataSetCreator> rDat = new ArrayList<>();
			//datJInitial = new DataSetCreator(datJ);
			// TODO treba li datJFull biti inicijalni datJ? Ako da, onda ovo nije dobro jer se datJ mijenja.
			datJFull = datJ;
			datJTest = datJ;
			if (appset.trainFileName.equals("") || appset.testFileName.equals("")) {
				rDat = datJ.createSplit(appset.percentageForTrain);
				datJ = rDat.get(0);
				datJTest = rDat.get(1);

				try {
					if (appset.system.equals("windows")) {
						writeArff(appset.outFolderPath + "\\JinputTrain.arff", datJ.data);
						writeArff(appset.outFolderPath + "\\JinputTest.arff", datJTest.data);
					} else {
						writeArff(appset.outFolderPath + "/JinputTrain.arff", datJ.data);
						writeArff(appset.outFolderPath + "/JinputTest.arff", datJTest.data);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

			} else {
				if (appset.system.equals("windows")) {
					datJ = new DataSetCreator(appset.outFolderPath + "\\" + appset.trainFileName);
					datJTest = new DataSetCreator(appset.outFolderPath + "\\" + appset.testFileName);
				} else {
					datJ = new DataSetCreator(appset.outFolderPath + "/" + appset.trainFileName);
					datJTest = new DataSetCreator(appset.outFolderPath + "/" + appset.testFileName);
				}

				try {
					datJ.readDataset();
					datJTest.readDataset();
				} catch (IOException e) {
					e.printStackTrace();
				}

				datJ.W2indexs.addAll(datJFull.W2indexs);
				datJTest.W2indexs.addAll(datJFull.W2indexs);

			}

			fid.clearMaps();
			if (appset.system.equals("windows"))
				fid.createIndex(appset.outFolderPath + "\\JinputTrain.arff");
			else
				fid.createIndex(appset.outFolderPath + "/JinputTrain.arff");
			System.out.println("Full: " + datJFull.numExamples);
			System.out.println("Part: " + datJ.numExamples);
		}

		Random r = new Random();
		RedescriptionSet rs = new RedescriptionSet();
		RuleReader rr = new RuleReader();
		RuleReader rr1 = new RuleReader();
		boolean oom[] = new boolean[1];

		int elemFreq[] = null;
		int attrFreq[] = null;
		ArrayList<Double> redScores = null;
		ArrayList<Double> redScoresAtt = null;
		ArrayList<Double> targetAtScore = null;
		ArrayList<Double> redDistCoverage = null;
		ArrayList<Double> redDistCoverageAt = null;
		ArrayList<Double> redDistNetwork = null;
		double statistics[] = { 0.0, 0.0, 0.0 };// previousMedian - 0, numberIterationsStable - 1, minDifference - 2
		ArrayList<Double> maxDiffScoreDistribution = null;

		if (appset.optimizationType == 0) {
			if (appset.redesSetSizeType == 1 && appset.numRetRed != Integer.MAX_VALUE)
				appset.numInitial = appset.numRetRed;
			else {
				if (appset.numRetRed != Integer.MAX_VALUE && appset.numRetRed != -1)
					appset.numInitial = appset.numRetRed;
				else
					appset.numInitial = 20;
			}
		}

		if (appset.optimizationType == 0) {

			elemFreq = new int[datJ.numExamples];
			attrFreq = new int[datJ.schema.getNbAttributes()];
			
			System.out.println("Number of redescriptions: " + appset.numInitial);

			redScores = new ArrayList<>(appset.numInitial);
			redScoresAtt = new ArrayList<>(appset.numInitial);
			redDistCoverage = new ArrayList<>(appset.numInitial);
			redDistCoverageAt = new ArrayList<>(appset.numInitial);
			if (appset.useNetworkAsBackground)
				redDistNetwork = new ArrayList<>(appset.numInitial);
			targetAtScore = null;
			// double Statistics[]={0.0,0.0,0.0};//previousMedian - 0,
			// numberIterationsStable - 1, minDifference - 2
			maxDiffScoreDistribution = new ArrayList<>(appset.numInitial);

			if (appset.attributeImportance != 0)
				targetAtScore = new ArrayList<>(appset.numInitial);

			for (int z = 0; z < appset.numInitial; z++) {
				redScores.add(Double.NaN);
				redScoresAtt.add(Double.NaN);
				redDistCoverage.add(Double.NaN);
				redDistCoverageAt.add(Double.NaN);
				maxDiffScoreDistribution.add(Double.NaN);
				if (appset.useNetworkAsBackground)
					redDistNetwork.add(Double.NaN);
				if (appset.attributeImportance != 0)
					targetAtScore.add(Double.NaN);
			}
		}

		NHMCDistanceMatrix nclMatInit = null;
		if (appset.distanceFilePaths.size() > 0) {
			nclMatInit = new NHMCDistanceMatrix(datJ.numExamples, appset);
			nclMatInit.loadDistance(new File(appset.distanceFilePaths.get(0)), fid);
			if (appset.distanceFilePaths.size() > 0) {
				nclMatInit.resetFile(new File(appset.outFolderPath + "\\distances.csv"));
				nclMatInit.writeToFile(new File(appset.outFolderPath + "\\distances.csv"), fid, appset);
			} else {
				nclMatInit.resetFile(new File(appset.outFolderPath + "/distances.csv"));
				nclMatInit.writeToFile(new File(appset.outFolderPath + "/distances.csv"), fid, appset);
			}
			nclMatInit = null;
		}

		for (int runTest = 0; runTest < appset.numRandomRestarts; runTest++) {
			numIter = 0;
			rr.newRuleIndex = 0;
			rr1.newRuleIndex = 0;
			System.out.println("RunIndex: " + runTest);
			DataSetCreator datJInit = null;

			if (!appset.useSplitTesting && !appset.initClusteringFileName.equals("")) {
				
				if (appset.system.equals("windows"))
					datJInit = new DataSetCreator(appset.outFolderPath + "\\" + appset.initClusteringFileName);
				else
					datJInit = new DataSetCreator(appset.initClusteringFileName);
				
				try {
					datJInit.readDataset();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			} else {
				// već su podaci očitani i spremljeni u datJ
				datJInit = new DataSetCreator(datJ);
			}

			datJInit.W2indexs.addAll(datJ.W2indexs);

			if (appset.initClusteringFileName.equals("")) {
				//JinputInitial.arff
				if ("kmeans".equalsIgnoreCase(appset.initializationMethod))
					datJInit.initialClusteringKmeans(appset, r);
				else if ("dbscan".equalsIgnoreCase(appset.initializationMethod))
					datJInit.initialClusteringDbscan(appset);
				else if ("kmodes".equalsIgnoreCase(appset.initializationMethod))
					datJInit.initialClusteringKmodes(appset, r);
				else if ("default".equalsIgnoreCase(appset.initializationMethod))
					datJInit.writeArffInitialClusteringGen1(appset, r); // so we actually save init file
//					datJInit.initialClusteringGen1(appset, r);
				else
					throw new IllegalStateException("Nepoznata InitializationMethod: "
							+ appset.initializationMethod);
			}

			System.out.println("WIndexes size: " + datJ.W2indexs.size());
			System.out.println("distance file size: " + appset.distanceFilePaths.size() + "");
			System.out.println("use nc: " + appset.useNC.size());
			
			int w2IndexStart = !appset.useNC.get(0) ? 3 : 4;
			
			ClusProcessExecutor exec = new ClusProcessExecutor();

			initSetCapacity = (int)(getDatJ().numExamples * 0.1 * (1 + appset.numThreads));
			
			// view 1
			RedescriptionMiningRunner runnerView1 = new RedescriptionMiningRunner(new String[] {"-forest", "view1.s"}, 0, w2IndexStart, datJ.W2indexs.get(0),
					datJ.schema.getNbAttributes(), appset, 1, datJInit);	
			ArrayList<Rule> rulesAndCoveredExamples = runnerView1.getRulesAndCoveredExamples(false, fid, datJInit, appset);
			
			// RunInitW1S1
			System.out.println("Process 1 side 1 finished!");

			// read the rules obtained from first attribute set
			rr1.rules = rulesAndCoveredExamples;

			if (appset.distanceFilePaths.size() > 1) {
				nclMatInit = new NHMCDistanceMatrix(datJ.numExamples, appset);
				nclMatInit.loadDistance(new File(appset.distanceFilePaths.get(1)), fid);
				if (appset.system.equals("windows")) {
					nclMatInit.resetFile(new File(appset.outFolderPath + "\\distances.csv"));
					nclMatInit.writeToFile(new File(appset.outFolderPath + "\\distances.csv"), fid, appset);
				} else {
					nclMatInit.resetFile(new File(appset.outFolderPath + "/distances.csv"));
					nclMatInit.writeToFile(new File(appset.outFolderPath + "/distances.csv"), fid, appset);
				}
				nclMatInit = null;
			}

			SettingsReader set = null;
			SettingsReader set1 = null;
			SettingsReader setF = null;
			SettingsReader setF1 = null;

			// RunInitW1S2

			int w2IndexEnd = datJ.W2indexs.size() > 1 ? datJ.W2indexs.get(1) : datJ.schema.getNbAttributes() + 1;
			
			// view 2
			RedescriptionMiningRunner runnerView2 = new RedescriptionMiningRunner(new String[] {"-forest", "view1.s"}, 1, datJ.W2indexs.get(0) + 1, w2IndexEnd,
					datJ.schema.getNbAttributes(), appset, 1, datJInit);	

			rulesAndCoveredExamples = runnerView2.getRulesAndCoveredExamples(false, fid, datJInit, appset);
			
			System.out.println("Process 1 side 2 finished!");

			// read the rules obtained from first attribute set
			rr.rules = rulesAndCoveredExamples;

			datJInit = null;

			leftSide = 1; rightSide = 0;// set left to 1 when computing lf, otherwise right
			leftSide1 = 0; rightSide1 = 1; // left, right side for Side 2
			int it = 0;
			Jacard js = new Jacard();
			Jacard jsN[] = new Jacard[3];

			for (int i = 0; i < jsN.length; i++)
				jsN[i] = new Jacard();

			int newRedescriptions = 1;
			numIter = 0;
			int runInd = 0;

			int naex = datJ.numExamples;

			// add arrayList of view rules
			ArrayList<RuleReader> readers = new ArrayList<>();
			int oldRIndex[] = { 0 };

			NHMCDistanceMatrix nclMat = null;
			if ((appset.distanceFilePaths.size() > 0 || appset.useNC.get(0)) && !appset.networkInit)
				nclMat = new NHMCDistanceMatrix(datJ.numExamples, appset);
			NHMCDistanceMatrix nclMat1 = null;
			if ((appset.distanceFilePaths.size() > 1 || appset.useNC.get(1)) && !appset.networkInit)
				nclMat1 = new NHMCDistanceMatrix(datJ.numExamples, appset);

			if (appset.useNetworkAsBackground)
				appset.networkInit = false;

			Settings initSettingsRunnerView2 = runnerView2.getSettings();
			// view 2
			if (appset.useNC.size() >= 2 && appset.useNC.get(1)) {
				if (appset.useNC.size() > 2)
					initSettingsRunnerView2.createInitialSettingsGenN(1, datJ.W2indexs.get(0) + 1, datJ.W2indexs.get(1),
							datJ.schema.getNbAttributes(), appset);
				else
					initSettingsRunnerView2.createInitialSettingsGenN(1, datJ.W2indexs.get(0) + 1,
							datJ.schema.getNbAttributes() + 1, datJ.schema.getNbAttributes(), appset);
			}

			Settings initSettingsRunnerView1 = runnerView1.getSettings();
			// view 1
			if (appset.useNC.size() > 1 && appset.useNC.get(0)) {

				initSettingsRunnerView1.createInitialSettingsGenN(1, 4, datJ.W2indexs.get(0), datJ.schema.getNbAttributes(),
						appset);
			
			}

			// settings koji se koriste u petlji
			Settings view2tmpSettings = null;
			Settings view2tmpFSettings = null;
			
			Settings view1tmpSettings = null;
			Settings view1tmpFSettings = null;
			
			Settings view2tmp1Settings = null;
			Settings view2tmp1FSettings = null;
			
			Settings view1tmp1Settings = null;
			Settings view1tmp1FSettings = null;
			
			// main loop
			while (newRedescriptions != 0 && runInd < appset.numIterations) {
				
				DataSetCreator dsc = null;
				DataSetCreator dsc1 = null;

				rr.setSize();
				rr1.setSize();

				// dsc.data.getNbRows();
				int nARules = 0, nARules1 = 0;
				int oldIndexRR = rr.newRuleIndex;
				int oldIndexRR1 = rr1.newRuleIndex;
				System.out.println("OOIndRR: " + oldIndexRR);
				System.out.println("OOIndRR1: " + oldIndexRR1);
				int endIndexRR = 0, endIndexRR1 = 0;
				newRedescriptions = 0;
				System.out.println("Iteration: " + (++numIter));

				int numBins = 0;
				int Size = Math.max(rr.rules.size() - oldIndexRR, rr1.rules.size() - oldIndexRR1);
				if (Size % appset.numTargets == 0)
					numBins = Size / appset.numTargets;
				else
					numBins = Size / appset.numTargets + 1;

				for (int z = 0; z < numBins; z++) {

					nARules = 0;
					nARules1 = 0;
					double startPerc = 0;
					double endPerc = 1;

					System.out.println("startPerc: " + startPerc);
					System.out.println("endPerc: " + endPerc);

					if (z == 0) {
						endIndexRR = rr.rules.size();
						endIndexRR1 = rr1.rules.size();

						// compute network things only here
						if (appset.useNC.get(1) == true && appset.networkInit == false
								&& appset.useNetworkAsBackground == false) {
							nclMat.reset(appset);
							nclMat1.reset(appset);
							if (leftSide == 1) {
								if (appset.distanceFilePaths.size() >= 1) {
									nclMat.loadDistance(new File(appset.distanceFilePaths.get(1)), fid);
								} else if (appset.computeDMfromRules == true) {
									nclMat.computeDistanceMatrix(rr1, fid, appset.maxDistance, datJ.numExamples);
								}
							}
						}
						if (appset.useNC.get(0) == true && appset.networkInit == false
								&& appset.useNetworkAsBackground == false) {
							if (rightSide == 1) {
								if (appset.distanceFilePaths.size() >= 2) {
									nclMat.loadDistance(new File(appset.distanceFilePaths.get(0)), fid);
									// nclMat.writeToFile(new File(appset.outFolderPath+"\\distance.csv"), fid);
								} else if (appset.computeDMfromRules == true) {
									nclMat.computeDistanceMatrix(rr, fid, appset.maxDistance, datJ.numExamples);
								}
							}
						}
						if (appset.useNC.get(1) == true && appset.networkInit == false
								&& appset.useNetworkAsBackground == false) {
							if (leftSide1 == 1) {
								if (appset.distanceFilePaths.size() >= 1) {
									nclMat1.loadDistance(new File(appset.distanceFilePaths.get(1)), fid);
									// nclMat1.writeToFile(new File(appset.outFolderPath+"\\distance.csv"), fid);
								} else if (appset.computeDMfromRules == true) {
									nclMat1.computeDistanceMatrix(rr1, fid, appset.maxDistance, datJ.numExamples);
								}
							}
						}
						if (appset.useNC.get(0) == true && appset.networkInit == false
								&& appset.useNetworkAsBackground == false) {
							if (rightSide1 == 1) {
								if (appset.distanceFilePaths.size() >= 2) {
									nclMat1.loadDistance(new File(appset.distanceFilePaths.get(0)), fid);
									// nclMat1.writeToFile(new File(appset.outFolderPath+"\\distance.csv"), fid);
								} else if (appset.computeDMfromRules == true) {
									nclMat1.computeDistanceMatrix(rr, fid, appset.maxDistance, datJ.numExamples);
								}
							}
						}
					}

					// datJ sadrži train ako je split test
					dsc = new DataSetCreator(datJ);
					dsc1 = new DataSetCreator(datJ);
						
					naex = dsc.data.getNbRows();
					
					// create and modify settings for cicle 1
					if (leftSide == 1 && (endIndexRR1 - oldIndexRR1) > z * appset.numTargets) {
						
						// createSettings
						view2tmpSettings = Settings.cloneSettingsForRedescriptionMining(runnerView2.getSettings());
					
						if (appset.numSupplementTrees > 0) {
							
							view2tmpFSettings = Settings.cloneSettingsForRedescriptionMining(runnerView2.getSettings());
							
						}

						int endTmp = 0;
						if ((z + 1) * appset.numTargets > (endIndexRR1 - oldIndexRR1))
							endTmp = endIndexRR1;
						else
							endTmp = (z + 1) * appset.numTargets + oldIndexRR1;
						int startIndexRR1 = oldIndexRR1 + z * appset.numTargets;

						for (int i = startIndexRR1; i < endTmp; i++) { // do on the fly when reading rules
							if (rr1.rules.get(i).elements.size() >= appset.minSupport) { // do parameters analysis in this												// step
								nARules++;
							}
						}
						
						view2tmpSettings.modifySettings(nARules, dsc.schema.getNbAttributes());
						
						if (appset.numSupplementTrees > 0) {
							view2tmpFSettings.modifySettingsF(nARules, dsc.schema.getNbAttributes(), appset);
						}
						
					} else if (rightSide == 1 && (endIndexRR - oldIndexRR) > z * appset.numTargets) {
						
						view1tmpSettings = Settings.cloneSettingsForRedescriptionMining(runnerView1.getSettings());
						
						if (appset.numSupplementTrees > 0) {
							
							view1tmpFSettings = Settings.cloneSettingsForRedescriptionMining(runnerView1.getSettings());
							
						}

						int endTmp = 0;
						if ((z + 1) * appset.numTargets > (endIndexRR - oldIndexRR))
							endTmp = endIndexRR;
						else
							endTmp = (z + 1) * appset.numTargets + oldIndexRR;

						int startIndexRR = oldIndexRR + z * appset.numTargets;

						for (int i = startIndexRR; i < endTmp; i++) { // do on the fly when reading rules
							if (rr.rules.get(i).elements.size() >= appset.minSupport) {
								nARules++;
							}
						}
						
						view1tmpSettings.modifySettings(nARules, dsc1.schema.getNbAttributes());
						
						if (appset.numSupplementTrees > 0) {
							view1tmpFSettings.modifySettingsF(nARules, dsc1.schema.getNbAttributes(), appset);
						}
					}

					// create and modify settings for cicle 2
					if (leftSide1 == 1 && (endIndexRR1 - oldIndexRR1) > z * appset.numTargets) {
						
						view2tmp1Settings = Settings.cloneSettingsForRedescriptionMining(runnerView2.getSettings());
						
						if (appset.numSupplementTrees > 0) {
							
							view2tmp1FSettings = Settings.cloneSettingsForRedescriptionMining(runnerView2.getSettings());
							
						}

						int endTmp = 0;
						if ((z + 1) * appset.numTargets > (endIndexRR1 - oldIndexRR1))
							endTmp = endIndexRR1;
						else
							endTmp = oldIndexRR1 + (z + 1) * appset.numTargets;

						int startIndexRR1 = oldIndexRR1 + z * appset.numTargets;

						for (int i = startIndexRR1; i < endTmp; i++) {
							if (rr1.rules.get(i).elements.size() >= appset.minSupport) {
								nARules1++;
							}
						}
						
						view2tmp1Settings.modifySettings(nARules1, dsc.schema.getNbAttributes());
						
						if (appset.numSupplementTrees > 0) {
							view2tmp1FSettings.modifySettingsF(nARules1, dsc.schema.getNbAttributes(), appset);
						}

					} else if (rightSide1 == 1 && (endIndexRR - oldIndexRR) > z * appset.numTargets) {

						view1tmp1Settings = Settings.cloneSettingsForRedescriptionMining(runnerView1.getSettings());
						
						if (appset.numSupplementTrees > 0) {
							view1tmp1FSettings = Settings.cloneSettingsForRedescriptionMining(runnerView1.getSettings());
						}

						int endTmp = 0;
						if ((z + 1) * appset.numTargets > (endIndexRR - oldIndexRR))
							endTmp = endIndexRR;
						else
							endTmp = (z + 1) * appset.numTargets + oldIndexRR;

						int startIndexRR = oldIndexRR + z * appset.numTargets;

						for (int i = startIndexRR; i < endTmp; i++) {
							if (rr.rules.get(i).elements.size() >= appset.minSupport) {
								nARules1++;
							}
						}
						
						view1tmp1Settings.modifySettings(nARules1, dsc1.schema.getNbAttributes());
						
						if (appset.numSupplementTrees > 0) {
							view1tmp1FSettings.modifySettingsF(nARules1, dsc1.schema.getNbAttributes(), appset);
						}
					}

					RuleReader itRules = new RuleReader();
					RuleReader itRules1 = new RuleReader();
					RuleReader itRulesF = new RuleReader();
					RuleReader itRulesF1 = new RuleReader();

					// modify dataset for cicle 1
					
					TargetRulesMap.setCycle(Cycle.ONE);

					if (leftSide == 1 && (endIndexRR1 - oldIndexRR1) > z * appset.numTargets) {
						int startIndexRR1 = oldIndexRR1 + z * appset.numTargets;
						int endTmp = 0;
						if ((z + 1) * appset.numTargets > (endIndexRR1 - oldIndexRR1))
							endTmp = endIndexRR1;
						else
							endTmp = (z + 1) * appset.numTargets + oldIndexRR1;
						
						if (appset.treeTypes.get(1) == 1) {
							dsc.modifyDatasetS(startIndexRR1, endTmp, rr1, fid, appset);
						}
						else if (appset.treeTypes.get(1) == 0) {
							dsc.modifyDatasetCat(startIndexRR1, endTmp, rr1, fid, appset);
						}
						
					} else if (rightSide == 1 && (endIndexRR - oldIndexRR) > z * appset.numTargets) {
						int endTmp = 0;
						if ((z + 1) * appset.numTargets > (endIndexRR - oldIndexRR))
							endTmp = endIndexRR;
						else
							endTmp = (z + 1) * appset.numTargets + oldIndexRR;

						int startIndexRR = oldIndexRR + z * appset.numTargets;

						
						if (appset.treeTypes.get(0) == 1) {
							
							dsc.modifyDatasetS(startIndexRR, endTmp, rr, fid, appset);
							
						} else if (appset.treeTypes.get(0) == 0) {
							
							dsc.modifyDatasetCat(startIndexRR, endTmp, rr, fid, appset);
							
						}	
					}

					// modify dataset for cycle 2
					TargetRulesMap.setCycle(Cycle.TWO);

					if (leftSide1 == 1 && (endIndexRR1 - oldIndexRR1) > z * appset.numTargets) {
						int startIndexRR1 = oldIndexRR1 + z * appset.numTargets;
						int endTmp = 0;
						if ((z + 1) * appset.numTargets > (endIndexRR1 - oldIndexRR1))
							endTmp = endIndexRR1;
						else
							endTmp = (z + 1) * appset.numTargets + oldIndexRR1;

						
						if (appset.treeTypes.get(1) == 1) {
							
							dsc1.modifyDatasetS(startIndexRR1, endTmp, rr1, fid, appset);
							
						} else if (appset.treeTypes.get(1) == 0) {
							
							dsc1.modifyDatasetCat(startIndexRR1, endTmp, rr1, fid, appset);
							
						}
							
					} else if (rightSide1 == 1 && (endIndexRR - oldIndexRR) > z * appset.numTargets) {
						int endTmp = 0;
						if ((z + 1) * appset.numTargets > (endIndexRR - oldIndexRR))
							endTmp = endIndexRR;
						else
							endTmp = (z + 1) * appset.numTargets + oldIndexRR;

						int startIndexRR = oldIndexRR + z * appset.numTargets;

						if (appset.treeTypes.get(0) == 1/* appset.typeOfLSTrees==1 */) {
							
							dsc1.modifyDatasetS(startIndexRR, endTmp, rr, fid, appset);
							
						} else if (appset.treeTypes.get(0) == 0) {
							
							dsc1.modifyDatasetCat(startIndexRR, endTmp, rr, fid, appset);
							
						}
					}

					if ((appset.useNC.get(0) == true && rightSide == 1 && appset.networkInit == false
							&& appset.useNetworkAsBackground == false)
							|| (appset.useNC.get(1) == true && leftSide == 1 && appset.networkInit == false
									&& appset.useNetworkAsBackground == false)) {
						if (appset.system.equals("windows")) {
							nclMat.resetFile(new File(appset.outFolderPath + "\\distances.csv"));
							nclMat.writeToFile(new File(appset.outFolderPath + "\\distances.csv"), fid, appset);
						} else {
							nclMat.resetFile(new File(appset.outFolderPath + "/distances.csv"));
							nclMat.writeToFile(new File(appset.outFolderPath + "/distances.csv"), fid, appset);
						}
					}

					// run the second process on new data
					// iterate until convergence (no new rules, or very small amount obtained)
					
					TargetRulesMap.setCycle(Cycle.ONE);

					if (leftSide == 1 && (endIndexRR1 - oldIndexRR1) > z * appset.numTargets) {
						runnerView2.setSchema(dsc.schema);
						runnerView2.setData(dsc.data);
						runnerView2.setSettings(view2tmpSettings);
						
						itRules.rules = runnerView2.getRulesAndCoveredExamples(true, fid, datJ, appset);
						itRules.setSize();
//						for(Rule rul : itRules.rules) {
//							System.out.println("UNUTAR CLUSRM");
//							System.out.println("ID: " + rul.getRuleId());
//							System.out.println("SIZE: " + rul.elements.size());
//						}
						
						if (appset.numSupplementTrees > 0) {
							runnerView2.setSettings(view2tmpFSettings);

							itRulesF.rules = runnerView2.getRulesAndCoveredExamples(true, fid, datJ, appset);
							runnerView2.setSettings(view2tmpSettings);
							numSupplmentingForestRules += itRulesF.rules.size();
							itRulesF.setSize();
						}
						System.out.println("Process 2 side 1 finished!");
						
					} else if (rightSide == 1 && (endIndexRR - oldIndexRR) > z * appset.numTargets) {
						runnerView1.setSchema(dsc.schema);
						runnerView1.setData(dsc.data);
						runnerView1.setSettings(view1tmpSettings);
						
						itRules.rules = runnerView1.getRulesAndCoveredExamples(true, fid, datJ, appset);
						itRules.setSize();
//						for(Rule rul : itRules.rules) {
//							System.out.println("UNUTAR CLUSRM");
//							System.out.println("ID: " + rul.getRuleId());
//							System.out.println("SIZE: " + rul.elements.size());
//						}
						if (appset.numSupplementTrees > 0) {
							runnerView1.setSettings(view1tmpFSettings);

							itRulesF.rules = runnerView1.getRulesAndCoveredExamples(true, fid, datJ, appset);
							runnerView1.setSettings(view1tmpSettings);
							numSupplmentingForestRules += itRulesF.rules.size();
							itRulesF.setSize();
						}
						
						System.out.println("Process 1 side 1 finished!");
						
					}

					if ((appset.useNC.get(0) == true && rightSide1 == 1 && appset.networkInit == false
							&& appset.useNetworkAsBackground == false)
							|| (appset.useNC.get(1) == true && leftSide1 == 1 && appset.networkInit == false
									&& appset.useNetworkAsBackground == false)) {
						if (appset.system.equals("windows")) {
							nclMat1.resetFile(new File(appset.outFolderPath + "\\distances.csv"));
							nclMat1.writeToFile(new File(appset.outFolderPath + "\\distances.csv"), fid, appset);
						} else {
							nclMat1.resetFile(new File(appset.outFolderPath + "/distances.csv"));
							nclMat1.writeToFile(new File(appset.outFolderPath + "/distances.csv"), fid, appset);
						}
					}

					// run the second process for cycle 2 on new data
					// iterate until convergence (no new rules, or very small amount obtained)
					
					TargetRulesMap.setCycle(Cycle.TWO);

					if (leftSide1 == 1 && (endIndexRR1 - oldIndexRR1) > z * appset.numTargets) {
						runnerView2.setSchema(dsc1.schema);
						runnerView2.setData(dsc1.data);
						runnerView2.setSettings(view2tmp1Settings);
						
						itRules1.rules = runnerView2.getRulesAndCoveredExamples(true, fid, datJ, appset);
						itRules1.setSize();
//						for(Rule rul : itRules.rules) {
//							System.out.println("UNUTAR CLUSRM");
//							System.out.println("ID: " + rul.getRuleId());
//							System.out.println("SIZE: " + rul.elements.size());
//						}
						if (appset.numSupplementTrees > 0) {
							runnerView2.setSettings(view2tmp1FSettings);

							itRulesF1.rules = runnerView2.getRulesAndCoveredExamples(true, fid, datJ, appset);
							runnerView2.setSettings(view2tmp1Settings);
							numSupplmentingForestRules += itRulesF1.rules.size();
							itRulesF1.setSize();
						}
						
						System.out.println("Process 2 side 2 finished!");
						
					} else if (rightSide1 == 1 && (endIndexRR - oldIndexRR) > z * appset.numTargets) {
						runnerView1.setSchema(dsc1.schema);
						runnerView1.setData(dsc1.data);
						runnerView1.setSettings(view1tmp1Settings);
						
						itRules1.rules = runnerView1.getRulesAndCoveredExamples(true, fid, datJ, appset);
						itRules1.setSize();
//						for(Rule rul : itRules.rules) {
//							System.out.println("UNUTAR CLUSRM");
//							System.out.println("ID: " + rul.getRuleId());
//							System.out.println("SIZE: " + rul.elements.size());
//						}
						if (appset.numSupplementTrees > 0) {
							runnerView1.setSettings(view1tmp1FSettings);

							itRulesF1.rules = runnerView1.getRulesAndCoveredExamples(true, fid, datJ, appset);
							runnerView1.setSettings(view1tmp1Settings);
							numSupplmentingForestRules += itRulesF1.rules.size();
							itRulesF1.setSize();
						}
						
						System.out.println("Process 1 side 2 finished!");
						
					}

					dsc = null;
					dsc1 = null;
					
					// extract rules for cicle 1
									
					int newRules = 0;
					TargetRulesMap.setCycle(Cycle.ONE);
					if (leftSide == 1 && (endIndexRR1 - oldIndexRR1) > z * appset.numTargets) {
						if (z == 0)
							newRules = rr.addNewRulesC(itRules, appset.numnewRAttr, 1);
						else
							newRules = rr.addNewRulesC(itRules, appset.numnewRAttr, 0);
						if (appset.numSupplementTrees > 0)
							rr.addNewRulesCF(itRulesF, appset.numnewRAttr);
						// rr.extractRules(input);
					} else if (rightSide == 1 && (endIndexRR - oldIndexRR) > z * appset.numTargets) {
						if (z == 0)
							newRules = rr1.addNewRulesC(itRules, appset.numnewRAttr, 1);
						else
							newRules = rr1.addNewRulesC(itRules, appset.numnewRAttr, 0);
						if (appset.numSupplementTrees > 0)
							rr1.addNewRulesCF(itRulesF, appset.numnewRAttr);
						// rr1.extractRules(input);
					}
					
					// extract rules for cicle 2
					
					int newRules1 = 0;
					TargetRulesMap.setCycle(Cycle.TWO);
					if (leftSide1 == 1 && (endIndexRR1 - oldIndexRR1) > z * appset.numTargets) {
						if (z == 0)
							newRules1 = rr.addNewRulesC(itRules1, appset.numnewRAttr, 1);
						else
							newRules1 = rr.addNewRulesC(itRules1, appset.numnewRAttr, 0);
						if (appset.numSupplementTrees > 0)
							rr.addNewRulesCF(itRulesF1, appset.numnewRAttr);
						// rr.extractRules(input);
					} else if (rightSide1 == 1 && (endIndexRR - oldIndexRR) > z * appset.numTargets) {
						if (z == 0)
							newRules1 = rr1.addNewRulesC(itRules1, appset.numnewRAttr, 1);
						else
							newRules1 = rr1.addNewRulesC(itRules1, appset.numnewRAttr, 0);
						if (appset.numSupplementTrees > 0)
							rr1.addNewRulesCF(itRulesF1, appset.numnewRAttr);
						// rr1.extractRules(input);
					}

					System.out.println("New rules cicle 1: " + newRules);
					System.out.println("New rules cicle 2: " + newRules1);
				}
				// add the redescription creation code

				if (appset.optimizationType == 0) {
					if (appset.useJoin) {
						// TODO oldIndexRR1 i oldIndexRR
						// add computation of rule support if bagging
						newRedescriptions = rs.createGuidedJoinBasic(rr1, rr, jsN, appset, oldIndexRR1, oldIndexRR,
								runInd, oom, fid, datJ, elemFreq, attrFreq, redScores, redScoresAtt, redDistCoverage,
								redDistCoverageAt, redDistNetwork, targetAtScore, statistics, maxDiffScoreDistribution,
								nclMatInit, 0);
//						newRedescriptions = rs.createGuidedJoinBasicOld(rr1, rr, jsN, appset, oldIndexRR1, oldIndexRR,
//								runInd, oom, fid, datJ, elemFreq, attrFreq, redScores, redScoresAtt, redDistCoverage,
//								redDistCoverageAt, redDistNetwork, targetAtScore, statistics, maxDiffScoreDistribution,
//								nclMatInit, 0);
						if (appset.numSupplementTrees > 0) {
							rr.removeRulesCF();
							rr1.removeRulesCF();
						}
						rr.removeElements();
						rr1.removeElements();
					} else if (!appset.useJoin) {
						newRedescriptions = rs.createGuidedNoJoinBasic(rr1, rr, jsN, appset, oldIndexRR1, oldIndexRR,
								runInd, oom, fid, datJ, elemFreq, attrFreq, redScores, redScoresAtt, redDistCoverage,
								redDistCoverageAt, redDistNetwork, targetAtScore, statistics, maxDiffScoreDistribution,
								nclMatInit, 0);
						if (appset.numSupplementTrees > 0) {
							rr.removeRulesCF();
							rr1.removeRulesCF();
						}
						rr.removeElements();
						rr1.removeElements();
					}
				} else {
					if (appset.useJoin) {
						newRedescriptions = rs.createGuidedJoinExt(rr1, rr, jsN, appset, oldIndexRR1, oldIndexRR,
								runInd, oom, fid, datJ);// sqitch sides of rules
						rr.removeElements();
						rr1.removeElements();
					} else if (!appset.useJoin) {
						newRedescriptions = rs.createGuidedNoJoinExt(rr1, rr, jsN, appset, oldIndexRR1, oldIndexRR,
								runInd, oom, fid, datJ);
						rr.removeElements();
						rr1.removeElements();
					}
				}

				/*
				 * if(rs.redescriptions.size()==2) return;
				 */

				it++;

				TargetRulesMap.clearAll();
				
				System.out.println("New redescriptions: " + newRedescriptions);

				// if more than two viewes get guided search in here for further views...
				// should be modified for redescription addition
				// new join procedures should be created
				System.out.println("Number of viewes: " + datJ.W2indexs.size());
				for (int nws = 2; nws < datJ.W2indexs.size() + 1; nws++) {
					if (readers.size() < (datJ.W2indexs.size() - 2) + 1)
						readers.add(new RuleReader());
					int oldIndW = readers.get(nws - 2).newRuleIndex, endIndW = 0;

					SettingsReader setMW = new SettingsReader();// (appset.outFolderPath+"\\view3tmp.s",appset.outFolderPath+"\\view2.s");
					if (appset.system.equals("windows")) {
						setMW.setPath(appset.outFolderPath + "\\view3tmp.s");
						setMW.setStaticFilePath = appset.outFolderPath + "\\view3tmp.s";
						setMW.setDataFilePath(appset.outFolderPath + "\\Jinputnew.arff");
					} else {
						setMW.setPath(appset.outFolderPath + "/view3tmp.s");
						setMW.setStaticFilePath = appset.outFolderPath + "/view3tmp.s";
						setMW.setDataFilePath(appset.outFolderPath + "/Jinputnew.arff");
					}
					if ((nws - 1) < (datJ.W2indexs.size() - 2 + 1))
						setMW.createInitialSettingsGen(nws, datJ.W2indexs.get(nws - 1) + 1, datJ.W2indexs.get(nws) + 1,
								datJ.schema.getNbAttributes(), appset, 0);
					else
						setMW.createInitialSettingsGen(nws, datJ.W2indexs.get(nws - 1) + 1,
								datJ.schema.getNbAttributes() + 1, datJ.schema.getNbAttributes(), appset, 0);

					numBins = 0;
					Size = rs.redescriptions.size();
					if (Size % appset.numTargets == 0)
						numBins = Size / appset.numTargets;
					else
						numBins = Size / appset.numTargets + 1;

					for (int z = 0; z < numBins/* percentage.length-1 */; z++) {

						if (z == 0) {// should create network from redescriptions!
							if (appset.useNC.size() > nws && appset.networkInit == false) {
								if (appset.useNC.get(nws) == true && readers.get(nws - 2).rules.size() > 0) {
									nclMat.reset(appset);
									if (appset.distanceFilePaths.size() >= nws && appset.networkInit == false
											&& appset.useNetworkAsBackground == false) {
										nclMat.loadDistance(new File(appset.distanceFilePaths.get(nws)), fid);
										if (appset.system.equals("windows")) {
											nclMat.writeToFile(new File(appset.outFolderPath + "\\distance.csv"), fid,
													appset);
										} else {
											nclMat.writeToFile(new File(appset.outFolderPath + "/distance.csv"), fid,
													appset);
										}
									} else if (appset.computeDMfromRules == true) {
										nclMat.computeDistanceMatrix(rs.redescriptions, fid, appset.maxDistance,
												datJ.numExamples, oldRIndex);
										if (appset.system.equals("windows")) {
											nclMat.resetFile(new File(appset.outFolderPath + "\\distances.csv"));
											nclMat.writeToFile(new File(appset.outFolderPath + "\\distances.csv"), fid,
													appset);
										} else {
											nclMat.resetFile(new File(appset.outFolderPath + "/distances.csv"));
											nclMat.writeToFile(new File(appset.outFolderPath + "/distances.csv"), fid,
													appset);
										}
									}
								}
							}
							endIndW = readers.get(nws - 2).rules.size();
						}

						nARules = 0;
						nARules1 = 0;
						double startPerc = 0;// percentage[z];
						double endPerc = 0;// percentage[z+1];
						int minCovElements[] = new int[] { 0 };
						int maxCovElements[] = new int[] { 0 };
						int cuttof = 0;

						/*
						 * cuttof=rs.findCutoff(naex, startPerc, endPerc,
						 * minCovElements,maxCovElements,oldRIndex,
						 * appset.minSupport,appset.maxSupport,appset.numTargets);
						 * System.out.println("minCovElements: "+minCovElements[0]);
						 * System.out.println("maxCovElements: "+maxCovElements[0]);
						 * System.out.println("cuttof: "+cuttof);
						 * 
						 * if(cuttof==-1) continue;
						 */

						if (appset.system.equals("windows"))
							dsc = new DataSetCreator(appset.outFolderPath + "\\Jinput.arff");
						else
							dsc = new DataSetCreator(appset.outFolderPath + "/Jinput.arff");

						try {
							dsc.readDataset();
						} catch (IOException e) {
							e.printStackTrace();
						}

						System.out.println("startPerc: " + startPerc);
						System.out.println("endPerc: " + endPerc);

						int endTmp = 0;
						if ((z + 1) * appset.numTargets > rs.redescriptions.size())
							endTmp = rs.redescriptions.size();
						else
							endTmp = (z + 1) * appset.numTargets;
						// add conditions in the this part of the code...!!!
						int startIndexRR = oldRIndex[0] + z * appset.numTargets;

						for (int i = startIndexRR; i < endTmp; i++)// oldRIndex[0];i<rs.redescriptions.size();i++) //do
																	// on the fly when reading rules
							// if(rs.redescriptions.get(i).elements.size()<=naex*endPerc &&
							// rs.redescriptions.get(i).elements.size()>=naex*startPerc &&
							// rs.redescriptions.get(i).elements.size()>=minCovElements[0] &&
							// rs.redescriptions.get(i).elements.size()<=maxCovElements[0]) //do parameters
							// analysis in this step
							nARules++;
						setMW.ModifySettings(nARules, dsc.schema.getNbAttributes());
						try {
							if (appset.treeTypes.get(nws) == 1/* appset.typeOfRSTrees==1 */) {
								if (appset.system.equals("windows"))
									dsc.modifyDatasetS(startIndexRR, endTmp, rs.redescriptions,
											appset.outFolderPath + "\\Jinputnew.arff", fid, appset);
								else
									dsc.modifyDatasetS(startIndexRR, endTmp, rs.redescriptions,
											appset.outFolderPath + "/Jinputnew.arff", fid, appset);
							} else if (appset.treeTypes.get(nws) == 0/* appset.typeOfRSTrees==0 */) {
								if (appset.system.equals("windows"))
									dsc.modifyDatasetCat(startIndexRR, endTmp, rs.redescriptions,
											appset.outFolderPath + "\\Jinputnew.arff", fid, appset);
								else
									dsc.modifyDatasetCat(startIndexRR, endTmp, rs.redescriptions,
											appset.outFolderPath + "/Jinputnew.arff", fid, appset);
							}
							// if(appset.treeTypes.get(nws)==1/*appset.typeOfRSTrees==1*/)
							// dsc.modifyDatasetS(nARules, startPerc, endPerc, oldRIndex[0],
							// rs.redescriptions.size(), minCovElements[0],maxCovElements[0],
							// rs.redescriptions,appset.outFolderPath+"\\Jinputnew.arff",fid);
							// else if(appset.treeTypes.get(nws)==0/*appset.typeOfRSTrees==0*/)
							// dsc.modifyDatasetCat(nARules, startPerc, endPerc, oldRIndex[0],
							// rs.redescriptions.size(), minCovElements[0],maxCovElements[0],
							// rs.redescriptions,appset.outFolderPath+"\\Jinputnew.arff",fid);
						} catch (IOException e) {
							e.printStackTrace();
						}

						exec.run(appset.javaPath, appset.clusPath, appset.outFolderPath, "view3tmp.s"/* "wbtmp.s" */, 0,
								appset.clusteringMemory);// was 1 for rules before
						System.out.println("Process 1 side " + nws + " finished!");

						String input;
						if (appset.system.equals("windows"))
							input = appset.outFolderPath + "\\view3tmp.out";
						else
							input = appset.outFolderPath + "/view3tmp.out";

						int newRules = 0;
						RuleReader ItRules = new RuleReader();

						ItRules.extractRules(input, fid, datJ, appset);
						ItRules.setSize();
						if (z == 0)
							newRules = readers.get(nws - 2).addNewRulesC(ItRules, appset.numnewRAttr, 1);
						else
							newRules = readers.get(nws - 2).addNewRulesC(ItRules, appset.numnewRAttr, 0);
					}

					if (appset.useJoin) {// do redescription construction
						rs.combineViewRulesJoin(readers.get(nws - 2), jsN, appset, oldIndW, runInd, oom, fid, datJ,
								oldRIndex, nws);
					} else {// (rr, rr1, jsN, appset, oldIndexRR, oldIndexRR1, RunInd, oom,fid,datJ);
						rs.combineViewRules(readers.get(nws - 2), jsN, appset, oldIndW, runInd, oom, fid, datJ,
								oldRIndex, nws);
					}

					// rs.combineViewRules(readers.get(nws-2), jsN, appset, oldIndW, RunInd, oom,
					// fid, datJ, oldRIndex ,nws);
				}

				if (leftSide == 1) {
					leftSide = 0;
					rightSide = 1;
				} else if (rightSide == 1) {
					rightSide = 0;
					leftSide = 1;
				}
				if (leftSide1 == 1) {
					leftSide1 = 0;
					rightSide1 = 1;
				} else if (rightSide1 == 1) {
					rightSide1 = 0;
					leftSide1 = 1;
				}
				runInd++;
				System.out.println("Running index: " + runInd);
			}
		}

		System.out.println("Redescription size main: " + rs.redescriptions.size());

		// removing all redescriptions with inadequate minSupport and minJS
		rs.remove(appset);

		System.out.println("Redescription size main after remove: " + rs.redescriptions.size());

		// filtering
		// rs.filter(appset, rr, rr1,fid,datJ); // think about what we want and if we
		// need it

		System.out.println("Redescription size main after filter: " + rs.redescriptions.size());

		int numFullRed = 0;
		// computing pVal...
		numFullRed = rs.computePVal(datJ, fid);
		rs.removePVal(appset);

		System.out.println("Redescription size main after Pvalremove: " + rs.redescriptions.size());

		System.out.println("Found " + numFullRed + " redescriptions with JS=1.0 and minsupport>" + appset.minSupport);
		System.out.println("Found " + rs.redescriptions.size() + " redescriptions with JS>" + appset.minJS);

		int minimize = 0;
		if (appset.minimizeRules == true)
			minimize = 1;

		rs.adaptSet(datJ, fid, 0);
		System.out.println(rs.redescriptions.size() + " redescriptions with JS>" + appset.minJS
				+ " left after elimination of long rules");
		// further reduce before computing optimized set

		/*
		 * if(numFullRed>appset.numRetRed){ appset.minJS=1.0; rs.remove(appset); } else
		 * if(numFullRed<appset.numRetRed && rs.redescriptions.size()>appset.numRetRed){
		 * while(true){ int num=rs.countNumber(appset.minJS+0.1);
		 * if((rs.redescriptions.size()-num)>appset.numRetRed){
		 * appset.minJS=appset.minJS+0.1; rs.remove(appset); } else break; } }
		 */ // removed for the time being

		// sorting redescriptions
		System.out.println("Sorting rules!");
		rs.sortRedescriptions();

		/*
		 * System.out.println("Validation in main"); //this.adaptSet(dat, map); for(int
		 * i=0;i<rs.redescriptions.size();i++)//uncomment
		 * rs.redescriptions.get(i).removeRedundant();
		 */

		for (int i = 0; i < rs.redescriptions.size(); i++) {
			// rs.redescriptions.get(i).closeInterval(datJ, fid);
			rs.redescriptions.get(i).validate(datJ, fid);
		}

		if (appset.useSplitTesting == true)
			for (int i = 0; i < rs.redescriptions.size(); i++) {
				rs.redescriptions.get(i).ComputeValidationStatistics(datJ, datJFull, fid);
				rs.redescriptions.get(i).ComputeTestStatistics(datJ, datJTest, fid);
			}

		for (int i = 0; i < rs.redescriptions.size(); i++)
			rs.redescriptions.get(i).clearRuleMaps();

		if (appset.attributeImportance == 0)
			rs.adaptSet(datJ, fid, minimize);
		else
			rs.adaptSet(datJ, fid, 0);

		/*
		 * CoocurenceMatrix coc=new
		 * CoocurenceMatrix(datJ.numExamples,datJ.schema.getNbAttributes()-1);
		 * coc.computeMatrix(rs, datJ); File out=new
		 * File(appset.outFolderPath+"\\Elements.txt"); coc.writeToFileElements(out,
		 * datJ.numExamples); out=new File(appset.outFolderPath+"\\Attributes.txt");
		 * coc.writeToFileAttributes(out,datJ.schema.getNbAttributes()-1);
		 */

		System.out.println("Computing rule score and sorting!");
		RedescriptionSet Result = rs;// new RedescriptionSet();

		if (appset.optimizationType == 0) {

			double sumN = 0.0;
			/*
			 * if(appset.JSImpWeight+appset.PValImpWeight+appset.AttDivImpWeight+appset.
			 * ElemDivImpWeight+appset.RuleSizeImpWeight>1.0){
			 * sumN=appset.JSImpWeight+appset.PValImpWeight+appset.AttDivImpWeight+appset.
			 * ElemDivImpWeight+appset.RuleSizeImpWeight; appset.JSImpWeight/=sumN;
			 * appset.PValImpWeight/=sumN; appset.AttDivImpWeight/=sumN;
			 * appset.ElemDivImpWeight/=sumN; appset.RuleSizeImpWeight/=sumN; }
			 */

			double heuristicWeights[] = appset.preferences.get(0);
			// double heuristicWeights[]=new
			// double[]{appset.JSImpWeight,appset.PValImpWeight,appset.ElemDivImpWeight,appset.AttDivImpWeight,appset.RuleSizeImpWeight};
			// Result.createRedescriptionSetCooc(rs, heuristicWeights, appset,datJ,fid,coc);
			// Result.createRedescriptionSet(rs, heuristicWeights, appset,datJ,fid);
			double coverage[] = new double[2];
			double ResultsScore = Result.computeRedescriptionSetScore(heuristicWeights, coverage, datJ, fid);
			System.out.println("Results score: " + ResultsScore);
			// writing redescriptions to file
			// rs.writeToFile(appset.outFolderPath+"\\"+appset.outputName, datJ, fid, rr,
			// rr1, startTime,numFullRed,appset);//fix output file name
			if (appset.system.equals("windows")) {
				Result.writeToFile(appset.outFolderPath + "\\" + appset.outputName + (1) + ".rr", datJ, fid, startTime,
						numFullRed, appset, ResultsScore, coverage, oom);
				Result.writePlots(appset.outFolderPath + "\\" + "RuleData" + (1) + ".csv", appset, datJ, fid);
			} else {
				Result.writeToFile(appset.outFolderPath + "/" + appset.outputName + (1) + ".rr", datJ, fid, startTime,
						numFullRed, appset, ResultsScore, coverage, oom);
				Result.writePlots(appset.outFolderPath + "/" + "RuleData" + (1) + ".csv", appset,datJ,fid);
			}
		} else {
			double coverage[];
			rs.computeAllMeasureFS(datJ, appset, fid);

			double ResultsScore = 0.0;

			CoocurenceMatrix coc = null;

			if (datJ.numExamples < 10000 && datJ.schema.getNbAttributes() - 1 < 10000) {
				coc = new CoocurenceMatrix(datJ.numExamples, datJ.schema.getNbAttributes() - 1);
				coc.computeMatrix(rs, datJ);
			}

			Result = new RedescriptionSet();

			double sumN = 0.0;
			/*
			 * if(appset.JSImpWeight+appset.PValImpWeight+appset.AttDivImpWeight+appset.
			 * ElemDivImpWeight+appset.RuleSizeImpWeight>1.0){
			 * sumN=appset.JSImpWeight+appset.PValImpWeight+appset.AttDivImpWeight+appset.
			 * ElemDivImpWeight+appset.RuleSizeImpWeight; appset.JSImpWeight/=sumN;
			 * appset.PValImpWeight/=sumN; appset.AttDivImpWeight/=sumN;
			 * appset.ElemDivImpWeight/=sumN; appset.RuleSizeImpWeight/=sumN; }
			 */

			if (appset.parameters.size() == 0 && appset.exhaustiveTesting == 0) {
				ArrayList<Double> par = new ArrayList<>();
				par.add(appset.minJS);
				par.add((double) appset.minSupport);
				par.add((double) appset.missingValueJSType);
				System.out.println("Configuring the default parameters...");
				appset.parameters.add(par);
			}

			if (appset.exhaustiveTesting == 0) {
				for (int i = 0; i < appset.parameters.size(); i++) {
					appset.minJS = appset.parameters.get(i).get(0);
					appset.minSupport = appset.parameters.get(i).get(1).intValue();
					Result = new RedescriptionSet();

					ArrayList<RedescriptionSet> resSets = null;
					if (datJ.numExamples < 10000 && datJ.schema.getNbAttributes() - 1 < 10000)
						resSets = Result.createRedescriptionSetsCoocGen(rs, appset.preferences,
								appset.parameters.get(i).get(2).intValue(), appset, datJ, fid, coc);// adds the most
																									// specific
																									// redescription
																									// first
					else
						resSets = Result.createRedescriptionSetsRandGen(rs, appset.preferences,
								appset.parameters.get(i).get(2).intValue(), appset, datJ, fid, coc);// should add one
																									// highly accurate
																									// redescription at
																									// random

					if (resSets == null) {// perhaps create a null file
						break;
					}

					for (int rset = 0; rset < resSets.size(); rset++)
						resSets.get(rset).computeLift(datJ, fid);

					System.out.println("exhaustiveTesting = 0");
					System.out.println("RS size: " + resSets.size());

					for (int fit = 0; fit < resSets.size(); fit++) {
						coverage = new double[2];

						ResultsScore = resSets.get(fit).computeRedescriptionSetScoreGen(appset.preferences.get(fit),
								appset.parameters.get(i).get(2).intValue(), coverage, datJ, appset, fid);
						System.out.println("Results score: " + ResultsScore);
						numFullRed = resSets.get(fit).computePVal(datJ, fid);

						if (appset.system.equals("windows"))
							resSets.get(fit).writeToFile(
									appset.outFolderPath + "\\" + appset.outputName + "StLev_" + fit + " minjs "
											+ appset.minJS + " JSType " + appset.parameters.get(i).get(2).intValue()
											+ ".rr",
									datJ, fid, startTime, numFullRed, appset, ResultsScore, coverage, oom);
						else
							resSets.get(fit).writeToFile(
									appset.outFolderPath + "/" + appset.outputName + "StLev_" + fit + " minjs "
											+ appset.minJS + " JSType " + appset.parameters.get(i).get(2).intValue()
											+ ".rr",
									datJ, fid, startTime, numFullRed, appset, ResultsScore, coverage, oom);
						resSets.get(fit).writePlots(
								appset.outFolderPath + "\\" + "RuleData" + "StLev_" + fit + " minjs " + appset.minJS
										+ "JSType " + appset.parameters.get(i).get(2).intValue() + ".csv",
								appset, datJ, fid);

						// coc.init(datJ.numExamples, datJ.schema.getNbAttributes()-1);

						// resSets.get(fit).redescriptions.clear();
					}
				}
			} else if (appset.exhaustiveTesting == 1) {
				System.out.println("type of experimentation: " + appset.exhaustiveTesting);
				for (int type = appset.parameters.get(2).get(0).intValue(); type <= appset.parameters.get(2).get(1)
						.intValue(); type++) {
					for (double minjs = appset.parameters.get(0).get(0); minjs <= appset.parameters.get(0)
							.get(1); minjs += appset.parameters.get(0).get(2)) {
						for (int minSupp = appset.parameters.get(1).get(0).intValue(); minSupp <= appset.parameters
								.get(1).get(1).intValue(); minSupp += appset.parameters.get(1).get(2).intValue()) {
							appset.minJS = minjs;
							appset.minSupport = minSupp;
							Result = new RedescriptionSet();

							// ArrayList<RedescriptionSet>
							// resSets=Result.createRedescriptionSetsCoocGen(rs,appset.preferences,type,
							// appset,datJ,fid,coc);
							ArrayList<RedescriptionSet> resSets = null;
							if (datJ.numExamples < 10000 && datJ.schema.getNbAttributes() - 1 < 10000)
								resSets = Result.createRedescriptionSetsCoocGen(rs, appset.preferences, type, appset,
										datJ, fid, coc);// adds the most specific redescription first
							else
								resSets = Result.createRedescriptionSetsRandGen(rs, appset.preferences, type, appset,
										datJ, fid, coc);// should add one highly accurate redescription at random

							for (int rset = 0; rset < resSets.size(); rset++)
								resSets.get(rset).computeLift(datJ, fid);

							for (int fit = 0; fit < resSets.size(); fit++) {
								coverage = new double[2];

								ResultsScore = resSets.get(fit).computeRedescriptionSetScoreGen(
										appset.preferences.get(fit), type, coverage, datJ, appset, fid);
								// resSets.get(fit).adaptSet(datJ, fid, 0);
								numFullRed = resSets.get(fit).computePVal(datJ, fid);
								System.out.println("Results score: " + ResultsScore);

								if (appset.system.equals("windows"))
									resSets.get(fit).writeToFile(
											appset.outFolderPath + "\\" + appset.outputName + "StLev_" + fit + " minjs "
													+ appset.minJS + " JSType " + type + "minSupp " + appset.minSupport
													+ ".rr",
											datJ, fid, startTime, numFullRed, appset, ResultsScore, coverage, oom);
								else
									resSets.get(fit).writeToFile(
											appset.outFolderPath + "/" + appset.outputName + "StLev_" + fit + " minjs "
													+ appset.minJS + " JSType " + type + "minSupp " + appset.minSupport
													+ ".rr",
											datJ, fid, startTime, numFullRed, appset, ResultsScore, coverage, oom);
								// resSets.get(fit).writePlots(appset.outFolderPath+"\\"+"RuleData"+"StLev_"+fit+"
								// minjs "+appset.minJS+"JSType "+type+"minSupp "+appset.minSupport+".csv",
								// appset,datJ,fid);

								// resSets.get(fit).redescriptions.clear();
							}
						}
					}
				}
			}
		}

		if (appset.numSupplementTrees > 0) System.out.println("number of rules found by supplementing forest: " + numSupplmentingForestRules);

		Result.redescriptions.clear();
		rs.redescriptions.clear();
		rr.rules.clear();
		rr1.rules.clear();
		
	}

	public static int getInitSetCapacity() {
		return initSetCapacity;
	}

	public static void setInitSetCapacity(int initSetCapacity) {
		SupplementingRandomForest.initSetCapacity = initSetCapacity;
	}
}
