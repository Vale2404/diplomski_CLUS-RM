/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package redescriptionmining;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author matej
 */
public class SettingsReader {
    String setStaticFilePath;
    String setPath;
    String setDataFilePath;
    final static Charset ENCODING = StandardCharsets.UTF_8;

    SettingsReader() {
        setPath = "";
    }

    SettingsReader(String path, String statPath) {
        setPath = path;
        setStaticFilePath = statPath;
    }

    void setPath(String path) {
        setPath = path;
    }

    void setDataFilePath(String path) {
        setDataFilePath = path;
    }

    void changeAlpha(Double value) {
        File settings = new File(setPath);
        File statsettings = new File(setStaticFilePath);
        int lineNum = 0;
        int ClusteringMax = 0;

        BufferedReader reader;
        String file = "";
        try {
            Path path = Paths.get(statsettings.getAbsolutePath());
            System.out.println("Path: " + statsettings.getAbsolutePath());
            reader = Files.newBufferedReader(path, ENCODING);
            String line = null;
            while ((line = reader.readLine()) != null) {
                //return set;
                if (line.contains("Alpha")) {
                    String s[] = line.split(" = ");
                    file += "Alpha = " + value + "\n";
                } else {
                    file += line + "\n";
                }
            }
            reader.close();
        } catch (IOException io) {
            io.printStackTrace();
        }

        try {
            PrintWriter out = new PrintWriter(setPath);
            out.write(file);
            out.close();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
        //return set;
    }

    void changeSeed() {
        File statsettings = new File(setStaticFilePath);

        BufferedReader reader;
        String file = "";
        try {
            Path path = Paths.get(statsettings.getAbsolutePath());
            System.out.println("Path: " + statsettings.getAbsolutePath());
            reader = Files.newBufferedReader(path, ENCODING);
            String line = null;
            while ((line = reader.readLine()) != null) {
                //return set;
                if (line.contains("RandomSeed")) {
                    String s[] = line.split(" = ");
                    file += "RandomSeed = " + System.currentTimeMillis() + "\n";
                } else {
                    file += line + "\n";
                }
            }
            reader.close();
        } catch (IOException io) {
            io.printStackTrace();
        }

        try {
            PrintWriter out = new PrintWriter(setPath);
            out.write(file);
            out.close();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }

    }


    void ModifySettingsF(int RuleCount, int numElements, ApplicationSettings appset) {

        File settings = new File(setPath);
        File statsettings = new File(setStaticFilePath);
        int lineNum = 0;
        int ClusteringMax = 0;

        System.out.println("ModifySettingsF: ");
        System.out.println(RuleCount + " " + numElements);

        if (RuleCount == 0)
            RuleCount = 1;

        BufferedReader reader;
        String file = "";
        try {
            Path path = Paths.get(statsettings.getAbsolutePath());
            System.out.println("Path: " + statsettings.getAbsolutePath());
            reader = Files.newBufferedReader(path, ENCODING);
            String line = null;
            while ((line = reader.readLine()) != null) {
                //return set;
                if (line.contains("Clustering")) {
                    String s[] = line.split(" = ");
                    ClusteringMax = numElements;
                    file += "Clustering = " + (ClusteringMax + 1) + "-" + (ClusteringMax + RuleCount) + "\n";

                } else if (line.contains("Target")) {
                    file += "Target = " + (ClusteringMax + 1) + "-" + (ClusteringMax + RuleCount) + "\n";
                } else if (line.contains("File = ")) {
                    file += "File = " + setDataFilePath + "\n";
                } else if (line.contains("Iterations")) {
                    file += "Iterations = " + appset.numSupplementTrees + "\n";
                } else if (line.contains("SelectRandomSubspaces")) {
                    file += "SelectRandomSubspaces = " + ((int) Math.max(((ClusteringMax + RuleCount - 1 - ClusteringMax - 1 + 1 - 2 + 1) * (1 - Math.pow(0.001, 1.0 / (appset.numTreesinForest * appset.aTreeDepth)))) + 1, ((Math.log10((ClusteringMax + RuleCount - 1 - ClusteringMax - 1 + 1 - 2 + 1)) / Math.log10(2)) + 1)))/*((int)Math.max((((double)W2indexEnd-1-W2indexStart+1-2+1)/(double)appset.numTreesinForest),((Math.log10((W2indexEnd-1-W2indexStart+1-2+1))/Math.log10(2))+1)))/*((int)Math.sqrt((W2indexEnd-1-W2indexStart+1-2+1))+1)*/ + "\n";
                } else {
                    file += line + "\n";
                }
            }
            reader.close();
        } catch (IOException io) {
            io.printStackTrace();
        }

        try {
            PrintWriter out = new PrintWriter(setPath);
            out.write(file);
            out.close();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
        //return set;
    }


    void ModifySettings(int RuleCount, int numElements) {

        File settings = new File(setPath);
        File statsettings = new File(setStaticFilePath);
        int lineNum = 0;
        int ClusteringMax = 0;

        BufferedReader reader;
        String file = "";
        try {
            Path path = Paths.get(statsettings.getAbsolutePath());
            System.out.println("Path: " + statsettings.getAbsolutePath());
            reader = Files.newBufferedReader(path, ENCODING);
            String line = null;
            while ((line = reader.readLine()) != null) {
                //return set;
                if (line.contains("Clustering")) {
                    String s[] = line.split(" = ");
                    ClusteringMax = numElements;
                    file += "Clustering = " + (ClusteringMax + 1) + "-" + (ClusteringMax + RuleCount) + "\n";

                } else if (line.contains("Target")) {
                    file += "Target = " + (ClusteringMax + 1) + "-" + (ClusteringMax + RuleCount) + "\n";
                } else if (line.contains("File = ")) {
                    file += "File = " + setDataFilePath + "\n";
                } else {
                    file += line + "\n";
                }
            }
            reader.close();
        } catch (IOException io) {
            io.printStackTrace();
        }

        try {
            PrintWriter out = new PrintWriter(setPath);
            out.write(file);
            out.close();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
        //return set;
    }

    void createInitialSettings(int view, int W2index, int numAttr, ApplicationSettings appset) {
        String file = "";

        if (view == 1) {
            file += "[Data]\n";
            file += "File = " + setDataFilePath + "\n";
            file += "TestSet = None\n";
            file += "PruneSet = None\n";
            file += "PruneSetMax = Infinity\n";
            file += "[General]\n";
            file += "Verbose = 1\n";
            file += "RandomSeed = 0\n";
            file += "ResourceInfoLoaded = No\n";
            if (appset.legacy == 1)
                file += "Compatibility = Latest\n";
            file += "[Attributes]\n";
            file += "Descriptive = 2-" + (W2index - 1) + "\n";
            file += "Clustering = " + W2index + "-" + numAttr + "\n";
            file += "Key = 1\n";
            file += "[Tree]\n";
            file += "Heuristic = VarianceReduction\n";//Default
            file += "BinarySplit = Yes\n";
            file += "PruningMethod = None\n";
            file += "ConvertToRules = AllNodes\n";
            file += "[Constraints]\n";
            if (appset.aTreeDepth == Integer.MAX_VALUE)
                file += "MaxDepth = Infinity\n";
            else
                file += "MaxDepth = " + appset.aTreeDepth + "\n";
            if (appset.legacy == 1) {
                file += "[Rules]\n";
                file += "CoveringMethod=RulesfromTree\n";
                file += "RuleAddingMethod=IfBetter\n";
            }
            file += "[Ensemble]\n";
            file += "Iterations= " + appset.numTreesinForest + "\n";
            file += "EnsembleMethod=RSubspaces\n";
            file += "SelectRandomSubspaces = " + (W2index - 1 - 2 + 1) + "\n";//((int)Math.sqrt((W2index-1-2+1))+1)+"\n";
            file += "ConvertToRules = Yes\n";
            file += "[Output]\n";
            file += "AllFoldModels = Yes\n";
            file += "AllFoldErrors = No\n";
            file += "TrainErrors = No\n";
            file += "UnknownFrequency = No\n";
            file += "BranchFrequency = No\n";
            if (appset.legacy == 1) {
                file += "ShowInfo = {Count}\n";
                file += "ShowModels = {Default, Pruned, Others}\n";
            } else {
                file += "ShowInfo = [Count]\n";
                file += "ShowModels = [Default, Pruned, Others]\n";
            }
            file += "PrintModelAndExamples = Yes\n";
            file += "ModelIDFiles = No\n";
            file += "OutputPythonModel = No\n";
            file += "OutputDatabaseQueries = No\n";
        } else {
            file += "[Data]\n";
            file += "File = " + setDataFilePath + "\n";
            file += "TestSet = None\n";
            file += "PruneSet = None\n";
            file += "PruneSetMax = Infinity\n";
            file += "[General]\n";
            file += "Verbose = 1\n";
            file += "RandomSeed = 0\n";
            file += "ResourceInfoLoaded = No\n";
            if (appset.legacy == 1)
                file += "Compatibility = Latest\n";
            file += "[Attributes]\n";
            file += "Descriptive = " + (W2index) + "-" + numAttr + "\n";
            file += "Clustering = 2-" + (W2index - 1) + "\n";
            file += "Key = 1\n";
            file += "[Tree]\n";
            file += "Heuristic = VarianceReduction\n";//Default
            file += "BinarySplit = Yes\n";
            file += "PruningMethod = None\n";
            file += "ConvertToRules = AllNodes\n";
            file += "[Constraints]\n";
            if (appset.aTreeDepth == Integer.MAX_VALUE)
                file += "MaxDepth = Infinity\n";
            else
                file += "MaxDepth = " + appset.aTreeDepth + "\n";
            file += "[Rules]\n";
            file += "CoveringMethod=RulesfromTree\n";
            file += "RuleAddingMethod=IfBetter\n";
            file += "[Ensemble]\n";
            file += "Iterations= " + appset.numTreesinForest + "\n";
            file += "EnsembleMethod=RSubspaces\n";
            file += "SelectRandomSubspaces = " + (numAttr - W2index + 1) + "\n";//(((int)Math.sqrt((numAttr-W2index+1))+1))+"\n";
            file += "ConvertToRules = Yes\n";
            file += "[Output]\n";
            file += "AllFoldModels = Yes\n";
            file += "AllFoldErrors = No\n";
            file += "TrainErrors = No\n";
            file += "UnknownFrequency = No\n";
            file += "BranchFrequency = No\n";
            if (appset.legacy == 1) {
                file += "ShowInfo = {Count}\n";
                file += "ShowModels = {Default, Pruned, Others}\n";
            } else {
                file += "ShowInfo = [Count]\n";
                file += "ShowModels = [Default, Pruned, Others]\n";
            }
            file += "PrintModelAndExamples = Yes\n";
            file += "ModelIDFiles = No\n";
            file += "OutputPythonModel = No\n";
            file += "OutputDatabaseQueries = No\n";
        }

        try {
            PrintWriter out = new PrintWriter(setPath);
            out.write(file);
            out.close();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
    }


    void createInitialSettings1(int view, int W2index, int numAttr, ApplicationSettings appset) {
        String file = "";

        if (view == 1) {
            file += "[Data]\n";
            file += "File = " + setDataFilePath + "\n";
            file += "TestSet = None\n";
            file += "PruneSet = None\n";
            file += "PruneSetMax = Infinity\n";
            file += "[General]\n";
            file += "Verbose = 1\n";
            file += "RandomSeed = 0\n";
            file += "ResourceInfoLoaded = No\n";
            if (appset.legacy == 1)
                file += "Compatibility = Latest\n";
            file += "[Attributes]\n";
            file += "Descriptive = 2-" + (W2index - 1) + "\n";
            file += "Clustering = " + numAttr + "\n";
            file += "Target = " + numAttr + "\n";
            file += "Key = 1\n";
            file += "[Tree]\n";
            file += "Heuristic = VarianceReduction\n";//Default
            file += "BinarySplit = Yes\n";
            file += "PruningMethod = None\n";
            file += "ConvertToRules = AllNodes\n";
            file += "[Constraints]\n";
            if (appset.aTreeDepth == Integer.MAX_VALUE)
                file += "MaxDepth = Infinity\n";
            else
                file += "MaxDepth = " + appset.aTreeDepth + "\n";
            file += "[Rules]\n";
            file += "CoveringMethod=RulesfromTree\n";
            file += "RuleAddingMethod=IfBetter\n";
            file += "[Ensemble]\n";
            file += "Iterations= " + appset.numTreesinForest + "\n";
            file += "EnsembleMethod=RSubspaces\n";
            file += "SelectRandomSubspaces = " + (W2index - 1 - 2 + 1) + "\n";//((int)Math.sqrt((W2index-1-2+1))+1)+"\n";
            file += "ConvertToRules = Yes\n";
            file += "[Output]\n";
            file += "AllFoldModels = Yes\n";
            file += "AllFoldErrors = No\n";
            file += "TrainErrors = No\n";
            file += "UnknownFrequency = No\n";
            file += "BranchFrequency = No\n";
            file += "ShowInfo = {Count}\n";
            file += "ShowModels = {Default, Pruned, Others}\n";
            file += "PrintModelAndExamples = Yes\n";
            file += "ModelIDFiles = No\n";
            file += "OutputPythonModel = No\n";
            file += "OutputDatabaseQueries = No\n";
        } else {
            file += "[Data]\n";
            file += "File = " + setDataFilePath + "\n";
            file += "TestSet = None\n";
            file += "PruneSet = None\n";
            file += "PruneSetMax = Infinity\n";
            file += "[General]\n";
            file += "Verbose = 1\n";
            file += "RandomSeed = 0\n";
            file += "ResourceInfoLoaded = No\n";
            if (appset.legacy == 1)
                file += "Compatibility = Latest\n";
            file += "[Attributes]\n";
            file += "Descriptive = " + (W2index) + "-" + (numAttr - 1) + "\n";
            file += "Clustering = " + numAttr + "\n";
            file += "Target = " + numAttr + "\n";
            file += "Key = 1\n";
            file += "[Tree]\n";
            file += "Heuristic = VarianceReduction\n";//Default
            file += "BinarySplit = Yes\n";
            file += "PruningMethod = None\n";
            file += "ConvertToRules = AllNodes\n";
            file += "[Constraints]\n";
            if (appset.aTreeDepth == Integer.MAX_VALUE)
                file += "MaxDepth = Infinity\n";
            else
                file += "MaxDepth = " + appset.aTreeDepth + "\n";
            file += "[Rules]\n";
            file += "CoveringMethod=RulesfromTree\n";
            file += "RuleAddingMethod=IfBetter\n";
            file += "[Ensemble]\n";
            file += "Iterations= " + appset.numTreesinForest + "\n";
            file += "EnsembleMethod=RSubspaces\n";
            file += "SelectRandomSubspaces = " + (numAttr - W2index + 1) + "\n";//(((int)Math.sqrt((numAttr-W2index+1))+1))+"\n";
            file += "ConvertToRules = Yes\n";
            file += "[Output]\n";
            file += "AllFoldModels = Yes\n";
            file += "AllFoldErrors = No\n";
            file += "TrainErrors = No\n";
            file += "UnknownFrequency = No\n";
            file += "BranchFrequency = No\n";
            file += "ShowInfo = {Count}\n";
            file += "ShowModels = {Default, Pruned, Others}\n";
            file += "PrintModelAndExamples = Yes\n";
            file += "ModelIDFiles = No\n";
            file += "OutputPythonModel = No\n";
            file += "OutputDatabaseQueries = No\n";
        }

        try {
            PrintWriter out = new PrintWriter(setPath);
            out.write(file);
            out.close();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    void createInitialSettingsGen(int view, int W2indexStart, int W2indexEnd, int numAttr, ApplicationSettings appset, int initial) {
        String file = "";

        file += "[Data]\n";
        file += "File = " + setDataFilePath + "\n";
        file += "TestSet = None\n";
        file += "PruneSet = None\n";
        file += "PruneSetMax = Infinity\n";
        file += "[General]\n";
        file += "Verbose = 1\n";
        file += "RandomSeed = 0\n";
        file += "ResourceInfoLoaded = No\n";
        if (appset.legacy == 1)
            file += "Compatibility = Latest\n";
        file += "[Attributes]\n";
        file += "Descriptive = " + (W2indexStart - 1) + "-" + (W2indexEnd - 1) + "\n";
        file += "Clustering = " + (numAttr + 1) + "\n";
        file += "Target = " + (numAttr + 1) + "\n";
        file += "Key = 1\n";
        if ((appset.useNC.size() > view && initial == 0 && appset.networkInit == false) || ((appset.useNC.get(view) == true && appset.networkInit == true && initial != 0))) {
            if (appset.useNC.get(view) == true)
                file += "GIS=2\n";
        }
        file += "[Tree]\n";
        if (appset.useNC.size() > view) {
            if ((appset.useNC.get(view) == true && initial == 0 && appset.networkInit == false) || (appset.useNC.get(view) == true && appset.networkInit == true && initial != 0)) {
                file += "Heuristic = VarianceReductionGIS\n";//Default
                file += "SpatialMatrix = " + appset.spatialMatrix.get(view) + "\n";//change
                file += "SpatialMeasure = " + appset.spatialMeasures.get(view) + "\n";//change
                file += "Bandwidth=" + appset.Bandwith + "\n"; //add parameter
                file += "Alpha=" + appset.Alpha + "\n"; //add parameter
            } else {
                file += "Heuristic = VarianceReduction\n";//Default
            }
        } else
            file += "Heuristic = VarianceReduction\n";//Default
        file += "BinarySplit = Yes\n";
        file += "PruningMethod = None\n";
        file += "ConvertToRules = AllNodes\n";
        file += "[Constraints]\n";
        if (appset.aTreeDepth == Integer.MAX_VALUE)
            file += "MaxDepth = Infinity\n";
        else
            file += "MaxDepth = " + appset.aTreeDepth + "\n";
        if (appset.legacy == 1) {
            file += "[Rules]\n";
            file += "CoveringMethod=RulesfromTree\n";
            file += "RuleAddingMethod=IfBetter\n";
        }
        file += "[Ensemble]\n";
        file += "Iterations= " + appset.numTreesinForest + "\n";
        file += "EnsembleMethod=RSubspaces\n";
            
            /*if(appset.useNC.size()>view && appset.distanceFilePaths.size()>view){
                if(appset.useNC.get(view)==true){
                    W2indexStart--;
                    W2indexEnd--;
                    numAttr--;
             }
            }*/

        //file+="SelectRandomSubspaces = "+(W2indexEnd-1-W2indexStart+1-2+1)+"\n";//((int)Math.sqrt((W2indexEnd-1-W2indexStart+1-2+1))+1)+"\n";

        if (appset.numTreesinForest > 1)
            file += "SelectRandomSubspaces = " + ((int) (Math.max(((W2indexEnd - 1 - W2indexStart + 1 - 2 + 1) * (1 - Math.pow(0.001, 1.0 / (appset.numTreesinForest * appset.aTreeDepth)))) + 1, ((Math.log10((W2indexEnd - 1 - W2indexStart + 1 - 2 + 1)) / Math.log10(2)) + 1))))/*((int)Math.max((((double)W2indexEnd-1-W2indexStart+1-2+1)/(double)appset.numTreesinForest),((Math.log10((W2indexEnd-1-W2indexStart+1-2+1))/Math.log10(2))+1)))/*((int)Math.sqrt((W2indexEnd-1-W2indexStart+1-2+1))+1)*/ + "\n";
        else
            file += "SelectRandomSubspaces = " + (W2indexEnd - W2indexStart + 1) + "\n";

        file += "ConvertToRules = Yes\n";
        file += "[Output]\n";
        file += "AllFoldModels = Yes\n";
        file += "AllFoldErrors = No\n";
        file += "TrainErrors = No\n";
        file += "UnknownFrequency = No\n";
        file += "BranchFrequency = No\n";
        if (appset.legacy == 1) {
            file += "ShowInfo = {Count}\n";
            file += "ShowModels = {Default, Pruned, Others}\n";
        } else {
            file += "ShowInfo = [Count]\n";
            file += "ShowModels = [Default, Pruned, Others]\n";
        }
        file += "PrintModelAndExamples = Yes\n";
        file += "ModelIDFiles = No\n";
        file += "OutputPythonModel = No\n";
        file += "OutputDatabaseQueries = No\n";

        try {
            PrintWriter out = new PrintWriter(setPath);
            out.write(file);
            out.close();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    void createInitialSettingsGenN(int view, int W2indexStart, int W2indexEnd, int numAttr, ApplicationSettings appset) {
        String file = "";

        file += "[Data]\n";
        file += "File = " + setDataFilePath + "\n";
        file += "TestSet = None\n";
        file += "PruneSet = None\n";
        file += "PruneSetMax = Infinity\n";
        file += "[General]\n";
        file += "Verbose = 1\n";
        file += "RandomSeed = 0\n";
        file += "ResourceInfoLoaded = No\n";
        if (appset.legacy == 1)
            file += "Compatibility = Latest\n";
        file += "[Attributes]\n";
        file += "Descriptive = " + (W2indexStart - 1) + "-" + (W2indexEnd - 1) + "\n";
        file += "Clustering = " + (numAttr + 1) + "\n";
        file += "Target = " + (numAttr + 1) + "\n";
        file += "Key = 1\n";
        if (appset.useNC.size() > view) {
            if (appset.useNC.get(view) == true && appset.networkInit == false)
                file += "GIS=2\n";
        }
        file += "[Tree]\n";
        if (appset.useNC.size() > view) {
            if (appset.useNC.get(view) == true && appset.networkInit == false) {
                file += "Heuristic = VarianceReductionGIS\n";//Default
                file += "SpatialMatrix = " + appset.spatialMatrix.get(view) + "\n";//change
                file += "SpatialMeasure = " + appset.spatialMeasures.get(view) + "\n";//change
                file += "Bandwidth=" + appset.Bandwith + "\n"; //add parameter
                file += "Alpha=" + appset.Alpha + "\n"; //add parameter
            } else
                file += "Heuristic = VarianceReduction\n";
        } else
            file += "Heuristic = VarianceReduction\n";//Default
        file += "BinarySplit = Yes\n";
        file += "PruningMethod = None\n";
        file += "ConvertToRules = AllNodes\n";
        file += "[Constraints]\n";
        if (appset.aTreeDepth == Integer.MAX_VALUE)
            file += "MaxDepth = Infinity\n";
        else
            file += "MaxDepth = " + appset.aTreeDepth + "\n";
        if (appset.legacy == 1) {
            file += "[Rules]\n";
            file += "CoveringMethod=RulesfromTree\n";
            file += "RuleAddingMethod=IfBetter\n";
        }
        file += "[Ensemble]\n";
        file += "Iterations= " + appset.numTreesinForest + "\n";
        file += "EnsembleMethod=RSubspaces\n";

        //file+="SelectRandomSubspaces = "+(W2indexEnd-1-W2indexStart+1-2+1)+"\n";//((int)Math.sqrt((W2indexEnd-1-W2indexStart+1-2+1))+1)+"\n";
        if (appset.numTreesinForest > 1)
            file += "SelectRandomSubspaces = " + ((int) Math.max(((W2indexEnd - 1 - W2indexStart + 1 - 2 + 1) * (1 - Math.pow(0.001, 1.0 / (appset.numTreesinForest * appset.aTreeDepth)))) + 1, ((Math.log10((W2indexEnd - 1 - W2indexStart + 1 - 2 + 1)) / Math.log10(2)) + 1)))/*((int)Math.max((((double)W2indexEnd-1-W2indexStart+1-2+1)/(double)appset.numTreesinForest),((Math.log10((W2indexEnd-1-W2indexStart+1-2+1))/Math.log10(2))+1)))/*((int)Math.sqrt((W2indexEnd-1-W2indexStart+1-2+1))+1)*/ + "\n";
        else
            file += "SelectRandomSubspaces = " + (W2indexEnd - 1 - W2indexStart + 1) + "\n";

        file += "ConvertToRules = Yes\n";
        file += "[Output]\n";
        file += "AllFoldModels = Yes\n";
        file += "AllFoldErrors = No\n";
        file += "TrainErrors = No\n";
        file += "UnknownFrequency = No\n";
        file += "BranchFrequency = No\n";
        if (appset.legacy == 1) {
            file += "ShowInfo = {Count}\n";
            file += "ShowModels = {Default, Pruned, Others}\n";
        } else {
            file += "ShowInfo = [Count]\n";
            file += "ShowModels = [Default, Pruned, Others]\n";
        }
        file += "PrintModelAndExamples = Yes\n";
        file += "ModelIDFiles = No\n";
        file += "OutputPythonModel = No\n";
        file += "OutputDatabaseQueries = No\n";

        try {
            PrintWriter out = new PrintWriter(setPath);
            out.write(file);
            out.close();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
    }

}
