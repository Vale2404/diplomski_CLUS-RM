/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package redescriptionmining;

import gnu.trove.iterator.TIntIterator;
import java.io.BufferedReader;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.math3.distribution.BinomialDistribution;
import redescriptionmining.RedescriptionSet;
import static redescriptionmining.SettingsReader.ENCODING;

/**
 *
 * @author matej
 */
public class ReadCLUSRMReds {
    
    File inputFile;
    public RedescriptionSet set=new RedescriptionSet();
    
    public void readReds(DataSetCreator dat){
        
         BufferedReader reader;
            
            String dataInput="";//add path
            //DataSetCreator dat=new DataSetCreator(dataInput);
           // ArrayList<RedescriptionReReMi> red=new ArrayList<>();
            //RedescriptionSet rs=new RedescriptionSet();
        String file="";
         
        try {
      Path path =Paths.get(inputFile.getAbsolutePath());
      System.out.println("Path: "+inputFile.getAbsolutePath());
      reader = Files.newBufferedReader(path,ENCODING);
      String line = null;
      int count=0, elem=0;
     
    Redescription r=null;
      while ((line = reader.readLine()) != null){
        if(line.contains("Covered examples")){
                 r=new Redescription(dat);
            elem=1;
            continue;
        }
        if(elem==1){
            String tmp[]=line.split(" ");
            elem=0;
            for(int i=0;i<tmp.length;i++){
                tmp[i]=tmp[i].replaceAll("\"", "");
                r.elements.add(Integer.parseInt(tmp[i]));
            }
            set.redescriptions.add(r);
        }
        
    }
      reader.close();
         }
         catch(Exception e){
             e.printStackTrace();
         }
        
    }
    
     public void readReds(DataSetCreator dat, Mappings map){
        
         BufferedReader reader;
            
            String dataInput="";//add path
            //DataSetCreator dat=new DataSetCreator(dataInput);
           // ArrayList<RedescriptionReReMi> red=new ArrayList<>();
            //RedescriptionSet rs=new RedescriptionSet();
        String file="";
         
        try {
      Path path =Paths.get(inputFile.getAbsolutePath());
      System.out.println("Path: "+inputFile.getAbsolutePath());
      reader = Files.newBufferedReader(path,ENCODING);
      String line = null;
      int count=0, elem=0;
     
    Redescription r=null;
      while ((line = reader.readLine()) != null){
        if(line.contains("Covered examples")){
                 r=new Redescription(dat);
            elem=1;
            continue;
        }
        if(elem==1){
            String tmp[]=line.split(" ");
            elem=0;
            for(int i=0;i<tmp.length;i++){
                tmp[i]=tmp[i].replaceAll("\"", "");
                r.elements.add(map.exampleId.get(tmp[i]));
            }
            set.redescriptions.add(r);
        }
        
    }
      reader.close();
         }
         catch(Exception e){
             e.printStackTrace();
         }
        
    }
    
    
}
