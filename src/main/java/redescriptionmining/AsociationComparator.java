/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package redescriptionmining;

import java.util.Comparator;
import org.javatuples.Triplet;

/**
 *
 * @author matej
 */
 public class AsociationComparator implements Comparator<Triplet<Integer,Integer,Integer>>{

         public int compare(Triplet<Integer,Integer,Integer> s1, Triplet<Integer,Integer,Integer> s2){
              //here comes the comparison logic
             if(s1.getValue2() == s2.getValue2())
                 return 0;
             else if( s1.getValue2()>s2.getValue2())
                 return -1;
             else return 1;
         }
 }
