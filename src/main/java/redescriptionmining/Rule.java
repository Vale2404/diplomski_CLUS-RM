/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package redescriptionmining;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.hash.TIntHashSet;
import java.util.Collections;
import java.util.Set;

/**
 *
 * @author matej
 */
public class Rule {
	// HashSet<Integer> elements=null;
	public TIntHashSet elements = null;
	HashMap<Integer, ArrayList<Double>> ruleMap = null;
	TIntHashSet elementsMissing = null;
	// HashSet<Integer> ruleAtts=null;
	TIntHashSet ruleAtts = null;
	public String rule = null;
	public int numElements = 0;
	// int UsedIn=0;
	private int ruleId;
	private boolean isRemoved = false;
	public boolean hadMissing = false;
	public boolean isTarget = false;
	private double sumWeightNegacije;
    private TIntHashSet missingSetDifferenceMissingSetNegated;
	
	public Rule() {
		// elements=new HashSet<Integer>();
		elements = new TIntHashSet(200);
		ruleMap = new HashMap<>();
		ruleAtts = new TIntHashSet(200);
	}

	public Rule(String _rule, Mappings map) {
		rule = _rule;
		// elements=new HashSet<Integer>();
		elements = new TIntHashSet();
		ruleMap = new HashMap<>();
		ruleAtts = new TIntHashSet();

		String rs[] = rule.split(" AND ");
		// System.out.println("Rule: "+_rule);
		for (int i = 0; i < rs.length; i++) {
			if (rs[i].contains(">")) {// add case for categorical data contains("=")
				String t[] = rs[i].split(" > ");
				// if(!ruleMap.containsKey(map.attId.get(t[0]))){
				if (!ruleAtts.contains(map.attId.get(t[0]))) {
					/*
					 * ArrayList<Double> tmp=new ArrayList<Double>(Collections.nCopies(4, 0.0));
					 * t[1]=t[1].replaceAll(",", "."); String num[]=t[1].split("\\."); String
					 * number=""; if(num.length>2){ for(int z=0;z<num.length;z++) if(z+1<num.length)
					 * number+=num[z]; else number+="."+num[z]; t[1]=number; } tmp.set(0,1.0);
					 * tmp.set(1,Double.parseDouble(t[1])); tmp.set(3, Double.MAX_VALUE);
					 */
					// ruleMap.put(map.attId.get(t[0]), null);
					ruleAtts.add(map.attId.get(t[0]));
				} else {
					/*
					 * ArrayList<Double> tmp=ruleMap.get(t[0]); t[1]=t[1].replaceAll(",", ".");
					 * String num[]=t[1].split("\\."); String number=""; if(num.length>2){ for(int
					 * z=0;z<num.length;z++) if(z+1<num.length) number+=num[z]; else
					 * number+="."+num[z]; t[1]=number; } Double value=Double.parseDouble(t[1]);
					 * tmp.set(0, 1.0); if(value>tmp.get(1)){ tmp.set(1,value); ruleMap.put(t[0],
					 * tmp); }
					 */
				}
			} else if (rs[i].contains("<=")) {
				String t[] = rs[i].split(" <= ");
				// if(!ruleMap.containsKey(map.attId.get(t[0]))){
				if (!ruleAtts.contains(map.attId.get(t[0]))) {
					/*
					 * ArrayList<Double> tmp=new ArrayList<Double>(Collections.nCopies(4, 0.0));
					 * t[1]=t[1].replaceAll(",", ".");
					 * 
					 * String num[]=t[1].split("\\."); String number=""; //
					 * System.out.println("num length: "+num.length); if(num.length>2){ for(int
					 * z=0;z<num.length;z++) if(z+1<num.length) number+=num[z]; else
					 * number+="."+num[z]; t[1]=number; // System.out.println("number: "+number); }
					 * // System.out.println("t[1]: "+t[1]); tmp.set(2,1.0);
					 * tmp.set(3,Double.parseDouble(t[1]));
					 */
					// ruleMap.put(map.attId.get(t[0]), null);
					ruleAtts.add(map.attId.get(t[0]));
				} else {
					/*
					 * ArrayList<Double> tmp=ruleMap.get(t[0]); t[1]=t[1].replaceAll(",", ".");
					 * String num[]=t[1].split("\\."); String number=""; if(num.length>2){ for(int
					 * z=0;z<num.length;z++) if(z+1<num.length) number+=num[z]; else
					 * number+="."+num[z]; t[1]=number; } Double value=Double.parseDouble(t[1]);
					 * tmp.set(2,1.0); if(value<tmp.get(3)){ tmp.set(3, value); ruleMap.put(t[0],
					 * tmp); }
					 */
				}
			} else if (rs[i].contains("=") && !rs[i].contains("<")) {
				String t[] = rs[i].split(" = ");
				// if(!ruleMap.containsKey(map.attId.get(t[0]))){
				// System.out.println("Attribute: "+t[0]);
				// if(!map.attId.containsKey(t[0]))
				// System.out.println("No such attribute found!");
				if (!ruleAtts.contains(map.attId.get(t[0]))) {
					// ArrayList<Double> tmp=new ArrayList<>();
					// tmp.add((double)map.cattAtt.get(t[0]).getValue0().get(t[1]));
					// ruleMap.put(map.attId.get(t[0]), tmp);
					ruleAtts.add(map.attId.get(t[0]));
				}
			} else if (rs[i].contains("in")) {
				numElements = -1;
				return;
			}
		}
	}

	void addElementsMissing(Mappings map, DataSetCreator dat, int mode) { // 0 for rule, 1 if a rule is negated

		elementsMissing = new TIntHashSet(100);

		for (int i = 0; i < dat.numExamples; i++) {
			if (!elements.contains(i)) {
				TIntIterator it = ruleAtts.iterator();
				if (mode == 0) {
					while (it.hasNext()) {
						int at = it.next();
						if (!map.catAttInd.contains(at)/* !cat.contains(map.idAtt.get(at)) */) {
							if (dat.getValue(at, i) == Double.POSITIVE_INFINITY) {
								elementsMissing.add(i);
								break;
							}
						} else {
							Set<String> catVal = map.cattAtt.get(at).getValue0().keySet();
							if (!catVal.contains(dat.getValueCategorical(at, i))) {
								elementsMissing.add(i);
								break;
							}
						}
					}
				} else if (mode == 1) {
					int contained = 1;
					while (it.hasNext()) {
						int at = it.next();
						if (!map.catAttInd.contains(at)/* !cat.contains(map.idAtt.get(at)) */) {
							if (dat.getValue(at, i) != Double.POSITIVE_INFINITY) {
								contained = 0;
								break;
							}
						} else {
							Set<String> catVal = map.cattAtt.get(at).getValue0().keySet();
							if (catVal.contains(dat.getValueCategorical(at, i))) {
								contained = 0;
								break;
							}
						}
					}

					if (contained == 1)
						elementsMissing.add(i);
				}
			}
		}
	}

	void closeInterval(DataSetCreator dat, Mappings map) {

		TIntIterator it = ruleAtts.iterator();

		while (it.hasNext()) {
			ruleMap.put(it.next(), null);
		}

		Iterator<Integer> itL = ruleMap.keySet().iterator();

		while (itL.hasNext()) {
			double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
			int att = itL.next();

			if (!map.cattAtt.containsKey(att)) {
				TIntIterator itT = elements.iterator();
				while (itT.hasNext()) {

					int s = itT.next();
					int ik = s;
					double val = dat.getValue(att, ik);

					if (val < min)
						min = val;
					if (val > max)
						max = val;
				}
				ArrayList<Double> attVal = new ArrayList<>(Collections.nCopies(4, 0.0));// leftRule.get(att);

				attVal.set(0, 1.0);
				attVal.set(2, 1.0);
				attVal.set(1, min);
				attVal.set(3, max);
				ruleMap.put(att, attVal);
				// leftRule.put(att, attVal);
			} else {
				TIntIterator itT = elements.iterator();

				while (itT.hasNext()) {
					System.out.println("Iterating through elements");
					int s = itT.next();
					// for(int s:leftRuleElements){
					int ik = s;
					String val = dat.getValueCategorical(att, ik);
					ArrayList<Double> attVal = new ArrayList<>();
					attVal.add((double) map.cattAtt.get(att).getValue0().get(val));
					System.out.println(attVal.get(0));
					this.ruleMap.put(att, attVal);
					// leftRule.put(att, attVal);
					break;
				}
			}
		}

		TIntHashSet RSupport = new TIntHashSet(dat.numExamples);

		for (int i = 0; i < dat.numExamples; i++) {
			// Iterator<Integer> itL=leftRule.keySet().iterator();
			itL = ruleMap.keySet().iterator();
			int contained = 1;
			while (itL.hasNext()) {
				int attr = itL.next();

				ArrayList<Double> attrVal = ruleMap.get(attr);

				if (!map.cattAtt.containsKey(attr)) {
					double val = dat.getValue(attr, i);
					if (val >= attrVal.get(1) && val <= attrVal.get(3))
						continue;
					else {
						contained = 0;
						break;
					}
				} else {
					String cat = map.cattAtt.get(attr).getValue1().get((int) (double) attrVal.get(0));
					String realCat = dat.getValueCategorical(attr, i);
					if (cat.contentEquals(realCat))
						continue;
					else {
						contained = 0;
						break;
					}
				}
			}

			if (contained == 0)
				continue;
			else {
				RSupport.add(i);
			}
		}

		if (RSupport.size() != elements.size()) {
			System.out.println("Rule size mismatch...");
			System.out.println("CLUS rule size: " + elements.size());
			System.out.println("Validation size: " + RSupport.size());
		}

		it = RSupport.iterator();
		int ok = 1;

		while (it.hasNext()) {
			int elem = it.next();
			if (!elements.contains(elem)) {
				System.out.println("Not contained element in rule: " + map.idExample.get(elem));
				ok = 0;
			}
		}

		it = elements.iterator();

		while (it.hasNext()) {
			int elem = it.next();
			if (!RSupport.contains(elem)) {
				System.out.println("Not contained element in validation: " + map.idExample.get(elem));
				ok = 0;
			}
		}

		if (ok == 0) {
			System.out.println("Rule string");
			System.out.println(this.rule);
			it = elements.iterator();
			while (it.hasNext())
				System.out.print(map.idExample.get(it.next()) + " ");
			System.exit(-1);
		}
	}

	void clearRuleMap() {
		ruleMap.clear();
	}

	void addRule(String _rule) {
		rule = _rule;
	}

	public void addElement(String element, Mappings map) {
		if (map.exampleId.containsKey(element))
			elements.add(map.exampleId.get(element));
		/*
		 * if(rule.contentEquals("DATE = true")){ System.out.println("Add element");
		 * System.out.println(element+" "+map.exampleId.get(element)); }
		 */
	}

	public void checkElements(Mappings map, DataSetCreator dat) {
		Iterator<Integer> it;
		HashSet<Integer> toRemove = new HashSet<>();

		TIntIterator attrsIterator = ruleAtts.iterator();
		// for(Integer att:ruleMap.keySet()){
		while (attrsIterator.hasNext()) {
			int att = attrsIterator.next();
			// for(Integer att:ruleAtts){
			if (!map.cattAtt.containsKey(att)) {
				/*
				 * System.out.println("att id: "+att);
				 * System.out.println("attribute: "+map.idAtt.get(att));
				 * System.out.println("type numeric...");
				 */
				TIntIterator iterator = elements.iterator();
				while (iterator.hasNext()) {
					int elem = iterator.next();
					double val = dat.getValue(att, elem);
					if (val == Double.POSITIVE_INFINITY) {
						toRemove.add(elem);
					}
				}

				/*
				 * for(Integer elem:elements){ double val=dat.getValue(att, elem);
				 * if(val==Double.POSITIVE_INFINITY){ toRemove.add(elem); } }
				 */
			} else {
				TIntIterator iterator = elements.iterator();

				while (iterator.hasNext()) {
					int elem = iterator.next();
					String attrVal = dat.getValueCategorical(att, elem);
					if (!map.cattAtt.get(att).getValue0().containsKey(attrVal))
						toRemove.add(elem);

					/*
					 * System.out.println("att id: "+att);
					 * System.out.println("attribute: "+map.idAtt.get(att));
					 * System.out.println("type categorical...");
					 * System.out.println("Element id: "+elem);
					 * System.out.println("Element: "+map.idExample.get(elem));
					 * System.out.println("Value: "+attrVal);
					 */

				}

				/*
				 * for(Integer elem:elements){ String attrVal=dat.getValueCategorical(att,
				 * elem);
				 * if(!map.cattAtt.get(map.idAtt.get(att)).getValue0().containsKey(attrVal))
				 * toRemove.add(elem); }
				 */
			}
		}

		it = toRemove.iterator();

		while (it.hasNext()) {
			elements.remove(it.next());
		}

	}

	public static void checkAndRemoveElements(List<Rule> rules, Mappings map, DataSetCreator dat) {
		rules.get(rules.size() - 1).checkElements(map, dat);
        if (rules.get(rules.size() - 1).elements.isEmpty() || rules.get(rules.size() - 1).elements.size() < ApplicationSettings.getInstance().minSupport || rules.get(rules.size() - 1).numElements == -1) {//make general
            rules.remove(rules.size() - 1);
        }
	}
	
	public Integer calculateSupportSizeForNegation(DataSetCreator dat, Mappings map) {
		
		int supportSize = 0;
		
		for(int i = 0; i < dat.numExamples; i++) {
			if(!elements.contains(i)) {
				TIntIterator it = ruleAtts.iterator();
				
				while (it.hasNext()) {
					int at = it.next();
					if (!map.catAttInd.contains(at)) {
						if (dat.getValue(at, i) != Double.POSITIVE_INFINITY) {
							supportSize++;
						}
					} else {
						Set<String> catVal = map.cattAtt.get(at).getValue0().keySet();
						if (catVal.contains(dat.getValueCategorical(at, i))) {
							supportSize++;
							break;
						}
					}
				}
			}
		}
		return supportSize;
	}
	
	Rule(Rule RNeg, DataSetCreator dat, Mappings map) {

		elements = new TIntHashSet(dat.numExamples);
		for (int i = 0; i < dat.numExamples; i++) {
			if (!RNeg.elements.contains(i)) {
				TIntIterator it = RNeg.ruleAtts.iterator();
				while (it.hasNext()) {
					int at = it.next();
					if (!map.catAttInd.contains(at)/* !cat.contains(map.idAtt.get(at)) */) {
						if (dat.getValue(at, i) != Double.POSITIVE_INFINITY) {
							elements.add(i);
							break;
						}
					} else {
						Set<String> catVal = map.cattAtt.get(at).getValue0().keySet();
						if (catVal.contains(dat.getValueCategorical(at, i))) {
							elements.add(i);
							break;
						}
					}
				}
			}
		}

		numElements = elements.size();
		ruleAtts = new TIntHashSet(RNeg.ruleAtts);
		rule = RNeg.rule;
	}

	public int checkConstraints(ArrayList<ArrayList<ArrayList<String>>> importantAttributes, int view, int constType,
			Mappings map) {

		ArrayList<ArrayList<String>> constraints = importantAttributes.get(view);

		if (constraints.size() == 0)
			return 1;

		TIntIterator it = this.ruleAtts.iterator();

		int s1 = 0;

		if (constType == 2) {
			for (int i = 0; i < constraints.size(); i++) {
				s1 = 1;
				for (int j = 0; j < constraints.get(i).size(); j++) {
					if (constraints.get(i).get(j).equals(""))
						continue;
					/*
					 * System.out.println("constraint: "+constraints.get(i).get(j));
					 * System.out.println(this.rule);
					 */
					if (!ruleAtts.contains(map.attId.get(constraints.get(i).get(j)))) {
						s1 = 0;
						break;
					}
				}
				// System.out.println("s1: "+s1);
				if (s1 == 1)
					return 1;
			}
			return 0;
		} else if (constType == 1) {
			for (int i = 0; i < constraints.size(); i++) {
				for (int j = 0; j < constraints.get(i).size(); j++) {
					if (constraints.get(i).get(j).equals(""))
						return 1;
					if (ruleAtts.contains(map.attId.get(constraints.get(i).get(j)))) {
						return 1;
					}
				}
			}
			return 0;
		}

		return 1;
	}

	public boolean isRemoved() {
		return isRemoved;
	}
	
	public void setRemoved() {
		isRemoved = true;
	}
	
	public int getRuleId() {
		return ruleId;
	}
	
	public void setRuleId(int ruleId) {
		this.ruleId = ruleId;
	}
	
	public String toString() {
		return (isTarget ? "Target---" : "") + ruleId + ": " + rule;
	}

	public double getSumWeightNegacije() {
		return sumWeightNegacije;
	}

	public void setSumWeightNegacije(double sumWeightNegacije) {
		this.sumWeightNegacije = sumWeightNegacije;
	}

	public TIntHashSet getMissingSetDifferenceMissingSetNegated() {
		return missingSetDifferenceMissingSetNegated;
	}

	public void setMissingSetDifferenceMissingSetNegated(TIntHashSet missingSetDifferenceMissingSetNegated) {
		this.missingSetDifferenceMissingSetNegated = missingSetDifferenceMissingSetNegated;
	}
	
}
