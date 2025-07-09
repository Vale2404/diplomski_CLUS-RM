/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package redescriptionmining;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.hash.TIntHashSet;
import java.util.ArrayList;
import static javax.management.ImmutableDescriptor.union;
import org.apache.commons.math3.distribution.BinomialDistribution;
import static org.apache.commons.math3.geometry.euclidean.threed.Plane.intersection;

/**
 *
 * @author matej
 */
public class RedescriptionReReMiGen {
    public ArrayList<TIntHashSet> supports = new ArrayList<>();
    public ArrayList<String> queries = new ArrayList<>();
    public double JS = 0.0;
    public double pval = 1.0;
    public TIntHashSet elements, elementsUnion;
    
    RedescriptionReReMiGen(int numViews){
        elements = new TIntHashSet();
        elementsUnion = new TIntHashSet();
        
        for(int i=0;i<numViews;i++){
            queries.add(null);
            supports.add(new TIntHashSet());
        }
    }
    
     ArrayList<RedescriptionReReMiGen> merge(RedescriptionReReMiGen R){
        ArrayList<RedescriptionReReMiGen> tmp = new ArrayList<>();
        int index = -1;
        
        for(int i=0;i<queries.size();i++)
            if(queries.get(i)!=null && R.queries.get(i)!=null){
                index = 1;
                break;
            }
        
        RedescriptionReReMiGen rtmp = new RedescriptionReReMiGen(queries.size());
        
        for(int i=0;i<queries.size();i++)
                if(queries.get(i)!=null){
                    rtmp.queries.set(i, queries.get(i));
                    rtmp.supports.get(i).addAll(supports.get(i));
                }
        
        int found = 0;
        
        for(int i=0;i<R.queries.size();i++)
            if(R.queries.get(i)!=null && rtmp.queries.get(i) == null){
                rtmp.queries.set(i, R.queries.get(i));
                rtmp.supports.get(i).clear();
                rtmp.supports.get(i).addAll(R.supports.get(i));
                found = 1;
            }
                       
        if(found == 0)
            return tmp;
        
        tmp.add(rtmp);
        
        if(index == 1){
            rtmp = new RedescriptionReReMiGen(queries.size());
            
            for(int i=0;i<R.queries.size();i++)
                if(R.queries.get(i)!=null){
                    rtmp.queries.set(i, R.queries.get(i));
                    rtmp.supports.get(i).addAll(R.supports.get(i));
                }
        
        for(int i=0;i<queries.size();i++)
            if(queries.get(i)!=null && rtmp.queries.get(i) == null){
                rtmp.queries.set(i, queries.get(i));
                 rtmp.supports.get(i).clear();
                rtmp.supports.get(i).addAll(supports.get(i));
            }
        
          tmp.add(rtmp);
        }
        
        return tmp;
    }
     
     void computeStatistics(ArrayList<DataSetCreator> dats, ArrayList<Mappings> mappings){
      
      this.elements = new TIntHashSet();
      this.elementsUnion = new TIntHashSet();
     
      int first = 0, index = 0;
      TIntIterator it = null;
      
      for(int i=0;i<this.supports.size();i++){
          if(first == 0 && this.queries.get(i)!=null){
              it = this.supports.get(i).iterator();
              index = i;
              first = 1;
          }
          if(this.queries.get(i)!=null)
          elementsUnion.addAll(this.supports.get(i));
      }
      
      while(it.hasNext()){
          int el = it.next();
          int contained = 1;
        for(int i=0;i<supports.size();i++){
          if(i==index || queries.get(i) == null)
              continue;
          if(!supports.get(i).contains(el)){
              contained = 0;
              break;
          }
       }
        if(contained==1)
            elements.add(el);
      }
      
      this.JS = (double)elements.size()/(double)elementsUnion.size();
      
       double elemSize=1.0, numExamples=1.0;
            
            for(int k=0;k<this.supports.size();k++){
                if(queries.get(k)!=null){
                    elemSize*=supports.get(k).size();
                    numExamples*=dats.get(0).numExamples;
                }
            }
            
            double prob=elemSize/numExamples;
            BinomialDistribution dist=new BinomialDistribution(dats.get(0).numExamples,prob);
            this.pval=1.0-dist.cumulativeProbability(this.elements.size());
     }
    
}
