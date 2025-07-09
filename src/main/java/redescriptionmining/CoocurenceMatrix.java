/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package redescriptionmining;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.hash.TIntHashSet;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 *
 * @author matej
 */
public class CoocurenceMatrix {
    public int matrix[][];
    public int attributeMatrix[][];
    
    CoocurenceMatrix(int numElements, int numAttributes){
        matrix= new int[numElements][numElements];
        attributeMatrix=new int[numAttributes][numAttributes];
    }
    
    public void init(int numElements, int numAttributes){
        for(int i=0;i<numElements;i++)
            for(int j=0;j<numElements;j++)
                matrix[i][j]=0;
        
        for(int i=0;i<numAttributes;i++)
            for(int j=0;j<numAttributes;j++)
                attributeMatrix[i][j]=0;     
    }
    
    public void computeMatrix(RedescriptionSet rs, DataSetCreator dat){
        for(int i=0;i<rs.redescriptions.size();i++){
            Redescription r=rs.redescriptions.get(i);
            TIntIterator it=r.elements.iterator();
            
            while(it.hasNext()){
                int elem=it.next();
                matrix[elem][elem]++;
                TIntIterator it1=r.elements.iterator();
                
                while(it1.hasNext()){
                    int elem2=it1.next();
                    if(elem!=elem2){
                        matrix[elem][elem2]++;
                    }
                }
            }         
             ArrayList<TIntHashSet> lsA=r.computeAttributes(r.viewElementsLists, dat);
            
          for(int k=0;k<lsA.size();k++){
             it=lsA.get(k).iterator();
             
             while(it.hasNext()){
                int attr=it.next();
                attributeMatrix[attr][attr]++;
                TIntIterator it1=lsA.get(k).iterator();
                
                while(it1.hasNext()){
                    int attr2=it1.next();
                    if(attr!=attr2){
                        attributeMatrix[attr][attr2]++;
                    }
                }
                
                for(int k1=0;k1<lsA.size();k1++){
                
                    if(k1==k)
                        continue;
                it1=lsA.get(k1).iterator();
                while(it1.hasNext()){
                    int attr2=it1.next();
                    if(attr!=attr2){
                        attributeMatrix[attr][attr2]++;
                    }
                }
              }
            }              
        }        
    }
  }
    
    public void writeToFileElements(File output, int numExamples){

        try{
         PrintWriter out = new PrintWriter(output.getAbsolutePath());
         
         for(int i=0;i<numExamples;i++){
             for(int j=0;j<numExamples;j++)
                 if((j+1)<numExamples)
                 out.write(matrix[i][j]+" ");
             else
                     out.write(matrix[i][j]+"");
             out.write("\n");
         }
         
         out.close();
         }
         catch(FileNotFoundException ex){
             ex.printStackTrace();
         }
    }
    
    public void writeToFileAttributes(File output, int numAttrs){

        try{
         PrintWriter out = new PrintWriter(output.getAbsolutePath());
         
         for(int i=0;i<numAttrs;i++){
             for(int j=0;j<numAttrs;j++)
                 if((j+1)<numAttrs)
                 out.write(attributeMatrix[i][j]+" ");
             else
                     out.write(attributeMatrix[i][j]+"");
             out.write("\n");
         }
         
         out.close();
         }
         catch(FileNotFoundException ex){
             ex.printStackTrace();
         }
    }
}
