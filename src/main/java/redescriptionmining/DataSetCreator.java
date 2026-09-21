/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package redescriptionmining;


import kmeans.Initializer;
import kmeans.Initializers;
import kmeans.Kmeans;
import si.ijs.kt.clus.data.ClusSchema;
import si.ijs.kt.clus.data.io.ARFFFile;
import si.ijs.kt.clus.data.io.ClusReader;
import si.ijs.kt.clus.data.io.ClusView;
import si.ijs.kt.clus.data.rows.DataTuple;
import si.ijs.kt.clus.data.rows.RowData;
import si.ijs.kt.clus.data.type.ClusAttrType;
import si.ijs.kt.clus.data.type.ClusAttrType.AttributeUseType;
import si.ijs.kt.clus.data.type.primitive.NominalAttrType;
import si.ijs.kt.clus.data.type.primitive.NumericAttrType;
import si.ijs.kt.clus.main.settings.Settings;
import si.ijs.kt.clus.util.exception.ClusException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.*;

/**
 * @author matej
 */
public class DataSetCreator {
    public String DataPath;
    public int numExamples;
    public ClusReader cread = null;
    Settings cset;
    public RowData data = null;
    public ClusSchema schema = null;
    ArrayList<Integer> W2indexs = new ArrayList<>();

    public DataSetCreator() {
    	cset = Settings.getDefaultSettings();
    }

    public DataSetCreator(DataSetCreator dataSetCreator) {
    	data = new RowData(dataSetCreator.data.getData(), dataSetCreator.data.getNbRows());
    	data.setSchema(dataSetCreator.data.getSchema().cloneSchema());
    	schema = data.getSchema();
    	schema.addRowsIndex();
    	if(dataSetCreator.cset == null) {
        	cset = Settings.getDefaultSettings();
    	} else {
    		cset = Settings.cloneSettingsForRedescriptionMining(dataSetCreator.cset);
    	}
    }

    public ArrayList<DataSetCreator> createSplit(double percentage) {

        DataSetCreator train = new DataSetCreator();
        DataSetCreator test = new DataSetCreator();
        //DataSetCreator test=new DataSetCreator();

        train.W2indexs = this.W2indexs;
        test.W2indexs = this.W2indexs;
        //  test.W2indexs=this.W2indexs;

        train.DataPath = this.DataPath;
        test.DataPath = this.DataPath;
        // test.DataPath=this.DataPath;

        train.W2indexs = new ArrayList<>();
        train.W2indexs.addAll(this.W2indexs);

        test.W2indexs = new ArrayList<>();
        test.W2indexs.addAll(this.W2indexs);

        //test.W2indexs=new ArrayList<>();
        //  test.W2indexs.addAll(this.W2indexs);

        try {
            train.cread = new ClusReader(train.DataPath, this.cset);
            test.cread = new ClusReader(test.DataPath, this.cset);
            //  test.cread=new ClusReader(test.DataPath,this.cset);
        } catch (IOException e) {
            e.printStackTrace();
        }

        train.cset = new Settings();
        test.cset = new Settings();
        // test.cset = new Settings();

        train.schema = this.schema;
        test.schema = this.schema;
        //test.schema = this.schema;
        train.data = new RowData(schema);
        test.data = new RowData(schema);

        RowData datTrain = null;
        RowData datTest = null;

        double a = this.numExamples * percentage;
        int numExTrain = (int) a;
        int numExTest = this.numExamples - numExTrain;

        Random rand = new Random();

        ArrayList<DataTuple> dataList = data.toArrayList();
        ArrayList<DataTuple> dataTrainList = new ArrayList<>();

        ArrayList<DataTuple> dataTestList = new ArrayList<>();

        HashSet<Integer> indexes = new HashSet<>();
        HashSet<Integer> indexesTest = new HashSet<>();

        int count = 0;

        while (count < numExTrain) {

            int randomNum = rand.nextInt((dataList.size() - 0) + 0) + 0;


            if (!indexes.contains(randomNum)) {
                count++;
                indexes.add(randomNum);
            }

        }

        for (int i = 0; i < dataList.size(); i++)
            if (!indexes.contains(i))
                indexesTest.add(i);

        for (int i : indexes) {
            dataTrainList.add(dataList.get(i));
        }

        for (int i : indexesTest) {
            dataTestList.add(dataList.get(i));
        }


        train.data.setFromList(dataTrainList);
        train.numExamples = numExTrain;

        test.data.setFromList(dataTestList);
        test.data.setFromList(dataTestList);
        test.numExamples = numExTest;

        ArrayList<DataSetCreator> res = new ArrayList<>();
        res.add(train);
        res.add(test);

        System.out.println("Num examples train: " + dataTrainList.size());
        System.out.println("Num examples test: " + dataTestList.size());

        //return train;

        return res;

        // return TrainTest;

    }

    public void importInitialData(ArrayList<String> inputs, ApplicationSettings appset) {
    	 ArrayList<DataSetCreator> dats = new ArrayList<>();
         for (int i = 1; i < inputs.size(); i++) {
             DataSetCreator dt = new DataSetCreator(inputs.get(i));
             dats.add(dt);
         }

         //DataSetCreator d2=new DataSetCreator(input2);
         cset = Settings.getDefaultSettings();
         
         try {
         	// čita prvi dataset koji sprema u this
             this.readDataset();
         } catch (Exception e) {
             e.printStackTrace();
         }

         W2indexs.add(this.schema.getNbAttributes() + 1);
         for (int i = 0; i < dats.size(); i++) {
             try {
                 //this.readDataset();
             	// čita drugi dataset kojeg drži u dats polju
                 dats.get(i).readDataset();
             } catch (Exception e) {
                 e.printStackTrace();
             }
             // ako imamo samo 2 viewa ovo se nikada neće dogoditi
             if ((i + 1) < dats.size())
                 W2indexs.add(this.schema.getNbAttributes() + 1 + dats.get(i).schema.getNbAttributes() - 1);
         }

         //System.out.println("Num attributes dataset 1: "+this.schema.getNbAttributes());
         //System.out.println("Num attributes dataset 2: "+d2.schema.getNbAttributes());

         //int numAttributes=schema.getNbAttributes()-1;

         // ArrayList<DataTuple> dataList2=d2.data.toArrayList();
         ArrayList<DataTuple> dataList = data.toArrayList();
         // System.out.println("object: "+t.getNominal(d2.data.m_Data[0]));
         int lastDouble = schema.getNumericAttrUse(AttributeUseType.All).length;
         System.out.println("lastDouble: " + lastDouble);
         int lastCat = schema.getNominalAttrUse(AttributeUseType.All).length;

         // int schCount=lastDouble, schCatCount=lastCat;
         
         // vadi broj numeričkih i nominalnih atributa od preostalih datasetova
         int shCount = 0, nhCount = 0;
         for (int i = 0; i < dats.size(); i++) {
             shCount += dats.get(i).schema.getNumericAttrUse(AttributeUseType.All).length;
             nhCount += dats.get(i).schema.getNominalAttrUse(AttributeUseType.All).length;
         }

         System.out.println("Number of numeric attributes in remaining datasets: " + shCount);

         for (int j = 0; j < data.getNbRows(); j++) {
             int l = data.getSchema().getNumericAttrUse(AttributeUseType.All).length + shCount;
             double arow[] = null;
             int carow[] = null;
             // System.out.println("Joint length: "+l);
             int l1 = data.getSchema().getNominalAttrUse(AttributeUseType.All).length + nhCount;

             if (l1 > 0)
                 carow = new int[l1];

             if (l > 0)
                 arow = new double[l];
             if (data.getSchema().getNumericAttrUse(AttributeUseType.All).length > 0) {
                 for (int k = 0; k < dataList.get(j).m_Doubles.length; k++) {
                     arow[k] = dataList.get(j).m_Doubles[k];
                 }
             }

             if (data.getSchema().getNominalAttrUse(AttributeUseType.All).length > 0) {
                 for (int k = 0; k < dataList.get(j).m_Ints.length; k++) {
                     carow[k] = dataList.get(j).m_Ints[k];
                 }
             }

                    /*  System.out.println("last: "+last);
                      System.out.println("dataset2 size: "+dataList2.get(0).m_Doubles.length);
                      System.out.println("arow length: "+arow.length);
                     */
             int ind = lastDouble;
             for (int k1 = 0; k1 < dats.size(); k1++) {
                 DataSetCreator d2 = dats.get(k1);
                 ArrayList<DataTuple> dataList2 = d2.data.toArrayList();
                 if (d2.data.getSchema().getNumericAttrUse(AttributeUseType.All).length > 0) {
                     for (int k = 0; k < dataList2.get(j).m_Doubles.length; k++) {
                         arow[k + ind] = dataList2.get(j).m_Doubles[k];
                     }
                 }

                 if (d2.data.getSchema().getNominalAttrUse(AttributeUseType.All).length > 0) {
                     for (int k = 0; k < dataList2.get(j).m_Ints.length; k++) {
                         carow[k + lastCat] = dataList2.get(j).m_Ints[k];
                     }
                 }
                 ind += d2.schema.getNumericAttrUse(AttributeUseType.All).length;
             }
             dataList.get(j).m_Doubles = arow;
             dataList.get(j).m_Ints = carow;
             //}
         }

         if (appset.useNC.get(0) == true) {
             ClusAttrType cat = schema.getAttrType(schema.getNbAttributes() - 1);
             ClusAttrType copy = cat.cloneType();
             schema.addAttrType(copy);
             for (int i = schema.getNbAttributes() - 2; i >= 2; i--) {
                 ClusAttrType copy1 = schema.getAttrType(i - 1).cloneType();
                 schema.setAttrType(copy1, i);
             }

             ClusAttrType copy1 = new NumericAttrType("GIS");//schema.getAttrType(2).cloneType();
             schema.setAttrType(copy1, 1);

             schema.getAttrType(1).setName("GIS");

             int numD = -1;
             double count = 0.0;
             for (int j = 0; j < data.getNbRows(); j++) {
                 double row[] = null;
                 if (dataList.get(j).m_Doubles != null) {
                     row = new double[dataList.get(j).m_Doubles.length + 1];
                     numD = dataList.get(j).m_Doubles.length;
                 } else
                     row = new double[1];
                 row[0] = count;//Double.parseDouble(dataList.get(j).m_Objects[0].toString().replace("\"", ""));
                 count++;

                 if (numD > 0) {
                     row[1] = dataList.get(j).m_Doubles[0];
                     for (int i = 1; i < dataList.get(j).m_Doubles.length; i++)
                         row[i + 1] = dataList.get(j).m_Doubles[i];
                 }

                 dataList.get(j).m_Doubles = row;
             }

             for (int i = 0; i < W2indexs.size(); i++)
                 W2indexs.set(i, W2indexs.get(i) + 1);

         }

         for (int k = 0; k < dats.size(); k++) {
             DataSetCreator d2 = dats.get(k);
             for (int i = 1; i < d2.schema.getNbAttributes(); i++) {
                 ClusAttrType attr = d2.schema.getAttrType(i);
                 ClusAttrType copy = attr.cloneType();
                 schema.addAttrType(copy);
           /*if(d2.schema.getAttrType(i).getTypeName().contains("Numeric"))
             schema.getAttrType(schema.getNbAttributes()-1).setArrayIndex(schCount++);//should generalize
           else if(d2.schema.getAttrType(i).getTypeName().contains("Nominal"))
               schema.getAttrType(schema.getNbAttributes()-1).setArrayIndex(schCatCount++);*/
             }
         }

         try {
             schema.addIndices(0);
         } catch (ClusException e) {
             e.printStackTrace();
         }

         data.setFromList(dataList);
         data.setSchema(schema);
         schema.setSettings(cset);
    }
    
    // konstruktor koji ne kreira inicijalnu arff datoteku
    public DataSetCreator(ArrayList<String> inputs, ApplicationSettings appset) {
    	importInitialData(inputs, appset);
    }
    
    public DataSetCreator(ArrayList<String> inputs, String outFolder, ApplicationSettings appset) {
        this.DataPath = inputs.get(0);
       
        String name = outFolder;

        if (appset.system.equals("windows"))
            name += "\\Jinput.arff";
        else
            name += "/Jinput.arff";

        importInitialData(inputs, appset);

        try {
            writeArff(name, data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public DataSetCreator(String input1, String input2, String outFolder) {

        DataSetCreator d2 = new DataSetCreator(input2);
        this.DataPath = input1;
        cset = new Settings();
        String name = outFolder;

        name += "\\Jinput.arff";

        try {
            this.readDataset();
            d2.readDataset();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //System.out.println("Num attributes dataset 1: "+this.schema.getNbAttributes());
        //System.out.println("Num attributes dataset 2: "+d2.schema.getNbAttributes());
        W2indexs.add(this.schema.getNbAttributes() + 1);

        cset = new Settings();

        //int numAttributes=schema.getNbAttributes()-1;

        ArrayList<DataTuple> dataList2 = d2.data.toArrayList();
        ArrayList<DataTuple> dataList = data.toArrayList();
        // System.out.println("object: "+t.getNominal(d2.data.m_Data[0]));
        int lastDouble = schema.getNumericAttrUse(AttributeUseType.All).length;
        System.out.println("lastDouble: " + lastDouble);
        int lastCat = schema.getNominalAttrUse(AttributeUseType.All).length;

        // int schCount=lastDouble, schCatCount=lastCat;

        for (int j = 0; j < data.getNbRows(); j++) {
            int l = data.getSchema().getNumericAttrUse(AttributeUseType.All).length + d2.data.getSchema().getNumericAttrUse(AttributeUseType.All).length;
            double arow[] = null;
            int carow[] = null;
            // System.out.println("Joint length: "+l);
            int l1 = data.getSchema().getNominalAttrUse(AttributeUseType.All).length + d2.data.getSchema().getNominalAttrUse(AttributeUseType.All).length;

            if (l1 > 0)
                carow = new int[l1];

            if (l > 0)
                arow = new double[l];
            if (data.getSchema().getNumericAttrUse(AttributeUseType.All).length > 0) {
                for (int k = 0; k < dataList.get(j).m_Doubles.length; k++) {
                    arow[k] = dataList.get(j).m_Doubles[k];
                }
            }

            if (data.getSchema().getNominalAttrUse(AttributeUseType.All).length > 0) {
                for (int k = 0; k < dataList.get(j).m_Ints.length; k++) {
                    carow[k] = dataList.get(j).m_Ints[k];
                }
            }


                   /*  System.out.println("last: "+last);
                     System.out.println("dataset2 size: "+dataList2.get(0).m_Doubles.length);
                     System.out.println("arow length: "+arow.length);
                    */
            if (d2.data.getSchema().getNumericAttrUse(AttributeUseType.All).length > 0) {
                for (int k = 0; k < dataList2.get(j).m_Doubles.length; k++) {
                    arow[k + lastDouble] = dataList2.get(j).m_Doubles[k];
                }
            }

            if (d2.data.getSchema().getNominalAttrUse(AttributeUseType.All).length > 0) {
                for (int k = 0; k < dataList2.get(j).m_Ints.length; k++) {
                    carow[k + lastCat] = dataList2.get(j).m_Ints[k];
                }
            }

            dataList.get(j).m_Doubles = arow;
            dataList.get(j).m_Ints = carow;
        }

        for (int i = 1; i < d2.schema.getNbAttributes(); i++) {
            ClusAttrType attr = d2.schema.getAttrType(i);
            ClusAttrType copy = attr.cloneType();
            schema.addAttrType(copy);
          /*if(d2.schema.getAttrType(i).getTypeName().contains("Numeric"))
            schema.getAttrType(schema.getNbAttributes()-1).setArrayIndex(schCount++);//should generalize
          else if(d2.schema.getAttrType(i).getTypeName().contains("Nominal"))
              schema.getAttrType(schema.getNbAttributes()-1).setArrayIndex(schCatCount++);*/
        }

        try {
            schema.addIndices(0);
        } catch (ClusException e) {
            e.printStackTrace();
        }

        data.setFromList(dataList);
        data.setSchema(schema);
        schema.setSettings(cset);

        try {
            writeArff(name, data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void initialClustering(String outFolder, ApplicationSettings appset) {
        ArrayList<DataTuple> dataList = data.toArrayList();
        ArrayList<DataTuple> newTuples = new ArrayList<>();

        int lastDouble = schema.getNumericAttrUse(AttributeUseType.All).length;

        System.out.println("initial lastDouble: " + lastDouble);
        System.out.println("old attr size: " + schema.getNbAttributes());
        schema.addAttrType(new NumericAttrType("target"));
        schema.getAttrType(schema.getNbAttributes() - 1).setArrayIndex(lastDouble++);
        System.out.println("new attr size: " + schema.getNbAttributes());

        int numShufflingStep = Math.min(500, Math.min((int) (0.7 * data.getNbRows()), schema.getNbAttributes()));

        Random rand = new Random();

        System.out.println("Doing initial clustering...");
        System.out.println("data list size: " + dataList.size());
        System.out.println("Num shuffling steps: " + numShufflingStep);

        int numIt = 0, step = dataList.size() / 100;

        for (int j = 0; j < dataList.size(); j++) {
            DataTuple tup = dataList.get(j).deepCloneTuple();
            String id = tup.m_Objects[0].toString().replace("\"", "");
            id = "\"" + id + "new-Init\"";
            tup.m_Objects[0] = id;

            for (int i = 0; i < numShufflingStep; i++) {//should fix
                int ind = rand.nextInt(dataList.size());
                DataTuple told = dataList.get(ind);
                for (int k = 0; k < ((int) (Math.max(1.0, 0.08 * schema.getNbAttributes()))); k++) {
                    int toChange = 0;
                    int toChange1 = 0;

                    for (int nview = 0; nview < W2indexs.size() - 1; nview++) {
                        if (nview == 0)
                            toChange = rand.nextInt(W2indexs.get(0) - 1/*schema.getNbAttributes()-1*/);
                        else {
                            toChange1 = rand.nextInt(W2indexs.get(nview) - 1);
                            toChange = rand.nextInt(W2indexs.get(nview + 1) - W2indexs.get(k) + 1);
                            toChange += toChange1;
                        }
                        //int toChange1=rand.nextInt(W2indexs.get(0)-1);
                        //int toChange2=rand.nextInt(W2indexs.get(1)-W2indexs.get(0)+1);
                        //toChange2+=toChange1;
                        //this.W2index-1
                        ClusAttrType t = schema.getAttrType(toChange);
                        // System.out.println("attr name: "+t.getTypeName());
                        if (t.getTypeName().contains("Numeric")) {
                            int elemInd = t.getArrayIndex();
                            tup.m_Doubles[elemInd] = told.m_Doubles[elemInd];
                        } else if (t.getTypeName().contains("Nominal")) {
                            int elemInd = t.getArrayIndex();
                            tup.m_Ints[elemInd] = told.m_Ints[elemInd];
                        }
                    }

                    toChange = rand.nextInt(schema.getNbAttributes() - 1 - W2indexs.get(W2indexs.size() - 1) + 1);
                    toChange1 = rand.nextInt(W2indexs.get(W2indexs.size() - 1) - 1);
                    toChange += toChange1;

                    ClusAttrType t1 = schema.getAttrType(toChange);
                    // System.out.println("attr name: "+t.getTypeName());
                    if (t1.getTypeName().contains("Numeric")) {
                        int elemInd = t1.getArrayIndex();
                        if (elemInd == 0 && appset.networkInit == true)
                            elemInd++;
                        if (elemInd < tup.m_Doubles.length)
                            tup.m_Doubles[elemInd] = told.m_Doubles[elemInd];
                        else {
                            k--;
                            continue;
                        }
                    } else if (t1.getTypeName().contains("Nominal")) {
                        int elemInd = t1.getArrayIndex();
                        tup.m_Ints[elemInd] = told.m_Ints[elemInd];
                    }

                }
                //System.out.println("done "+(i+1));
            }

            newTuples.add(tup);

            numIt++;
           /* if (numIt % step == 0)
                System.out.println((((double) numIt / dataList.size()) * 100) + "% completed...");*/
            if (numIt == dataList.size())
                System.out.println("100% completed!");

        }

          /*System.out.println("dataList ds: "+dataList.get(0).m_Doubles.length);
          System.out.println("newT ds: "+newTuples.get(0).m_Doubles.length);*/

        for (int i = 0; i < dataList.size(); i++) {
            double arow[];

            if (data.getSchema().getNumericAttrUse(AttributeUseType.All).length > 0)
                arow = new double[dataList.get(i).m_Doubles.length + 1];
            else
                arow = new double[1];

            if (data.getSchema().getNumericAttrUse(AttributeUseType.All).length > 0) {
                for (int j = 0; j < dataList.get(i).m_Doubles.length; j++)
                    arow[j] = dataList.get(i).m_Doubles[j];
            }

            if (data.getSchema().getNumericAttrUse(AttributeUseType.All).length > 0)
                arow[dataList.get(i).m_Doubles.length] = 1.0;
            else
                arow[0] = 1.0;
            dataList.get(i).m_Doubles = arow;
        }

        for (int i = 0; i < newTuples.size(); i++) {
            double arow[];

            if (data.getSchema().getNumericAttrUse(AttributeUseType.All).length > 0)
                arow = new double[newTuples.get(i).m_Doubles.length + 1];
            else
                arow = new double[1];

            if (data.getSchema().getNumericAttrUse(AttributeUseType.All).length > 0) {
                for (int j = 0; j < newTuples.get(i).m_Doubles.length; j++) {
                    if (j == 0 && appset.networkInit == true)
                        arow[j] = data.getNbRows() + i;
                    else
                        arow[j] = newTuples.get(i).m_Doubles[j];
                }
            }
            if (data.getSchema().getNumericAttrUse(AttributeUseType.All).length > 0)
                arow[newTuples.get(i).m_Doubles.length] = 0.0;
            else arow[0] = 0.0;
            newTuples.get(i).m_Doubles = arow;
        }

        for (int i = 0; i < newTuples.size(); i++)
            dataList.add(newTuples.get(i));

        data.setFromList(dataList);
        data.setSchema(schema);
        schema.setSettings(cset);

        String name = outFolder;

        if (appset.system.equals("windows"))
            name += "\\JinputInitial.arff";
        else
            name += "/JinputInitial.arff";

        try {
            writeArff(name, data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    protected void writeArffInitialClusteringGen1(ApplicationSettings appset, Random r) {
    	
    	initialClusteringGen1(appset, r);
    	
    	String name = appset.outFolderPath;

        if (appset.system.equals("windows"))
            name += "\\JinputInitial.arff";
        else
            name += "/JinputInitial.arff";

        try {
            writeArff(name, data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    protected void initialClusteringGen1(ApplicationSettings appset, Random r) {
        ArrayList<DataTuple> dataList = data.toArrayList();
        ArrayList<DataTuple> newTuples = new ArrayList<>();

        int lastDouble = schema.getNumericAttrUse(AttributeUseType.All).length;

        System.out.println("initial lastDouble: " + lastDouble);
        System.out.println("old attr size: " + schema.getNbAttributes());
        schema.addAttrType(new NumericAttrType("target"));
        schema.getAttrType(schema.getNbAttributes() - 1).setArrayIndex(lastDouble++);
        System.out.println("new attr size: " + schema.getNbAttributes());

        int numShufflingStep = Math.min(500, Math.min((int) (0.7 * data.getNbRows()), schema.getNbAttributes()));
        System.out.println("Doing initial clustering...");
        System.out.println("data list size: " + dataList.size());
        System.out.println("Num shuffling steps: " + numShufflingStep);

        int numIt = 0, step = dataList.size() / 100;
        if (step == 0)
            step = 1;
        //Random r=new Random();

        for (int j = 0; j < dataList.size(); j++) {
            DataTuple tup = dataList.get(j).deepCloneTuple();
            String id = tup.m_Objects[0].toString();
            tup.m_Objects[0] = id.substring(0, id.length() - 1) + "new-Init\"";

            //create random permuatation containing k elements
            int[] array = new int[dataList.size()];

            for (int i = 0; i < dataList.size(); i++)
                array[i] = i;

            for (int i = 0; i < dataList.size(); i++) {
                int ran = i + r.nextInt(dataList.size() - i);

                int temp = array[i];
                array[i] = array[ran];
                array[ran] = temp;
            }

            int numAttrs = schema.getNbAttributes() - 2;
            //System.out.println("numAttr: "+numAttrs);
            for (int i = 0; i < numAttrs; i++) {
                ClusAttrType t = schema.getAttrType(i + 1);
                //System.out.println("attr name: "+t.getTypeName());
                if (t.getTypeName().contains("Numeric")) {
                    int elemInd = t.getArrayIndex();
                            /*System.out.println("Elem index: "+elemInd);
                            System.out.println("array: "+array[i%dataList.size()]);
                            System.out.println("Tuple size: "+tup.m_Doubles.length);*/
                    tup.m_Doubles[elemInd] = dataList.get(array[i % dataList.size()]).m_Doubles[elemInd];
                } else if (t.getTypeName().contains("Nominal")) {
                    int elemInd = t.getArrayIndex();
                    tup.m_Ints[elemInd] = dataList.get(array[i % dataList.size()]).m_Ints[elemInd];
                }
            }

            newTuples.add(tup);

            numIt++;
           /* if (numIt % step == 0)
                System.out.println((((double) numIt / dataList.size()) * 100) + "% completed...");*/
            if (numIt == dataList.size())
                System.out.println("100% completed!");
        }

        for (int i = 0; i < dataList.size(); i++) {
            double arow[];

            if (data.getSchema().getNumericAttrUse(AttributeUseType.All).length > 0)
                arow = new double[dataList.get(i).m_Doubles.length + 1];
            else
                arow = new double[1];

            if (data.getSchema().getNumericAttrUse(AttributeUseType.All).length > 0) {
                for (int j = 0; j < dataList.get(i).m_Doubles.length; j++)
                    arow[j] = dataList.get(i).m_Doubles[j];
            }

            if (data.getSchema().getNumericAttrUse(AttributeUseType.All).length > 0)
                arow[dataList.get(i).m_Doubles.length] = 1.0;
            else
                arow[0] = 1.0;
            dataList.get(i).m_Doubles = arow;
        }

        for (int i = 0; i < newTuples.size(); i++) {
            double arow[];

            if (data.getSchema().getNumericAttrUse(AttributeUseType.All).length > 0)
                arow = new double[newTuples.get(i).m_Doubles.length + 1];
            else
                arow = new double[1];

            if (data.getSchema().getNumericAttrUse(AttributeUseType.All).length > 0) {
                for (int j = 0; j < newTuples.get(i).m_Doubles.length; j++) {
                    if (j == 0 && appset.networkInit == true)
                        arow[j] = data.getNbRows() + i;
                    else
                        arow[j] = newTuples.get(i).m_Doubles[j];
                }
            }
            if (data.getSchema().getNumericAttrUse(AttributeUseType.All).length > 0)
                arow[newTuples.get(i).m_Doubles.length] = 0.0;
            else arow[0] = 0.0;
            newTuples.get(i).m_Doubles = arow;
        }

        for (int i = 0; i < newTuples.size(); i++)
            dataList.add(newTuples.get(i));

        data.setFromList(dataList);
        data.setSchema(schema);
        schema.setSettings(cset); 
    }


    protected void initialClusteringKmeans(ApplicationSettings appset, Random r) {
        // metoda nista ne vraca vec mijenja data i schema
        ArrayList<DataTuple> dataList = data.toArrayList();
        int n = data.getNbRows();

        // pozicija prvog atributa iz W2, ali brojana od 1
        int firstW2 = W2indexs.get(0) - 1;

        // u cols spremamo pozicije u m_Doubles onih numerickih atributa
        // koji pripadaju trazenom pogledu (0 = W1, 1 = W2, -1 = oba)
        ClusAttrType[] numericAttrs = schema.getNumericAttrUse(AttributeUseType.All);
        ArrayList<Integer> cols = new ArrayList<>();

        for (ClusAttrType t : numericAttrs) {
            boolean inW1 = t.getIndex() < firstW2;
            if (appset.viewForClustering == -1
                    || (appset.viewForClustering == 0 && inW1)
                    || (appset.viewForClustering == 1 && !inW1)) {
                cols.add(t.getArrayIndex());
            }
        }

        System.out.println("k-means initialization: k = " + appset.numOfClusters
                + ", view = " + appset.viewForClustering
                + ", number of used attrs = " + cols.size());

        // X je skup podataka prepisan u obicnu matricu,
        // koju ocekuje kmeans algoritam
        int d = cols.size();
        double[][] X = new double[n][d];

        for (int i = 0; i < n; i++) {
            double[] row = dataList.get(i).m_Doubles;
            for (int f = 0; f < d; f++) {
                X[i][f] = row[cols.get(f)];
            }
        }


        // standardizacija matrice
        for (int f = 0; f < d; f++) {
            double mean = 0.0;
            for (int i = 0; i < n; i++) {
                mean += X[i][f];
            }
            mean /= n;

            double var = 0.0;
            for (int i = 0; i < n; i++) {
                double diff = X[i][f] - mean;
                var += diff * diff;
            }
            double sd = Math.sqrt(var / n);

            // konstantan stupac ne nosi informaciju, izbjegavamo dijeljenje s nulom
            if (sd > 0.0) {
                for (int i = 0; i < n; i++) {
                    X[i][f] = (X[i][f] - mean) / sd;
                }
            } else {
                for (int i = 0; i < n; i++) {
                    X[i][f] = 0.0;
                }
            }
        }

        // poziv kmeansa
        int k = appset.numOfClusters;

        Initializer kmeansInit = "Kmeans++".equalsIgnoreCase(appset.kmeansInitMethod)
                ? new Initializers.KmeansPlusPlus()
                : new Initializers.Forgy();

        System.out.println("k-means initializer: " + kmeansInit.name());

        Kmeans kmeans = new Kmeans(k, kmeansInit);
        Kmeans.Result result = kmeans.fit(X, new Random(42));
        int[] labels = result.labels;

        System.out.println("k-means rezultat: " + result);

        int[] clusterSizes = new int[k];
        for (int i = 0; i < n; i++)
            clusterSizes[labels[i]]++;
        System.out.println("Velicine klastera: " + Arrays.toString(clusterSizes));

        // k novih atributa u shemu
        int lastDouble = schema.getNumericAttrUse(AttributeUseType.All).length;

        for (int j = 0; j < k; j++) {
            schema.addAttrType(new NumericAttrType("target" + (j + 1)));
            schema.getAttrType(schema.getNbAttributes() - 1).setArrayIndex(lastDouble + j);
        }

        System.out.println("Novi broj atributa u shemi: " + schema.getNbAttributes()
                + ", prvi ciljni arrayIndex: " + lastDouble);

        // prosirenje redaka
        ArrayList<DataTuple> newList = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            DataTuple tup = dataList.get(i).deepCloneTuple();

            double[] arow = new double[lastDouble + k];
            System.arraycopy(tup.m_Doubles, 0, arow, 0, lastDouble);
            arow[lastDouble + labels[i]] = 1.0;

            tup.m_Doubles = arow;
            newList.add(tup);
        }

        data.setFromList(newList);
        data.setSchema(schema);
        schema.setSettings(cset);

        System.out.println("Duljina m_Doubles nakon prosirenja: "
                + data.toArrayList().get(0).m_Doubles.length);


    }


    void initialClusteringGen(String outFolder, ApplicationSettings appset, int numChangedAttrs) {
        ArrayList<DataTuple> dataList = data.toArrayList();
        ArrayList<DataTuple> newTuples = new ArrayList<>();

        int lastDouble = schema.getNumericAttrUse(AttributeUseType.All).length;

        System.out.println("initial lastDouble: " + lastDouble);
        System.out.println("old attr size: " + schema.getNbAttributes());
        schema.addAttrType(new NumericAttrType("target"));
        schema.getAttrType(schema.getNbAttributes() - 1).setArrayIndex(lastDouble++);
        System.out.println("new attr size: " + schema.getNbAttributes());

        int numShufflingStep = Math.min(500, Math.min((int) (0.7 * data.getNbRows()), schema.getNbAttributes()));
        System.out.println("Doing initial clustering...");
        System.out.println("data list size: " + dataList.size());
        System.out.println("Num shuffling steps: " + numShufflingStep);

        int numIt = 0, step = dataList.size() / 100;
        Random r = new Random();

        for (int j = 0; j < dataList.size(); j++) {
            DataTuple tup = dataList.get(j).deepCloneTuple();
            String id = tup.m_Objects[0].toString().replace("\"", "");
            id = "\"" + id + "new-Init\"";
            tup.m_Objects[0] = id;

            //create random permuatation containing k elements
            int[] array = new int[dataList.size()];

            for (int i = 0; i < dataList.size(); i++)
                array[i] = i;

            for (int i = 0; i < dataList.size(); i++) {
                int ran = i + r.nextInt(dataList.size() - i);

                int temp = array[i];
                array[i] = array[ran];
                array[ran] = temp;
            }

            int numAttrs = schema.getNbAttributes() - 2;
            //System.out.println("numAttr: "+numAttrs);
            for (int i = 0; i < numAttrs; i++) {
                ClusAttrType t = schema.getAttrType(i + 1);
                //System.out.println("attr name: "+t.getTypeName());
                if (t.getTypeName().contains("Numeric")) {
                    int elemInd = t.getArrayIndex();
                            /*System.out.println("Elem index: "+elemInd);
                            System.out.println("array: "+array[i%dataList.size()]);
                            System.out.println("Tuple size: "+tup.m_Doubles.length);*/
                    tup.m_Doubles[elemInd] = dataList.get(array[i % dataList.size()]).m_Doubles[elemInd];
                } else if (t.getTypeName().contains("Nominal")) {
                    int elemInd = t.getArrayIndex();
                    tup.m_Ints[elemInd] = dataList.get(array[i % dataList.size()]).m_Ints[elemInd];
                }
            }

            newTuples.add(tup);

            numIt++;
            /*if (numIt % step == 0)
                System.out.println((((double) numIt / dataList.size()) * 100) + "% completed...");*/
            if (numIt == dataList.size())
                System.out.println("100% completed!");
        }

        for (int i = 0; i < dataList.size(); i++) {
            double arow[];

            if (data.getSchema().getNumericAttrUse(AttributeUseType.All).length > 0)
                arow = new double[dataList.get(i).m_Doubles.length + 1];
            else
                arow = new double[1];

            if (data.getSchema().getNumericAttrUse(AttributeUseType.All).length > 0) {
                for (int j = 0; j < dataList.get(i).m_Doubles.length; j++)
                    arow[j] = dataList.get(i).m_Doubles[j];
            }

            if (data.getSchema().getNumericAttrUse(AttributeUseType.All).length > 0)
                arow[dataList.get(i).m_Doubles.length] = 1.0;
            else
                arow[0] = 1.0;
            dataList.get(i).m_Doubles = arow;
        }

        for (int i = 0; i < newTuples.size(); i++) {
            double arow[];

            if (data.getSchema().getNumericAttrUse(AttributeUseType.All).length > 0)
                arow = new double[newTuples.get(i).m_Doubles.length + 1];
            else
                arow = new double[1];

            if (data.getSchema().getNumericAttrUse(AttributeUseType.All).length > 0) {
                for (int j = 0; j < newTuples.get(i).m_Doubles.length; j++) {
                    if (j == 0 && appset.networkInit == true)
                        arow[j] = data.getNbRows() + i;
                    else
                        arow[j] = newTuples.get(i).m_Doubles[j];
                }
            }
            if (data.getSchema().getNumericAttrUse(AttributeUseType.All).length > 0)
                arow[newTuples.get(i).m_Doubles.length] = 0.0;
            else arow[0] = 0.0;
            newTuples.get(i).m_Doubles = arow;
        }

        for (int i = 0; i < newTuples.size(); i++)
            dataList.add(newTuples.get(i));

        data.setFromList(dataList);
        data.setSchema(schema);
        schema.setSettings(cset);

        String name = outFolder;

        if (appset.system.equals("windows"))
            name += "\\JinputInitial.arff";
        else
            name += "/JinputInitial.arff";

        try {
            writeArff(name, data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    void initialClusteringCategorical(String outFolder) {
        ArrayList<DataTuple> dataList = data.toArrayList();
        ArrayList<DataTuple> newTuples = new ArrayList<>();

        int lastDouble = schema.getNumericAttrUse(AttributeUseType.All).length;

        System.out.println("initial lastDouble: " + lastDouble);
        System.out.println("old attr size: " + schema.getNbAttributes());
        schema.addAttrType(new NominalAttrType("target"));
        schema.getAttrType(schema.getNbAttributes() - 1).setArrayIndex(lastDouble++);
        System.out.println("new attr size: " + schema.getNbAttributes());

        int numShufflingStep = Math.min(500, Math.min((int) (0.7 * data.getNbRows()), schema.getNbAttributes()));

        Random rand = new Random();

        System.out.println("Doing initial clustering...");
        System.out.println("data list size: " + dataList.size());
        System.out.println("Num shuffling steps: " + numShufflingStep);

        int numIt = 0, step = dataList.size() / 100;

        for (int j = 0; j < dataList.size(); j++) {
            DataTuple tup = dataList.get(j).deepCloneTuple();
            String id = tup.m_Objects[0].toString().replace("\"", "");
            id = "\"" + id + "new-Init\"";
            tup.m_Objects[0] = id;

            for (int i = 0; i < numShufflingStep; i++) {//should fix
                int ind = rand.nextInt(dataList.size());
                DataTuple told = dataList.get(ind);
                for (int k = 0; k < ((int) (Math.max(1.0, 0.08 * schema.getNbAttributes()))); k++) {
                    int toChange = rand.nextInt(schema.getNbAttributes() - 1);
                    ClusAttrType t = schema.getAttrType(toChange);
                    // System.out.println("attr name: "+t.getTypeName());
                    if (t.getTypeName().contains("Numeric")) {
                        int elemInd = t.getArrayIndex();
                        tup.m_Doubles[elemInd] = told.m_Doubles[elemInd];
                    } else if (t.getTypeName().contains("Nominal")) {
                        int elemInd = t.getArrayIndex();
                        tup.m_Ints[elemInd] = told.m_Ints[elemInd];
                    }

                }
                //System.out.println("done "+(i+1));
            }

            newTuples.add(tup);

            numIt++;
           /* if (numIt % step == 0)
                System.out.println((((double) numIt / dataList.size()) * 100) + "% completed...");*/
            if (numIt == dataList.size())
                System.out.println("100% completed!");

        }

          /*System.out.println("dataList ds: "+dataList.get(0).m_Doubles.length);
          System.out.println("newT ds: "+newTuples.get(0).m_Doubles.length);*/

        for (int i = 0; i < dataList.size(); i++) {
            int arow[];

            if (data.getSchema().getNominalAttrUse(AttributeUseType.All).length > 0)
                arow = new int[dataList.get(i).m_Ints.length + 1];
            else
                arow = new int[1];

            if (data.getSchema().getNominalAttrUse(AttributeUseType.All).length > 0) {
                for (int j = 0; j < dataList.get(i).m_Ints.length; j++)
                    arow[j] = dataList.get(i).m_Ints[j];
            }

            if (data.getSchema().getNominalAttrUse(AttributeUseType.All).length > 0)
                arow[dataList.get(i).m_Ints.length] = 0;
            else
                arow[0] = 1;
            dataList.get(i).m_Ints = arow;
        }

        for (int i = 0; i < newTuples.size(); i++) {
            int arow[];

            if (data.getSchema().getNominalAttrUse(AttributeUseType.All).length > 0)
                arow = new int[newTuples.get(i).m_Ints.length + 1];
            else
                arow = new int[1];

            if (data.getSchema().getNominalAttrUse(AttributeUseType.All).length > 0) {
                for (int j = 0; j < newTuples.get(i).m_Ints.length; j++)
                    arow[j] = newTuples.get(i).m_Ints[j];
            }
            if (data.getSchema().getNominalAttrUse(AttributeUseType.All).length > 0)
                arow[newTuples.get(i).m_Ints.length] = 1;
            else arow[0] = 0;
            newTuples.get(i).m_Ints = arow;
        }

        for (int i = 0; i < newTuples.size(); i++)
            dataList.add(newTuples.get(i));

        data.setFromList(dataList);
        data.setSchema(schema);
        schema.setSettings(cset);

        String name = outFolder;

        name += "\\JinputInitial.arff";

        try {
            writeArff(name, data);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public DataSetCreator(String path) {
        DataPath = path;
        cset = Settings.getDefaultSettings();
    }

    HashSet<Integer> analyzeAttributes(double misPerc) {
        HashSet<Integer> attrIndex = new HashSet<>();

        double missingPerc = 0.0;
        int missing = 0;
        ArrayList<DataTuple> dataList = data.toArrayList();


        for (int k = 0; k < dataList.get(0).m_Doubles.length; k++) {
            missing = 0;
            missingPerc = 0.0;
            for (int j = 0; j < data.getNbRows(); j++) {
                if (dataList.get(j).m_Doubles[k] == Double.POSITIVE_INFINITY)
                    missing++;
            }

            missingPerc = (double) missing / data.getNbRows();
            if (missingPerc >= misPerc)
                attrIndex.add(k + 1);
        }

        return attrIndex;
    }

    public void removeAttributes(HashSet<Integer> attributeIndexes, String outFolder) {

        ArrayList<DataTuple> dataList = data.toArrayList();
        RowData dat = data;
        ClusSchema sch = new ClusSchema(dat.getSchema().getRelationName());

        sch.addAttrType(schema.getAttrType(0));
        System.out.println("Number of doubles: " + dataList.get(0).m_Doubles.length);

        for (int i = 1; i < schema.getNbAttributes() - 1; i++)
            if (!attributeIndexes.contains(i))
                sch.addAttrType(schema.getAttrType(i + 1));

        for (int j = 0; j < data.getNbRows(); j++) {
            double arow[] = new double[dataList.get(j).m_Doubles.length - attributeIndexes.size()];
            int count = 0;
            for (int k = 0; k < dataList.get(j).m_Doubles.length; k++) {
                if (!attributeIndexes.contains(k + 1))
                    arow[count++] = dataList.get(j).m_Doubles[k];
            }
            dataList.get(j).m_Doubles = arow;
        }
        dat.setFromList(dataList);
        dat.setSchema(sch);
        sch.setSettings(cset);

        String out = outFolder + "\\JinputTmp.arff";

        try {
            writeArff(out, data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void readDataset() throws IOException {

        cread = new ClusReader(DataPath, cset);
        //ClusRandom.initialize(cset);
        ARFFFile a = new ARFFFile(cread);
        try {
            schema = a.read(cset);
            schema.initialize();
            ClusView view = schema.createNormalView();
            data = view.readData(cread, schema);
            schema = data.getSchema();
               /* NominalAttrType[] nom = schema.getNominalAttrUse(AttributeUseType.All);
                NumericAttrType[] num = schema.getNumericAttrUse(AttributeUseType.All);
                System.out.println("Num nominal: "+nom.length);
                System.out.println("Num numeric: "+num.length);
               /* if(nom.length>0){
                    System.out.println(nom[0].m_Values[0]+" "+nom[0].m_Values[1]);
                    System.out.println("Nominal array index: "+nom[0].getArrayIndex());
                    DataTuple tup=data.getTuple(0);
                    System.out.println("att 2: "+tup.getIntVal(0));
                    System.out.println("Integer in tuple size: "+tup.m_Ints.length);
                    for(int i=0;i<tup.m_Ints.length;i++)
                        System.out.print(tup.m_Ints[i]+" ");
                    System.out.println();
                    for(int i=0;i<tup.m_Ints.length;i++)
                        System.out.print(nom[0].m_Values[tup.m_Ints[i]]+" ");
                    System.out.println();

                    for(int i=0;i<schema.getNbAttributes();i++)
                        System.out.println(schema.getAttrType(i).getTypeName());
                }

                if(num.length>0){
                    for(int i=0;i<schema.getNbAttributes();i++)
                        System.out.println(schema.getAttrType(i).getTypeName());
                }*/
            // data=a.readArff(DataPath);
            // schema=data.getSchema();
            //System.out.println("data: "+data.getNbRows());
            //System.out.println("data: "+data.m_Schema.getRelationName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        numExamples = data.getNbRows();
        cread.close();

        System.out.println("Read dataset num doubles: " + schema.getNumericAttrUse(AttributeUseType.All).length);
    }


    public double getValue(int attId, int rowInd) {//should count number of numeric before
        double val = 0.0;
        int dIndex = -1;

        /*for(int i=1;i<=attId+1;i++){
            if(schema.getAttrType(i).getTypeName().contains("Numeric"))
                dIndex++;
        }*/

        dIndex = schema.getAttrType(attId + 1).getArrayIndex();

        /*System.out.println("attId: "+attId);
        System.out.println("rowInd: "+rowInd);
        System.out.println("dIndex: "+dIndex);
        System.out.println("attr name: "+schema.getAttrType(attId+1).getName());
        System.out.println("attr type: "+schema.getAttrType(attId+1).getTypeName());*/

        val = data.getTuple(rowInd).getDoubleVal(dIndex);

        return val;
    }

    public String getValueCategorical(int attId, int rowInd) {//should count number of categorical before
        String val = "";
        int nIndex = -1;

        /*for(int i=1;i<=attId+1;i++)
            if(schema.getAttrType(i).getTypeName().contains("Nominal"))
                nIndex++;*/

        nIndex = schema.getAttrType(attId + 1).getArrayIndex();//test if it works

        NominalAttrType[] nom = schema.getNominalAttrUse(AttributeUseType.All);

        DataTuple tup = data.getTuple(rowInd);
        //int vl=tup.m_Ints[nIndex];
        //NominalAttrType tN=nom[nIndex];
        //System.out.println("tup value: "+tup.m_Ints[nIndex]);
        if (tup.m_Ints[nIndex] >= nom[nIndex].m_Values.length)
            return "?";
        val = nom[nIndex].m_Values[tup.m_Ints[nIndex]];
        //val=data.getTuple(rowInd).getObjVal(attId);
        return val;
    }

    public void modifyDataset(int numRules, RuleReader RR, Mappings map, String Output) throws IOException {//not used but should be changed

        int naex = numExamples;
        int nARules = numRules;
        //locate the percentage of the rules to look at (that are specific enough)
        //change the target label in the setting2.s file and save it
        //create a new .arff file with the corresponding target attributes

        ArrayList<DataTuple> dataList = data.toArrayList();

        System.out.println("New rule index: " + RR.newRuleIndex);
        System.out.println("Rule size: " + RR.rules.size());

        for (int i = RR.newRuleIndex; i < RR.rules.size(); i++)
            if (RR.rules.get(i).elements.size() <= naex * 0.2 && RR.rules.get(i).elements.size() > 3)
                schema.addAttrType(new NumericAttrType("target" + (i + 1 - RR.newRuleIndex)));
        //schema.setClusteringAll(true);
        System.out.println("nARules: " + nARules);
        System.out.println("New number of attributes: " + schema.getNbAttributes());
        //Random rand=new Random();
        int count = 0;

        for (int j = 0; j < data.getNbRows(); j++) {
            count = 0;
            double arow[] = new double[schema.getNbAttributes() - 1];
            for (int k = 0; k < dataList.get(j).m_Doubles.length; k++) {
                arow[k] = dataList.get(j).m_Doubles[k];
            }
            for (int i = RR.newRuleIndex; i < RR.rules.size(); i++)
                if (RR.rules.get(i).elements.size() <= naex * 0.2 && RR.rules.get(i).elements.size() > 3) {
                    if (RR.rules.get(i).elements.contains(map.exampleId.get((String) dataList.get(j).m_Objects[0])))
                        arow[dataList.get(j).m_Doubles.length + count] = 1.0;
                    else
                        arow[dataList.get(j).m_Doubles.length + count] = 0.0;
                    count++;
                }
            dataList.get(j).m_Doubles = arow;
        }

        data.setFromList(dataList);
        data.setSchema(schema);
        schema.setSettings(cset);

        try {
            // writeArffHeader(wrt, schema);
            writeArff(Output, data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void modifyDatasetCat(int numRules, double startPercentage, double endPercentage, int startIndex, int endIndex, int cuttof, int cuttofMax, RuleReader RR, String Output, Mappings map) throws IOException {

        int naex = numExamples;
        int nARules = numRules;
        //locate the percentage of the rules to look at (that are specific enough)
        //change the target label in the setting2.s file and save it
        //create a new .arff file with the corresponding target attributes

        ArrayList<DataTuple> dataList = data.toArrayList();

        System.out.println("New rule index: " + RR.newRuleIndex);
        System.out.println("Rule size: " + RR.rules.size());

        int lastDouble = schema.getNominalAttrUse(AttributeUseType.All).length;
        int prevDouble = schema.getNominalAttrUse(AttributeUseType.All).length;
        int inc = 0;

        for (int i = startIndex; i < endIndex; i++)
            if (RR.rules.get(i).elements.size() <= naex * endPercentage && RR.rules.get(i).elements.size() >= naex * startPercentage && RR.rules.get(i).elements.size() >= cuttof && RR.rules.get(i).elements.size() <= cuttofMax) {
                schema.addAttrType(new NominalAttrType("target" + (i + 1 - startIndex)));
                schema.getAttrType(schema.getNbAttributes() - 1).setArrayIndex(lastDouble++);//temporary should be generalized
                inc++;
            }
        //schema.setClusteringAll(true);
        System.out.println("nARules: " + nARules);
        System.out.println("New number of attributes: " + schema.getNbAttributes());
        //Random rand=new Random();
        int count = 0;

        int l = schema.getNominalAttrUse(AttributeUseType.All).length;
        System.out.println("New size: " + (l + inc));
        System.out.println("l: " + l);
        System.out.println("inc: " + inc);
        System.out.println("l+inc: " + (l + inc));//wrong!
        System.out.println("Old size: " + prevDouble);

        for (int j = 0; j < data.getNbRows(); j++) {
            count = 0;

            int arow[] = new int[l + inc];
            if (prevDouble > 0) {
                for (int k = 0; k < dataList.get(j).m_Ints.length; k++) {
                    arow[k] = dataList.get(j).m_Ints[k];
                }
            }

            for (int i = startIndex; i < endIndex; i++)
                if (RR.rules.get(i).elements.size() <= naex * endPercentage && RR.rules.get(i).elements.size() >= naex * startPercentage && RR.rules.get(i).elements.size() >= cuttof && RR.rules.get(i).elements.size() <= cuttofMax) {
                    if (RR.rules.get(i).elements.contains(map.exampleId.get((String) dataList.get(j).m_Objects[0])))
                        arow[l + count] = 0;
                    else
                        arow[l + count] = 1;
                    count++;
                }
            dataList.get(j).m_Ints = arow;
        }

        data.setFromList(dataList);
        data.setSchema(schema);
        schema.setSettings(cset);

        try {
            // writeArffHeader(wrt, schema);
            writeArff(Output, data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void modifyDatasetCat(int numRules, double startPercentage, double endPercentage, int startIndex, int endIndex, int cuttof, int cuttofMax, ArrayList<Redescription> redescriptions, String Output, Mappings map) throws IOException {

        int naex = numExamples;
        int nARules = numRules;
        //locate the percentage of the rules to look at (that are specific enough)
        //change the target label in the setting2.s file and save it
        //create a new .arff file with the corresponding target attributes

        ArrayList<DataTuple> dataList = data.toArrayList();

        int lastDouble = schema.getNominalAttrUse(AttributeUseType.All).length;
        int prevDouble = schema.getNominalAttrUse(AttributeUseType.All).length;
        int inc = 0;

        for (int i = startIndex; i < endIndex; i++)
            if (redescriptions.get(i).elements.size() <= naex * endPercentage && redescriptions.get(i).elements.size() >= naex * startPercentage && redescriptions.get(i).elements.size() >= cuttof && redescriptions.get(i).elements.size() <= cuttofMax) {
                schema.addAttrType(new NominalAttrType("target" + (i + 1 - startIndex)));
                schema.getAttrType(schema.getNbAttributes() - 1).setArrayIndex(lastDouble++);//temporary should be generalized
                inc++;
            }
        //schema.setClusteringAll(true);
        System.out.println("nARules: " + nARules);
        System.out.println("New number of attributes: " + schema.getNbAttributes());
        //Random rand=new Random();
        int count = 0;

        int l = schema.getNominalAttrUse(AttributeUseType.All).length;
        System.out.println("New size: " + (l + inc));
        System.out.println("l: " + l);
        System.out.println("inc: " + inc);
        System.out.println("l+inc: " + (l + inc));//wrong!
        System.out.println("Old size: " + prevDouble);

        for (int j = 0; j < data.getNbRows(); j++) {
            count = 0;

            int arow[] = new int[l + inc];
            if (prevDouble > 0) {
                for (int k = 0; k < dataList.get(j).m_Ints.length; k++) {
                    arow[k] = dataList.get(j).m_Ints[k];
                }
            }

            for (int i = startIndex; i < endIndex; i++)
                if (redescriptions.get(i).elements.size() <= naex * endPercentage && redescriptions.get(i).elements.size() >= naex * startPercentage && redescriptions.get(i).elements.size() >= cuttof && redescriptions.get(i).elements.size() <= cuttofMax) {
                    if (redescriptions.get(i).elements.contains(map.exampleId.get((String) dataList.get(j).m_Objects[0])))
                        arow[l + count] = 0;
                    else
                        arow[l + count] = 1;
                    count++;
                }
            dataList.get(j).m_Ints = arow;
        }

        data.setFromList(dataList);
        data.setSchema(schema);
        schema.setSettings(cset);

        try {
            // writeArffHeader(wrt, schema);
            writeArff(Output, data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void modifyDatasetS(int startIndex, int endIndex, RuleReader RR, String output, Mappings map, ApplicationSettings appset) throws IOException {

        int naex = numExamples;
        //locate the percentage of the rules to look at (that are specific enough)
        //change the target label in the setting2.s file and save it
        //create a new .arff file with the corresponding target attributes

        ArrayList<DataTuple> dataList = data.toArrayList();

        System.out.println("New rule index: " + RR.newRuleIndex);
        System.out.println("Rule size: " + RR.rules.size());

        int lastDouble = schema.getNumericAttrUse(AttributeUseType.All).length;
        int prevDouble = schema.getNumericAttrUse(AttributeUseType.All).length;
        int inc = 0;

        for (int i = startIndex; i < endIndex; i++)
            if (RR.rules.get(i).elements.size() >= appset.minSupport) {
                schema.addAttrType(new NumericAttrType("target" + (i + 1 - startIndex)));
                schema.getAttrType(schema.getNbAttributes() - 1).setArrayIndex(lastDouble++);//temporary should be generalized
                inc++;
            }
        //schema.setClusteringAll(true);
        System.out.println("nARules: " + inc);
        System.out.println("New number of attributes: " + schema.getNbAttributes());
        //Random rand=new Random();
        int count = 0;

        int l = schema.getNumericAttrUse(AttributeUseType.All).length;
        System.out.println("New size: " + (l + inc));
        System.out.println("l: " + l);
        System.out.println("inc: " + inc);
        System.out.println("l+inc: " + (l + inc));//wrong!
        System.out.println("Old size: " + prevDouble);

        for (int j = 0; j < data.getNbRows(); j++) {
            count = 0;

            double arow[] = new double[l + inc];
            if (prevDouble > 0) {
                for (int k = 0; k < dataList.get(j).m_Doubles.length; k++) {
                    arow[k] = dataList.get(j).m_Doubles[k];
                }
            }

            for (int i = startIndex; i < endIndex; i++)
                if (RR.rules.get(i).elements.size() >= appset.minSupport) {
                    if (RR.rules.get(i).elements.contains(map.exampleId.get((String) dataList.get(j).m_Objects[0])))
                        arow[l + count] = 1.0;
                    else
                        arow[l + count] = 0.0;
                    count++;
                }
            dataList.get(j).m_Doubles = arow;
        }

        data.setFromList(dataList);
        data.setSchema(schema);
        schema.setSettings(cset);

        try {
            // writeArffHeader(wrt, schema);
            writeArff(output, data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void modifyDatasetS(int startIndex, int endIndex, RuleReader RR, Mappings map, ApplicationSettings appset) {

        int naex = numExamples;
        //locate the percentage of the rules to look at (that are specific enough)
        //change the target label in the setting2.s file and save it
        //create a new .arff file with the corresponding target attributes

        ArrayList<DataTuple> dataList = data.toArrayList();

        System.out.println("New rule index: " + RR.newRuleIndex);
        System.out.println("Rule size: " + RR.rules.size());

        int lastDouble = schema.getNumericAttrUse(AttributeUseType.All).length;
        int prevDouble = schema.getNumericAttrUse(AttributeUseType.All).length;
        int inc = 0;
        
        Map<Integer, Rule> targetSupportMap = TargetRulesMap.getTargetSupportMap();
        Map<Integer, Integer> targetSupportNegatedMap = TargetRulesMap.getTargetSupportNegatedMap();
        
        for (int i = startIndex; i < endIndex; i++)
            if (RR.rules.get(i).elements.size() >= appset.minSupport) {
            	String target = "target" + (i + 1 - startIndex);
                schema.addAttrType(new NumericAttrType(target));
                schema.getAttrType(schema.getNbAttributes() - 1).setArrayIndex(lastDouble++);//temporary should be generalized
                
                RR.rules.get(i).isTarget = true;
                targetSupportMap.put(i + 1 - startIndex, RR.rules.get(i));
                //OVO JE KRIVO - approx broj elemenata (datj) - R.rules.get(i).elements.size
                targetSupportNegatedMap.put(-(i + 1 - startIndex), SupplementingRandomForest.getDatJ().numExamples - RR.rules.get(i).elements.size());
//              targetSupportNegatedMap.put(-(i + 1 - startIndex), new Rule(RR.rules.get(i), SupplementingRandomForest.getDatJ(), SupplementingRandomForest.getFid()).elements.size());

                inc++;
            }
        //schema.setClusteringAll(true);
        System.out.println("nARules: " + inc);
        System.out.println("New number of attributes: " + schema.getNbAttributes());
        //Random rand=new Random();
        int count = 0;

        int l = schema.getNumericAttrUse(AttributeUseType.All).length;
        System.out.println("New size: " + (l + inc));
        System.out.println("l: " + l);
        System.out.println("inc: " + inc);
        System.out.println("l+inc: " + (l + inc));//wrong!
        System.out.println("Old size: " + prevDouble);

        
        for (int j = 0; j < data.getNbRows(); j++) {
            count = 0;

            double arow[] = new double[l + inc];
            if (prevDouble > 0) {
                for (int k = 0; k < dataList.get(j).m_Doubles.length; k++) {
                    arow[k] = dataList.get(j).m_Doubles[k];
                }
            }
            
            for (int i = startIndex; i < endIndex; i++) {
                if (RR.rules.get(i).elements.size() >= appset.minSupport) {
                    if (RR.rules.get(i).elements.contains(map.exampleId.get((String) dataList.get(j).m_Objects[0]))) {
                        arow[l + count] = 1.0;
                    }
                    else {
                        arow[l + count] = 0.0;
                    }
                    count++;
                }
                
               //targetSupportMap.put(-(i + 1 - startIndex), rule.calculateSupportSizeForNegation(SupplementingRandomForest.getDatJ(), map));
            }
            dataList.get(j).m_Doubles = arow;
        }

        data.setFromList(dataList);
        data.setSchema(schema);
        schema.setSettings(cset);

    }

    public void modifyDatasetCat(int startIndex, int endIndex, RuleReader RR, String Output, Mappings map, ApplicationSettings appset) throws IOException {

        int naex = numExamples;
        // int nARules=numRules;
        //locate the percentage of the rules to look at (that are specific enough)
        //change the target label in the setting2.s file and save it
        //create a new .arff file with the corresponding target attributes

        ArrayList<DataTuple> dataList = data.toArrayList();

        System.out.println("New rule index: " + RR.newRuleIndex);
        System.out.println("Rule size: " + RR.rules.size());

        int lastDouble = schema.getNominalAttrUse(AttributeUseType.All).length;
        int prevDouble = schema.getNominalAttrUse(AttributeUseType.All).length;
        int inc = 0;

        for (int i = startIndex; i < endIndex; i++)
            if (RR.rules.get(i).elements.size() >= appset.minSupport) {
                schema.addAttrType(new NominalAttrType("target" + (i + 1 - startIndex)));
                schema.getAttrType(schema.getNbAttributes() - 1).setArrayIndex(lastDouble++);//temporary should be generalized
                inc++;
            }
        //schema.setClusteringAll(true);
        System.out.println("nARules: " + inc);
        System.out.println("New number of attributes: " + schema.getNbAttributes());
        //Random rand=new Random();
        int count = 0;

        int l = schema.getNominalAttrUse(AttributeUseType.All).length;
        System.out.println("New size: " + (l + inc));
        System.out.println("l: " + l);
        System.out.println("inc: " + inc);
        System.out.println("l+inc: " + (l + inc));//wrong!
        System.out.println("Old size: " + prevDouble);

        for (int j = 0; j < data.getNbRows(); j++) {
            count = 0;

            int arow[] = new int[l + inc];
            if (prevDouble > 0) {
                for (int k = 0; k < dataList.get(j).m_Ints.length; k++) {
                    arow[k] = dataList.get(j).m_Ints[k];
                }
            }

            for (int i = startIndex; i < endIndex; i++)
                if (RR.rules.get(i).elements.size() >= appset.minSupport) {
                    if (RR.rules.get(i).elements.contains(map.exampleId.get((String) dataList.get(j).m_Objects[0])))
                        arow[l + count] = 0;
                    else
                        arow[l + count] = 1;
                    count++;
                }
            dataList.get(j).m_Ints = arow;
        }

        data.setFromList(dataList);
        data.setSchema(schema);
        schema.setSettings(cset);
        
        try {
            // writeArffHeader(wrt, schema);
            writeArff(Output, data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void modifyDatasetCat(int startIndex, int endIndex, RuleReader RR, Mappings map, ApplicationSettings appset) {

        int naex = numExamples;
        // int nARules=numRules;
        //locate the percentage of the rules to look at (that are specific enough)
        //change the target label in the setting2.s file and save it
        //create a new .arff file with the corresponding target attributes

        ArrayList<DataTuple> dataList = data.toArrayList();

        System.out.println("New rule index: " + RR.newRuleIndex);
        System.out.println("Rule size: " + RR.rules.size());

        int lastDouble = schema.getNominalAttrUse(AttributeUseType.All).length;
        int prevDouble = schema.getNominalAttrUse(AttributeUseType.All).length;
        int inc = 0;

        for (int i = startIndex; i < endIndex; i++)
            if (RR.rules.get(i).elements.size() >= appset.minSupport) {
                schema.addAttrType(new NominalAttrType("target" + (i + 1 - startIndex)));
                schema.getAttrType(schema.getNbAttributes() - 1).setArrayIndex(lastDouble++);//temporary should be generalized
                inc++;
            }
        //schema.setClusteringAll(true);
        System.out.println("nARules: " + inc);
        System.out.println("New number of attributes: " + schema.getNbAttributes());
        //Random rand=new Random();
        int count = 0;

        int l = schema.getNominalAttrUse(AttributeUseType.All).length;
        System.out.println("New size: " + (l + inc));
        System.out.println("l: " + l);
        System.out.println("inc: " + inc);
        System.out.println("l+inc: " + (l + inc));//wrong!
        System.out.println("Old size: " + prevDouble);

        for (int j = 0; j < data.getNbRows(); j++) {
            count = 0;

            int arow[] = new int[l + inc];
            if (prevDouble > 0) {
                for (int k = 0; k < dataList.get(j).m_Ints.length; k++) {
                    arow[k] = dataList.get(j).m_Ints[k];
                }
            }

            for (int i = startIndex; i < endIndex; i++)
                if (RR.rules.get(i).elements.size() >= appset.minSupport) {
                    if (RR.rules.get(i).elements.contains(map.exampleId.get((String) dataList.get(j).m_Objects[0])))
                        arow[l + count] = 0;
                    else
                        arow[l + count] = 1;
                    count++;
                }
            dataList.get(j).m_Ints = arow;
        }

        data.setFromList(dataList);
        data.setSchema(schema);
        schema.setSettings(cset);
    }

    public void modifyDatasetS(int startIndex, int endIndex, ArrayList<Redescription> redescriptions, String Output, Mappings map, ApplicationSettings appset) throws IOException {

        int naex = numExamples;
        //locate the percentage of the rules to look at (that are specific enough)
        //change the target label in the setting2.s file and save it
        //create a new .arff file with the corresponding target attributes

        ArrayList<DataTuple> dataList = data.toArrayList();

        int lastDouble = schema.getNumericAttrUse(AttributeUseType.All).length;
        int prevDouble = schema.getNumericAttrUse(AttributeUseType.All).length;
        int inc = 0;

        for (int i = startIndex; i < endIndex; i++)
            if (redescriptions.get(i).elements.size() >= appset.minSupport) {
                schema.addAttrType(new NumericAttrType("target" + (i + 1 - startIndex)));
                schema.getAttrType(schema.getNbAttributes() - 1).setArrayIndex(lastDouble++);//temporary should be generalized
                inc++;
            }
        //schema.setClusteringAll(true);
        //Random rand=new Random();
        int count = 0;

        int l = schema.getNumericAttrUse(AttributeUseType.All).length;
        System.out.println("New size: " + (l + inc));
        System.out.println("l: " + l);
        System.out.println("inc: " + inc);
        System.out.println("l+inc: " + (l + inc));//wrong!
        System.out.println("Old size: " + prevDouble);

        for (int j = 0; j < data.getNbRows(); j++) {
            count = 0;

            double arow[] = new double[l + inc];
            if (prevDouble > 0) {
                for (int k = 0; k < dataList.get(j).m_Doubles.length; k++) {
                    arow[k] = dataList.get(j).m_Doubles[k];
                }
            }

            for (int i = startIndex; i < endIndex; i++)
                if (redescriptions.get(i).elements.size() >= appset.minSupport) {
                    if (redescriptions.get(i).elements.contains(map.exampleId.get((String) dataList.get(j).m_Objects[0])))
                        arow[l + count] = 1.0;
                    else
                        arow[l + count] = 0.0;
                    count++;
                }
            dataList.get(j).m_Doubles = arow;
        }

        data.setFromList(dataList);
        data.setSchema(schema);
        schema.setSettings(cset);

        try {
            // writeArffHeader(wrt, schema);
            writeArff(Output, data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void modifyDatasetCat(int startIndex, int endIndex, ArrayList<Redescription> redescriptions, String Output, Mappings map, ApplicationSettings appset) throws IOException {

        int naex = numExamples;
        //locate the percentage of the rules to look at (that are specific enough)
        //change the target label in the setting2.s file and save it
        //create a new .arff file with the corresponding target attributes

        ArrayList<DataTuple> dataList = data.toArrayList();

        int lastDouble = schema.getNominalAttrUse(AttributeUseType.All).length;
        int prevDouble = schema.getNominalAttrUse(AttributeUseType.All).length;
        int inc = 0;

        for (int i = startIndex; i < endIndex; i++)
            if (redescriptions.get(i).elements.size() >= appset.minSupport) {
                schema.addAttrType(new NominalAttrType("target" + (i + 1 - startIndex)));
                schema.getAttrType(schema.getNbAttributes() - 1).setArrayIndex(lastDouble++);//temporary should be generalized
                inc++;
            }
        //schema.setClusteringAll(true);
        System.out.println("New number of attributes: " + schema.getNbAttributes());
        //Random rand=new Random();
        int count = 0;

        int l = schema.getNominalAttrUse(AttributeUseType.All).length;
        System.out.println("New size: " + (l + inc));
        System.out.println("l: " + l);
        System.out.println("inc: " + inc);
        System.out.println("l+inc: " + (l + inc));//wrong!
        System.out.println("Old size: " + prevDouble);

        for (int j = 0; j < data.getNbRows(); j++) {
            count = 0;

            int arow[] = new int[l + inc];
            if (prevDouble > 0) {
                for (int k = 0; k < dataList.get(j).m_Ints.length; k++) {
                    arow[k] = dataList.get(j).m_Ints[k];
                }
            }

            for (int i = startIndex; i < endIndex; i++)
                if (redescriptions.get(i).elements.size() >= appset.minSupport) {
                    if (redescriptions.get(i).elements.contains(map.exampleId.get((String) dataList.get(j).m_Objects[0])))
                        arow[l + count] = 0;
                    else
                        arow[l + count] = 1;
                    count++;
                }
            dataList.get(j).m_Ints = arow;
        }

        data.setFromList(dataList);
        data.setSchema(schema);
        schema.setSettings(cset);

        try {
            // writeArffHeader(wrt, schema);
            writeArff(Output, data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void modifyDatasetS(int numRules, double startPercentage, double endPercentage, int startIndex, int endIndex, int cuttof, int cuttofMax, RuleReader RR, String Output, Mappings map) throws IOException {

        int naex = numExamples;
        int nARules = numRules;
        //locate the percentage of the rules to look at (that are specific enough)
        //change the target label in the setting2.s file and save it
        //create a new .arff file with the corresponding target attributes

        ArrayList<DataTuple> dataList = data.toArrayList();

        System.out.println("New rule index: " + RR.newRuleIndex);
        System.out.println("Rule size: " + RR.rules.size());

        int lastDouble = schema.getNumericAttrUse(AttributeUseType.All).length;
        int prevDouble = schema.getNumericAttrUse(AttributeUseType.All).length;
        int inc = 0;

        for (int i = startIndex; i < endIndex; i++)
            if (RR.rules.get(i).elements.size() <= naex * endPercentage && RR.rules.get(i).elements.size() >= naex * startPercentage /*&& RR.rules.get(i).elements.size()>=cuttof && RR.rules.get(i).elements.size()<=cuttofMax*/) {
                schema.addAttrType(new NumericAttrType("target" + (i + 1 - startIndex)));
                schema.getAttrType(schema.getNbAttributes() - 1).setArrayIndex(lastDouble++);//temporary should be generalized
                inc++;
            }
        //schema.setClusteringAll(true);
        System.out.println("nARules: " + nARules);
        System.out.println("New number of attributes: " + schema.getNbAttributes());
        //Random rand=new Random();
        int count = 0;

        int l = schema.getNumericAttrUse(AttributeUseType.All).length;
        System.out.println("New size: " + (l + inc));
        System.out.println("l: " + l);
        System.out.println("inc: " + inc);
        System.out.println("l+inc: " + (l + inc));//wrong!
        System.out.println("Old size: " + prevDouble);

        for (int j = 0; j < data.getNbRows(); j++) {
            count = 0;

            double arow[] = new double[l + inc];
            if (prevDouble > 0) {
                for (int k = 0; k < dataList.get(j).m_Doubles.length; k++) {
                    arow[k] = dataList.get(j).m_Doubles[k];
                }
            }

            for (int i = startIndex; i < endIndex; i++)
                if (RR.rules.get(i).elements.size() <= naex * endPercentage && RR.rules.get(i).elements.size() >= naex * startPercentage /*&& RR.rules.get(i).elements.size()>=cuttof && RR.rules.get(i).elements.size()<=cuttofMax*/) {
                    if (RR.rules.get(i).elements.contains(map.exampleId.get((String) dataList.get(j).m_Objects[0])))
                        arow[l + count] = 1.0;
                    else
                        arow[l + count] = 0.0;
                    count++;
                }
            dataList.get(j).m_Doubles = arow;
        }

        data.setFromList(dataList);
        data.setSchema(schema);
        schema.setSettings(cset);

        try {
            // writeArffHeader(wrt, schema);
            writeArff(Output, data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void modifyDatasetS(int numRules, double startPercentage, double endPercentage, int startIndex, int endIndex, int cuttof, int cuttofMax, ArrayList<Redescription> redescriptions, String Output, Mappings map) throws IOException {

        int naex = numExamples;
        int nARules = numRules;
        //locate the percentage of the rules to look at (that are specific enough)
        //change the target label in the setting2.s file and save it
        //create a new .arff file with the corresponding target attributes

        ArrayList<DataTuple> dataList = data.toArrayList();

        int lastDouble = schema.getNumericAttrUse(AttributeUseType.All).length;
        int prevDouble = schema.getNumericAttrUse(AttributeUseType.All).length;
        int inc = 0;

        for (int i = startIndex; i < endIndex; i++)
            if (redescriptions.get(i).elements.size() <= naex * endPercentage && redescriptions.get(i).elements.size() >= naex * startPercentage && redescriptions.get(i).elements.size() >= cuttof && redescriptions.get(i).elements.size() <= cuttofMax) {
                schema.addAttrType(new NumericAttrType("target" + (i + 1 - startIndex)));
                schema.getAttrType(schema.getNbAttributes() - 1).setArrayIndex(lastDouble++);//temporary should be generalized
                inc++;
            }
        //schema.setClusteringAll(true);
        System.out.println("nARules: " + nARules);
        System.out.println("New number of attributes: " + schema.getNbAttributes());
        //Random rand=new Random();
        int count = 0;

        int l = schema.getNumericAttrUse(AttributeUseType.All).length;
        System.out.println("New size: " + (l + inc));
        System.out.println("l: " + l);
        System.out.println("inc: " + inc);
        System.out.println("l+inc: " + (l + inc));//wrong!
        System.out.println("Old size: " + prevDouble);

        for (int j = 0; j < data.getNbRows(); j++) {
            count = 0;

            double arow[] = new double[l + inc];
            if (prevDouble > 0) {
                for (int k = 0; k < dataList.get(j).m_Doubles.length; k++) {
                    arow[k] = dataList.get(j).m_Doubles[k];
                }
            }

            for (int i = startIndex; i < endIndex; i++)
                if (redescriptions.get(i).elements.size() <= naex * endPercentage && redescriptions.get(i).elements.size() >= naex * startPercentage && redescriptions.get(i).elements.size() >= cuttof && redescriptions.get(i).elements.size() <= cuttofMax) {
                    if (redescriptions.get(i).elements.contains(map.exampleId.get((String) dataList.get(j).m_Objects[0])))
                        arow[l + count] = 1.0;
                    else
                        arow[l + count] = 0.0;
                    count++;
                }
            dataList.get(j).m_Doubles = arow;
        }

        data.setFromList(dataList);
        data.setSchema(schema);
        schema.setSettings(cset);

        try {
            // writeArffHeader(wrt, schema);
            writeArff(Output, data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void splitDataset(ArrayList<Integer> splitIndex, String outFolder) {
        int oldIndex = 0, numCreated = 1;

        System.out.println("splitIndex size: " + splitIndex.size());

        for (int index : splitIndex) {
            //Settings set=new Settings();
            //settings.add(set);
            try {
                readDataset();
            } catch (IOException e) {
                e.printStackTrace();
            }

            ArrayList<DataTuple> dataList = data.toArrayList();
            RowData dat = new RowData(data);
            ClusSchema sch = new ClusSchema(dat.getSchema().getRelationName());
            System.out.println("nb attr: " + schema.getNbAttributes());
            // System.out.println("attribute: "+schema.getAttrType(0));
            sch.addAttrType(schema.getAttrType(0));
            System.out.println("Number of doubles: " + dataList.get(0).m_Doubles.length);

            for (int i = oldIndex; i < index; i++)
                sch.addAttrType(schema.getAttrType(i + 1));


            for (int j = 0; j < data.getNbRows(); j++) {
                double arow[] = new double[index - oldIndex];
                for (int k = oldIndex; k < index; k++) {
                    arow[k - oldIndex] = dataList.get(j).m_Doubles[k];
                }
                dataList.get(j).m_Doubles = arow;
            }


            dat.setFromList(dataList);
            dat.setSchema(sch);
            sch.setSettings(cset);

            try {
                writeArff(outFolder + "\\input" + (numCreated) + ".arff", dat);
            } catch (Exception e) {
                e.printStackTrace();
            }

            oldIndex = index;
            numCreated++;

            if ((numCreated - 1) == splitIndex.size()) {

                try {
                    readDataset();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                System.out.println("Number of doubles: " + dataList.get(0).m_Doubles.length);
                index = schema.getNbAttributes() - 1;
                dataList = data.toArrayList();
                dat = new RowData(data);
                sch = new ClusSchema(dat.getSchema().getRelationName());
                sch.clearAttributeStatusClusteringAndTarget();
                sch.addAttrType(schema.getAttrType(0));

                for (int i = oldIndex; i < index; i++)
                    sch.addAttrType(schema.getAttrType(i + 1));


                for (int j = 0; j < data.getNbRows(); j++) {
                    double arow[] = new double[index - oldIndex];
                    for (int k = oldIndex; k < index; k++) {
                        arow[k - oldIndex] = dataList.get(j).m_Doubles[k];
                    }
                    dataList.get(j).m_Doubles = arow;
                }
                dat.setFromList(dataList);
                dat.setSchema(sch);
                sch.setSettings(cset);
                oldIndex = index;

                try {
                    writeArff(outFolder + "\\input" + (numCreated) + ".arff", dat);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }

    }

    public static void writeArff(String fname, RowData data) throws IOException, ClusException {
        PrintWriter wrt = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fname)));
        ClusSchema schema = data.getSchema();
        ARFFFile.writeArffHeader(wrt, schema);
        ARFFFile.writeArff(fname, data);
		/*wrt.println("@DATA");
		for (int j = 0; j < data.getNbRows(); j++) {
			DataTuple tuple = data.getTuple(j);
                        wrt.write(tuple.m_Objects[0]+",");
                        for(int k=0;k<tuple.m_Doubles.length;k++)
                            if(k+1<tuple.m_Doubles.length)
                                if(Double.isInfinite(tuple.m_Doubles[k]))
                                    wrt.write("?"+",");
                                else
                                     wrt.write(""+tuple.m_Doubles[k]+",");
                        else
                                if(Double.isInfinite(tuple.m_Doubles[k]))
                                     wrt.write("?"+"\n");
                                else
                                    wrt.write(""+tuple.m_Doubles[k]+"\n");
			//tuple.writeTuple(wrt);
		}*/
        wrt.close();
    }
}
