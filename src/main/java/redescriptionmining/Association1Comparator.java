/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package redescriptionmining;

import java.util.Comparator;
import java.util.HashSet;
import org.javatuples.Pair;

/**
 *
 * @author matej
 */
public class Association1Comparator implements Comparator<Pair<HashSet<Integer>,Integer>> {
      public int compare(Pair<HashSet<Integer>,Integer> s1, Pair<HashSet<Integer>,Integer> s2){
              //here comes the comparison logic
             if(s1.getValue1() == s2.getValue1())
                 return 0;
             else if( s1.getValue1()>s2.getValue1())
                 return -1;
             else return 1;
         }
}
