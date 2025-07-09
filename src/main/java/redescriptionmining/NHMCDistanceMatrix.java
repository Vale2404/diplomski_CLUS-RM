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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import org.javatuples.Triplet;
import static redescriptionmining.SettingsReader.ENCODING;
/**
 *
 * @author matej
 */
public class NHMCDistanceMatrix {
    int distanceMatrix[][];
    int numElem=0;
    HashMap<String,Double> m_distancesS = new HashMap<>();// = new TObjectDoubleHashMap();
    HashMap<String,Double> m_distancesN = new HashMap<>();
    HashMap<Integer,HashSet<Integer>> connectivity = new HashMap<>();
    HashMap<Integer,HashMap<Integer,ArrayList<Triplet<Integer,Integer,Double>>>> connectivityMultiplex=new HashMap<Integer,HashMap<Integer,ArrayList<Triplet<Integer,Integer,Double>>>>();
     int networkType=0, numLayers=1, inputType=0;
    NHMCDistanceMatrix(int numElements, ApplicationSettings appset){
        distanceMatrix=new int[numElements][numElements];
        numElem=numElements;
        
        for(int i=0;i<numElements;i++)
            for(int j=0;j<numElements;j++)
                distanceMatrix[i][j]=appset.maxDistance;
        
        for(int i=0;i<numElements;i++)
            distanceMatrix[i][i]=0;
        
    }
    
    void computeDistanceMatrix(RuleReader rr, Mappings map, int MaxDistance, int numElements){
       for(int i=0;i<numElements-1;i++){
           for(int j=i+1;j<numElements;j++){
                   double sum=0.0;
                   int coocurence=0, finalSum=0;
                   for(int k=rr.newRuleIndex;k<rr.rules.size();k++){
                       if(rr.rules.get(k).elements.contains(/*map.idExample.get(i)*/i) && rr.rules.get(k).elements.contains(/*map.idExample.get(j)*/j)){
                           coocurence++; sum+=(1-((double)(rr.rules.get(k).elements.size()-2))/numElements);
                       }
                   }
                   if(coocurence!=0)
                   finalSum=(int)(MaxDistance-(sum/coocurence)*MaxDistance);
                   else
                       finalSum=100;
                   distanceMatrix[map.exampleId.get(map.idExample.get(i))][map.exampleId.get(map.idExample.get(j))]=finalSum;
                   distanceMatrix[map.exampleId.get(map.idExample.get(j))][map.exampleId.get(map.idExample.get(i))]=finalSum;
           }
       }
    }
    
    void computeDistanceMatrix(ArrayList<Redescription> redescriptions, Mappings map, int MaxDistance, int numElements, int[] oldRindex){
       for(int i=0;i<numElements-1;i++){
           for(int j=i+1;j<numElements;j++){
                   double sum=0.0;
                   int coocurence=0, finalSum=0;
                   for(int k=oldRindex[0];k<redescriptions.size();k++){
                       if(redescriptions.get(k).elements.contains(/*map.idExample.get(i)*/i) && redescriptions.get(k).elements.contains(/*map.idExample.get(j)*/j)){
                           coocurence++; sum+=(1-((double)(redescriptions.get(k).elements.size()-2))/numElements);
                       }
                   }
                   finalSum=(int)(MaxDistance-(sum/coocurence)*MaxDistance);
                   distanceMatrix[map.exampleId.get(map.idExample.get(i))][map.exampleId.get(map.idExample.get(j))]=finalSum;
                   distanceMatrix[map.exampleId.get(map.idExample.get(j))][map.exampleId.get(map.idExample.get(i))]=finalSum;
           }
       }
    }
    
    void loadDistance(File input, Mappings map){
        
        try {
      Path path =Paths.get(input.getAbsolutePath());
      System.out.println("Path: "+input.getAbsolutePath());
      BufferedReader reader;
      String file="";
      reader = Files.newBufferedReader(path,ENCODING);
      String line = null;
      distanceMatrix=new int[map.exampleId.keySet().size()][map.exampleId.keySet().size()];
      int rowInd=0;
      while ((line = reader.readLine()) != null) {
               String tmp[]=line.split(" ");
               for(int j=0;j<tmp.length;j++)
                   distanceMatrix[rowInd][j]=Integer.parseInt(tmp[j].trim());
               rowInd++;
        
    }
      reader.close();
         }
         catch(IOException io){
             io.printStackTrace();
         }
    }
    
    void reset(ApplicationSettings appset){
        
        for(int i=0;i<numElem;i++)
            for(int j=0;j<numElem;j++)
                distanceMatrix[i][j]=appset.maxDistance;
        
        for(int i=0;i<numElem;i++)
            distanceMatrix[i][i]=0;
    }
    
    void writeToFile(File output, Mappings map, ApplicationSettings appset){
  
         /*try{
         PrintWriter out = new PrintWriter(output.getAbsolutePath());
         for(int i=0;i<numElem;i++)
             for(int j=0;j<numElem;j++){
                /* String ex=map.idExample.get(i);
                 String ex1=map.idExample.get(j);
                 String tmp[]=ex.split("\\\"");
                 String tmp1[]=ex1.split("\\\"");
                 int value=Integer.parseInt(tmp[1]),value1=Integer.parseInt(tmp1[1]);*/
        // out.write(value+","+value1+","+distanceMatrix[i][j]+"\n");
        /*  out.write(i+","+j+","+distanceMatrix[i][j]+"\n");
             }
         out.close();
         }
         catch(FileNotFoundException ex){
             ex.printStackTrace();
         }
         */ //write sparse
           try{
         PrintWriter out = new PrintWriter(output.getAbsolutePath());
         for(int i=0;i<numElem;i++)
             for(int j=0;j<numElem;j++){
                /* String ex=map.idExample.get(i);
                 String ex1=map.idExample.get(j);
                 String tmp[]=ex.split("\\\"");
                 String tmp1[]=ex1.split("\\\"");
                 int value=Integer.parseInt(tmp[1]),value1=Integer.parseInt(tmp1[1]);*/
        // out.write(value+","+value1+","+distanceMatrix[i][j]+"\n");
                 if(distanceMatrix[i][j]!=appset.maxDistance)
          out.write(i+","+j+","+distanceMatrix[i][j]+"\n");
                 else continue;
             }
         out.close();
         }
         catch(FileNotFoundException ex){
             ex.printStackTrace();
         }
         
         //return set;
    }
    
    void resetFile(File output){
        try{
         PrintWriter out = new PrintWriter(output.getAbsolutePath());
         out.close();
         }
         catch(FileNotFoundException ex){
             ex.printStackTrace();
         }
    }
    
}

