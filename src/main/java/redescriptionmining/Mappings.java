/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package redescriptionmining;

import gnu.trove.set.hash.TIntHashSet;
import si.ijs.kt.clus.data.ClusSchema;
import si.ijs.kt.clus.data.rows.DataTuple;
import si.ijs.kt.clus.data.type.ClusAttrType;
import si.ijs.kt.clus.data.type.ClusAttrType.AttributeType;
import si.ijs.kt.clus.data.type.ClusAttrType.AttributeUseType;
import si.ijs.kt.clus.data.type.primitive.NominalAttrType;
import si.ijs.kt.clus.data.type.primitive.NumericAttrType;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.javatuples.Pair;

import com.thoughtworks.xstream.io.binary.Token.Attribute;

import static redescriptionmining.SettingsReader.ENCODING;

/**
 * @author matej
 */
public class Mappings {
    public HashMap<String, Integer> attId = new HashMap<>();
    HashMap<Integer, Pair<HashMap<String, Integer>, HashMap<Integer, String>>> cattAtt = new HashMap<>();
    public HashMap<Integer, String> idExample = new HashMap<>();
    public HashMap<String, Integer> exampleId = new HashMap<>();
    public HashMap<Integer, String> idAtt = new HashMap<>();
    public TIntHashSet catAttInd = new TIntHashSet();
    private static Pattern pattern = Pattern.compile("\\{|\\}");

    public void clearMaps() {

        attId.clear();
        cattAtt.clear();
        idExample.clear();
        exampleId.clear();
        idAtt.clear();
        catAttInd.clear();

    }

    public void createIndex(DataSetCreator dataSetCreator) {
    	int attInd = 0, exInd = 0;
    	
    	for(ClusAttrType attribute : dataSetCreator.schema.getAllAttrUse(AttributeUseType.All)) {
    		
    		if(attribute.getAttributeType().equals(AttributeType.Numeric)) {
    			
    			attId.put(attribute.getName(), attInd++);
        		idAtt.put(attInd - 1, attribute.getName());
        		
    		} else if(attribute.getAttributeType().equals(AttributeType.Nominal)) {
    			
    			attId.put(attribute.getName(), attInd++);
        		idAtt.put(attInd - 1, attribute.getName());
        		
    			HashMap<String, Integer> lm = new HashMap<>();
                HashMap<Integer, String> rm = new HashMap<>();
                
                String[] catVals = ((NominalAttrType) attribute).getValues();
                
                for (int i = 0; i < catVals.length; i++) {
                    lm.put(catVals[i], i);
                    rm.put(i, catVals[i]);
                }
                
                Pair<HashMap<String, Integer>, HashMap<Integer, String>> tmpPr = new Pair<>(lm, rm);
                cattAtt.put(attId.get(attribute.getName()), tmpPr);
                catAttInd.add(attId.get(attribute.getName()));
    		}
    	}
    	
    	for(DataTuple dataTuple : dataSetCreator.data.getData()) {
    		exampleId.put((String) dataTuple.getObjects()[0], exInd++);
            idExample.put(exInd - 1, (String) dataTuple.getObjects()[0]);
    	}
    }
    
    
    void createIndex(String pathStr) {
        BufferedReader reader;
        int attInd = 0, dataSection = 0, exInd = 0;
        try {
            File input = new File(pathStr);
            Path path = Paths.get(input.getAbsolutePath());
            System.out.println("Path: " + input.getAbsolutePath());
            reader = Files.newBufferedReader(path, ENCODING);

            String line = null;
            while ((line = reader.readLine()) != null) {
                if (dataSection != 1 && line.contains("@ATTRIBUTE")) {
                    String[] tmp = line.split("\\s+");
                    if (tmp.length == 3 && tmp[2].contentEquals("numeric")) {
                        attId.put(tmp[1], attInd++);
                        idAtt.put(attInd - 1, tmp[1]);
                    } else if (tmp.length == 3 && tmp[2].contains("{")) {
                        attId.put(tmp[1], attInd++);
                        idAtt.put(attInd - 1, tmp[1]);
                        HashMap<String, Integer> lm = new HashMap<>();
                        HashMap<Integer, String> rm = new HashMap<>();

                        String catVal = pattern.matcher(tmp[2]).replaceAll("");

                        String[] catVals = catVal.split(",");

                        for (int i = 0; i < catVals.length; i++) {
                            lm.put(catVals[i], i);
                            rm.put(i, catVals[i]);
                        }
                        Pair<HashMap<String, Integer>, HashMap<Integer, String>> tmpPr = new Pair(lm, rm);
                        cattAtt.put(attId.get(tmp[1]), tmpPr);
                        catAttInd.add(this.attId.get(tmp[1]));
                    }
                }

                if (dataSection == 1) {
                    String dataLabel = line.substring(0, line.indexOf(','));
                    exampleId.put(dataLabel, exInd++);
                    idExample.put(exInd - 1, dataLabel);
                } else {
                    if (line.contains("@DATA")) {
                        dataSection = 1;
                    }
                }
            }
            reader.close();
        } catch (IOException ioe) {
            System.err.println("IOException: " + ioe.getMessage());
        }
    }

    void printMapping() {
    /* public HashMap<String,Integer> attId=new HashMap<>();
    HashMap<String,Pair<HashMap<String,Integer>,HashMap<Integer,String>>> cattAtt=new HashMap<>();
    public HashMap<Integer,String> idExample=new HashMap<>();
    public HashMap<String,Integer> exampleId=new HashMap<>();
    public HashMap<Integer,String> idAtt=new HashMap<>();*/

        Iterator<String> it = attId.keySet().iterator();

        System.out.println("attribute id mapping...");
        while (it.hasNext()) {
            String key = it.next();
            System.out.println("attr: " + key + " index: " + attId.get(key));
        }
        System.out.println();

        Iterator<Integer> it11 = cattAtt.keySet().iterator();
        //it=cattAtt.keySet().iterator();

        while (it11.hasNext()) {
            int key = it11.next();
            System.out.println("attr: " + idAtt.get(key));
            Iterator<String> it1 = cattAtt.get(key).getValue0().keySet().iterator();

            while (it1.hasNext()) {
                String k1 = it1.next();
                System.out.println("catt value: " + k1 + " index: " + cattAtt.get(key).getValue0().get(k1));
            }
        }

        it = exampleId.keySet().iterator();

        while (it.hasNext()) {
            String key = it.next();
            System.out.println("example: " + key + " value" + exampleId.get(key));
        }

    }

	@Override
	public String toString() {
		return "Mappings [attId=" + attId + ", cattAtt=" + cattAtt + ", idExample=" + idExample + ", exampleId="
				+ exampleId + ", idAtt=" + idAtt + ", catAttInd=" + catAttInd + ", getClass()=" + getClass()
				+ ", hashCode()=" + hashCode() + ", toString()=" + super.toString() + "]";
	}

    
    
}
