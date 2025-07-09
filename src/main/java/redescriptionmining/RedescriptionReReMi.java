/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package redescriptionmining;

import java.util.ArrayList;
import java.util.HashSet;

/**
 *
 * @author matej
 */
public class RedescriptionReReMi {
    HashSet<Integer> support=new HashSet();
    HashSet<Integer> suppLeft=new HashSet();
    HashSet<Integer> suppRight=new HashSet();
    ArrayList<Integer> attributes=new ArrayList<>();
    ArrayList<String> queries = new ArrayList<>();
    double JS=0.0, pVal=0.0;
}
