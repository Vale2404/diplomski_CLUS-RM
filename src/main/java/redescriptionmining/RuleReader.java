/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package redescriptionmining;

import gnu.trove.iterator.TIntIterator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * @author matej
 */
public class RuleReader {
    ArrayList<Rule> rules;
    int newRuleIndex, newRuleIndexF;

    RuleReader() {
        rules = new ArrayList<>();
        newRuleIndex = 0;
    }

    int hasNewAtt(Rule r1, Rule r2, int NumNA) { //possibly change back
        int tmp = 0;

        //Iterator<Integer> it=r1.ruleMap.keySet().iterator();
        // Iterator<Integer> it=r1.ruleAtts.iterator();
        TIntIterator it = r1.ruleAtts.iterator();

        while (it.hasNext()) {
            Integer at = it.next();
            //if(!r2.ruleMap.containsKey(at))
            if (!r2.ruleAtts.contains(at))
                tmp++;
        }


        // if(tmp==0 && r1.ruleMap.keySet().size()==r2.ruleMap.keySet().size() && r1.numElements>r2.numElements)
        if (tmp == 0 && r1.ruleAtts.size() == r2.ruleAtts.size() && r1.numElements > r2.numElements)
            return 1;

        // if(tmp==0 && r1.ruleMap.keySet().size()!=r2.ruleMap.keySet().size())
        if (tmp == 0 && r1.ruleAtts.size() != r2.ruleAtts.size())
            return 1;

        if (tmp >= NumNA)
            return 1;
        else
            return 0;
    }

    int addnewRules(RuleReader NRules, int NumNA) {
        newRuleIndex = rules.size();
        int index = 0;

        if (rules.size() == 0) {
            for (int i = 0; i < NRules.rules.size(); i++) {
                rules.add(NRules.rules.get(i));
                System.out.println("addnewRules: " + NRules.rules.get(i).rule);
            }
            return NRules.rules.size();
        }

        for (int i = 0; i < NRules.rules.size(); i++) {
            int hasNewAttribute = 1;
            for (int j = 0; j < rules.size(); j++) {
                if (hasNewAtt(NRules.rules.get(i), rules.get(j), NumNA) == 0) {
                    hasNewAttribute = 0;
                    break;
                }
            }
            if (hasNewAttribute == 1) {
                rules.add(NRules.rules.get(i));
                index++;
            }
        }
        return index;
    }

    void removeElements() {
        for (int i = 0; i < newRuleIndex; i++)
            if (!rules.get(i).elements.isEmpty())
                rules.get(i).elements.clear();
    }

    int addNewRulesC(RuleReader NRules, int NumNA, int changeIndex) {

        System.out.println("Adding new rules to set...");

        if (changeIndex == 1)
            newRuleIndex = rules.size();

        int index = 0;

        if (rules.size() == 0) {
            for (int i = 0; i < NRules.rules.size(); i++) {
                rules.add(NRules.rules.get(i));
                NRules.rules.get(i).isTarget = false;
            	TargetRulesMap.getIdRuleMap().put(NRules.rules.get(i).getRuleId(), NRules.rules.get(i));
            }
            return NRules.rules.size();
        }

        for (int i = 0; i < NRules.rules.size(); i++) {
            int hasNewAttribute = 1;
            for (int j = 0; j < rules.size(); j++) {
                if (hasNewAtt(NRules.rules.get(i), rules.get(j), NumNA) == 0) {
                    hasNewAttribute = 0;
                    break;
                }
            }
            if (hasNewAttribute == 1) {
                rules.add(NRules.rules.get(i));
                NRules.rules.get(i).isTarget = false;
                TargetRulesMap.getIdRuleMap().put(NRules.rules.get(i).getRuleId(), NRules.rules.get(i));
                //System.out.println("AddNewrulesC: "+NRules.rules.get(i).rule);
                index++;
            }
            if (i % 100 == 0)
                if (i != 0)
                    System.out.println(i / 100);
        }
        newRuleIndexF = index;
        return index;
    }

    void removeRulesCF() {
        if (rules.size() > newRuleIndexF) {
            rules.subList(newRuleIndexF, rules.size()).clear();
        }
    }

    void addNewRulesCF(RuleReader NRules, int NumNA) {

        System.out.println("Adding new rules to set...");

        newRuleIndexF = rules.size();

        if (rules.size() == 0) return;

        for (int i = 0; i < NRules.rules.size(); i++) {
            int hasNewAttribute = 1;
            for (int j = 0; j < rules.size(); j++) {
                if (hasNewAtt(NRules.rules.get(i), rules.get(j), NumNA) == 0) {
                    hasNewAttribute = 0;
                    break;
                }
            }
            if (hasNewAttribute == 1) {
                Rule rule = NRules.rules.get(i);
                rules.add(rule);
                rule.isTarget = false;
                //System.out.println("AddNewrulesC: "+NRules.rules.get(i).rule);
                TargetRulesMap.getIdRuleMap().put(rule.getRuleId(), rule);
            }
            if (i % 100 == 0)
                if (i != 0)
                    System.out.println(i / 100);
        }
    }

    void setSize() {
        for (Rule rule : rules)
            if (rule.elements.size() != 0)
                rule.numElements = rule.elements.size();
    }

    void reduceRuleSet(ArrayList<ArrayList<ArrayList<String>>> importantAttributes, Mappings map, ApplicationSettings appset) {
        int contains = 0;
        ArrayList<Integer> toRemove = new ArrayList<>();

        for (int i = (rules.size() - 1); i >= 0; i--) {
            contains = 0;
            for (int j = 0; j < importantAttributes.size(); j++) {
                if (rules.get(i).ruleAtts.contains(map.attId.get(importantAttributes.get(j))))
                    contains = 1;
                break;
            }

            if (contains == 0)
                rules.remove(i);
            else System.out.println("Sadrzi atribut!");
        }
    }

    void extractRules(HashMap<String, ArrayList<String>> rulesAndExamples, Mappings map, DataSetCreator dat, ApplicationSettings appset) {

        for(Map.Entry<String, ArrayList<String>> ruleAndExamples : rulesAndExamples.entrySet()) {
            rules.add(new Rule(ruleAndExamples.getKey(), map));

            for(String ex : ruleAndExamples.getValue()) {
                if (ex.length() > 10) {
                    if (!ex.substring(ex.length() - 10, ex.length() - 1).contains("new-Init")) {
                        //System.out.println("Substring: "+ex.substring(ex.length()-10, ex.length()-1).contains("new-Init"));
                        rules.get(rules.size() - 1).addElement(ex, map);
                        // System.out.println("Added: "+ex);
                    }
                } else
                    rules.get(rules.size() - 1).addElement(ex, map);
            }
            rules.get(rules.size() - 1).checkElements(map, dat);
            if (rules.get(rules.size() - 1).elements.isEmpty() || rules.get(rules.size() - 1).elements.size() < appset.minSupport || rules.get(rules.size() - 1).numElements == -1) {//make general
                rules.remove(rules.size() - 1);
            }
        }

        if (rules.size() != 0) {
            rules.get(rules.size() - 1).checkElements(map, dat);
            if (rules.get(rules.size() - 1).elements.isEmpty() || rules.get(rules.size() - 1).elements.size() < appset.minSupport || rules.get(rules.size() - 1).numElements == -1) {
                rules.remove(rules.size() - 1);
            }
        }
    }

    void extractRules(String inputFile, Mappings map, DataSetCreator dat, ApplicationSettings appset) {
        File input = new File(inputFile);
        BufferedReader reader;

        try {
            reader = new BufferedReader(new FileReader(input));
            String line = null;
            int lineNum = 0, found = 0, endRule = 1, examples = 0;
            String rule = null;
            String ex = null;
            while ((line = reader.readLine()) != null) {

                //if(line.contentEquals("Rules-Original Model")){//use with decision tree to rules
                if (line.contentEquals("Rules Model")) {//use with random forest to rules
                    found = 1;
                }

                if (found == 1) {
                    lineNum++;
                }

                if (lineNum == 4) {
                    found = 0;
                    String lTmp = line;
                    if (lTmp.split(" ")[0].equals("IF")) {
                        endRule = 0;
                        rule = new String(line.substring(3));
                        //System.out.println("Rule in IF"+rule);
                    } else if (lTmp.split(" ")[0].equals("THEN")) {
                        endRule = 1;
                        rules.add(new Rule(rule, map));
                        //System.out.println("Rule extract: "+rule);
                        /*if(rule.contains("NPIFTOT")){
                    System.out.println("In rule construction");
                    System.out.println(rule);
                    }*/
                        continue;
                    } else if (endRule == 0) {
                        rule = rule.concat(" ");
                        rule = rule.concat(line.substring(3));
                    } else if (line.contains("Covered examples:")) {
                        examples = 1;
                    } else if (examples == 1) {

                        if (line.contentEquals("")) {
                            examples = 0;
                            rules.get(rules.size() - 1).checkElements(map, dat);
                            if (rules.get(rules.size() - 1).elements.isEmpty() || rules.get(rules.size() - 1).elements.size() < appset.minSupport || rules.get(rules.size() - 1).numElements == -1) {//make general
                                rules.remove(rules.size() - 1);
                            }
                            continue;
                        }

                        String tok[] = line.split(" ");
                        //ex=new String(tok[1].split(",")[0]);
                        ex = new String(tok[1]);

                        if (ex.length() > 10) {
                            if (!ex.substring(ex.length() - 10, ex.length() - 1).contains("new-Init")) {
                                //System.out.println("Substring: "+ex.substring(ex.length()-10, ex.length()-1).contains("new-Init"));
                                rules.get(rules.size() - 1).addElement(ex, map);
                                // System.out.println("Added: "+ex);
                            }
                        } else
                            rules.get(rules.size() - 1).addElement(ex, map);
                    }
                }
            }
            if (rules.size() != 0) {
                rules.get(rules.size() - 1).checkElements(map, dat);
                if (rules.get(rules.size() - 1).elements.isEmpty() || rules.get((rules.size() - 1)).elements.size() < appset.minSupport || rules.get(rules.size() - 1).numElements == -1) {
                    rules.remove(rules.size() - 1);
                }
            }
            reader.close();
        
        /*for(int i=0;i<rules.size();i++){
            if(i==0)
            System.out.println("CLUS Rule Validation");
            rules.get(i).closeInterval(dat, map);
            rules.get(i).clearRuleMap();
        }*/

        } catch (IOException ioe) {
            System.err.println("IOException: " + ioe.getMessage());
        }
    }

    //keep number of rules <=1600
    int findCutoff(int numExamples, double startPercentage, double endPercentage, int StartIndex, int EndIndex, int[] minCovElem, int[] maxCovElem, int minSupp, int maxSupp, int numRules) {
        int nelMin = (int) (numExamples * startPercentage);
        int nelMax = (int) (numExamples * endPercentage);
        
        /*if(nelMax>maxSupp)
           nelMax=maxSupp;*/

        int ruleCount = 0;
        int minCovElemT = minSupp;
        int maxCovElemT = numExamples;//maxSupp;

        if (nelMin < minSupp)
            nelMin = minSupp;
        
        /*System.out.println("Num examples: "+numExamples);
        System.out.println("nelMax: "+nelMax);
        System.out.println("nelMin: "+nelMin);
        System.out.println("startIndex: "+StartIndex);
        System.out.println("endIndex: "+EndIndex);
        System.out.println("minSupp: "+minSupp);*/

        if (nelMin > minSupp)
            minCovElemT = nelMin;
        
        /*if(nelMax<maxSupp)
               maxCovElemT=nelMax;*/

        int step = (nelMax - nelMin) / 20;

        if (step == 0)
            step = 1;
        
        /*System.out.println("nelMin: "+nelMin);
        System.out.println("nelMax"+nelMax);
        System.out.println("minSupp: "+minSupp);
        System.out.println("maxSupp: "+maxSupp);*/

        if (nelMax < minSupp)
            return -1;

        while (true) {
            for (int i = StartIndex; i < EndIndex; i++) {
                if (rules.get(i).numElements >= startPercentage * numExamples && rules.get(i).numElements <= endPercentage * numExamples && rules.get(i).numElements >= minCovElemT && rules.get(i).numElements <= maxCovElemT)
                    ruleCount++;
                //System.out.println("Rule count: "+ruleCount);
                //System.out.println("Rule size: "+rules.get(i).elements.size());
            }

            if (ruleCount == 0)
                return -1;

            if (ruleCount > numRules) {
                ruleCount = 0;
                minCovElemT += step;
            } else break;
        }
        minCovElem[0] = minCovElemT;
        maxCovElem[0] = maxCovElemT;
       /*System.out.println("min: "+minCovElemT);
        System.out.println("max: "+maxCovElemT);
        System.out.println("numRules: "+ruleCount);*/
        return 0;
    }

}
