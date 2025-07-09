/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package redescriptionmining;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.math3.distribution.BinomialDistribution;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author matej
 */
public class RedescriptionSet {
	ArrayList<Redescription> redescriptions = new ArrayList<>();
	public double aJS = 0.0, aEJS = 0.0, aAJS = 0.0, aPvalSc = 0.0, aRS = 0.0, aRSt = 0.0;
	public long NumTests = 0;

	private RuleReader rr;
	private RuleReader rr1;
	private Jacard js[];
	private ApplicationSettings appset;
	private int oldIndexRR;
	private int oldIndexRR1;
	private int runInd;
	private boolean outOfmemory[];
	private Mappings map;
	private DataSetCreator dat;
	private int elemFreq[];
	private int attrFreq[];
	private ArrayList<Double> redScore;
	private ArrayList<Double> redScoreAt;
	private ArrayList<Double> redDistCoverage;
	private ArrayList<Double> redDistCoverageAt;
	private ArrayList<Double> redDistNetwork;
	private ArrayList<Double> targetAtScore;
	double statistics[];
	private ArrayList<Double> maxDiffScoreDistribution;
	private NHMCDistanceMatrix mat;
	int preferenceRow;

	public int createGuidedJoinExt(RuleReader rr, RuleReader rr1, Jacard js[], ApplicationSettings appset,
			int oldIndexRR, int oldIndexRR1, int RunInd, boolean outOfmemory[], Mappings map, DataSetCreator dat) {

		System.out.println("Using guided expansion with join procedure!");

		int oldSize = redescriptions.size();
		int maxNum, repetition, tmpJ = -1;
		double memcheck = 0.5;
		boolean memoryCanbeOptimized = false;

		if (appset.unguidedExpansion) {
			if (RunInd == 0) {
				maxNum = (rr.rules.size() - oldIndexRR) * (rr1.rules.size() - oldIndexRR1);
			} else {
				maxNum = (rr.rules.size() - oldIndexRR) * (rr1.rules.size() - oldIndexRR1)
						- (rr.newRuleIndex - oldIndexRR) * (rr1.newRuleIndex - oldIndexRR1);
			}
		} else {
			maxNum = ((rr.newRuleIndex - oldIndexRR) * (rr1.rules.size() - rr1.newRuleIndex))
					+ ((rr.rules.size() - rr.newRuleIndex) * (rr1.newRuleIndex - oldIndexRR1));
		}

		System.out.println("max number of rules: " + maxNum);

		ArrayList<Rule> negLeft = new ArrayList<>();
		ArrayList<Rule> negRight = new ArrayList<>();

		for (int i = oldIndexRR; i < rr.rules.size(); i++) {
			Rule rtemp = new Rule(rr.rules.get(i), dat, map);
			if (appset.missingValueJSType == 1 || appset.missingValueJSType == 2) {
				rtemp.addElementsMissing(map, dat, 1);
				rr.rules.get(i).addElementsMissing(map, dat, 0);
			}
			negLeft.add(rtemp);
		}

		for (int i = oldIndexRR1; i < rr1.rules.size(); i++) {
			Rule rtemp = new Rule(rr1.rules.get(i), dat, map);
			if (appset.missingValueJSType == 1 || appset.missingValueJSType == 2) {
				rtemp.addElementsMissing(map, dat, 1);
				rr1.rules.get(i).addElementsMissing(map, dat, 0);
			}
			negRight.add(rtemp);
		}

		int numIt = 0;
		int step = maxNum / 100;
		if (step == 0)
			step = 1;
		int newRedescriptions = 0;

		double JSPos = 0.0, JSPosNeg = 0.0, JSNegPos = 0.0;
		for (int i = oldIndexRR; i < rr.rules.size(); i++) {
			repetition = 0;

			for (int j = oldIndexRR1; j < rr1.rules.size(); j++) {
				int EmergencyExit = 0;
				if (appset.unguidedExpansion == true) {
					if (RunInd != 0 && i < rr.newRuleIndex && j < rr1.newRuleIndex)
						continue;
				} else {
					if ((i < rr.newRuleIndex && j < rr1.newRuleIndex)
							|| (i >= rr.newRuleIndex && j >= rr1.newRuleIndex))
						continue;
				}

				for (int it = 0; it < js.length; it++)
					js[it].initialize();
				numIt++;
				try {
					JSPos = js[0].computeJacard(rr.rules.get(i), rr1.rules.get(j));
					if (JSPos > appset.minAddRedJS && js[0].intersectSize < appset.maxSupport
							&& js[0].intersectSize >= appset.minSupport) {
						Redescription tmp = new Redescription(rr.rules.get(i).rule, rr1.rules.get(j).rule, js[0].JS,
								map, dat);
						tmp.computeElements(rr.rules.get(i), rr1.rules.get(j));
						tmp.computeUnion(rr.rules.get(i), rr1.rules.get(j));
						// if(tmp.elements.size()<appset.minSupport ||
						// tmp.elements.size()>appset.maxSupport)
						// continue;

						/*
						 * for(int t=0;t<2;t++){ System.out.println("Validation new redescr"+t);
						 * tmp.closeInterval(dat, map); // long startTime = System.currentTimeMillis();
						 * tmp.createRuleString(map); //System.out.println("Before"); //
						 * System.out.println(redescriptions.get(i).ruleStrings.get(0)); //
						 * System.out.println(redescriptions.get(i).ruleStrings.get(1));
						 * //tmp.minimizeOptimal(dat, map,1);
						 * 
						 * int val= tmp.validate(dat, map); if(val==0){
						 * System.out.println("Rule one string: ");
						 * System.out.println(rr.rules.get(i).rule); TIntIterator
						 * it=rr.rules.get(i).elements.iterator(); while(it.hasNext()){
						 * System.out.print(it.next()+" "); } System.out.println();
						 * System.out.println("Rule two string: ");
						 * System.out.println(rr1.rules.get(j).rule);
						 * it=rr1.rules.get(j).elements.iterator(); while(it.hasNext()){
						 * System.out.print(it.next()+" "); } System.out.println();
						 * System.out.println("Redescription elements"); it=tmp.elements.iterator();
						 * while(it.hasNext()){ System.out.print(it.next()+" "); } System.out.println();
						 * it=tmp.elements.iterator(); while(it.hasNext()){
						 * System.out.print(map.idExample.get(it.next())+" "); } System.out.println();
						 * System.exit(-1); }
						 * 
						 * 
						 * tmp.clearRuleMaps(); }
						 */

						Redescription tmp1 = new Redescription(tmp, dat);
						int found = 0;

						ArrayList<Redescription> toRemove = new ArrayList<>();
						ArrayList<Redescription> toAdd = new ArrayList<>();
						ArrayList<Redescription> toRefine = new ArrayList<>();
						int refInd = 0, refinementFound = 0;
						int refined = 0;
						for (int k = 0; k < redescriptions.size(); k++) {

							long maxMemory = Runtime.getRuntime().maxMemory() / (1024 * 1024);
							long freemem = Runtime.getRuntime().freeMemory() / (1024 * 1024);
							long totalMemory = Runtime.getRuntime().totalMemory() / (1024 * 1024);

							long minMemory = Math.max(10, (long) (0.15 * maxMemory));

							if ((maxMemory - (totalMemory - freemem) < minMemory) && memoryCanbeOptimized == true) {
								EmergencyExit = 1;
								break;
							}

							if (redescriptions.get(k).JS == 1.0) {
								if (redescriptions.get(k).CompareEqual(tmp) == 2 && refinementFound == 0) {
									toRefine.add(redescriptions.get(k));
									refInd = k;
									tmp = redescriptions.get(k);
									refinementFound = 1;
									found = 1;
									refined = 1;
									// break;
								}
								continue;
							}
							int quality = tmp.CompareQuality(redescriptions.get(k));
							found = 0;
							if (quality == -1) {
								continue;
							} else if (quality == 2 && refinementFound == 0) {

								if (tmp1.elements.size() == redescriptions.get(k).elements.size()) {
									redescriptions.get(k).join(tmp1, map, dat);
									if (redescriptions.get(k).JS == 1.0)
										refined = 1;
								} else {
									if (refined == 0) {// rafiniraj dok ne zadovoljiš uvjet
										tmp1.join(redescriptions.get(k), map, dat);
										if (tmp1.JS >= appset.minJS && tmp1.elements.size() <= appset.maxSupport) {
											toAdd.add(tmp1);
											refined = 1;
											// break;
										}
									}
								}

								found = 1;
							} else if (quality == 1 && redescriptions.get(k).JS < 1.0) {
								if (tmp.JS > tmp1.JS)
									redescriptions.get(k).join(tmp, map, dat);
								else
									redescriptions.get(k).join(tmp1, map, dat);
								// found=1;

								if (redescriptions.get(k).elements.size() < appset.minSupport)
									toRemove.add(redescriptions.get(k));

							}
						}

						for (int k = 0; k < toRemove.size(); k++)
							redescriptions.remove(toRemove.get(k));
						if (toRemove.size() > 0) {
							newRedescriptions -= toRemove.size();
							toRemove.clear();
						}

						for (int k = 0; k < toAdd.size(); k++)
							redescriptions.add(toAdd.get(k));
						if (toAdd.size() > 0) {
							newRedescriptions += toAdd.size();
							toAdd.clear();
						}

						if (found == 0) {
							if (tmp.elements.size() >= appset.minSupport && tmp.JS >= appset.minJS
									&& tmp.elements.size() <= appset.maxSupport) {// tmp.JS>0.4 ||
																					// (tmp.elements.size()>1 &&
																					// tmp.JS>0.2)
								redescriptions.add(tmp);
								newRedescriptions++;
							}
						}

						if (toRefine.size() > 0) {

							ArrayList<Integer> rmInd = new ArrayList();
							for (int k = redescriptions.size() - 1; k >= 0; k--) {
								if (k != refInd && redescriptions.get(k).CompareEqual(toRefine.get(0)) == 2)
									rmInd.add(k);
							}
							toRefine.clear();
							refInd = 0;
							refinementFound = 0;
							newRedescriptions -= rmInd.size();
							for (int k = 0; k < rmInd.size(); k++)
								redescriptions.remove((int) rmInd.get(k));
							rmInd.clear();
						}
					} // add negations and disjunction afterwards

					if (appset.rightNegation == true && !(EmergencyExit == 1) && JSPos <= (1.0 - appset.minJS)) {
						// JSPosNeg=js[1].computeJacard(rr.rules.get(i), rr1.rules.get(j), dat, map, 1);
						JSPosNeg = js[1].computeJacard(rr.rules.get(i),
								negRight.get(j - oldIndexRR1)/* rr1.rules.get(j) */);
						if (JSPosNeg >= appset.minJS
								&& js[1].computePval(rr.rules.get(i), negRight.get(j - oldIndexRR1),
										dat/* rr1.rules.get(j), dat,map,1 */) <= appset.maxPval
								&& js[1].intersectSize <= appset.maxSupport) {
							Redescription tmp = new Redescription(rr.rules.get(i).rule, rr1.rules.get(j).rule, js[1].JS,
									map, dat, 1);
							tmp.computeElements(rr.rules.get(i), negRight.get(j - oldIndexRR1), rr.rules.get(i),
									rr1.rules.get(j)/* rr1.rules.get(j) *//* ,dat,map,1 */);
							tmp.computeUnion(rr.rules.get(i),
									negRight.get(j - oldIndexRR1)/* rr1.rules.get(j),dat,map,1 */);
							/*
							 * System.out.println("Is negated right: "+tmp.rightRuleElements.get(0).
							 * isNegated); tmp.closeInterval(dat, map); tmp.validate(dat, map);
							 */
							if (tmp.elements.size() >= appset.minSupport) {// tmp.JS>0.4 || (tmp.elements.size()>1 &&
																			// tmp.JS>0.2)
								redescriptions.add(tmp);
								newRedescriptions++;
							}
						}
					}
					if (appset.leftNegation == true && !(EmergencyExit == 1) && JSPos <= (1.0 - appset.minJS)) {
						// JSNegPos=js[2].computeJacard(rr.rules.get(i), rr1.rules.get(j), dat, map, 2);
						JSNegPos = js[2].computeJacard(/* rr.rules.get(i) */negLeft.get(i - oldIndexRR),
								rr1.rules.get(j)/* , dat, map, 2 */);
						if (JSNegPos >= appset.minJS
								&& js[2].computePval(negLeft.get(i - oldIndexRR)/* rr.rules.get(i) */, rr1.rules.get(j),
										dat/* ,map,2 */) <= appset.maxPval
								&& js[2].intersectSize <= appset.maxSupport) {
							Redescription tmp = new Redescription(rr.rules.get(i).rule, rr1.rules.get(j).rule, js[2].JS,
									map, dat, 2);
							tmp.computeElements(negLeft.get(i - oldIndexRR)/* rr.rules.get(i) */, rr1.rules.get(j),
									rr.rules.get(i), rr1.rules.get(j)/* ,dat,map,2 */);
							tmp.computeUnion(negLeft.get(i - oldIndexRR)/* rr.rules.get(i) */,
									rr1.rules.get(j)/* ,dat,map,2 */);
							/*
							 * System.out.println("Is negated left: "+tmp.leftRuleElements.get(0).isNegated)
							 * ; tmp.closeInterval(dat, map); tmp.validate(dat, map);
							 */
							if (tmp.elements.size() >= appset.minSupport) {// tmp.JS>0.4 || (tmp.elements.size()>1 &&
																			// tmp.JS>0.2)
								redescriptions.add(tmp);
								newRedescriptions++;
							}
						}
					}

					if (numIt % step == 0) {
						System.out.println((((double) numIt / maxNum) * 100) + "% completed...");
					}
					if (numIt == maxNum)
						System.out.println("100% completed!"); // do memory optimization here

					if (numIt % 100 == 0 || EmergencyExit == 1) { // do memory check every 100 iterations

						if (EmergencyExit == 1) {
							j--;
							if (tmpJ != j && repetition == 0)
								tmpJ = j;
							else if (tmpJ == j)
								repetition++;
							else if (tmpJ != j && repetition != 0) {
								tmpJ = j;
								repetition = 0;
							}
						}

						long maxMemory = Runtime.getRuntime().maxMemory() / (1024 * 1024);
						long freemem = Runtime.getRuntime().freeMemory() / (1024 * 1024);
						long totalMemory = Runtime.getRuntime().totalMemory() / (1024 * 1024);
						long minMemory = Math.max(10, (long) (0.15 * maxMemory));
						/*
						 * System.out.println("other mem value: "+((long)(0.02*maxMemory)));
						 * System.out.println("Memory: "+((maxMemory-(totalMemory-freemem))<minMemory));
						 * System.out.println("Amount memory: "+((maxMemory-(totalMemory-freemem))));
						 * System.out.println("Min memory: "+minMemory);
						 * System.out.println("Rules size: "+(rr.rules.size()+rr1.rules.size()));
						 * System.out.println("Redescriptions set size: "+redescriptions.size());
						 * System.out.println("totalMemory-freemem: "+(totalMemory-freemem));
						 */

						/*
						 * if(((maxMemory-(totalMemory-freemem))<Math.max(memcheck, 0.15)*maxMemory) &&
						 * redescriptions.size()!=0){ memcheck=memcheck/2;
						 * 
						 * adaptSet(dat,map); this.filter(appset, rr, rr1,map,dat);
						 * computePVal(dat,map); this.removePVal(appset); this.sortRedescriptions();
						 * double coverage[]={-1.0,-1.0};
						 * this.writeToFileTmp(appset.outFolderPath+"\\"+"RulesTmp.rr", dat, map,
						 * appset); for(int irt=0;irt<redescriptions.size();irt++)
						 * redescriptions.get(irt).clearRuleMaps(); Runtime.getRuntime().gc(); }
						 */

						if (((maxMemory - (totalMemory - freemem)) < minMemory) && memoryCanbeOptimized == true) {

							adaptSet(dat, map, 1);
							/*
							 * for(int ti=0;ti<redescriptions.size();ti++)
							 * redescriptions.get(ti).minimizeOptimal(dat, map, 0);
							 */

							// this.filter(appset, rr, rr1,map,dat);

							// computePVal(dat,map);
							// this.removePVal(appset);
							Runtime.getRuntime().gc();
							maxMemory = Runtime.getRuntime().maxMemory() / (1024 * 1024);
							freemem = Runtime.getRuntime().freeMemory() / (1024 * 1024);
							totalMemory = Runtime.getRuntime().totalMemory() / (1024 * 1024);

							System.out.println(
									"Memory status after filtering: " + ((maxMemory - (totalMemory - freemem))));
							System.out.println("Min memory: " + minMemory);
							System.out.println("Redescription size: " + redescriptions.size());
							if ((this.redescriptions.size() < appset.numRetRed
									&& ((maxMemory - (totalMemory - freemem)) > minMemory))
									|| (((maxMemory - (totalMemory - freemem)) > 1.5 * minMemory) && repetition < 3)) {
								System.out.println("redescription size after filtering: " + redescriptions.size());
								for (int irt = 0; irt < redescriptions.size(); irt++)
									redescriptions.get(irt).clearRuleMaps();
								continue;
							}

							RedescriptionSet rTemp = new RedescriptionSet();
							double weights[] = { 0.2, 0.2, 0.2, 0.2, 0.2 };
							rTemp.createRedescriptionSet(this, weights, appset, dat, map);
							this.redescriptions.clear();

							for (int itm = 0; itm < rTemp.redescriptions.size(); itm++)
								this.redescriptions.add(rTemp.redescriptions.get(itm));

							// this.redescriptions=rTemp.redescriptions;

							for (int irt = 0; irt < redescriptions.size(); irt++)
								redescriptions.get(irt).clearRuleMaps();

							newRedescriptions = redescriptions.size();
							System.out.println("New redescriptions after filtering..." + newRedescriptions);
							EmergencyExit = 0;

							Runtime.getRuntime().gc();

							maxMemory = Runtime.getRuntime().maxMemory() / (1024 * 1024);
							freemem = Runtime.getRuntime().freeMemory() / (1024 * 1024);
							totalMemory = Runtime.getRuntime().totalMemory() / (1024 * 1024);
							/*
							 * if(appset.minJS!=1.0){ appset.minJS=appset.minJS+0.1; if(appset.minJS>1.0)
							 * appset.minJS=1.0;
							 * 
							 * int num=countNumber(appset.minJS);
							 * if((redescriptions.size()-num)>appset.numRetRed){ remove(appset);
							 * Runtime.getRuntime().gc(); } else{
							 * if(appset.numRetRed>redescriptions.size()){ outOfmemory[0]=true; return
							 * newRedescriptions; } else{ remove(appset,appset.numRetRed);
							 * appset.minJS=appset.minJS-0.1; Runtime.getRuntime().gc(); } } } else{
							 * remove(appset); memoryCanbeOptimized=false; Runtime.getRuntime().gc(); }
							 */

							System.out.println("Max memory: " + maxMemory);
							System.out.println("Memory status: " + ((maxMemory - (totalMemory - freemem))));
							System.out.println("Min memory: " + minMemory);

							if ((maxMemory - (totalMemory - freemem)) < minMemory)
								return newRedescriptions;
							System.out.println("Memory status: " + ((maxMemory - (totalMemory - freemem))));
							System.out.println("Changin minJS level");
							System.out.println("New minJS level: " + appset.minJS);
						} else if (((maxMemory - (totalMemory - freemem)) < minMemory)
								&& memoryCanbeOptimized == false) {
							// outOfmemory[0]=true;
							// return newRedescriptions;
						}

						/*
						 * for(int k=0;k<redescriptions.size();k++){
						 * redescriptions.get(k).closeInterval(dat, map);
						 * redescriptions.get(k).minimize(dat, map);
						 * redescriptions.get(k).deleteBounds(); }
						 */
					}
				} catch (java.lang.OutOfMemoryError e) {
					e.printStackTrace();
				}
			}
		}

		if ((appset.leftDisjunction == true || appset.rightDisjunction == true)) {
			System.out.println("Computing disjunctive refinement...");

			step = (redescriptions.size() - oldSize + 1) / 100;
			if ((redescriptions.size() - oldSize + 1) < 100)
				step = (redescriptions.size() - oldSize + 1);

			for (int k = oldSize; k < redescriptions.size(); k++) {
				if (redescriptions.get(k).JS == 1.0)
					continue;

				double joinJS = 0.0, maxJoinJS = 0.0;
				int maxInd = 0;
				int negated = 0;

				if (appset.rightDisjunction == true) {
					// TIntHashSet
					// lRE=redescriptions.get(k).computeElements(redescriptions.get(k).viewElementsLists.get(0),
					// dat, map);
					// TIntHashSet
					// rRE=redescriptions.get(k).computeElements(redescriptions.get(k).viewElementsLists.get(1),
					// dat, map);
					ArrayList<TIntHashSet> sideElems = redescriptions.get(k).computeElementsGen(dat, map);// new
																											// ArrayList<>();//to
																											// make
																											// proper
																											// generalization
					int interCount = js[0].computeGenInterCount(redescriptions.get(k), sideElems, 1);
					// sideElems.add(lRE); sideElems.add(rRE);
					for (int j = oldIndexRR1; j < rr1.rules.size(); j++) {
						// joinJS=js[0].computeRedescriptionRuleElementJacard(redescriptions.get(k),
						// rr1.rules.get(j), lRE, rRE, 1,0, dat, map,appset);
						joinJS = js[0].computeRedescriptionRuleElementJacardGen(redescriptions.get(k), rr1.rules.get(j),
								sideElems, 1, 0, dat, map, appset, interCount);
						if (joinJS > maxJoinJS) {
							maxJoinJS = joinJS;
							maxInd = j;
							negated = 0;
						}

						if (appset.rightNegation == true) {
							// joinJS=js[0].computeRedescriptionRuleElementJacard(redescriptions.get(k),
							// rr1.rules.get(j), lRE, rRE, 1,1, dat, map,appset);
							// joinJS=js[0].computeRedescriptionRuleElementJacardGen(redescriptions.get(k),
							// rr1.rules.get(j), sideElems, 1,1, dat, map,appset,interCount);
							joinJS = js[0].computeRedescriptionRuleElementJacardGen(redescriptions.get(k),
									negRight.get(j - oldIndexRR1)/* rr1.rules.get(j) */, sideElems, 1, 0, dat, map,
									appset, interCount);
							if (joinJS > maxJoinJS) {
								maxJoinJS = joinJS;
								maxInd = j;
								negated = 1;
							}
						}
					}
					// System.out.println("maxJoinJS: "+maxJoinJS);
					if (maxJoinJS > 0.5) {
						/*
						 * System.out.println("max index: "+maxInd);
						 * System.out.println("maxJoinJS: "+maxJoinJS);
						 * System.out.println("negated: "+negated);
						 */
						redescriptions.get(k).disjunctiveJoin(rr1.rules.get(maxInd), appset, dat, map, sideElems, 1,
								negated);
					}
				}
				if (appset.leftDisjunction == true) {
					// TIntHashSet
					// lRE=redescriptions.get(k).computeElements(redescriptions.get(k).viewElementsLists.get(0),
					// dat, map);//to generalize
					// TIntHashSet
					// rRE=redescriptions.get(k).computeElements(redescriptions.get(k).viewElementsLists.get(1),
					// dat, map);
					ArrayList<TIntHashSet> sideElems = redescriptions.get(k).computeElementsGen(dat, map);
					int interCount = js[0].computeGenInterCount(redescriptions.get(k), sideElems, 0);
					// ArrayList<TIntHashSet> sideElems=new ArrayList<>();//to make proper
					// generalization
					// sideElems.add(lRE); sideElems.add(rRE);
					joinJS = 0.0;
					maxJoinJS = 0.0;
					maxInd = 0;
					negated = 0;
					for (int i = oldIndexRR; i < rr.rules.size(); i++) {
						// joinJS=js[0].computeRedescriptionRuleElementJacard(redescriptions.get(k),
						// rr.rules.get(i),lRE,rRE, 0,0, dat, map,appset);
						joinJS = js[0].computeRedescriptionRuleElementJacardGen(redescriptions.get(k), rr.rules.get(i),
								sideElems, 0, 0, dat, map, appset, interCount);
						if (joinJS > maxJoinJS) {
							maxJoinJS = joinJS;
							maxInd = i;
							negated = 0;
						}

						if (appset.leftNegation == true) {
							// joinJS=js[0].computeRedescriptionRuleElementJacard(redescriptions.get(k),
							// rr.rules.get(i),lRE,rRE, 0,1, dat, map,appset);
							// joinJS=js[0].computeRedescriptionRuleElementJacardGen(redescriptions.get(k),
							// rr.rules.get(i),sideElems, 0,1, dat, map,appset,interCount);
							joinJS = js[0].computeRedescriptionRuleElementJacardGen(redescriptions.get(k),
									negLeft.get(i - oldIndexRR)/* rr.rules.get(i) */, sideElems, 0, 0, dat, map, appset,
									interCount);
							if (joinJS > maxJoinJS) {
								maxJoinJS = joinJS;
								maxInd = i;
								negated = 1;
							}
						}
					}
					// System.out.println("maxJoinJS: "+maxJoinJS);
					if (maxJoinJS > 0.5) {
						/*
						 * System.out.println("max index: "+maxInd);
						 * System.out.println("maxJoinJS: "+maxJoinJS);
						 * System.out.println("negated: "+negated);
						 */
						redescriptions.get(k).disjunctiveJoin(rr.rules.get(maxInd), appset, dat, map, sideElems, 0,
								negated);
					}
				}

				if ((k + 1) % step == 0) {
					System.out.println((((double) (k + 1 - oldSize) / (redescriptions.size() - oldSize + 1)) * 100)
							+ "% completed...");
					System.out.println("num redescriptions: " + redescriptions.size());
					// Runtime.getRuntime().gc();
				}
			}
		}

		/*
		 * for(int t=0;t<2;t++){ System.out.println("Validation"+t); adaptSet(dat, map);
		 * 
		 * for(int i=0;i<redescriptions.size();i++){ redescriptions.get(i).validate(dat,
		 * map); }
		 * 
		 * for(int i=0;i<redescriptions.size();i++)
		 * redescriptions.get(i).clearRuleMaps(); }
		 */

		if (newRedescriptions == 0)// should be removed
			newRedescriptions = 1;

		return newRedescriptions;
	}

	void computeLift(DataSetCreator dat, Mappings map) {

		for (int i = 0; i < redescriptions.size(); i++) {
			double prod = 1.0;
			for (int j = 0; j < redescriptions.get(i).numUsedViews(); j++) {
				TIntHashSet t = redescriptions.get(i).computeElements(redescriptions.get(i).viewElementsLists.get(j),
						dat, map);
				prod *= ((double) t.size() / (double) dat.numExamples);
			}

			// System.out.println("support: "+redescriptions.get(i).elements.size());
			// System.out.println("product: "+prod);
			redescriptions.get(i).lift = (double) (redescriptions.get(i).elements.size() / ((double) dat.numExamples))
					/ prod;

		}

	}

	double computeRedescriptionSetScoreGen(double weights[], int jsType, double coverage[], DataSetCreator dat,
			ApplicationSettings appset, Mappings map) {

		if (redescriptions.size() == 0)
			return 1.0;

		double score = 0.0, minPval = 17.0, ruleSize = 0.0;
		Jacard js = new Jacard();
		double AEJ = 0.0, AAJ = 0.0, avrgJS = 0.0, avrPval = 0.0, avrStability = 0.0;
		int numIt = 0, step = (redescriptions.size() * (redescriptions.size() - 1)) / 100;
		if (step == 0)
			step = 1;
		HashSet<Integer> elements = new HashSet<>();
		HashSet<Integer> attributes = new HashSet<>();

		for (int i = 0; i < redescriptions.size() - 1; i++) {
			avrStability += (redescriptions.get(i).JSOpt - redescriptions.get(i).JSPes);
			for (int j = i + 1; j < redescriptions.size(); j++) {
				AEJ += js.computeRedescriptionElementJacard(redescriptions.get(i), redescriptions.get(j));
				AAJ += js.computeAttributeJacard(redescriptions.get(i), redescriptions.get(j), dat);
				numIt++;
			}
			if (jsType == 0)
				avrgJS += (1.0 - redescriptions.get(i).JS);
			else if (jsType == 1)
				avrgJS += (1.0 - redescriptions.get(i).JSPes);
			else if (jsType == 2)
				avrgJS += (1.0 - redescriptions.get(i).JSOpt);
			else if (jsType == 3)
				avrgJS += (1.0 - redescriptions.get(i).JSGR);
			double scorePV = Math.log10(redescriptions.get(i).pVal) / minPval + 1.0;
			if (redescriptions.get(i).pVal == 0.0)
				scorePV = 0.0;
			avrPval += scorePV;

			ArrayList<TIntHashSet> attrL = redescriptions.get(i)
					.computeAttributes(redescriptions.get(i).viewElementsLists, dat);// generalize
			// TIntHashSet
			// attrR=redescriptions.get(i).computeAttributes(redescriptions.get(i).viewElementsLists.get(1),
			// dat);

			int attrS = 0;

			for (int k = 0; k < attrL.size(); k++)
				attrS += attrL.get(k).size();

			// ruleSize+=((double)(attrL.size()+attrR.size()))/dat.schema.getNbAttributes();

			if (attrS > appset.ruleSizeNormalization)
				ruleSize += 1;
			else
				ruleSize += ((double) (attrS)) / appset.ruleSizeNormalization /* dat.schema.getNbAttributes() */;

			if (numIt % step == 0)
				System.out.println((((double) numIt / (redescriptions.size() * (redescriptions.size() - 1))) * 100)
						+ "% completed...");
			if (numIt == (redescriptions.size() * (redescriptions.size() - 1)))
				System.out.println("100% completed!");

			TIntIterator it = redescriptions.get(i).elements.iterator();

			while (it.hasNext()) {
				int s = it.next();
				elements.add(s);
			}

			for (int k = 0; k < attrL.size(); k++) {
				it = attrL.get(k).iterator();

				while (it.hasNext()) {
					int attr = it.next();
					attributes.add(attr);
				}

			}
		}

		avrStability += (redescriptions.get(redescriptions.size() - 1).JSOpt
				- redescriptions.get(redescriptions.size() - 1).JSPes);
		avrgJS += (1.0 - redescriptions.get(redescriptions.size() - 1).JS);
		double scorePV = Math.log10(redescriptions.get(redescriptions.size() - 1).pVal) / minPval + 1.0;
		if (redescriptions.get(redescriptions.size() - 1).pVal == 0.0)
			scorePV = 0.0;
		avrPval += scorePV;

		ArrayList<TIntHashSet> attrL = redescriptions.get(redescriptions.size() - 1)
				.computeAttributes(redescriptions.get(redescriptions.size() - 1).viewElementsLists, dat);// generalize
		// TIntHashSet
		// attrR=redescriptions.get(redescriptions.size()-1).computeAttributes(redescriptions.get(redescriptions.size()-1).rightRuleElements,
		// dat);

		int attrS = 0;

		for (int k = 0; k < attrL.size(); k++)
			attrS += attrL.get(k).size();

		if (attrS > appset.ruleSizeNormalization)
			ruleSize += 1;
		else
			ruleSize += ((double) (attrS)) / appset.ruleSizeNormalization /* dat.schema.getNbAttributes() */;

		TIntIterator it = redescriptions.get(redescriptions.size() - 1).elements.iterator();

		while (it.hasNext()) {
			int s = it.next();
			elements.add(s);
		}

		/*
		 * for(int s:redescriptions.get(redescriptions.size()-1).elements)
		 * elements.add(s);
		 */
		for (int k = 0; k < attrL.size(); k++)
			it = attrL.get(k).iterator();
		while (it.hasNext()) {
			attributes.add(it.next());
		}

		coverage[0] = (double) elements.size() / dat.numExamples;
		coverage[1] = (double) attributes.size() / dat.schema.getNbAttributes();

		AEJ /= ((redescriptions.size() * (redescriptions.size() - 1)) / 2);
		AAJ /= ((redescriptions.size() * (redescriptions.size() - 1)) / 2);
		avrgJS /= redescriptions.size();
		avrPval /= redescriptions.size();

		/*
		 * if(ruleSize<appset.ruleSizeNormalization)
		 * ruleSize/=(double)appset.ruleSizeNormalization;//redescriptions.size(); else
		 * ruleSize=1.0;
		 */
		ruleSize /= redescriptions.size();
		avrStability /= redescriptions.size();

		aJS = avrgJS;
		aEJS = AEJ;
		aAJS = AAJ;
		aPvalSc = avrPval;
		aRS = ruleSize;
		aRSt = avrStability;

		System.out.println("avrgJS: " + avrgJS);
		System.out.println("avrPval: " + avrPval);
		System.out.println("AEJ: " + AEJ);
		System.out.println("AAJ: " + AAJ);
		System.out.println("Rule size: " + ruleSize);
		System.out.println("avrgStability: " + avrStability);
		score = weights[0] * avrgJS + weights[1] * avrPval + weights[2] * AEJ + weights[3] * AAJ + weights[4] * ruleSize
				+ weights[5] * avrStability;

		return score;
	}

	double computeRedescriptionSetScoreGenBaseline(double weights[], int jsType, DataSetCreator dat,
			ApplicationSettings appset) {

		if (redescriptions.size() == 0)
			return 1.0;

		double score = 0.0, minPval = 17.0, ruleSize = 0.0;
		Jacard js = new Jacard();
		double AEJ = 0.0, AAJ = 0.0, avrgJS = 0.0, avrPval = 0.0, avrStability = 0.0;
		int numIt = 0, step = (redescriptions.size() * (redescriptions.size() - 1)) / 100;
		if (step == 0)
			step = 1;

		for (int i = 0; i < redescriptions.size() - 1; i++) {
			avrStability += (redescriptions.get(i).JSOpt - redescriptions.get(i).JSPes);
			for (int j = i + 1; j < redescriptions.size(); j++) {
				AEJ += js.computeRedescriptionElementJacard(redescriptions.get(i), redescriptions.get(j));

				AAJ += js.computeAttributeJacardBaseline(redescriptions.get(i), redescriptions.get(j));
				numIt++;
			}
			if (jsType == 0)
				avrgJS += (1.0 - redescriptions.get(i).JS);
			else if (jsType == 1)
				avrgJS += (1.0 - redescriptions.get(i).JSPes);
			else if (jsType == 2)
				avrgJS += (1.0 - redescriptions.get(i).JSOpt);
			else if (jsType == 3)
				avrgJS += (1.0 - redescriptions.get(i).JSGR);
			double scorePV = Math.log10(redescriptions.get(i).pVal) / minPval + 1.0;
			if (redescriptions.get(i).pVal == 0.0)
				scorePV = 0.0;
			avrPval += scorePV;

		}

		avrStability += (redescriptions.get(redescriptions.size() - 1).JSOpt
				- redescriptions.get(redescriptions.size() - 1).JSPes);
		avrgJS += (1.0 - redescriptions.get(redescriptions.size() - 1).JS);
		double scorePV = Math.log10(redescriptions.get(redescriptions.size() - 1).pVal) / minPval + 1.0;
		if (redescriptions.get(redescriptions.size() - 1).pVal == 0.0)
			scorePV = 0.0;
		avrPval += scorePV;

		ArrayList<TIntHashSet> attrL = redescriptions.get(redescriptions.size() - 1)
				.computeAttributes(redescriptions.get(redescriptions.size() - 1).viewElementsLists, dat);// generalize
		// TIntHashSet
		// attrR=redescriptions.get(redescriptions.size()-1).computeAttributes(redescriptions.get(redescriptions.size()-1).rightRuleElements,
		// dat);

		int attrS = 0;

		for (int k = 0; k < attrL.size(); k++)
			attrS += attrL.get(k).size();

		if (attrS > appset.ruleSizeNormalization)
			ruleSize += 1;
		else
			ruleSize += ((double) (attrS)) / appset.ruleSizeNormalization /* dat.schema.getNbAttributes() */;

		TIntIterator it = redescriptions.get(redescriptions.size() - 1).elements.iterator();

		AEJ /= ((redescriptions.size() * (redescriptions.size() - 1)) / 2);
		AAJ /= ((redescriptions.size() * (redescriptions.size() - 1)) / 2);
		avrgJS /= redescriptions.size();
		avrPval /= redescriptions.size();

		/*
		 * if(ruleSize<appset.ruleSizeNormalization)
		 * ruleSize/=(double)appset.ruleSizeNormalization;//redescriptions.size(); else
		 * ruleSize=1.0;
		 */
		ruleSize /= redescriptions.size();
		avrStability /= redescriptions.size();

		aJS = avrgJS;
		aEJS = AEJ;
		aAJS = AAJ;
		aPvalSc = avrPval;
		aRS = ruleSize;
		aRSt = avrStability;

		System.out.println("avrgJS: " + avrgJS);
		System.out.println("avrPval: " + avrPval);
		System.out.println("AEJ: " + AEJ);
		System.out.println("AAJ: " + AAJ);
		System.out.println("Rule size: " + ruleSize);
		System.out.println("avrgStability: " + avrStability);
		score = weights[0] * avrgJS + weights[1] * avrPval + weights[2] * AEJ + weights[3] * AAJ + weights[4] * ruleSize
				+ weights[5] * avrStability;

		return score;
	}

	public int createGuidedNoJoinExt(RuleReader rr, RuleReader rr1, Jacard js[], ApplicationSettings appset,
			int oldIndexRR, int oldIndexRR1, int RunInd, boolean outOfmemory[], Mappings map, DataSetCreator dat) {

		int oldSize = redescriptions.size();
		System.out.println("Using guided expansion without join procedure!");
		boolean memoryCanbeOptimized = true;
		int maxNum = 0;

		ArrayList<Rule> negLeft = new ArrayList<>();
		ArrayList<Rule> negRight = new ArrayList<>();

		for (int i = oldIndexRR; i < rr.rules.size(); i++) {
			Rule rtemp = new Rule(rr.rules.get(i), dat, map);
			negLeft.add(rtemp);
		}

		for (int i = oldIndexRR1; i < rr1.rules.size(); i++) {
			Rule rtemp = new Rule(rr1.rules.get(i), dat, map);
			negRight.add(rtemp);
		}

		if (appset.unguidedExpansion) {
			if (RunInd == 0) {
				maxNum = (rr.rules.size() - oldIndexRR) * (rr1.rules.size() - oldIndexRR1);
			} else {
				maxNum = (rr.rules.size() - oldIndexRR) * (rr1.rules.size() - oldIndexRR1)
						- (rr.newRuleIndex - oldIndexRR) * (rr1.newRuleIndex - oldIndexRR1);
			}
		} else {
			maxNum = ((rr.newRuleIndex - oldIndexRR) * (rr1.rules.size() - rr1.newRuleIndex))
					+ ((rr.rules.size() - rr.newRuleIndex) * (rr1.newRuleIndex - oldIndexRR1));
		}

		System.out.println("max number of rules: " + maxNum);

		int numIt = 0;
		int step = maxNum / 100;

		if (step == 0)
			step = 1;

		int newRedescriptions = 0;

		for (int i = oldIndexRR/* rr.newRuleIndex-addon */; i < rr.rules.size(); i++) {
			// long startTime = System.currentTimeMillis();
			for (int j = oldIndexRR1/* rr1.newRuleIndex-addon1 */; j < rr1.rules.size(); j++) {

				if (appset.unguidedExpansion == true) {
					if (RunInd != 0 && i < rr.newRuleIndex && j < rr1.newRuleIndex)
						continue;
				} else {
					if ((i < rr.newRuleIndex && j < rr1.newRuleIndex)
							|| (i >= rr.newRuleIndex && j >= rr1.newRuleIndex))
						continue;
				}

				for (int jinit = 0; jinit < js.length; jinit++)
					js[jinit].initialize();
				numIt++;

				double JSPos = 0.0, JSPosNeg = 0.0, JSNegPos = 0.0;
				JSPos = js[0].computeJacard(rr.rules.get(i), rr1.rules.get(j));

				if (JSPos >= appset.minJS && js[0].computePval(rr.rules.get(i), rr1.rules.get(j), dat) <= appset.maxPval
						&& js[0].intersectSize >= appset.minSupport && js[0].intersectSize <= appset.maxSupport) {
					Redescription tmp = new Redescription(rr.rules.get(i).rule, rr1.rules.get(j).rule, js[0].JS, map,
							dat, 0);
					tmp.computeElements(rr.rules.get(i), rr1.rules.get(j));
					tmp.computeUnion(rr.rules.get(i), rr1.rules.get(j));

					if (tmp.elements.size() >= appset.minSupport && tmp.elements.size() <= appset.maxSupport) {// tmp.JS>0.4
																												// ||
																												// (tmp.elements.size()>1
																												// &&
																												// tmp.JS>0.2)
						redescriptions.add(tmp);
						newRedescriptions++;
					} // fix negation and validation!
				}
				if (appset.rightNegation == true && JSPos <= (1.0 - appset.minJS)) {
					JSPosNeg = js[1].computeJacard(rr.rules.get(i),
							negRight.get(j - oldIndexRR1)/* rr1.rules.get(j)*, dat, map, 1 */);
					if (JSPosNeg >= appset.minJS
							&& js[1].computePval(rr.rules.get(i), negRight.get(j - oldIndexRR1) /* rr1.rules.get(j) */,
									dat/* ,map,1 */) <= appset.maxPval
							&& js[1].intersectSize >= appset.minSupport && js[1].intersectSize <= appset.maxSupport) {
						Redescription tmp = new Redescription(rr.rules.get(i).rule, rr1.rules.get(j).rule, js[1].JS,
								map, dat, 1);
						// tmp.computeElements(rr.rules.get(i), negRight.get(j-oldIndexRR1)
						// /*rr1.rules.get(j)*//*,dat,map,1*/);
						tmp.computeElements(rr.rules.get(i), negRight.get(j - oldIndexRR1), rr.rules.get(i),
								rr1.rules.get(j)/* rr1.rules.get(j) *//* ,dat,map,1 */);
						tmp.computeUnion(rr.rules.get(i),
								negRight.get(j - oldIndexRR1)/* rr1.rules.get(j) *//* ,dat,map,1 */);
						if (tmp.elements.size() >= appset.minSupport && tmp.elements.size() <= appset.maxSupport) {// tmp.JS>0.4
																													// ||
																													// (tmp.elements.size()>1
																													// &&
																													// tmp.JS>0.2)
							redescriptions.add(tmp);
							newRedescriptions++;
						}
					}
				}
				if (appset.leftNegation == true && JSPos <= (1.0 - appset.minJS)) {
					JSNegPos = js[2].computeJacard(negLeft.get(i - oldIndexRR)/* rr.rules.get(i) */,
							rr1.rules.get(j)/* , dat, map, 2 */);
					if (JSNegPos >= appset.minJS
							&& js[2].computePval(negLeft.get(i - oldIndexRR)/* rr.rules.get(i) */, rr1.rules.get(j),
									dat/* ,map,2 */) <= appset.maxPval
							&& js[2].intersectSize >= appset.minSupport && js[2].intersectSize <= appset.maxSupport) {
						Redescription tmp = new Redescription(rr.rules.get(i).rule, rr1.rules.get(j).rule, js[2].JS,
								map, dat, 2);
						// tmp.computeElements(negLeft.get(i-oldIndexRR)/*rr.rules.get(i)*/,
						// rr1.rules.get(j)/*,dat,map,2*/);
						tmp.computeElements(negLeft.get(i - oldIndexRR), rr1.rules.get(j), rr.rules.get(i),
								rr1.rules.get(j)/* rr1.rules.get(j) *//* ,dat,map,1 */);
						tmp.computeUnion(negLeft.get(i - oldIndexRR)/* rr.rules.get(i) */,
								rr1.rules.get(j)/* ,dat,map,2 */);
						if (tmp.elements.size() >= appset.minSupport && tmp.elements.size() <= appset.maxSupport) {// tmp.JS>0.4
																													// ||
																													// (tmp.elements.size()>1
																													// &&
																													// tmp.JS>0.2)
							redescriptions.add(tmp);
							newRedescriptions++;
						}
					}
				}
				if (numIt % step == 0) {
					System.out.println((((double) numIt / maxNum) * 100) + "% completed...");
					System.out.println("num redescriptions: " + redescriptions.size());
					// Runtime.getRuntime().gc();
				}
				if (numIt == maxNum)
					System.out.println("100% completed!");

				long maxMemory = Runtime.getRuntime().maxMemory() / (1024 * 1024);
				long freemem = Runtime.getRuntime().freeMemory() / (1024 * 1024);
				long totalMemory = Runtime.getRuntime().totalMemory() / (1024 * 1024);

				long minMemory = Math.max(10, (long) 0.15 * maxMemory);

				if ((maxMemory - (totalMemory - freemem) < minMemory) && memoryCanbeOptimized == true) {
					// this.filter(appset, rr, rr1, map, dat);

					System.out.println("Memory status: " + ((maxMemory - (totalMemory - freemem))));
					System.out.println("Min memory: " + minMemory);

					this.filter(appset, rr, rr1, map, dat);
					computePVal(dat, map);
					RedescriptionSet rTemp = new RedescriptionSet();
					double weights[] = { 0.2, 0.2, 0.2, 0.2, 0.2 };
					rTemp.createRedescriptionSet(this, weights, appset, dat, map);
					this.redescriptions.clear();

					for (int itm = 0; itm < rTemp.redescriptions.size(); itm++)
						this.redescriptions.add(rTemp.redescriptions.get(itm));

					for (int irt = 0; irt < redescriptions.size(); irt++)
						redescriptions.get(irt).clearRuleMaps();

					newRedescriptions = redescriptions.size();

					Runtime.getRuntime().gc();

					System.out.println("Memory status: " + ((maxMemory - (totalMemory - freemem))));
					System.out.println("Changing minJS level");
					System.out.println("New minJS level: " + appset.minJS);
				} else if ((maxMemory - (totalMemory - freemem) < minMemory) && memoryCanbeOptimized == false) {
					outOfmemory[0] = true;
					return newRedescriptions;
				}
				// }

			}
		}

		// this.remove(appset);

		if ((appset.leftDisjunction == true || appset.rightDisjunction == true)) {
			System.out.println("Computing disjunctive refinement...");

			step = (redescriptions.size() - oldSize) / 100;
			if ((redescriptions.size() - oldSize) < 100)
				step = (redescriptions.size() - oldSize);

			for (int k = oldSize; k < redescriptions.size(); k++) {
				if (redescriptions.get(k).JS == 1.0)
					continue;

				double joinJS = 0.0, maxJoinJS = 0.0;
				int maxInd = 0;
				int negated = 0;

				if (appset.rightDisjunction == true) {
					// TIntHashSet
					// lRE=redescriptions.get(k).computeElements(redescriptions.get(k).viewElementsLists.get(0),
					// dat, map);
					// TIntHashSet
					// rRE=redescriptions.get(k).computeElements(redescriptions.get(k).viewElementsLists.get(1),
					// dat, map);
					ArrayList<TIntHashSet> sideElems = redescriptions.get(k).computeElementsGen(dat, map);// new
																											// ArrayList<>();//to
																											// make
																											// proper
																											// generalization
					int interCount = js[0].computeGenInterCount(redescriptions.get(k), sideElems, 1);
					// sideElems.add(lRE); sideElems.add(rRE);
					for (int j = oldIndexRR1; j < rr1.rules.size(); j++) {
						// joinJS=js[0].computeRedescriptionRuleElementJacard(redescriptions.get(k),
						// rr1.rules.get(j), lRE, rRE, 1,0, dat, map,appset);
						joinJS = js[0].computeRedescriptionRuleElementJacardGen(redescriptions.get(k), rr1.rules.get(j),
								sideElems, 1, 0, dat, map, appset, interCount);
						if (joinJS > maxJoinJS) {
							maxJoinJS = joinJS;
							maxInd = j;
							negated = 0;
						}

						if (appset.rightNegation == true) {
							// joinJS=js[0].computeRedescriptionRuleElementJacard(redescriptions.get(k),
							// rr1.rules.get(j), lRE, rRE, 1,1, dat, map,appset);
							// joinJS=js[0].computeRedescriptionRuleElementJacardGen(redescriptions.get(k),
							// rr1.rules.get(j), sideElems, 1,1, dat, map,appset,interCount);
							joinJS = js[0].computeRedescriptionRuleElementJacardGen(redescriptions.get(k),
									negRight.get(j - oldIndexRR1)/* rr1.rules.get(j) */, sideElems, 1, 0, dat, map,
									appset, interCount);
							if (joinJS > maxJoinJS) {
								maxJoinJS = joinJS;
								maxInd = j;
								negated = 1;
							}
						}
					}
					// System.out.println("maxJoinJS: "+maxJoinJS);
					if (maxJoinJS > 0.5) {
						/*
						 * System.out.println("max index: "+maxInd);
						 * System.out.println("maxJoinJS: "+maxJoinJS);
						 * System.out.println("negated: "+negated);
						 */
						redescriptions.get(k).disjunctiveJoin(rr1.rules.get(maxInd), appset, dat, map, sideElems, 1,
								negated);
					}
				}
				if (appset.leftDisjunction == true) {
					// TIntHashSet
					// lRE=redescriptions.get(k).computeElements(redescriptions.get(k).viewElementsLists.get(0),
					// dat, map);//to generalize
					// TIntHashSet
					// rRE=redescriptions.get(k).computeElements(redescriptions.get(k).viewElementsLists.get(1),
					// dat, map);
					ArrayList<TIntHashSet> sideElems = redescriptions.get(k).computeElementsGen(dat, map);
					int interCount = js[0].computeGenInterCount(redescriptions.get(k), sideElems, 0);
					// ArrayList<TIntHashSet> sideElems=new ArrayList<>();//to make proper
					// generalization
					// sideElems.add(lRE); sideElems.add(rRE);
					joinJS = 0.0;
					maxJoinJS = 0.0;
					maxInd = 0;
					negated = 0;
					for (int i = oldIndexRR; i < rr.rules.size(); i++) {
						// joinJS=js[0].computeRedescriptionRuleElementJacard(redescriptions.get(k),
						// rr.rules.get(i),lRE,rRE, 0,0, dat, map,appset);
						joinJS = js[0].computeRedescriptionRuleElementJacardGen(redescriptions.get(k), rr.rules.get(i),
								sideElems, 0, 0, dat, map, appset, interCount);
						if (joinJS > maxJoinJS) {
							maxJoinJS = joinJS;
							maxInd = i;
							negated = 0;
						}

						if (appset.leftNegation == true) {
							// joinJS=js[0].computeRedescriptionRuleElementJacard(redescriptions.get(k),
							// rr.rules.get(i),lRE,rRE, 0,1, dat, map,appset);
							// joinJS=js[0].computeRedescriptionRuleElementJacardGen(redescriptions.get(k),
							// rr.rules.get(i),sideElems, 0,1, dat, map,appset,interCount);
							joinJS = js[0].computeRedescriptionRuleElementJacardGen(redescriptions.get(k),
									negLeft.get(i - oldIndexRR)/* rr.rules.get(i) */, sideElems, 0, 0, dat, map, appset,
									interCount);
							if (joinJS > maxJoinJS) {
								maxJoinJS = joinJS;
								maxInd = i;
								negated = 1;
							}
						}
					}
					// System.out.println("maxJoinJS: "+maxJoinJS);
					if (maxJoinJS > 0.5) {
						/*
						 * System.out.println("max index: "+maxInd);
						 * System.out.println("maxJoinJS: "+maxJoinJS);
						 * System.out.println("negated: "+negated);
						 */
						redescriptions.get(k).disjunctiveJoin(rr.rules.get(maxInd), appset, dat, map, sideElems, 0,
								negated);
					}
				}

				if ((k + 1) % step == 0) {
					System.out.println(
							(((double) (k + 1) / (redescriptions.size() - oldSize + 1)) * 100) + "% completed...");
					System.out.println("num redescriptions: " + redescriptions.size());
					Runtime.getRuntime().gc();
				}
			}
		}

		/*
		 * for(int t=0;t<2;t++){ System.out.println("Validation"+t); this.adaptSet(dat,
		 * map); for(int i=0;i<redescriptions.size();i++)
		 * redescriptions.get(i).validate(dat, map);
		 * 
		 * for(int i=0;i<redescriptions.size();i++)
		 * redescriptions.get(i).clearRuleMaps(); }
		 */

		// return newRedescriptions;
		return 1;
	}

	private void setParameters(RuleReader rr, RuleReader rr1, Jacard[] js, ApplicationSettings appset, int oldIndexRR,
			int oldIndexRR1, int runInd, boolean[] outOfmemory, Mappings map, DataSetCreator dat, int[] elemFreq,
			int[] attrFreq, ArrayList<Double> redScore, ArrayList<Double> redScoreAt, ArrayList<Double> redDistCoverage,
			ArrayList<Double> redDistCoverageAt, ArrayList<Double> redDistNetwork, ArrayList<Double> targetAtScore,
			double[] statistics, ArrayList<Double> maxDiffScoreDistribution, NHMCDistanceMatrix mat,
			int preferenceRow) {
		this.rr = rr;
		this.rr1 = rr1;
		this.js = js;
		this.appset = appset;
		this.oldIndexRR = oldIndexRR;
		this.oldIndexRR1 = oldIndexRR1;
		this.runInd = runInd;
		this.outOfmemory = outOfmemory;
		this.map = map;
		this.dat = dat;
		this.elemFreq = elemFreq;
		this.attrFreq = attrFreq;
		this.redScore = redScore;
		this.redScoreAt = redScoreAt;
		this.redDistCoverage = redDistCoverage;
		this.redDistCoverageAt = redDistCoverageAt;
		this.redDistNetwork = redDistNetwork;
		this.targetAtScore = targetAtScore;
		this.statistics = statistics;
		this.maxDiffScoreDistribution = maxDiffScoreDistribution;
		this.mat = mat;
		this.preferenceRow = preferenceRow;
	}

	private int checkAndTryAdd(Rule rule, Rule target, Jacard tJS, double minJaccard, int i, Cycle cycle, boolean isRR) {
//		double jaccard = tJS.computeJacard(rule, target);
		tJS.JS = TargetRulesMap.getmapaJaccardova(cycle).get(isRR ? rule.getRuleId() : target.getRuleId()).get(i);
//		double izracunatiSumWeight = (rule.elements.size() + target.elements.size() - tJS.intersectSize);
//		double aproxSumWeight = TargetRulesMap.getmapaPVrijednosti(cycle).get(isRR ? rule.getRuleId() : target.getRuleId()).get(i);
		
//		if(tJS.intersectSize != approxJaccard || izracunatiSumWeight != aproxSumWeight/*&& Math.abs(jaccard - approxJaccard) > 0.001*/) {
//			System.out.println("POZITIVNO X POZITIVNO NE RADI");
//			System.out.println(isRR ? "Prvo stablo" : "Drugo stablo");
//			System.out.println("Izračunati Jaccard: " + tJS.intersectSize);
//			System.out.println("Aprox Jaccard: " + approxJaccard);
//			
//			
//			System.out.println("IZRAČUNATI SUMWEIGHT: " + izracunatiSumWeight);
//			System.out.println("APPROX SUMWEIGHT: " + aproxSumWeight);
//			
////			System.out.println("IZRAČUNATA UNIJA: " + (rule.elements.size() + target.elements.size() - tJS.intersectSize));
//			
//			
//			System.out.println("Had missing: " + (isRR ? rule.hadMissing : target.hadMissing));
//			System.out.println(rule);
//			System.out.println(target);
//			System.out.println();
//			throw new IllegalArgumentException("Iteracija:" + SupplementingRandomForest.getNumIter());
//		} 

//		if (jaccard >= minJaccard && tJS.computePval(rule, target, dat) <= appset.maxPval
//				&& tJS.intersectSize >= appset.minSupport && tJS.intersectSize <= appset.maxSupport) {
			Redescription tmp = new Redescription(rule.rule, target.rule, tJS.JS, map, dat, 0);
			tmp.computeElements(rule, target);
			tmp.computeUnion(rule, target);

			if (tmp.elements.size() >= appset.minSupport && tmp.elements.size() <= appset.maxSupport) {

				return tryAdd(tmp, tJS, false);

			}
			return 1;
//		}
//		System.out.println("POZITIVNI S POZITIVNIM NE RADI NE ZBOG JACCARD");
//		System.out.println("Izračunati pValue: " + tJS.computePval(rule,  target,  dat));
//		System.out.println("Aprox PValue: " + TargetRulesMap.getmapaPVrijednosti(cycle).get(isRR ? rule.getRuleId() : target.getRuleId()).get(i));
//		throw new IllegalArgumentException("Iteracija:" + SupplementingRandomForest.getNumIter());
//		return 0;
	}

	private int checkAndTryAddNegatedLeft(Rule ruleToBeNegated, Rule target, Rule negatedRule, Jacard tJS, double minJaccard, int i, Cycle cycle, boolean isRR) {
		
//		double jaccard = tJS.computeJacard(negatedRule, target);
		tJS.JS = TargetRulesMap.getmapaJaccardova(cycle).get(isRR ? -ruleToBeNegated.getRuleId() : target.getRuleId()).get(i);
//		double izracunatiSumWeight = (negatedRule.elements.size() + target.elements.size() - tJS.intersectSize);
//		double aproxSumWeight = TargetRulesMap.getmapaPVrijednosti(cycle).get(isRR ? -ruleToBeNegated.getRuleId() : target.getRuleId()).get(i);
		
//		if(tJS.intersectSize != approxJaccard || izracunatiSumWeight != aproxSumWeight) {
//			if(Math.abs(tJS.intersectSize - approxJaccard) != Math.abs(izracunatiSumWeight - aproxSumWeight)) {
//				System.out.println("Razlika je različita od dva");
//			}
//			System.out.println("NEGIRANI RULE");
//			System.out.println("DRUGI JE TARGET");
//			System.out.println(isRR ? "Prvo stablo" : "Drugo stablo");
//			System.out.println("Izračunati Jaccard: " + jaccard);
//			System.out.println("Izračunati presjek: " + tJS.intersectSize);
//			System.out.println("Approx presjek: " + approxJaccard);
//			
//			System.out.println("IZRAČUNATA UNIJA: " + izracunatiSumWeight);
//			System.out.println("APPROX UNIJA: " + aproxSumWeight);
//
//			System.out.println(ruleToBeNegated);
//			System.out.println(target);
//			System.out.println();
//			throw new IllegalArgumentException("Iteracija:" + SupplementingRandomForest.getNumIter());
//		}
		
//		if (jaccard >= minJaccard && tJS.computePval(target, negatedRule, dat) <= appset.maxPval
//				&& tJS.intersectSize >= appset.minSupport && tJS.intersectSize <= appset.maxSupport) {

			Redescription tmp = new Redescription(ruleToBeNegated.rule, target.rule, tJS.JS, map, dat, 2);
			tmp.computeElements(negatedRule, target, ruleToBeNegated, target);
			tmp.computeUnion(negatedRule, target);

			if (tmp.elements.size() >= appset.minSupport && tmp.elements.size() <= appset.maxSupport) {
				tryAdd(tmp, tJS, false);
			}
			return 1;
//		}
//		System.out.println("NEGIRANO PRAVILO X TARGET NE PROLAZI NE ZBOG JACCARDA");
//		System.out.println();
//		throw new IllegalArgumentException("Iteracija:" + SupplementingRandomForest.getNumIter());
//		return 0;
	}

	private int checkAndTryAddNegatedRight(Rule rule, Rule targetToBeNegated, Rule negatedTarget, Jacard tJS, double minJaccard, int i, Cycle cycle, boolean isRR) {
		
//		double jaccard = tJS.computeJacard(rule, negatedTarget);

		tJS.JS = TargetRulesMap.getmapaJaccardova(cycle).get(isRR ? rule.getRuleId() : -targetToBeNegated.getRuleId()).get(i);

//		double izracunatiSumWeight = (rule.elements.size() + negatedTarget.elements.size() - tJS.intersectSize);
//		double pValue = TargetRulesMap.getmapaPVrijednosti(cycle).get(isRR ? rule.getRuleId() : -targetToBeNegated.getRuleId()).get(i);
		
//		if(tJS.intersectSize != approxJaccard || izracunatiSumWeight != aproxSumWeight) {
//			if(Math.abs(tJS.intersectSize - approxJaccard) != Math.abs(izracunatiSumWeight - aproxSumWeight)) {
//				System.out.println("Razlika je različita od dva");
//			}
//			System.out.println("NEGIRANI RULE");
//			System.out.println("PRVI JE TARGET");
//			System.out.println(isRR ? "Prvo stablo" : "Drugo stablo");
//			System.out.println("Izračunati Jaccard: " + jaccard);
//			System.out.println("Izračunati presjek: " + tJS.intersectSize);
//			System.out.println("Approx presjek: " + approxJaccard);
//			
//			System.out.println("IZRAČUNATA UNIJA: " + izracunatiSumWeight);
//			System.out.println("APPROX UNIJA: " + aproxSumWeight);
//
//			System.out.println(rule);
//			System.out.println(targetToBeNegated);
//			System.out.println();
//			throw new IllegalArgumentException("Iteracija:" + SupplementingRandomForest.getNumIter());
//		}
		
//		if (jaccard >= minJaccard && tJS.computePval(rule, negatedTarget, dat) <= appset.maxPval
//				&& tJS.intersectSize >= appset.minSupport && tJS.intersectSize <= appset.maxSupport) {

			Redescription tmp = new Redescription(rule.rule, targetToBeNegated.rule, tJS.JS, map, dat, 1);
			tmp.computeElements(rule, negatedTarget, rule, targetToBeNegated);
			tmp.computeUnion(rule, negatedTarget);

			if (tmp.elements.size() >= appset.minSupport && tmp.elements.size() <= appset.maxSupport) {
				tryAdd(tmp, tJS, false);
			}
			return 1;
//		}
//		System.out.println("PRAVILO X NEGIRAN TARGET NE ZBOG JACCARDA");
//		throw new IllegalArgumentException("Iteracija:" + SupplementingRandomForest.getNumIter());
//		return 0;
	}

	private int addRedescriptions(Jacard tJS, Cycle cycle, boolean isRR) {
		int newRedescriptions = 0;
		Map<Integer, List<Integer>> nodeTargets = TargetRulesMap.getNodeTargets(cycle);
		Map<Integer, Rule> targetSupportMap = TargetRulesMap.getTargetSupportMap(cycle);

		double minJaccard = appset.useJoin ? appset.minAddRedJS : appset.minJS;

		// promijeniti - iterirati po hashmap keySet()
		for (Integer ruleID: nodeTargets.keySet()) {
			// 1. POZITIVNA PRAVILA
			// ne moram provjeravati
			// provjera jaccard za negirane targete isto
			Rule rule = TargetRulesMap.getIdRuleMap(cycle).get(ruleID >= 0 ? ruleID : -ruleID);
			
			// obrisano pravilo
			if(rule == null) continue;
			
			int i = 0;
			if (ruleID >= 0) {
				for (Integer targetNumber : nodeTargets.get(rule.getRuleId())) {
					// TODO Jaccard? pValue?
					if (targetNumber >= 0) {
						Rule target = targetSupportMap.get(targetNumber);
						// pozitivni target
						if(isRR) {
							newRedescriptions += checkAndTryAdd(rule, target, tJS, minJaccard, i, cycle, isRR);
						} else {
							newRedescriptions += checkAndTryAdd(target, rule, tJS, minJaccard, i, cycle, isRR);
						}
						i++;
					} else {
						// negirani target - dohvaćamo pozitivni Rule
						Rule target = targetSupportMap.get(-targetNumber);
						if(isRR) {
							newRedescriptions += checkAndTryAddNegatedRight(rule, target, new Rule(target, dat, map), tJS, minJaccard, i, cycle, isRR);
						} else {
							newRedescriptions += checkAndTryAddNegatedLeft(target, rule, new Rule(target, dat, map), tJS, minJaccard, i, cycle, isRR);
						}
						i++;
					}
				}
				// 2. NEGIRANA PRAVILA
			} else {
				Rule negatedRule = new Rule(rule, dat, map);
				int j = 0;
				for (Integer targetNumber : nodeTargets.get(-rule.getRuleId())) {						
					Rule target = targetSupportMap.get(targetNumber);
					if(isRR) {
						newRedescriptions += checkAndTryAddNegatedLeft(rule, target, negatedRule, tJS, minJaccard, j, cycle, isRR);

					} else {
						newRedescriptions += checkAndTryAddNegatedRight(target, rule, negatedRule, tJS, minJaccard, j, cycle, isRR);
					}
					j++;
				}
			}
		}
		return newRedescriptions;
	}

	public int createGuidedJoinBasic(RuleReader rr, RuleReader rr1, Jacard js[], ApplicationSettings appset,
			int oldIndexRR, int oldIndexRR1, int RunInd, boolean outOfmemory[], Mappings map, DataSetCreator dat,
			int elemFreq[], int attrFreq[], ArrayList<Double> redScore, ArrayList<Double> redScoreAt,
			ArrayList<Double> redDistCoverage, ArrayList<Double> redDistCoverageAt, ArrayList<Double> redDistNetwork,
			ArrayList<Double> targetAtScore, double statistics[], ArrayList<Double> maxDiffScoreDistribution,
			NHMCDistanceMatrix mat, int preferenceRow) {

		System.out.println("Using guided expansion with join procedure constrained!");

		setParameters(null, null, js, appset, oldIndexRR, oldIndexRR1, RunInd, outOfmemory, map, dat, elemFreq, attrFreq,
				redScore, redScoreAt, redDistCoverage, redDistCoverageAt, redDistNetwork, targetAtScore, statistics,
				maxDiffScoreDistribution, mat, preferenceRow);

		// TODO
		Jacard tJS = new Jacard();
		tJS.initialize();

		int newRedescriptions = 0;
		// join veća točnost provjeriti csv
		
		Cycle cycle = SupplementingRandomForest.getLeftSide() == 1 ? Cycle.TWO : Cycle.ONE;

		newRedescriptions += addRedescriptions(tJS, cycle, true);
		newRedescriptions += addRedescriptions(tJS, cycle.equals(Cycle.ONE) ? Cycle.TWO : Cycle.ONE, false);
	        
		if ((appset.leftDisjunction || appset.rightDisjunction)) {
			System.out.println("Computing disjunctive refinement...");

			for (int k = 0; k < redescriptions.size(); k++) {
				if (redescriptions.get(k).JS == 1.0)
					continue;
				double joinJS = 0.0, maxJoinJS = 0.0, maxScoreGain = 0.0;
				int maxInd = 0;
				int negated = 0;
				int totalElFreq = 0, totalAtFreq = 0, rScoreEl = 0, rScoreAt = 0;

				for (int tm = 0; tm < elemFreq.length; tm++)
					totalElFreq += elemFreq[tm];

				for (int tm = 0; tm < attrFreq.length; tm++)
					totalAtFreq += attrFreq[tm];

				TIntIterator it = redescriptions.get(k).elements.iterator();
				while (it.hasNext()) {
					int element = it.next();
					rScoreEl += elemFreq[element] - 1;
				}

				double atrScore = 0.0;
				ArrayList<TIntHashSet> attr = redescriptions.get(k)
						.computeAttributes(redescriptions.get(k).viewElementsLists, dat);
				for (int itSet = 0; itSet < attr.size(); itSet++) {
					it = attr.get(itSet).iterator();
					while (it.hasNext()) {
						int at = it.next();
						rScoreAt += attrFreq[at] - 1;
					}
				}

				if (appset.rightDisjunction) {
					ArrayList<TIntHashSet> sideElems = redescriptions.get(k).computeElementsGen(dat, map);// new //
																											// proper
																											// generalization
					int interCount = js[0].computeGenInterCount(redescriptions.get(k), sideElems, 1);
					for (int j = oldIndexRR1; j < rr1.rules.size(); j++) {
						joinJS = js[0].computeRedescriptionRuleElementJacardGen(redescriptions.get(k), rr1.rules.get(j),
								sideElems, 1, 0, dat, map, appset, interCount);

						double scoreGain = computeScoreGain(redescriptions.get(k), rr1.rules.get(j), appset, dat,
								elemFreq, attrFreq, redScore, redScoreAt, totalElFreq, totalAtFreq, rScoreEl, rScoreAt,
								joinJS);

						if (joinJS > 0.5) {
							if (scoreGain > maxScoreGain) {
								maxScoreGain = scoreGain;
								maxInd = j;
								negated = 0;
							}
						}

						if (appset.rightNegation) {
							joinJS = js[0].computeRedescriptionRuleElementJacardGen(redescriptions.get(k),
									rr1.rules.get(j), sideElems, 1, 1, dat, map, appset, interCount);

							if (joinJS > 0.5) {
								if (scoreGain > maxScoreGain) {
									maxScoreGain = scoreGain;
									maxInd = j;
									negated = 1;
								}
							}
						}
					}
					if (maxScoreGain > 0.1) {
						System.out.println("maxScoreGain: " + maxScoreGain);
						Redescription tmp = new Redescription(redescriptions.get(k), dat);
						redescriptions.get(k).disjunctiveJoin(rr1.rules.get(maxInd), appset, dat, map, sideElems, 1,
								negated);
						redescriptions.get(k).computePVal(dat, map);
						if (redescriptions.get(k).pVal > appset.maxPval
								|| redescriptions.get(k).elements.size() > appset.maxSupport)
							redescriptions.set(k, tmp);
						else
							// tmp.updateScore(elemFreq, attrFreq, redScore, redScoreAt,
							// redescriptions.get(k), this, dat);
							tmp.updateScore(elemFreq, attrFreq, redScore, redScoreAt, redDistCoverage, statistics,
									redescriptions.get(k), this, dat);
					}
				}
				if (appset.leftDisjunction) {
					ArrayList<TIntHashSet> sideElems = redescriptions.get(k).computeElementsGen(dat, map);// new
																											// ArrayList<>();//to
																											// make
																											// proper
																											// generalization
					int interCount = js[0].computeGenInterCount(redescriptions.get(k), sideElems, 0);
					joinJS = 0.0;
					maxJoinJS = 0.0;
					maxInd = 0;
					negated = 0;
					for (int i = oldIndexRR; i < rr.rules.size(); i++) {
						joinJS = js[0].computeRedescriptionRuleElementJacardGen(redescriptions.get(k), rr.rules.get(i),
								sideElems, 0, 0, dat, map, appset, interCount);
						double scoreGain = computeScoreGain(redescriptions.get(k), rr.rules.get(i), appset, dat,
								elemFreq, attrFreq, redScore, redScoreAt, totalElFreq, totalAtFreq, rScoreEl, rScoreAt,
								joinJS);

						if (joinJS > 0.5) {
							if (scoreGain > maxScoreGain) {
								maxScoreGain = scoreGain;
								maxInd = i;
								negated = 0;
							}
						}

						if (appset.leftNegation) {
							joinJS = js[0].computeRedescriptionRuleElementJacardGen(redescriptions.get(k),
									rr.rules.get(i), sideElems, 0, 1, dat, map, appset, interCount);

							scoreGain = computeScoreGain(redescriptions.get(k), rr.rules.get(i), appset, dat, elemFreq,
									attrFreq, redScore, redScoreAt, totalElFreq, totalAtFreq, rScoreEl, rScoreAt,
									joinJS);

							if (joinJS > 0.5) {
								if (scoreGain > maxScoreGain) {
									maxScoreGain = scoreGain;
									maxInd = i;
									negated = 1;
								}
							}
						}
					}
					if (maxScoreGain > 0.1) {
						System.out.println("maxScoreGain: " + maxScoreGain);
						Redescription tmp = new Redescription(redescriptions.get(k), dat);
						redescriptions.get(k).disjunctiveJoin(rr.rules.get(maxInd), appset, dat, map, sideElems, 0,
								negated);

						redescriptions.get(k).computePVal(dat, map);
						if (redescriptions.get(k).pVal > appset.maxPval
								|| redescriptions.get(k).elements.size() > appset.maxSupport)
							redescriptions.set(k, tmp);
						else
							// tmp.updateScore(elemFreq, attrFreq, redScore, redScoreAt,
							// redescriptions.get(k), this, dat);
							tmp.updateScore(elemFreq, attrFreq, redScore, redScoreAt, redDistCoverage, statistics,
									redescriptions.get(k), this, dat);
					}
				}
			}
		}
		System.out.println("Num times join: " + equalSubs);

		return 1;
	}

	 public int createGuidedJoinBasicOld(RuleReader rr, RuleReader rr1, Jacard js[], ApplicationSettings appset, int oldIndexRR, int oldIndexRR1, int RunInd, boolean outOfmemory[], Mappings map, DataSetCreator dat, int elemFreq[], int attrFreq[], ArrayList<Double> redScore, ArrayList<Double> redScoreAt,ArrayList<Double> redDistCoverage, ArrayList<Double> redDistCoverageAt, ArrayList<Double> redDistNetwork ,ArrayList<Double> targetAtScore, double Statistics[], ArrayList<Double> maxDiffScoreDistribution, NHMCDistanceMatrix mat, int PreferenceRow){

		 System.out.println("Using guided expansion with join procedure constrained!");
	        boolean memoryCanbeOptimized = true;
	        int maxNum = 0;

	        ArrayList<Rule> negLeft = new ArrayList<>();
	        ArrayList<Rule> negRight = new ArrayList<>();

	        if (appset.leftNegation == true)
	            for (int i = oldIndexRR; i < rr.rules.size(); i++) {
	                Rule rtemp = new Rule(rr.rules.get(i), dat, map);
	                negLeft.add(rtemp);
	            }

	        if (appset.rightNegation == true)
	            for (int i = oldIndexRR1; i < rr1.rules.size(); i++) {
	                Rule rtemp = new Rule(rr1.rules.get(i), dat, map);
	                negRight.add(rtemp);
	            }


	        if (appset.unguidedExpansion) {
	            if (RunInd == 0) {
	                maxNum = (rr.rules.size() - oldIndexRR) * (rr1.rules.size() - oldIndexRR1);
	            } else {
	                maxNum = (rr.rules.size() - oldIndexRR) * (rr1.rules.size() - oldIndexRR1) - (rr.newRuleIndex - oldIndexRR) * (rr1.newRuleIndex - oldIndexRR1);
	            }
	        } else {
	            maxNum = ((rr.newRuleIndex - oldIndexRR) * (rr1.rules.size() - rr1.newRuleIndex)) + ((rr.rules.size() - rr.newRuleIndex) * (rr1.newRuleIndex - oldIndexRR1));
	        }

	        System.out.println("max number of rules: " + maxNum);

	        int numIt = 0;
	        int step = maxNum / 100;

	        if (step == 0)
	            step = 1;

	        int newRedescriptions = 0;
	        Jacard tJS = new Jacard();
	        tJS.initialize();

	        for (int i = oldIndexRR; i < rr.rules.size(); i++) {
	            for (int j = oldIndexRR1/*rr1.newRuleIndex-addon1*/; j < rr1.rules.size(); j++) {

	                if (appset.unguidedExpansion == true) {
	                    if (RunInd != 0 && i < rr.newRuleIndex && j < rr1.newRuleIndex)
	                        continue;
	                } else {
	                    if ((i < rr.newRuleIndex && j < rr1.newRuleIndex) || (i >= rr.newRuleIndex && j >= rr1.newRuleIndex))
	                        continue;
	                }

	                for (int jinit = 0; jinit < js.length; jinit++)
	                    js[jinit].initialize();
	                numIt++;

	                double JSPos = 0.0, JSPosNeg = 0.0, JSNegPos = 0.0;
	                JSPos = js[0].computeJacard(rr.rules.get(i), rr1.rules.get(j));
	                double RPval = js[0].computePval(rr.rules.get(i), rr1.rules.get(j), dat);

	                if (JSPos >= appset.minAddRedJS && RPval <= appset.maxPval && js[0].intersectSize >= appset.minSupport && js[0].intersectSize <= appset.maxSupport) {
	                    Redescription tmp = new Redescription(rr.rules.get(i).rule, rr1.rules.get(j).rule, js[0].JS, map, dat, 0);
	                    tmp.computeElements(rr.rules.get(i), rr1.rules.get(j));
	                    tmp.computeUnion(rr.rules.get(i), rr1.rules.get(j));
	                    tmp.pVal = RPval;

	                    if (tmp.elements.size() >= appset.minSupport && tmp.elements.size() <= appset.maxSupport) {
	                        //tryAdd(tmp, tJS, appset, dat,map,elemFreq,attrFreq,redScore,redScoreAt,targetAtScore,true);
	                        tryAdd(tmp, tJS, appset, dat, map, elemFreq, attrFreq, redScore, redScoreAt, redDistCoverage, redDistCoverageAt, redDistNetwork, targetAtScore, Statistics, maxDiffScoreDistribution, mat, PreferenceRow, false);

	                    }//fix negation and validation!
	                }
	                if (appset.rightNegation == true && JSPos <= (1.0 - appset.minJS)) {
	                    JSPosNeg = js[1].computeJacard(rr.rules.get(i), negRight.get(j - oldIndexRR1)/* rr1.rules.get(j)*, dat, map, 1*/);
	                    if (JSPosNeg >= appset.minJS && js[1].computePval(rr.rules.get(i), negRight.get(j - oldIndexRR1) /*rr1.rules.get(j)*/, dat/*,map,1*/) <= appset.maxPval && js[1].intersectSize >= appset.minSupport && js[1].intersectSize <= appset.maxSupport) {
	                        Redescription tmp = new Redescription(rr.rules.get(i).rule, rr1.rules.get(j).rule, js[1].JS, map, dat, 1);
	                        tmp.computeElements(rr.rules.get(i), negRight.get(j - oldIndexRR1), rr.rules.get(i), rr1.rules.get(j)/*rr1.rules.get(j)*//*,dat,map,1*/);
	                        tmp.computeUnion(rr.rules.get(i), negRight.get(j - oldIndexRR1)/*rr1.rules.get(j)*//*,dat,map,1*/);
	                        if (tmp.elements.size() >= appset.minSupport && tmp.elements.size() <= appset.maxSupport) {//tmp.JS>0.4 || (tmp.elements.size()>1 && tmp.JS>0.2)
	                            // tryAdd(tmp, tJS, appset, dat,map,elemFreq,attrFreq,redScore,redScoreAt,targetAtScore,true);  //redescriptions.add(tmp);
	                            tryAdd(tmp, tJS, appset, dat, map, elemFreq, attrFreq, redScore, redScoreAt, redDistCoverage, redDistCoverageAt, redDistNetwork, targetAtScore, Statistics, maxDiffScoreDistribution, mat, PreferenceRow, false);

	                            newRedescriptions++;
	                        }
	                    }
	                }
	                if (appset.leftNegation == true && JSPos <= (1.0 - appset.minJS)) {
	                    JSNegPos = js[2].computeJacard(negLeft.get(i - oldIndexRR)/*rr.rules.get(i)*/, rr1.rules.get(j)/*, dat, map, 2*/);
	                    if (JSNegPos >= appset.minJS && js[2].computePval(negLeft.get(i - oldIndexRR)/*rr.rules.get(i)*/, rr1.rules.get(j), dat/*,map,2*/) <= appset.maxPval && js[2].intersectSize >= appset.minSupport && js[2].intersectSize <= appset.maxSupport) {
	                        Redescription tmp = new Redescription(rr.rules.get(i).rule, rr1.rules.get(j).rule, js[2].JS, map, dat, 2);
	                        tmp.computeElements(negLeft.get(i - oldIndexRR), rr1.rules.get(j), rr.rules.get(i), rr1.rules.get(j)/*rr1.rules.get(j)*//*,dat,map,1*/);
	                        tmp.computeUnion(negLeft.get(i - oldIndexRR)/*rr.rules.get(i)*/, rr1.rules.get(j)/*,dat,map,2*/);
	                        if (tmp.elements.size() >= appset.minSupport && tmp.elements.size() <= appset.maxSupport) {//tmp.JS>0.4 || (tmp.elements.size()>1 && tmp.JS>0.2)
	                            // tryAdd(tmp, tJS, appset, dat,map,elemFreq,attrFreq,redScore,redScoreAt,targetAtScore,true);//redescriptions.add(tmp);
	                            tryAdd(tmp, tJS, appset, dat, map, elemFreq, attrFreq, redScore, redScoreAt, redDistCoverage, redDistCoverageAt, redDistNetwork, targetAtScore, Statistics, maxDiffScoreDistribution, mat, PreferenceRow, false);

	                            newRedescriptions++;
	                        }
	                    }
	                }
	                if (numIt % step == 0) {
	                    System.out.println((((double) numIt / maxNum) * 100) + "% completed...");
	                    System.out.println("num redescriptions: " + redescriptions.size());
	                }
	                if (numIt == maxNum)
	                    System.out.println("100% completed!");

	                long maxMemory = Runtime.getRuntime().maxMemory() / (1024 * 1024);
	                long freemem = Runtime.getRuntime().freeMemory() / (1024 * 1024);
	                long totalMemory = Runtime.getRuntime().totalMemory() / (1024 * 1024);

	                long minMemory = Math.max(10, (long) 0.15 * maxMemory);

	                if ((maxMemory - (totalMemory - freemem) < minMemory) && memoryCanbeOptimized == true) {
	                    //this.filter(appset, rr, rr1, map, dat);

	                    System.out.println("Memory status: " + ((maxMemory - (totalMemory - freemem))));
	                    System.out.println("Min memory: " + minMemory);

	                    this.filter(appset, rr, rr1, map, dat);
	                    computePVal(dat, map);
	                    RedescriptionSet rTemp = new RedescriptionSet();
	                    double weights[] = {0.2, 0.2, 0.2, 0.2, 0.2};
	                    rTemp.createRedescriptionSet(this, weights, appset, dat, map);
	                    this.redescriptions.clear();

	                    for (int itm = 0; itm < rTemp.redescriptions.size(); itm++)
	                        this.redescriptions.add(rTemp.redescriptions.get(itm));

	                    for (int irt = 0; irt < redescriptions.size(); irt++)
	                        redescriptions.get(irt).clearRuleMaps();

	                    newRedescriptions = redescriptions.size();

	                    Runtime.getRuntime().gc();

	                    System.out.println("Memory status: " + ((maxMemory - (totalMemory - freemem))));
	                    System.out.println("Changing minJS level");
	                    System.out.println("New minJS level: " + appset.minJS);
	                } else if ((maxMemory - (totalMemory - freemem) < minMemory) && memoryCanbeOptimized == false) {
	                    outOfmemory[0] = true;
	                    return newRedescriptions;
	                }
	            }
	        }

	        if ((appset.leftDisjunction == true || appset.rightDisjunction == true)) {
	            System.out.println("Computing disjunctive refinement...");

	            step = redescriptions.size() / 100;
	            if (redescriptions.size() < 100)
	                step = 1;

	            for (int k = 0; k < redescriptions.size(); k++) {
	                if (redescriptions.get(k).JS == 1.0)
	                    continue;
	                double joinJS = 0.0, maxJoinJS = 0.0, maxScoreGain = 0.0;
	                int maxInd = 0;
	                int negated = 0;
	                int totalElFreq = 0, totalAtFreq = 0, rScoreEl = 0, rScoreAt = 0;

	                for (int tm = 0; tm < elemFreq.length; tm++)
	                    totalElFreq += elemFreq[tm];

	                for (int tm = 0; tm < attrFreq.length; tm++)
	                    totalAtFreq += attrFreq[tm];

	                TIntIterator it = redescriptions.get(k).elements.iterator();
	                while (it.hasNext()) {
	                    int element = it.next();
	                    rScoreEl += elemFreq[element] - 1;
	                }

	                double atrScore = 0.0;
	                ArrayList<TIntHashSet> attr = redescriptions.get(k).computeAttributes(redescriptions.get(k).viewElementsLists, dat);
	                for (int itSet = 0; itSet < attr.size(); itSet++) {
	                    it = attr.get(itSet).iterator();
	                    while (it.hasNext()) {
	                        int at = it.next();
	                        rScoreAt += attrFreq[at] - 1;
	                    }
	                }

	                if (appset.rightDisjunction == true) {
	                    ArrayList<TIntHashSet> sideElems = redescriptions.get(k).computeElementsGen(dat, map);//new ArrayList<>();//to make proper generalization
	                    int interCount = js[0].computeGenInterCount(redescriptions.get(k), sideElems, 1);
	                    for (int j = oldIndexRR1; j < rr1.rules.size(); j++) {
	                        joinJS = js[0].computeRedescriptionRuleElementJacardGen(redescriptions.get(k), rr1.rules.get(j), sideElems, 1, 0, dat, map, appset, interCount);

	                        double scoreGain = computeScoreGain(redescriptions.get(k), rr1.rules.get(j), appset, dat, elemFreq, attrFreq, redScore, redScoreAt, totalElFreq, totalAtFreq, rScoreEl, rScoreAt, joinJS);

	                        if (joinJS > 0.5) {
	                            if (scoreGain > maxScoreGain) {
	                                maxScoreGain = scoreGain;
	                                maxInd = j;
	                                negated = 0;
	                            }
	                        }

	                        if (appset.rightNegation == true) {
	                            joinJS = js[0].computeRedescriptionRuleElementJacardGen(redescriptions.get(k), rr1.rules.get(j), sideElems, 1, 1, dat, map, appset, interCount);

	                            if (joinJS > 0.5) {
	                                if (scoreGain > maxScoreGain) {
	                                    maxScoreGain = scoreGain;
	                                    maxInd = j;
	                                    negated = 1;
	                                }
	                            }
	                        }
	                    }
	                    if (maxScoreGain > 0.1) {
	                        System.out.println("maxScoreGain: " + maxScoreGain);
	                        Redescription tmp = new Redescription(redescriptions.get(k), dat);
	                        redescriptions.get(k).disjunctiveJoin(rr1.rules.get(maxInd), appset, dat, map, sideElems, 1, negated);
	                        redescriptions.get(k).computePVal(dat, map);
	                        if (redescriptions.get(k).pVal > appset.maxPval || redescriptions.get(k).elements.size() > appset.maxSupport)
	                            redescriptions.set(k, tmp);
	                        else
	                            //tmp.updateScore(elemFreq, attrFreq, redScore, redScoreAt, redescriptions.get(k), this, dat);
	                            tmp.updateScore(elemFreq, attrFreq, redScore, redScoreAt, redDistCoverage, Statistics, redescriptions.get(k), this, dat);
	                    }
	                }
	                if (appset.leftDisjunction == true) {
	                    ArrayList<TIntHashSet> sideElems = redescriptions.get(k).computeElementsGen(dat, map);//new ArrayList<>();//to make proper generalization
	                    int interCount = js[0].computeGenInterCount(redescriptions.get(k), sideElems, 0);
	                    joinJS = 0.0;
	                    maxJoinJS = 0.0;
	                    maxInd = 0;
	                    negated = 0;
	                    for (int i = oldIndexRR; i < rr.rules.size(); i++) {
	                        joinJS = js[0].computeRedescriptionRuleElementJacardGen(redescriptions.get(k), rr.rules.get(i), sideElems, 0, 0, dat, map, appset, interCount);
	                        double scoreGain = computeScoreGain(redescriptions.get(k), rr.rules.get(i), appset, dat, elemFreq, attrFreq, redScore, redScoreAt, totalElFreq, totalAtFreq, rScoreEl, rScoreAt, joinJS);

	                        if (joinJS > 0.5) {
	                            if (scoreGain > maxScoreGain) {
	                                maxScoreGain = scoreGain;
	                                maxInd = i;
	                                negated = 0;
	                            }
	                        }

	                        if (appset.leftNegation == true) {
	                            joinJS = js[0].computeRedescriptionRuleElementJacardGen(redescriptions.get(k), rr.rules.get(i), sideElems, 0, 1, dat, map, appset, interCount);

	                            scoreGain = computeScoreGain(redescriptions.get(k), rr.rules.get(i), appset, dat, elemFreq, attrFreq, redScore, redScoreAt, totalElFreq, totalAtFreq, rScoreEl, rScoreAt, joinJS);

	                            if (joinJS > 0.5) {
	                                if (scoreGain > maxScoreGain) {
	                                    maxScoreGain = scoreGain;
	                                    maxInd = i;
	                                    negated = 1;
	                                }
	                            }
	                        }
	                    }
	                    if (maxScoreGain > 0.1) {
	                        System.out.println("maxScoreGain: " + maxScoreGain);
	                        Redescription tmp = new Redescription(redescriptions.get(k), dat);
	                        redescriptions.get(k).disjunctiveJoin(rr.rules.get(maxInd), appset, dat, map, sideElems, 0, negated);

	                        redescriptions.get(k).computePVal(dat, map);
	                        if (redescriptions.get(k).pVal > appset.maxPval || redescriptions.get(k).elements.size() > appset.maxSupport)
	                            redescriptions.set(k, tmp);
	                        else
	                            //tmp.updateScore(elemFreq, attrFreq, redScore, redScoreAt, redescriptions.get(k), this, dat);
	                            tmp.updateScore(elemFreq, attrFreq, redScore, redScoreAt, redDistCoverage, Statistics, redescriptions.get(k), this, dat);
	                    }
	                }

	                if ((k + 1) % step == 0) {
	                    System.out.println((((double) (k + 1) / redescriptions.size()) * 100) + "% completed...");
	                    System.out.println("num redescriptions: " + redescriptions.size());
	                }
	            }
	        }
	        System.out.println("Num times join: " + equalSubs);
	        return 1;
	    }

	    public int createGuidedJoin(RuleReader rr, RuleReader rr1, Jacard js[], ApplicationSettings appset, int oldIndexRR, int oldIndexRR1, int RunInd, boolean outOfmemory[], Mappings map, DataSetCreator dat) {

	        System.out.println("Using guided expansion with join procedure!");

	        int maxNum, repetition, tmpJ = -1;
	        double memcheck = 0.5;
	        boolean memoryCanbeOptimized = true;

	        if (appset.unguidedExpansion) {
	            if (RunInd == 0) {
	                maxNum = (rr.rules.size() - oldIndexRR) * (rr1.rules.size() - oldIndexRR1);
	            } else {
	                maxNum = (rr.rules.size() - oldIndexRR) * (rr1.rules.size() - oldIndexRR1) - (rr.newRuleIndex - oldIndexRR) * (rr1.newRuleIndex - oldIndexRR1);
	            }
	        } else {
	            maxNum = ((rr.newRuleIndex - oldIndexRR) * (rr1.rules.size() - rr1.newRuleIndex)) + ((rr.rules.size() - rr.newRuleIndex) * (rr1.newRuleIndex - oldIndexRR1));
	        }

	        System.out.println("max number of rules: " + maxNum);

	        ArrayList<Rule> negLeft = new ArrayList<>();
	        ArrayList<Rule> negRight = new ArrayList<>();

	        for (int i = oldIndexRR; i < rr.rules.size(); i++) {
	            Rule rtemp = new Rule(rr.rules.get(i), dat, map);
	            negLeft.add(rtemp);
	        }

	        for (int i = oldIndexRR1; i < rr1.rules.size(); i++) {
	            Rule rtemp = new Rule(rr1.rules.get(i), dat, map);
	            negRight.add(rtemp);
	        }

	        int numIt = 0;
	        int step = maxNum / 100;
	        if (step == 0)
	            step = 1;
	        int newRedescriptions = 0;

	        double JSPos = 0.0, JSPosNeg = 0.0, JSNegPos = 0.0;
	        for (int i = oldIndexRR; i < rr.rules.size(); i++) {
	            repetition = 0;

	            for (int j = oldIndexRR1; j < rr1.rules.size(); j++) {
	                int EmergencyExit = 0;
	                if (appset.unguidedExpansion == true) {
	                    if (RunInd != 0 && i < rr.newRuleIndex && j < rr1.newRuleIndex)
	                        continue;
	                } else {
	                    if ((i < rr.newRuleIndex && j < rr1.newRuleIndex) || (i >= rr.newRuleIndex && j >= rr1.newRuleIndex))
	                        continue;
	                }

	                for (int it = 0; it < js.length; it++)
	                    js[it].initialize();
	                numIt++;
	                try {
	                    JSPos = js[0].computeJacard(rr.rules.get(i), rr1.rules.get(j));
	                    if (JSPos > appset.minAddRedJS && js[0].intersectSize < appset.maxSupport && js[0].intersectSize >= appset.minSupport) {
	                        Redescription tmp = new Redescription(rr.rules.get(i).rule, rr1.rules.get(j).rule, js[0].JS, map, dat);
	                        tmp.computeElements(rr.rules.get(i), rr1.rules.get(j));
	                        tmp.computeUnion(rr.rules.get(i), rr1.rules.get(j));
	                        // if(tmp.elements.size()<appset.minSupport || tmp.elements.size()>appset.maxSupport)
	                        //  continue;

	                   /* for(int t=0;t<2;t++){
	         System.out.println("Validation new redescr"+t);
	         tmp.closeInterval(dat, map);
	          // long startTime = System.currentTimeMillis();
	          tmp.createRuleString(map);
	          //System.out.println("Before");
	         // System.out.println(redescriptions.get(i).ruleStrings.get(0));
	         // System.out.println(redescriptions.get(i).ruleStrings.get(1));
	          //tmp.minimizeOptimal(dat, map,1);

	        int val= tmp.validate(dat, map);
	        if(val==0){
	            System.out.println("Rule one string: ");
	            System.out.println(rr.rules.get(i).rule);
	            TIntIterator it=rr.rules.get(i).elements.iterator();
	            while(it.hasNext()){
	                System.out.print(it.next()+" ");
	            }
	             System.out.println();
	            System.out.println("Rule two string: ");
	            System.out.println(rr1.rules.get(j).rule);
	            it=rr1.rules.get(j).elements.iterator();
	            while(it.hasNext()){
	                System.out.print(it.next()+" ");
	            }
	             System.out.println();
	             System.out.println("Redescription elements");
	             it=tmp.elements.iterator();
	            while(it.hasNext()){
	                System.out.print(it.next()+" ");
	            }
	             System.out.println();
	             it=tmp.elements.iterator();
	            while(it.hasNext()){
	                System.out.print(map.idExample.get(it.next())+" ");
	            }
	             System.out.println();
	            System.exit(-1);
	        }


	         tmp.clearRuleMaps();
	       }*/

	                        Redescription tmp1 = new Redescription(tmp, dat);
	                        int found = 0;

	                        ArrayList<Redescription> toRemove = new ArrayList<>();
	                        ArrayList<Redescription> toAdd = new ArrayList<>();
	                        ArrayList<Redescription> toRefine = new ArrayList<>();
	                        int refInd = 0, refinementFound = 0;
	                        int refined = 0;
	                        for (int k = 0; k < redescriptions.size(); k++) {

	                    /*long maxMemory = Runtime.getRuntime().maxMemory()/(1024*1024);
	                    long freemem=Runtime.getRuntime().freeMemory()/(1024*1024);
	                    long totalMemory = Runtime.getRuntime().totalMemory()/(1024*1024);

	                    long minMemory=Math.max(10, (long)(0.15*maxMemory));

	                    if((maxMemory-(totalMemory-freemem)<minMemory)&& memoryCanbeOptimized==true){
	                        EmergencyExit=1;
	                            break;
	                    }*/

	                            if (redescriptions.get(k).JS == 1.0) {
	                                if (redescriptions.get(k).CompareEqual(tmp) == 2 && refinementFound == 0) {
	                                    toRefine.add(redescriptions.get(k));
	                                    refInd = k;
	                                    tmp = redescriptions.get(k);
	                                    refinementFound = 1;
	                                    found = 1;
	                                    refined = 1;
	                                    //break;
	                                }
	                                continue;
	                            }
	                            int quality = tmp.CompareQuality(redescriptions.get(k));
	                            found = 0;
	                            if (quality == -1) {
	                                continue;
	                            } else if (quality == 2 && refinementFound == 0) {

	                                if (tmp1.elements.size() == redescriptions.get(k).elements.size()) {
	                                    redescriptions.get(k).join(tmp1, map, dat);
	                                    if (redescriptions.get(k).JS == 1.0)
	                                        refined = 1;
	                                } else {
	                                    if (refined == 0) {//rafiniraj dok ne zadovoljiÅ¡ uvjet
	                                        tmp1.join(redescriptions.get(k), map, dat);
	                                        if (tmp1.JS >= appset.minJS && tmp1.elements.size() <= appset.maxSupport) {
	                                            toAdd.add(tmp1);
	                                            refined = 1;
	                                            //  break;
	                                        }
	                                    }
	                                }

	                                found = 1;
	                            } else if (quality == 1 && redescriptions.get(k).JS < 1.0) {
	                                if (tmp.JS > tmp1.JS)
	                                    redescriptions.get(k).join(tmp, map, dat);
	                                else
	                                    redescriptions.get(k).join(tmp1, map, dat);
	                                //found=1;

	                                if (redescriptions.get(k).elements.size() < appset.minSupport)
	                                    toRemove.add(redescriptions.get(k));

	                            }
	                        }


	                        for (int k = 0; k < toRemove.size(); k++)
	                            redescriptions.remove(toRemove.get(k));
	                        if (toRemove.size() > 0) {
	                            newRedescriptions -= toRemove.size();
	                            toRemove.clear();
	                        }

	                        for (int k = 0; k < toAdd.size(); k++)
	                            redescriptions.add(toAdd.get(k));
	                        if (toAdd.size() > 0) {
	                            newRedescriptions += toAdd.size();
	                            toAdd.clear();
	                        }

	                        if (found == 0) {
	                            if (tmp.elements.size() >= appset.minSupport && tmp.JS >= appset.minJS && tmp.elements.size() <= appset.maxSupport) {//tmp.JS>0.4 || (tmp.elements.size()>1 && tmp.JS>0.2)
	                                redescriptions.add(tmp);
	                                newRedescriptions++;
	                            }
	                        }

	                        if (toRefine.size() > 0) {

	                            ArrayList<Integer> rmInd = new ArrayList();
	                            for (int k = redescriptions.size() - 1; k >= 0; k--) {
	                                if (k != refInd && redescriptions.get(k).CompareEqual(toRefine.get(0)) == 2)
	                                    rmInd.add(k);
	                            }
	                            toRefine.clear();
	                            refInd = 0;
	                            refinementFound = 0;
	                            newRedescriptions -= rmInd.size();
	                            for (int k = 0; k < rmInd.size(); k++)
	                                redescriptions.remove((int) rmInd.get(k));
	                            rmInd.clear();
	                        }
	                    }//add negations and disjunction afterwards

	                    if (appset.rightNegation == true && !(EmergencyExit == 1) && JSPos <= (1.0 - appset.minJS)) {
	                        //JSPosNeg=js[1].computeJacard(rr.rules.get(i), rr1.rules.get(j), dat, map, 1);
	                        JSPosNeg = js[1].computeJacard(rr.rules.get(i), negRight.get(j - oldIndexRR1)/*rr1.rules.get(j)*/);
	                        if (JSPosNeg >= appset.minJS && js[1].computePval(rr.rules.get(i), negRight.get(j - oldIndexRR1), dat/*rr1.rules.get(j), dat,map,1*/) <= appset.maxPval && js[1].intersectSize <= appset.maxSupport) {
	                            Redescription tmp = new Redescription(rr.rules.get(i).rule, rr1.rules.get(j).rule, js[1].JS, map, dat, 1);
	                            tmp.computeElements(rr.rules.get(i), negRight.get(j - oldIndexRR1), rr.rules.get(i), rr1.rules.get(j)/*rr1.rules.get(j)*//*,dat,map,1*/);
	                            tmp.computeUnion(rr.rules.get(i), negRight.get(j - oldIndexRR1)/*rr1.rules.get(j),dat,map,1*/);
	                    /*System.out.println("Is negated right: "+tmp.rightRuleElements.get(0).isNegated);
	                    tmp.closeInterval(dat, map);
	                    tmp.validate(dat, map);*/
	                            if (tmp.elements.size() >= appset.minSupport) {//tmp.JS>0.4 || (tmp.elements.size()>1 && tmp.JS>0.2)
	                                redescriptions.add(tmp);
	                                newRedescriptions++;
	                            }
	                        }
	                    }
	                    if (appset.leftNegation == true && !(EmergencyExit == 1) && JSPos <= (1.0 - appset.minJS)) {
	                        //JSNegPos=js[2].computeJacard(rr.rules.get(i), rr1.rules.get(j), dat, map, 2);
	                        JSNegPos = js[2].computeJacard(/*rr.rules.get(i)*/negLeft.get(i - oldIndexRR), rr1.rules.get(j)/*, dat, map, 2*/);
	                        if (JSNegPos >= appset.minJS && js[2].computePval(negLeft.get(i - oldIndexRR)/*rr.rules.get(i)*/, rr1.rules.get(j), dat/*,map,2*/) <= appset.maxPval && js[2].intersectSize <= appset.maxSupport) {
	                            Redescription tmp = new Redescription(rr.rules.get(i).rule, rr1.rules.get(j).rule, js[2].JS, map, dat, 2);
	                            tmp.computeElements(negLeft.get(i - oldIndexRR)/*rr.rules.get(i)*/, rr1.rules.get(j), rr.rules.get(i), rr1.rules.get(j)/*,dat,map,2*/);
	                            tmp.computeUnion(negLeft.get(i - oldIndexRR)/*rr.rules.get(i)*/, rr1.rules.get(j)/*,dat,map,2*/);
	                    /*System.out.println("Is negated left: "+tmp.leftRuleElements.get(0).isNegated);
	                    tmp.closeInterval(dat, map);
	                    tmp.validate(dat, map);*/
	                            if (tmp.elements.size() >= appset.minSupport) {//tmp.JS>0.4 || (tmp.elements.size()>1 && tmp.JS>0.2)
	                                redescriptions.add(tmp);
	                                newRedescriptions++;
	                            }
	                        }
	                    }

	                    if (numIt % step == 0) {
	                        System.out.println((((double) numIt / maxNum) * 100) + "% completed...");
	                    }
	                    if (numIt == maxNum)
	                        System.out.println("100% completed!"); //do memory optimization here

	                    if (numIt % 100 == 0 || EmergencyExit == 1) { //do memory check every 100 iterations

	                        if (EmergencyExit == 1) {
	                            j--;
	                            if (tmpJ != j && repetition == 0)
	                                tmpJ = j;
	                            else if (tmpJ == j)
	                                repetition++;
	                            else if (tmpJ != j && repetition != 0) {
	                                tmpJ = j;
	                                repetition = 0;
	                            }
	                        }

	                        long maxMemory = Runtime.getRuntime().maxMemory() / (1024 * 1024);
	                        long freemem = Runtime.getRuntime().freeMemory() / (1024 * 1024);
	                        long totalMemory = Runtime.getRuntime().totalMemory() / (1024 * 1024);
	                        long minMemory = Math.max(10, (long) (0.15 * maxMemory));
	                    /*System.out.println("other mem value: "+((long)(0.02*maxMemory)));
	                    System.out.println("Memory: "+((maxMemory-(totalMemory-freemem))<minMemory));
	                    System.out.println("Amount memory: "+((maxMemory-(totalMemory-freemem))));
	                    System.out.println("Min memory: "+minMemory);
	                    System.out.println("Rules size: "+(rr.rules.size()+rr1.rules.size()));
	                    System.out.println("Redescriptions set size: "+redescriptions.size());
	                    System.out.println("totalMemory-freemem: "+(totalMemory-freemem));*/

	                        if (((maxMemory - (totalMemory - freemem)) < Math.max(memcheck, 0.15) * maxMemory) && redescriptions.size() != 0) {
	                            memcheck = memcheck / 2;

	                            adaptSet(dat, map, 1);
	                            this.filter(appset, rr, rr1, map, dat);
	                            computePVal(dat, map);
	                            this.removePVal(appset);
	                            this.sortRedescriptions();
	                            // double coverage[]={-1.0,-1.0};
	                            this.writeToFileTmp(appset.outFolderPath + "\\" + "RulesTmp.rr", dat, map, appset);
	                            for (int irt = 0; irt < redescriptions.size(); irt++)
	                                redescriptions.get(irt).clearRuleMaps();
	                            Runtime.getRuntime().gc();
	                        }


	                        if (((maxMemory - (totalMemory - freemem)) < minMemory) && memoryCanbeOptimized == true) {


	                            adaptSet(dat, map, 1);
	                                /* for(int ti=0;ti<redescriptions.size();ti++)
	                                     redescriptions.get(ti).minimizeOptimal(dat, map, 0);*/

	                            this.filter(appset, rr, rr1, map, dat);

	                            computePVal(dat, map);
	                            this.removePVal(appset);
	                            Runtime.getRuntime().gc();
	                            maxMemory = Runtime.getRuntime().maxMemory() / (1024 * 1024);
	                            freemem = Runtime.getRuntime().freeMemory() / (1024 * 1024);
	                            totalMemory = Runtime.getRuntime().totalMemory() / (1024 * 1024);

	                            System.out.println("Memory status after filtering: " + ((maxMemory - (totalMemory - freemem))));
	                            System.out.println("Min memory: " + minMemory);
	                            System.out.println("Redescription size: " + redescriptions.size());
	                            if ((this.redescriptions.size() < appset.numRetRed && ((maxMemory - (totalMemory - freemem)) > minMemory)) || (((maxMemory - (totalMemory - freemem)) > 1.5 * minMemory) && repetition < 3)) {
	                                System.out.println("redescription size after filtering: " + redescriptions.size());
	                                for (int irt = 0; irt < redescriptions.size(); irt++)
	                                    redescriptions.get(irt).clearRuleMaps();
	                                continue;
	                            }

	                            RedescriptionSet rTemp = new RedescriptionSet();
	                            double weights[] = {0.2, 0.2, 0.2, 0.2, 0.2};
	                            rTemp.createRedescriptionSet(this, weights, appset, dat, map);
	                            this.redescriptions.clear();

	                            for (int itm = 0; itm < rTemp.redescriptions.size(); itm++)
	                                this.redescriptions.add(rTemp.redescriptions.get(itm));

	                            //this.redescriptions=rTemp.redescriptions;

	                            for (int irt = 0; irt < redescriptions.size(); irt++)
	                                redescriptions.get(irt).clearRuleMaps();

	                            newRedescriptions = redescriptions.size();
	                            System.out.println("New redescriptions after filtering..." + newRedescriptions);
	                            EmergencyExit = 0;

	                            Runtime.getRuntime().gc();

	                            maxMemory = Runtime.getRuntime().maxMemory() / (1024 * 1024);
	                            freemem = Runtime.getRuntime().freeMemory() / (1024 * 1024);
	                            totalMemory = Runtime.getRuntime().totalMemory() / (1024 * 1024);
	                        /*if(appset.minJS!=1.0){
	                            appset.minJS=appset.minJS+0.1;
	                            if(appset.minJS>1.0)
	                                appset.minJS=1.0;

	                            int num=countNumber(appset.minJS);
	                            if((redescriptions.size()-num)>appset.numRetRed){
	                                remove(appset);
	                                Runtime.getRuntime().gc();
	                            }
	                            else{
	                                if(appset.numRetRed>redescriptions.size()){
	                                    outOfmemory[0]=true;
	                                    return newRedescriptions;
	                                }
	                                else{
	                                remove(appset,appset.numRetRed);
	                                appset.minJS=appset.minJS-0.1;
	                                Runtime.getRuntime().gc();
	                                }
	                            }
	                        }
	                        else{
	                            remove(appset);
	                            memoryCanbeOptimized=false;
	                            Runtime.getRuntime().gc();
	                        }*/

	                            System.out.println("Max memory: " + maxMemory);
	                            System.out.println("Memory status: " + ((maxMemory - (totalMemory - freemem))));
	                            System.out.println("Min memory: " + minMemory);

	                            if ((maxMemory - (totalMemory - freemem)) < minMemory)
	                                return newRedescriptions;
	                            System.out.println("Memory status: " + ((maxMemory - (totalMemory - freemem))));
	                            System.out.println("Changin minJS level");
	                            System.out.println("New minJS level: " + appset.minJS);
	                        } else if (((maxMemory - (totalMemory - freemem)) < minMemory) && memoryCanbeOptimized == false) {
	                            outOfmemory[0] = true;
	                            return newRedescriptions;
	                        }

	                    /*for(int k=0;k<redescriptions.size();k++){
	                            redescriptions.get(k).closeInterval(dat, map);
	                            redescriptions.get(k).minimize(dat, map);
	                            redescriptions.get(k).deleteBounds();
	                    }*/
	                    }
	                } catch (java.lang.OutOfMemoryError e) {
	                    e.printStackTrace();
	                }
	            }
	        }

	        if ((appset.leftDisjunction == true || appset.rightDisjunction == true)) {
	            System.out.println("Computing disjunctive refinement...");

	            step = redescriptions.size() / 100;
	            if (redescriptions.size() < 100)
	                step = redescriptions.size();

	            for (int k = 0; k < redescriptions.size(); k++) {
	                if (redescriptions.get(k).JS == 1.0)
	                    continue;

	                double joinJS = 0.0, maxJoinJS = 0.0;
	                int maxInd = 0;
	                int negated = 0;

	                if (appset.rightDisjunction == true) {
	                    // TIntHashSet lRE=redescriptions.get(k).computeElements(redescriptions.get(k).viewElementsLists.get(0), dat, map);
	                    // TIntHashSet rRE=redescriptions.get(k).computeElements(redescriptions.get(k).viewElementsLists.get(1), dat, map);
	                    ArrayList<TIntHashSet> sideElems = redescriptions.get(k).computeElementsGen(dat, map);//new ArrayList<>();//to make proper generalization
	                    int interCount = js[0].computeGenInterCount(redescriptions.get(k), sideElems, 1);
	                    //sideElems.add(lRE); sideElems.add(rRE);
	                    for (int j = oldIndexRR1; j < rr1.rules.size(); j++) {
	                        // joinJS=js[0].computeRedescriptionRuleElementJacard(redescriptions.get(k), rr1.rules.get(j), lRE, rRE, 1,0, dat, map,appset);
	                        joinJS = js[0].computeRedescriptionRuleElementJacardGen(redescriptions.get(k), rr1.rules.get(j), sideElems, 1, 0, dat, map, appset, interCount);
	                        if (joinJS > maxJoinJS) {
	                            maxJoinJS = joinJS;
	                            maxInd = j;
	                            negated = 0;
	                        }

	                        if (appset.rightNegation == true) {
	                            //joinJS=js[0].computeRedescriptionRuleElementJacard(redescriptions.get(k), rr1.rules.get(j), lRE, rRE, 1,1, dat, map,appset);
	                            joinJS = js[0].computeRedescriptionRuleElementJacardGen(redescriptions.get(k), rr1.rules.get(j), sideElems, 1, 1, dat, map, appset, interCount);
	                            if (joinJS > maxJoinJS) {
	                                maxJoinJS = joinJS;
	                                maxInd = j;
	                                negated = 1;
	                            }
	                        }
	                    }
	                    // System.out.println("maxJoinJS: "+maxJoinJS);
	                    if (maxJoinJS > 0.5) {
	                               /*System.out.println("max index: "+maxInd);
	                               System.out.println("maxJoinJS: "+maxJoinJS);
	                               System.out.println("negated: "+negated);*/
	                        redescriptions.get(k).disjunctiveJoin(rr1.rules.get(maxInd), appset, dat, map, sideElems, 1, negated);
	                    }
	                }
	                if (appset.leftDisjunction == true) {
	                    //TIntHashSet lRE=redescriptions.get(k).computeElements(redescriptions.get(k).viewElementsLists.get(0), dat, map);//to generalize
	                    // TIntHashSet rRE=redescriptions.get(k).computeElements(redescriptions.get(k).viewElementsLists.get(1), dat, map);
	                    ArrayList<TIntHashSet> sideElems = redescriptions.get(k).computeElementsGen(dat, map);
	                    int interCount = js[0].computeGenInterCount(redescriptions.get(k), sideElems, 0);
	                    //ArrayList<TIntHashSet> sideElems=new ArrayList<>();//to make proper generalization
	                    // sideElems.add(lRE); sideElems.add(rRE);
	                    joinJS = 0.0;
	                    maxJoinJS = 0.0;
	                    maxInd = 0;
	                    negated = 0;
	                    for (int i = oldIndexRR; i < rr.rules.size(); i++) {
	                        //joinJS=js[0].computeRedescriptionRuleElementJacard(redescriptions.get(k), rr.rules.get(i),lRE,rRE, 0,0, dat, map,appset);
	                        joinJS = js[0].computeRedescriptionRuleElementJacardGen(redescriptions.get(k), rr.rules.get(i), sideElems, 0, 0, dat, map, appset, interCount);
	                        if (joinJS > maxJoinJS) {
	                            maxJoinJS = joinJS;
	                            maxInd = i;
	                            negated = 0;
	                        }

	                        if (appset.leftNegation == true) {
	                            //joinJS=js[0].computeRedescriptionRuleElementJacard(redescriptions.get(k), rr.rules.get(i),lRE,rRE, 0,1, dat, map,appset);
	                            joinJS = js[0].computeRedescriptionRuleElementJacardGen(redescriptions.get(k), rr.rules.get(i), sideElems, 0, 1, dat, map, appset, interCount);
	                            if (joinJS > maxJoinJS) {
	                                maxJoinJS = joinJS;
	                                maxInd = i;
	                                negated = 1;
	                            }
	                        }
	                    }
	                    //System.out.println("maxJoinJS: "+maxJoinJS);
	                    if (maxJoinJS > 0.5) {
	                                    /* System.out.println("max index: "+maxInd);
	                                     System.out.println("maxJoinJS: "+maxJoinJS);
	                                     System.out.println("negated: "+negated);*/
	                        redescriptions.get(k).disjunctiveJoin(rr.rules.get(maxInd), appset, dat, map, sideElems, 0, negated);
	                    }
	                }

	                if ((k + 1) % step == 0) {
	                    System.out.println((((double) (k + 1) / redescriptions.size()) * 100) + "% completed...");
	                    System.out.println("num redescriptions: " + redescriptions.size());
	                    Runtime.getRuntime().gc();
	                }
	            }
	        }

	        for (int t = 0; t < 2; t++) {
	            System.out.println("Validation" + t);
	            adaptSet(dat, map, 1);

	            for (int i = 0; i < redescriptions.size(); i++) {
	                redescriptions.get(i).validate(dat, map);
	            }

	            for (int i = 0; i < redescriptions.size(); i++)
	                redescriptions.get(i).clearRuleMaps();
	        }

	        if (newRedescriptions == 0)//should be removed
	            newRedescriptions = 1;

	        return newRedescriptions;
	}

	double computeScoreGain(Redescription tmp, Rule rT, ApplicationSettings appset, DataSetCreator dat, int elemFreq[],
			int attrFreq[], ArrayList<Double> redScore, ArrayList<Double> redScoreAt, int totalElFreq, int totalAtFreq,
			int rScoreEl, int rScoreAt, double newJS) {

		double gain = 0.0;

		int totalEF = totalElFreq, totalAF = totalAtFreq;
		double score = rScoreEl, ruleScoreEl = 0.0, ruleScoreAt = 0.0;

		TIntIterator it = rT.elements.iterator();
		while (it.hasNext()) {
			int element = it.next();
			if (!tmp.elements.contains(element))
				ruleScoreEl += (elemFreq[element]);
		}

		double atrScore = rScoreAt;
		it = rT.ruleAtts.iterator();
		while (it.hasNext()) {
			int at = it.next();
			ruleScoreAt += (attrFreq[at]);
		}

		double rSc = (/* 0.5 **/score / totalEF + /* 0.5 **/atrScore / totalAF);
		double rScDisj = (/* 0.5 **/(score + ruleScoreEl) / totalEF + /* 0.5 **/(atrScore + ruleScoreAt) / totalAF);

		gain = ((2.0 - rScDisj + newJS) / 3.0 - (2.0 - rSc + tmp.JS) / 3.0);
		// gain=(/*0.5-0.5**/2.0-rScDisj+/*0.5**/newJS-0.5+0.5*rSc-0.5*tmp.JS);

		return gain;
	}

	public int equalSubs = 0;

	private int tryAdd(Redescription tmp, Jacard tJS, boolean join) {

		if (redescriptions.size() < appset.numInitial && tmp.JS >= appset.minJS) {
			if (appset.removePValBeforeJoin) {
				tmp.computePVal(dat, map);
				if (tmp.pVal > appset.maxPval) return -1;
			}
			int added = 0;
			for (int k = 0; k < redescriptions.size(); k++) {
				if (redescriptions.get(k).elements.size() == tmp.elements.size()) {
					tJS.computeRedescriptionElementJacard(tmp, redescriptions.get(k));
					if (tJS.JS == 1.0) {
						equalSubs++;
						if (tmp.JS > redescriptions.get(k).JS) {
							redescriptions.get(k).updateFreqTable(elemFreq, attrFreq, redScore, redScoreAt,
									redDistCoverage, redDistCoverageAt, tmp, this, dat);
							redescriptions.set(k, tmp);
							added = 1;
							break;
						} else {
							added = 1;
							break;
						}
					}
				}
			}

			if (added == 1) {
				return -1;
			}

			int index = redescriptions.size();
			redScoreAt.set(index, 0.0);
			redScore.set(index, 0.0);
			redDistCoverage.set(index, 0.0);
			// redDistCoverageAt.set(index, 0.0);
			if (appset.useNetworkAsBackground == true)
				redDistNetwork.set(index, 0.0);
			maxDiffScoreDistribution.set(index, 0.0);

			if (appset.attributeImportance != 0)
				targetAtScore.set(index, 0.0);
			redescriptions.add(tmp);

			TIntIterator it = tmp.elements.iterator();
			while (it.hasNext()) {
				int element = it.next();
				elemFreq[element] += 1;
			}

			ArrayList<Integer> attr = tmp.computeAttributesRed(tmp.viewElementsLists, dat);
			for (int itSet = 0; itSet < attr.size(); itSet++)
				attrFreq[attr.get(itSet)] += 1;

			if (redescriptions.size() == appset.numInitial) {

				int totalEF = 0, totalAF = 0;

				for (int tm = 0; tm < elemFreq.length; tm++)
					totalEF += elemFreq[tm];

				for (int tm = 0; tm < attrFreq.length; tm++)
					totalAF += attrFreq[tm];

				for (int k = 0; k < redescriptions.size(); k++) {

					redScore.set(k, redescriptions.get(k).computeScore(elemFreq, attrFreq, dat));
					redScoreAt.set(k, redescriptions.get(k).computeScoreAttr(elemFreq, attrFreq, dat));
					redScoreAt.set(k, redescriptions.get(k).computeScoreAttr(elemFreq, attrFreq, dat));
					redDistCoverage.set(k, redescriptions.get(k).computeDistCoverage(elemFreq, attrFreq, dat));
					// redDistCoverageAt.set(k,redescriptions.get(k).computeDistCoverageAt(elemFreq,
					// attrFreq, dat));
					if (appset.useNetworkAsBackground == true)
						redDistNetwork.set(k, redescriptions.get(k).computeNetworkDensity(mat, appset));
					if (appset.attributeImportance != 0)
						targetAtScore.set(k, redescriptions.get(k).computeScoreTargeted(map, appset, dat));
				}

				if (appset.redesSetSizeType == 0) {
					double prefs[] = appset.preferences.get(preferenceRow);
					for (int k = 0; k < redescriptions.size(); k++) {
						double score1 = redScore.get(k);
						double scoreAtt1 = redScoreAt.get(k);
						double pScore = 1.0;
						if (redescriptions.get(k).pVal > 0.0)
							pScore = Math.log10(redescriptions.get(k).pVal) / (-17.0);
						double cum1 = prefs[appset.preferenceHeader.get("ElemDivImp")] * score1
								/ (totalEF/* +redDistCoverage.get(k) */)
								+ prefs[appset.preferenceHeader.get("AttDivImp")] * scoreAtt1
										/ (totalAF/* +redDistCoverageAt.get(k) */)
								+ prefs[appset.preferenceHeader.get("JSImp")] * (1.0 - redescriptions.get(k).JS)
								+ prefs[appset.preferenceHeader.get("PValImp")] * (1.0 - pScore)
								+ prefs[appset.preferenceHeader.get("ECoverage")]
										* (1.0 - redDistCoverage.get(k) / dat.numExamples);
						if (appset.useNetworkAsBackground == true)
							cum1 += prefs[appset.preferenceHeader.get("NetImportance")] * (1.0 - redDistNetwork.get(k));

						double maxDiff = Double.POSITIVE_INFINITY;
						for (int z = 0; z < redescriptions.size(); z++) {
							if (z == k)
								continue;
							double score2 = redScore.get(z);
							double scoreAtt2 = redScoreAt.get(z);
							double pScore1 = 1.0;
							if (redescriptions.get(z).pVal > 0.0)
								pScore1 = Math.log10(redescriptions.get(z).pVal) / (-17.0);
							double cum2 = prefs[appset.preferenceHeader.get("ElemDivImp")] * score2
									/ (totalEF/* +redDistCoverage.get(z) */)
									+ prefs[appset.preferenceHeader.get("AttDivImp")] * scoreAtt2
											/ (totalAF/* +redDistCoverage.get(z) */)
									+ prefs[appset.preferenceHeader.get("JSImp")] * (1.0 - redescriptions.get(z).JS)
									+ prefs[appset.preferenceHeader.get("PValImp")] * (1.0 - pScore1)
									+ prefs[appset.preferenceHeader.get("ECoverage")]
											* (1.0 - redDistCoverage.get(z) / dat.numExamples);
							if (appset.useNetworkAsBackground == true)
								cum2 += prefs[appset.preferenceHeader.get("NetImportance")]
										* (1.0 - redDistNetwork.get(z));

							// double diff=cum2/5-cum1/5;
							double diff = cum2 - cum1;
							if (Math.abs(diff) < maxDiff)
								maxDiff = Math.abs(diff);
							/*
							 * if(diff>maxDiff) maxDiff=diff;
							 */
						}
						maxDiffScoreDistribution.set(k, maxDiff);
					}
					DescriptiveStatistics stat = new DescriptiveStatistics();
					for (Double num : maxDiffScoreDistribution)
						stat.addValue(num);

					double q1 = stat.getPercentile(25);
					double q3 = stat.getPercentile(75);
					double out = q3 + 1.5 * (q3 - q1);
					statistics[1] = out;
					statistics[0] = 0;
				}
			}
		}
		else {
			if (appset.removePValBeforeJoin) tmp.computePVal(dat, map);
			if (join) {// should update all arrays/attribute based after each join
				int count = 0;
				for (int k = 0; k < redescriptions.size(); k++) {
					int quality = tmp.CompareQuality(redescriptions.get(k));

					if (quality == -1) {
						continue;
					} else if (quality == 2) {

						if (tmp.elements.size() == redescriptions.get(k).elements.size()) {
							redescriptions.get(k).join(tmp, map, dat);
							count++;
						} else {
							tmp.join(redescriptions.get(k), map, dat);
							count++;
						}
					} else if (quality == 1 && redescriptions.get(k).JS < 1.0) {
						redescriptions.get(k).join(tmp, map, dat);
						count++;
					}
				}
				System.out.println("num join: " + count);
			}

			if (tmp.JS < appset.minJS) return -1;
			if (appset.removePValBeforeJoin && tmp.pVal > appset.maxPval) return -1;

			if (appset.numRetRed == Integer.MAX_VALUE) {
				for (int k = 0; k < redescriptions.size(); k++) {
					if (redescriptions.get(k).elements.size() == tmp.elements.size()) {
						tJS.computeRedescriptionElementJacard(tmp, redescriptions.get(k));
						if (tJS.JS == 1.0) {
							if (tmp.JS > redescriptions.get(k).JS)
								redescriptions.set(k, tmp);
						}
					}
				}
				redescriptions.add(tmp);
				return 1;
			}

			int totalEF = 0, totalAF = 0;

			for (int tm = 0; tm < elemFreq.length; tm++)
				totalEF += elemFreq[tm];

			for (int tm = 0; tm < attrFreq.length; tm++)
				totalAF += attrFreq[tm];

			if (statistics[0] == 1.0 && appset.redesSetSizeType == 0) {
				double prefs[] = appset.preferences.get(preferenceRow);
				for (int k = 0; k < redescriptions.size(); k++) {
					double score1 = redScore.get(k);
					double scoreAtt1 = redScoreAt.get(k);
					double pScore = 1.0;
					if (redescriptions.get(k).pVal > 0.0)
						pScore = Math.log10(redescriptions.get(k).pVal) / (-17.0);
					// double
					// cum1=score1/(totalEF/*+redDistCoverage.get(k)*/)+scoreAtt1/(totalAF/*+redDistCoverageAt.get(k)*/)+1.0-redescriptions.get(k).JS+1.0-pScore+redDistCoverage.get(k)/dat.numExamples;
					double cum1 = prefs[appset.preferenceHeader.get("ElemDivImp")] * score1
							/ (totalEF/* +redDistCoverage.get(k) */)
							+ prefs[appset.preferenceHeader.get("AttDivImp")] * scoreAtt1
									/ (totalAF/* +redDistCoverageAt.get(k) */)
							+ prefs[appset.preferenceHeader.get("JSImp")] * (1.0 - redescriptions.get(k).JS)
							+ prefs[appset.preferenceHeader.get("PValImp")] * (1.0 - pScore)
							+ prefs[appset.preferenceHeader.get("ECoverage")]
									* (1.0 - redDistCoverage.get(k) / dat.numExamples);
					if (appset.useNetworkAsBackground == true)
						cum1 += prefs[appset.preferenceHeader.get("NetImportance")] * (1.0 - redDistNetwork.get(k));
					// double
					// cum1=score1/(totalEF/*+redDistCoverage.get(k)*/)+scoreAtt1/(totalAF/*+redDistCoverageAt.get(k)*/)+1.0-redescriptions.get(k).JS;
					double maxDiff = Double.POSITIVE_INFINITY;
					for (int z = 0; z < redescriptions.size(); z++) {
						if (z == k)
							continue;

						double pScore1 = 1.0;
						if (redescriptions.get(z).pVal > 0.0)
							pScore1 = Math.log10(redescriptions.get(z).pVal) / (-17.0);

						double score2 = redScore.get(z);
						double scoreAtt2 = redScoreAt.get(z);
						// double
						// cum2=score2/(totalEF/*+redDistCoverage.get(z)*/)+scoreAtt2/(totalAF/*+redDistCoverageAt.get(z)*/)+1.0-redescriptions.get(z).JS;
						// double
						// cum2=score2/(totalEF/*+redDistCoverage.get(z)*/)+scoreAtt2/(totalAF/*+redDistCoverageAt.get(z)*/)+1.0-redescriptions.get(z).JS+1.0-pScore1+redDistCoverage.get(z)/dat.numExamples;
						double cum2 = prefs[appset.preferenceHeader.get("ElemDivImp")] * score2
								/ (totalEF/* +redDistCoverage.get(z) */)
								+ prefs[appset.preferenceHeader.get("AttDivImp")] * scoreAtt2
										/ (totalAF/* +redDistCoverage.get(z) */)
								+ prefs[appset.preferenceHeader.get("JSImp")] * (1.0 - redescriptions.get(z).JS)
								+ prefs[appset.preferenceHeader.get("PValImp")] * (1.0 - pScore1)
								+ prefs[appset.preferenceHeader.get("ECoverage")]
										* (1.0 - redDistCoverage.get(z) / dat.numExamples);
						if (appset.useNetworkAsBackground == true)
							cum2 += prefs[appset.preferenceHeader.get("NetImportance")] * (1.0 - redDistNetwork.get(z));
						// double diff=cum2/5-cum1/5;
						double diff = cum2 - cum1;
						if (maxDiff > Math.abs(diff))
							maxDiff = Math.abs(diff);
						/*
						 * if(diff>maxDiff) maxDiff=diff;
						 */
					}
					maxDiffScoreDistribution.set(k, maxDiff);
				}

				DescriptiveStatistics stat = new DescriptiveStatistics();
				for (Double num : maxDiffScoreDistribution)
					stat.addValue(num);

				double q1 = stat.getPercentile(25);
				double q3 = stat.getPercentile(75);
				double out = q3 + 1.5 * (q3 - q1);
				statistics[1] = out;
				statistics[0] = 0;
				System.out.println("Updating statistics");
				System.out.println("Outlier: " + statistics[1]);
			}

			ArrayList<Integer> indexes = new ArrayList<>();
			ArrayList<Integer> potEq = new ArrayList<>();
			double score = 0.0, uncovered = 0.0, uncoveredAt = 0.0;
			double maxDif = 0.0, maxDifNext = 0.0, maxDifPrev = 0.0, minDif = Double.POSITIVE_INFINITY;
			int control = 1;

			TIntIterator it = tmp.elements.iterator();
			while (it.hasNext()) {
				int element = it.next();
				score += elemFreq[element];
				if (elemFreq[element] == 0)
					uncovered++;
			}

			double atrScore = 0.0;
			// atrScore=tmp.computeScoreAttr(elemFreq, attrFreq, dat);
			ArrayList<Integer> attr = tmp.computeAttributesRed(tmp.viewElementsLists, dat);
			for (int itSet = 0; itSet < attr.size(); itSet++) {
				/*
				 * it=attr.get(itSet).iterator(); while(it.hasNext()){ int at=it.next();
				 */
				int at = attr.get(itSet);
				atrScore += attrFreq[at];

				if (attrFreq[at] == 0)
					uncoveredAt++;

				// }
			}

			int minInd = -1, minIndDJS = -1;

			double rSc = (/* 0.5 **/score / (totalEF/* +uncovered */)
					+ /* 0.5 **/atrScore / (totalAF/* +uncoveredAt */)), tRScG = 0.0;
			double targetedScore = 0.0;

			if (appset.attributeImportance != 0) {

				targetedScore = tmp.computeScoreTargeted(map, appset, dat);

			}

			double pScoreTmp = 1.0;
			if (tmp.pVal > 0)
				pScoreTmp = Math.log10(tmp.pVal) / (-17.0);

			double prefs[] = appset.preferences.get(preferenceRow);

			double rdens = 0.0;
			if (appset.useNetworkAsBackground == true)
				tmp.computeNetworkDensity(mat, appset);

			if (appset.attributeImportance == 0)
				for (int k = 0; k < redescriptions.size(); k++) {// change scores here!!!!
					double tRSc = (/* 0.5 **/(redScore.get(k) / (totalEF/* +redDistCoverage.get(k) */))
							+ /* 0.5 **/(redScoreAt.get(k) / (totalAF/* +redDistCoverageAt.get(k) */)));

					if (redescriptions.get(k).elements.size() == tmp.elements.size())
						potEq.add(k);

					double pScoreComp = 1.0;

					if (redescriptions.get(k).pVal > 0)
						pScoreComp = Math.log10(redescriptions.get(k).pVal) / (-17.0);

					double score1 = 0.0;
					score1 = prefs[appset.preferenceHeader.get("ElemDivImp")] * redScore.get(k)
							/ (totalEF/* +redDistCoverage.get(k) */)
							+ prefs[appset.preferenceHeader.get("AttDivImp")] * redScoreAt.get(k) / (totalAF)
							+ prefs[appset.preferenceHeader.get("JSImp")] * (1.0 - redescriptions.get(k).JS)
							+ prefs[appset.preferenceHeader.get("PValImp")] * (1.0 - pScoreComp)
							+ prefs[appset.preferenceHeader.get("ECoverage")]
									* (1.0 - redDistCoverage.get(k) / dat.numExamples);
					if (appset.useNetworkAsBackground == true)
						score1 += prefs[appset.preferenceHeader.get("NetImportance")] * (1.0 - redDistNetwork.get(k));

					double score2 = 0.0;

					score2 = prefs[appset.preferenceHeader.get("ElemDivImp")] * score
							/ (totalEF/* +redDistCoverage.get(k) */)
							+ prefs[appset.preferenceHeader.get("AttDivImp")] * atrScore / (totalAF)
							+ prefs[appset.preferenceHeader.get("JSImp")] * (1.0 - tmp.JS)
							+ prefs[appset.preferenceHeader.get("PValImp")] * (1.0 - pScoreTmp)
							+ prefs[appset.preferenceHeader.get("ECoverage")] * (1.0 - uncovered / dat.numExamples);
					if (appset.useNetworkAsBackground == true)
						score2 += prefs[appset.preferenceHeader.get("NetImportance")] * (1.0 - rdens);

					if (score2 < score1) {
						// if((2.0-tRSc+redescriptions.get(k).JS+pScoreComp+redDistCoverage.get(k)/dat.numExamples)<(2.0-rSc+tmp.JS+pScoreTmp+uncovered/dat.numExamples)
						// /*&& tmp.pVal<=redescriptions.get(k).pVal &&
						// redescriptions.get(k).JS!=1.0*/){
						// if((/*0.5**/1.0-0.5*tRSc+0.5*redescriptions.get(k).JS)<(/*0.5**/1.0-0.5*rSc+0.5*tmp.JS)
						// && tmp.pVal<=redescriptions.get(k).pVal && redescriptions.get(k).JS!=1.0){
						double d = score1 - score2;
						// double
						// d=(0.5*1.0-0.5*rSc+0.5*tmp.JS-0.5*1.0+0.5*tRSc-0.5*redescriptions.get(k).JS);
						// double
						// d=((2.0-rSc+tmp.JS+pScoreTmp+uncovered/dat.numExamples)/5.0-(2.0-tRSc+redescriptions.get(k).JS+pScoreComp+redDistCoverage.get(k)/dat.numExamples)/5.0);

						if (d > maxDif) {
							// maxDifPrev=maxDifNext;
							// maxDifNext= (tRSc+(1-redescriptions.get(k).JS))/3-(rSc+(1-tmp.JS))/3;
							minInd = k;
							maxDif = d;
							tRScG = tRSc;
						}
						if (d < minDif && d > 0) {
							maxDifPrev = maxDifNext;
							maxDifNext = score1 - score2;
							// maxDifNext=
							// (tRSc+(1-redescriptions.get(k).JS)+1.0-pScoreComp+1-redDistCoverage.get(k)/dat.numExamples)/5-(rSc+(1-tmp.JS)+1.0-pScoreTmp+1-uncovered/dat.numExamples)/5;

							minDif = d;
						} else if (d < 0) {
							control = 0;
							maxDifPrev = Double.NEGATIVE_INFINITY;
							maxDifNext = Double.NEGATIVE_INFINITY;
						}
					} // popraviti, ako postoji redescription s boljim svojstvima ne prosirimo skup.
						// Inace gledamo minimalnu razliku i odlucimo
					else {
						if (tmp.JS > 0.9) {
							System.out.println("Accurate redes: " + tmp.JS);
							System.out.println("EASc: " + (2.0 - rSc));
							System.out.println("pValSc: " + pScoreTmp);
							System.out.println("pVal: " + tmp.pVal);
							System.out.println("redCoverageSc: " + redDistCoverage.get(k) / dat.numExamples);
							System.out.println("minInd: " + minInd);

						}
						control = 0;
						maxDifPrev = Double.NEGATIVE_INFINITY;
						maxDifNext = Double.NEGATIVE_INFINITY;
					}
				}
			else { // in case targeted RM is used
				for (int k = 0; k < redescriptions.size(); k++) {
					double tRSc = (/* 0.5 **/(redScore.get(k) / (totalEF/* +redDistCoverage.get(k) */))
							+ /* 0.5 **/(redScoreAt.get(k) / (totalAF/* +redDistCoverageAt.get(k) */)));

					if (redescriptions.get(k).elements.size() == tmp.elements.size())
						potEq.add(k);
					if ((2.0 - tRSc + redescriptions.get(k).JS + targetAtScore.get(k)) < (2.0 - rSc + tmp.JS
							+ targetedScore) && tmp.pVal <= redescriptions.get(k).pVal
							&& redescriptions.get(k).JS != 1.0) {
						// if((/*0.5**/1.0-0.5*tRSc+0.5*redescriptions.get(k).JS)<(/*0.5**/1.0-0.5*rSc+0.5*tmp.JS)
						// && tmp.pVal<=redescriptions.get(k).pVal && redescriptions.get(k).JS!=1.0){
						double d = ((2.0 - rSc + tmp.JS + targetedScore) / 4.0
								- (2.0 - tRSc + redescriptions.get(k).JS + targetAtScore.get(k)) / 4.0);
						// double
						// d=(0.5*1.0-0.5*rSc+0.5*tmp.JS-0.5*1.0+0.5*tRSc-0.5*redescriptions.get(k).JS);
						if (d > maxDif) {
							minInd = k;
							maxDif = d;
							tRScG = tRSc;
						}
					}
				}
			}

			if (maxDifNext > statistics[1] && appset.redesSetSizeType == 0 && control == 1) {
				System.out.println("maxDifNext: " + maxDifNext);
				System.out.println("outlier: " + statistics[1]);
				redescriptions.add(tmp);
				redScore.add(score/* tmp.computeScore(elemFreq, attrFreq, dat) */);
				redScoreAt.add(atrScore/* tmp.computeScoreAttr(elemFreq, attrFreq, dat) */);
				redDistCoverage.add(uncovered);
				redDistCoverageAt.add(uncoveredAt);
				if (appset.useNetworkAsBackground == true)
					redDistNetwork.add(tmp.computeNetworkDensity(mat, appset));
				if (appset.attributeImportance != 0)
					targetAtScore.add(tmp.computeScoreTargeted(map, appset, dat));

				maxDiffScoreDistribution.add(maxDifPrev);
				tmp.updateScoreNew(elemFreq, attrFreq, redScore, redScoreAt, redDistCoverage, redDistCoverageAt,
						statistics, this, dat);// finish this function

				return 1;
			}
			/*
			 * else{ if(control==1){ System.out.println("Not sufficient difference.");
			 * System.out.println("maxDifNext: "+maxDifNext);
			 * System.out.println("outlier: "+Statistics[1]); } else{
			 * System.out.println("Redescription with better properties exists in the set");
			 * } }
			 */

			if (minInd != -1)
				indexes.add(minInd);

			int added = 0;

			for (int k : potEq) {
				// tmp.
				tJS.computeRedescriptionElementJacard(tmp, redescriptions.get(k));
				if (tJS.JS == 1.0) {// check for the case where target rm used
					if (appset.attributeImportance == 0) {
						equalSubs++;
						tRScG = /* 0.5 **/(redScore.get(k) / totalEF) + /* 0.5 **/(redScoreAt.get(k) / totalAF);

						if (appset.attributeImportance == 0)
							tRScG += 1.0;
						else
							tRScG += (1.0 - targetAtScore.get(k));
						// tRScG=0.5*(redScore.get(k)/totalEF)+0.5*(redScoreAt.get(k)/totalAF);
						if ((rSc + 1 - targetedScore) < tRScG && tmp.JS >= redescriptions.get(k).JS) {
							redescriptions.get(k).updateScore(elemFreq, attrFreq, redScore, redScoreAt, redDistCoverage,
									statistics, tmp, this, dat);
							System.out.println("update score equal supp");
							redescriptions.set(k, tmp);
							redScore.set(k, score);
							redScoreAt.set(k, atrScore);
							maxDiffScoreDistribution.set(k, maxDifPrev);
							if (appset.attributeImportance != 0)
								targetAtScore.set(k, targetedScore);
						}
						added = 1;
						break;
					}
				}
			}

			if (added == 0) {

				for (int k : indexes) {
					// tRScG=0.5*(redScore.get(k)/totalEF)+0.5*(redScoreAt.get(k)/totalAF);
					// if(rSc<tRScG){

					/*
					 * if(tRScG>2.0){ System.out.println("Problem with old score...");
					 * System.out.println("element score: "+redScore.get(k)/totalEF);
					 * System.out.println("attribute score: "+redScoreAt.get(k)/totalAF); }
					 * if(rSc>2.0){ System.out.println("Problem with new score...");
					 * System.out.println("element score: "+score/totalEF);
					 * System.out.println("attribute score: "+atrScore/totalAF); }
					 */

					redescriptions.get(k).updateScore(elemFreq, attrFreq, redScore, redScoreAt, redDistCoverage,
							statistics, tmp, this, dat);
					System.out.println("update score not equal exchange");
					System.out.println("Redescription removed.");
					System.out.println("Previous jaccard: " + redescriptions.get(k).JS);
					System.out.println("New jaccard: " + tmp.JS);
					System.out.println("Old score: " + tRScG);
					System.out.println("New score: " + rSc);
					redescriptions.set(k, tmp);

					TIntIterator el = tmp.elements.iterator();

					score = 0.0;
					while (el.hasNext()) {
						score += elemFreq[el.next()] - 1;
					}

					if (score < 0)
						System.out.println("Score<0 after update elements no equal supp");

					ArrayList<Integer> a = tmp.computeAttributesRed(tmp.viewElementsLists, dat);

					atrScore = 0.0;
					for (int iT = 0; iT < a.size(); iT++)
						atrScore += attrFreq[a.get(iT)] - 1;

					if (atrScore < 0)
						System.out.println("Score<0 after update atributes no equal supp");

					redScore.set(k, score);
					redScoreAt.set(k, atrScore);
					redDistCoverage.set(k, redescriptions.get(k).computeDistCoverageAt(elemFreq, attrFreq, dat));
					if (appset.useNetworkAsBackground == true)
						redDistNetwork.set(k, redescriptions.get(k).computeNetworkDensity(mat, appset));
					if (appset.attributeImportance != 0)
						targetAtScore.set(k, targetedScore/* tmp.computeScoreTargeted(map, appset, dat) */);

					added = 1;
					break;
				}
				// }
			}
			if (added == 1)
				return 1;
		}
		return 1;
	}

	int tryAdd(Redescription tmp, Jacard tJS, ApplicationSettings appset, DataSetCreator dat, Mappings map,
			int elemFreq[], int attrFreq[], ArrayList<Double> redScore, ArrayList<Double> redScoreAt,
			ArrayList<Double> redDistCoverage, ArrayList<Double> redDistCoverageAt, ArrayList<Double> redDistNetwork,
			ArrayList<Double> targetAtScore, double Statistics[], ArrayList<Double> maxDiffScoreDistribution,
			NHMCDistanceMatrix mat, int PreferenceRow, boolean join) {
		// Jacard tJS=new Jacard(); tJS.initialize();

		if (redescriptions.size() < appset.numInitial && tmp.JS >= appset.minJS) {
			int added = 0;
			for (int k = 0; k < redescriptions.size(); k++) {
				if (redescriptions.get(k).elements.size() == tmp.elements.size()) {
					tJS.computeRedescriptionElementJacard(tmp, redescriptions.get(k));
					if (tJS.JS == 1.0) {
						equalSubs++;
						if (tmp.JS > redescriptions.get(k).JS) {
							redescriptions.get(k).updateFreqTable(elemFreq, attrFreq, redScore, redScoreAt,
									redDistCoverage, redDistCoverageAt, tmp, this, dat);
							redescriptions.set(k, tmp);
							added = 1;
							break;
						} else {
							added = 1;
							break;
						}
					}
				}
			}

			if (added == 1) {
				return -1;
			}

			int index = redescriptions.size();
			redScoreAt.set(index, 0.0);
			redScore.set(index, 0.0);
			redDistCoverage.set(index, 0.0);
			// redDistCoverageAt.set(index, 0.0);
			if (appset.useNetworkAsBackground == true)
				redDistNetwork.set(index, 0.0);
			maxDiffScoreDistribution.set(index, 0.0);

			if (appset.attributeImportance != 0)
				targetAtScore.set(index, 0.0);
			redescriptions.add(tmp);

			TIntIterator it = tmp.elements.iterator();
			while (it.hasNext()) {
				int element = it.next();
				elemFreq[element] += 1;
			}

			ArrayList<Integer> attr = tmp.computeAttributesRed(tmp.viewElementsLists, dat);
			for (int itSet = 0; itSet < attr.size(); itSet++)
				attrFreq[attr.get(itSet)] += 1;

			if (redescriptions.size() == appset.numInitial) {

				int totalEF = 0, totalAF = 0;

				for (int tm = 0; tm < elemFreq.length; tm++)
					totalEF += elemFreq[tm];

				for (int tm = 0; tm < attrFreq.length; tm++)
					totalAF += attrFreq[tm];

				for (int k = 0; k < redescriptions.size(); k++) {

					redScore.set(k, redescriptions.get(k).computeScore(elemFreq, attrFreq, dat));
					redScoreAt.set(k, redescriptions.get(k).computeScoreAttr(elemFreq, attrFreq, dat));
					redScoreAt.set(k, redescriptions.get(k).computeScoreAttr(elemFreq, attrFreq, dat));
					redDistCoverage.set(k, redescriptions.get(k).computeDistCoverage(elemFreq, attrFreq, dat));
					// redDistCoverageAt.set(k,redescriptions.get(k).computeDistCoverageAt(elemFreq,
					// attrFreq, dat));
					if (appset.useNetworkAsBackground == true)
						redDistNetwork.set(k, redescriptions.get(k).computeNetworkDensity(mat, appset));
					if (appset.attributeImportance != 0)
						targetAtScore.set(k, redescriptions.get(k).computeScoreTargeted(map, appset, dat));
				}

				if (appset.redesSetSizeType == 0) {
					double prefs[] = appset.preferences.get(PreferenceRow);
					for (int k = 0; k < redescriptions.size(); k++) {
						double score1 = redScore.get(k);
						double scoreAtt1 = redScoreAt.get(k);
						double pScore = 1.0;
						if (redescriptions.get(k).pVal > 0.0)
							pScore = Math.log10(redescriptions.get(k).pVal) / (-17.0);
						double cum1 = prefs[appset.preferenceHeader.get("ElemDivImp")] * score1
								/ (totalEF/* +redDistCoverage.get(k) */)
								+ prefs[appset.preferenceHeader.get("AttDivImp")] * scoreAtt1
										/ (totalAF/* +redDistCoverageAt.get(k) */)
								+ prefs[appset.preferenceHeader.get("JSImp")] * (1.0 - redescriptions.get(k).JS)
								+ prefs[appset.preferenceHeader.get("PValImp")] * (1.0 - pScore)
								+ prefs[appset.preferenceHeader.get("ECoverage")]
										* (1.0 - redDistCoverage.get(k) / dat.numExamples);
						if (appset.useNetworkAsBackground == true)
							cum1 += prefs[appset.preferenceHeader.get("NetImportance")] * (1.0 - redDistNetwork.get(k));

						double maxDiff = Double.POSITIVE_INFINITY;
						for (int z = 0; z < redescriptions.size(); z++) {
							if (z == k)
								continue;
							double score2 = redScore.get(z);
							double scoreAtt2 = redScoreAt.get(z);
							double pScore1 = 1.0;
							if (redescriptions.get(z).pVal > 0.0)
								pScore1 = Math.log10(redescriptions.get(z).pVal) / (-17.0);
							double cum2 = prefs[appset.preferenceHeader.get("ElemDivImp")] * score2
									/ (totalEF/* +redDistCoverage.get(z) */)
									+ prefs[appset.preferenceHeader.get("AttDivImp")] * scoreAtt2
											/ (totalAF/* +redDistCoverage.get(z) */)
									+ prefs[appset.preferenceHeader.get("JSImp")] * (1.0 - redescriptions.get(z).JS)
									+ prefs[appset.preferenceHeader.get("PValImp")] * (1.0 - pScore1)
									+ prefs[appset.preferenceHeader.get("ECoverage")]
											* (1.0 - redDistCoverage.get(z) / dat.numExamples);
							if (appset.useNetworkAsBackground == true)
								cum2 += prefs[appset.preferenceHeader.get("NetImportance")]
										* (1.0 - redDistNetwork.get(z));

							// double diff=cum2/5-cum1/5;
							double diff = cum2 - cum1;
							if (Math.abs(diff) < maxDiff)
								maxDiff = Math.abs(diff);
							/*
							 * if(diff>maxDiff) maxDiff=diff;
							 */
						}
						maxDiffScoreDistribution.set(k, maxDiff);
					}
					DescriptiveStatistics stat = new DescriptiveStatistics();
					for (Double num : maxDiffScoreDistribution)
						stat.addValue(num);

					double q1 = stat.getPercentile(25);
					double q3 = stat.getPercentile(75);
					double out = q3 + 1.5 * (q3 - q1);
					Statistics[1] = out;
					Statistics[0] = 0;
				}
			}
		} else {
			if (join == true) {// should update all arrays/attribute based after each join
				int count = 0;
				for (int k = 0; k < redescriptions.size(); k++) {
					int quality = tmp.CompareQuality(redescriptions.get(k));

					if (quality == -1) {
						continue;
					} else if (quality == 2) {

						if (tmp.elements.size() == redescriptions.get(k).elements.size()) {
							redescriptions.get(k).join(tmp, map, dat);
							count++;
						} else {
							tmp.join(redescriptions.get(k), map, dat);
							count++;
						}
					} else if (quality == 1 && redescriptions.get(k).JS < 1.0) {
						redescriptions.get(k).join(tmp, map, dat);
						count++;
					}
				}
				System.out.println("num join: " + count);
			}

			if (tmp.JS < appset.minJS)
				return -1;

			if (appset.numRetRed == Integer.MAX_VALUE) {
				for (int k = 0; k < redescriptions.size(); k++) {
					if (redescriptions.get(k).elements.size() == tmp.elements.size()) {
						tJS.computeRedescriptionElementJacard(tmp, redescriptions.get(k));
						if (tJS.JS == 1.0) {
							if (tmp.JS > redescriptions.get(k).JS)
								redescriptions.set(k, tmp);
						}
					}
				}
				redescriptions.add(tmp);
				return 1;
			}

			int totalEF = 0, totalAF = 0;

			for (int tm = 0; tm < elemFreq.length; tm++)
				totalEF += elemFreq[tm];

			for (int tm = 0; tm < attrFreq.length; tm++)
				totalAF += attrFreq[tm];

			if (Statistics[0] == 1.0 && appset.redesSetSizeType == 0) {
				double prefs[] = appset.preferences.get(PreferenceRow);
				for (int k = 0; k < redescriptions.size(); k++) {
					double score1 = redScore.get(k);
					double scoreAtt1 = redScoreAt.get(k);
					double pScore = 1.0;
					if (redescriptions.get(k).pVal > 0.0)
						pScore = Math.log10(redescriptions.get(k).pVal) / (-17.0);
					// double
					// cum1=score1/(totalEF/*+redDistCoverage.get(k)*/)+scoreAtt1/(totalAF/*+redDistCoverageAt.get(k)*/)+1.0-redescriptions.get(k).JS+1.0-pScore+redDistCoverage.get(k)/dat.numExamples;
					double cum1 = prefs[appset.preferenceHeader.get("ElemDivImp")] * score1
							/ (totalEF/* +redDistCoverage.get(k) */)
							+ prefs[appset.preferenceHeader.get("AttDivImp")] * scoreAtt1
									/ (totalAF/* +redDistCoverageAt.get(k) */)
							+ prefs[appset.preferenceHeader.get("JSImp")] * (1.0 - redescriptions.get(k).JS)
							+ prefs[appset.preferenceHeader.get("PValImp")] * (1.0 - pScore)
							+ prefs[appset.preferenceHeader.get("ECoverage")]
									* (1.0 - redDistCoverage.get(k) / dat.numExamples);
					if (appset.useNetworkAsBackground == true)
						cum1 += prefs[appset.preferenceHeader.get("NetImportance")] * (1.0 - redDistNetwork.get(k));
					// double
					// cum1=score1/(totalEF/*+redDistCoverage.get(k)*/)+scoreAtt1/(totalAF/*+redDistCoverageAt.get(k)*/)+1.0-redescriptions.get(k).JS;
					double maxDiff = Double.POSITIVE_INFINITY;
					for (int z = 0; z < redescriptions.size(); z++) {
						if (z == k)
							continue;

						double pScore1 = 1.0;
						if (redescriptions.get(z).pVal > 0.0)
							pScore1 = Math.log10(redescriptions.get(z).pVal) / (-17.0);

						double score2 = redScore.get(z);
						double scoreAtt2 = redScoreAt.get(z);
						// double
						// cum2=score2/(totalEF/*+redDistCoverage.get(z)*/)+scoreAtt2/(totalAF/*+redDistCoverageAt.get(z)*/)+1.0-redescriptions.get(z).JS;
						// double
						// cum2=score2/(totalEF/*+redDistCoverage.get(z)*/)+scoreAtt2/(totalAF/*+redDistCoverageAt.get(z)*/)+1.0-redescriptions.get(z).JS+1.0-pScore1+redDistCoverage.get(z)/dat.numExamples;
						double cum2 = prefs[appset.preferenceHeader.get("ElemDivImp")] * score2
								/ (totalEF/* +redDistCoverage.get(z) */)
								+ prefs[appset.preferenceHeader.get("AttDivImp")] * scoreAtt2
										/ (totalAF/* +redDistCoverage.get(z) */)
								+ prefs[appset.preferenceHeader.get("JSImp")] * (1.0 - redescriptions.get(z).JS)
								+ prefs[appset.preferenceHeader.get("PValImp")] * (1.0 - pScore1)
								+ prefs[appset.preferenceHeader.get("ECoverage")]
										* (1.0 - redDistCoverage.get(z) / dat.numExamples);
						if (appset.useNetworkAsBackground == true)
							cum2 += prefs[appset.preferenceHeader.get("NetImportance")] * (1.0 - redDistNetwork.get(z));
						// double diff=cum2/5-cum1/5;
						double diff = cum2 - cum1;
						if (maxDiff > Math.abs(diff))
							maxDiff = Math.abs(diff);
						/*
						 * if(diff>maxDiff) maxDiff=diff;
						 */
					}
					maxDiffScoreDistribution.set(k, maxDiff);
				}

				DescriptiveStatistics stat = new DescriptiveStatistics();
				for (Double num : maxDiffScoreDistribution)
					stat.addValue(num);

				double q1 = stat.getPercentile(25);
				double q3 = stat.getPercentile(75);
				double out = q3 + 1.5 * (q3 - q1);
				Statistics[1] = out;
				Statistics[0] = 0;
				System.out.println("Updating statistics");
				System.out.println("Outlier: " + Statistics[1]);
			}

			ArrayList<Integer> indexes = new ArrayList<>();
			ArrayList<Integer> potEq = new ArrayList<>();
			double score = 0.0, uncovered = 0.0, uncoveredAt = 0.0;
			double maxDif = 0.0, maxDifNext = 0.0, maxDifPrev = 0.0, minDif = Double.POSITIVE_INFINITY;
			int control = 1;

			TIntIterator it = tmp.elements.iterator();
			while (it.hasNext()) {
				int element = it.next();
				score += elemFreq[element];
				if (elemFreq[element] == 0)
					uncovered++;
			}

			double atrScore = 0.0;
			// atrScore=tmp.computeScoreAttr(elemFreq, attrFreq, dat);
			ArrayList<Integer> attr = tmp.computeAttributesRed(tmp.viewElementsLists, dat);
			for (int itSet = 0; itSet < attr.size(); itSet++) {
				/*
				 * it=attr.get(itSet).iterator(); while(it.hasNext()){ int at=it.next();
				 */
				int at = attr.get(itSet);
				atrScore += attrFreq[at];

				if (attrFreq[at] == 0)
					uncoveredAt++;

				// }
			}

			int minInd = -1, minIndDJS = -1;

			double rSc = (/* 0.5 **/score / (totalEF/* +uncovered */)
					+ /* 0.5 **/atrScore / (totalAF/* +uncoveredAt */)), tRScG = 0.0;
			double targetedScore = 0.0;

			if (appset.attributeImportance != 0) {

				targetedScore = tmp.computeScoreTargeted(map, appset, dat);

			}

			double pScoreTmp = 1.0;
			if (tmp.pVal > 0)
				pScoreTmp = Math.log10(tmp.pVal) / (-17.0);

			double prefs[] = appset.preferences.get(PreferenceRow);

			double rdens = 0.0;
			if (appset.useNetworkAsBackground == true)
				tmp.computeNetworkDensity(mat, appset);

			if (appset.attributeImportance == 0)
				for (int k = 0; k < redescriptions.size(); k++) {// change scores here!!!!
					double tRSc = (/* 0.5 **/(redScore.get(k) / (totalEF/* +redDistCoverage.get(k) */))
							+ /* 0.5 **/(redScoreAt.get(k) / (totalAF/* +redDistCoverageAt.get(k) */)));

					if (redescriptions.get(k).elements.size() == tmp.elements.size())
						potEq.add(k);

					double pScoreComp = 1.0;

					if (redescriptions.get(k).pVal > 0)
						pScoreComp = Math.log10(redescriptions.get(k).pVal) / (-17.0);

					double score1 = 0.0;
					score1 = prefs[appset.preferenceHeader.get("ElemDivImp")] * redScore.get(k)
							/ (totalEF/* +redDistCoverage.get(k) */)
							+ prefs[appset.preferenceHeader.get("AttDivImp")] * redScoreAt.get(k) / (totalAF)
							+ prefs[appset.preferenceHeader.get("JSImp")] * (1.0 - redescriptions.get(k).JS)
							+ prefs[appset.preferenceHeader.get("PValImp")] * (1.0 - pScoreComp)
							+ prefs[appset.preferenceHeader.get("ECoverage")]
									* (1.0 - redDistCoverage.get(k) / dat.numExamples);
					if (appset.useNetworkAsBackground == true)
						score1 += prefs[appset.preferenceHeader.get("NetImportance")] * (1.0 - redDistNetwork.get(k));

					double score2 = 0.0;

					score2 = prefs[appset.preferenceHeader.get("ElemDivImp")] * score
							/ (totalEF/* +redDistCoverage.get(k) */)
							+ prefs[appset.preferenceHeader.get("AttDivImp")] * atrScore / (totalAF)
							+ prefs[appset.preferenceHeader.get("JSImp")] * (1.0 - tmp.JS)
							+ prefs[appset.preferenceHeader.get("PValImp")] * (1.0 - pScoreTmp)
							+ prefs[appset.preferenceHeader.get("ECoverage")] * (1.0 - uncovered / dat.numExamples);
					if (appset.useNetworkAsBackground == true)
						score2 += prefs[appset.preferenceHeader.get("NetImportance")] * (1.0 - rdens);

					if (score2 < score1) {
						// if((2.0-tRSc+redescriptions.get(k).JS+pScoreComp+redDistCoverage.get(k)/dat.numExamples)<(2.0-rSc+tmp.JS+pScoreTmp+uncovered/dat.numExamples)
						// /*&& tmp.pVal<=redescriptions.get(k).pVal &&
						// redescriptions.get(k).JS!=1.0*/){
						// if((/*0.5**/1.0-0.5*tRSc+0.5*redescriptions.get(k).JS)<(/*0.5**/1.0-0.5*rSc+0.5*tmp.JS)
						// && tmp.pVal<=redescriptions.get(k).pVal && redescriptions.get(k).JS!=1.0){
						double d = score1 - score2;
						// double
						// d=(0.5*1.0-0.5*rSc+0.5*tmp.JS-0.5*1.0+0.5*tRSc-0.5*redescriptions.get(k).JS);
						// double
						// d=((2.0-rSc+tmp.JS+pScoreTmp+uncovered/dat.numExamples)/5.0-(2.0-tRSc+redescriptions.get(k).JS+pScoreComp+redDistCoverage.get(k)/dat.numExamples)/5.0);

						if (d > maxDif) {
							// maxDifPrev=maxDifNext;
							// maxDifNext= (tRSc+(1-redescriptions.get(k).JS))/3-(rSc+(1-tmp.JS))/3;
							minInd = k;
							maxDif = d;
							tRScG = tRSc;
						}
						if (d < minDif && d > 0) {
							maxDifPrev = maxDifNext;
							maxDifNext = score1 - score2;
							// maxDifNext=
							// (tRSc+(1-redescriptions.get(k).JS)+1.0-pScoreComp+1-redDistCoverage.get(k)/dat.numExamples)/5-(rSc+(1-tmp.JS)+1.0-pScoreTmp+1-uncovered/dat.numExamples)/5;

							minDif = d;
						} else if (d < 0) {
							control = 0;
							maxDifPrev = Double.NEGATIVE_INFINITY;
							maxDifNext = Double.NEGATIVE_INFINITY;
						}
					} // popraviti, ako postoji redescription s boljim svojstvima ne prosirimo skup.
						// Inace gledamo minimalnu razliku i odlucimo
					else {
						if (tmp.JS > 0.9) {
							System.out.println("Accurate redes: " + tmp.JS);
							System.out.println("EASc: " + (2.0 - rSc));
							System.out.println("pValSc: " + pScoreTmp);
							System.out.println("pVal: " + tmp.pVal);
							System.out.println("redCoverageSc: " + redDistCoverage.get(k) / dat.numExamples);
							System.out.println("minInd: " + minInd);

						}
						control = 0;
						maxDifPrev = Double.NEGATIVE_INFINITY;
						maxDifNext = Double.NEGATIVE_INFINITY;
					}
				}
			else { // in case targeted RM is used
				for (int k = 0; k < redescriptions.size(); k++) {
					double tRSc = (/* 0.5 **/(redScore.get(k) / (totalEF/* +redDistCoverage.get(k) */))
							+ /* 0.5 **/(redScoreAt.get(k) / (totalAF/* +redDistCoverageAt.get(k) */)));

					if (redescriptions.get(k).elements.size() == tmp.elements.size())
						potEq.add(k);
					if ((2.0 - tRSc + redescriptions.get(k).JS + targetAtScore.get(k)) < (2.0 - rSc + tmp.JS
							+ targetedScore) && tmp.pVal <= redescriptions.get(k).pVal
							&& redescriptions.get(k).JS != 1.0) {
						// if((/*0.5**/1.0-0.5*tRSc+0.5*redescriptions.get(k).JS)<(/*0.5**/1.0-0.5*rSc+0.5*tmp.JS)
						// && tmp.pVal<=redescriptions.get(k).pVal && redescriptions.get(k).JS!=1.0){
						double d = ((2.0 - rSc + tmp.JS + targetedScore) / 4.0
								- (2.0 - tRSc + redescriptions.get(k).JS + targetAtScore.get(k)) / 4.0);
						// double
						// d=(0.5*1.0-0.5*rSc+0.5*tmp.JS-0.5*1.0+0.5*tRSc-0.5*redescriptions.get(k).JS);
						if (d > maxDif) {
							minInd = k;
							maxDif = d;
							tRScG = tRSc;
						}
					}
				}
			}

			if (maxDifNext > Statistics[1] && appset.redesSetSizeType == 0 && control == 1) {
				System.out.println("maxDifNext: " + maxDifNext);
				System.out.println("outlier: " + Statistics[1]);
				redescriptions.add(tmp);
				redScore.add(score/* tmp.computeScore(elemFreq, attrFreq, dat) */);
				redScoreAt.add(atrScore/* tmp.computeScoreAttr(elemFreq, attrFreq, dat) */);
				redDistCoverage.add(uncovered);
				redDistCoverageAt.add(uncoveredAt);
				if (appset.useNetworkAsBackground == true)
					redDistNetwork.add(tmp.computeNetworkDensity(mat, appset));
				if (appset.attributeImportance != 0)
					targetAtScore.add(tmp.computeScoreTargeted(map, appset, dat));

				maxDiffScoreDistribution.add(maxDifPrev);
				tmp.updateScoreNew(elemFreq, attrFreq, redScore, redScoreAt, redDistCoverage, redDistCoverageAt,
						Statistics, this, dat);// finish this function

				return 1;
			}
			/*
			 * else{ if(control==1){ System.out.println("Not sufficient difference.");
			 * System.out.println("maxDifNext: "+maxDifNext);
			 * System.out.println("outlier: "+Statistics[1]); } else{
			 * System.out.println("Redescription with better properties exists in the set");
			 * } }
			 */

			if (minInd != -1)
				indexes.add(minInd);

			int added = 0;

			for (int k : potEq) {
				// tmp.
				tJS.computeRedescriptionElementJacard(tmp, redescriptions.get(k));
				if (tJS.JS == 1.0) {// check for the case where target rm used
					if (appset.attributeImportance == 0) {
						equalSubs++;
						tRScG = /* 0.5 **/(redScore.get(k) / totalEF) + /* 0.5 **/(redScoreAt.get(k) / totalAF);

						if (appset.attributeImportance == 0)
							tRScG += 1.0;
						else
							tRScG += (1.0 - targetAtScore.get(k));
						// tRScG=0.5*(redScore.get(k)/totalEF)+0.5*(redScoreAt.get(k)/totalAF);
						if ((rSc + 1 - targetedScore) < tRScG && tmp.JS >= redescriptions.get(k).JS) {
							redescriptions.get(k).updateScore(elemFreq, attrFreq, redScore, redScoreAt, redDistCoverage,
									Statistics, tmp, this, dat);
							System.out.println("update score equal supp");
							redescriptions.set(k, tmp);
							redScore.set(k, score);
							redScoreAt.set(k, atrScore);
							maxDiffScoreDistribution.set(k, maxDifPrev);
							if (appset.attributeImportance != 0)
								targetAtScore.set(k, targetedScore);
						}
						added = 1;
						break;
					}
				}
			}

			if (added == 0) {

				for (int k : indexes) {
					// tRScG=0.5*(redScore.get(k)/totalEF)+0.5*(redScoreAt.get(k)/totalAF);
					// if(rSc<tRScG){

					/*
					 * if(tRScG>2.0){ System.out.println("Problem with old score...");
					 * System.out.println("element score: "+redScore.get(k)/totalEF);
					 * System.out.println("attribute score: "+redScoreAt.get(k)/totalAF); }
					 * if(rSc>2.0){ System.out.println("Problem with new score...");
					 * System.out.println("element score: "+score/totalEF);
					 * System.out.println("attribute score: "+atrScore/totalAF); }
					 */

					redescriptions.get(k).updateScore(elemFreq, attrFreq, redScore, redScoreAt, redDistCoverage,
							Statistics, tmp, this, dat);
					System.out.println("update score not equal exchange");
					System.out.println("Redescription removed.");
					System.out.println("Previous jaccard: " + redescriptions.get(k).JS);
					System.out.println("New jaccard: " + tmp.JS);
					System.out.println("Old score: " + tRScG);
					System.out.println("New score: " + rSc);
					redescriptions.set(k, tmp);

					TIntIterator el = tmp.elements.iterator();

					score = 0.0;
					while (el.hasNext()) {
						score += elemFreq[el.next()] - 1;
					}

					if (score < 0)
						System.out.println("Score<0 after update elements no equal supp");

					ArrayList<Integer> a = tmp.computeAttributesRed(tmp.viewElementsLists, dat);

					atrScore = 0.0;
					for (int iT = 0; iT < a.size(); iT++)
						atrScore += attrFreq[a.get(iT)] - 1;

					if (atrScore < 0)
						System.out.println("Score<0 after update atributes no equal supp");

					redScore.set(k, score);
					redScoreAt.set(k, atrScore);
					redDistCoverage.set(k, redescriptions.get(k).computeDistCoverageAt(elemFreq, attrFreq, dat));
					if (appset.useNetworkAsBackground == true)
						redDistNetwork.set(k, redescriptions.get(k).computeNetworkDensity(mat, appset));
					if (appset.attributeImportance != 0)
						targetAtScore.set(k, targetedScore/* tmp.computeScoreTargeted(map, appset, dat) */);

					added = 1;
					break;
				}
				// }
			}
			if (added == 1)
				return 1;
		}
		return 1;
	}

	int tryAddOld(Redescription tmp, Jacard tJS, ApplicationSettings appset, DataSetCreator dat, Mappings map,
			int elemFreq[], int attrFreq[], ArrayList<Double> redScore, ArrayList<Double> redScoreAt,
			ArrayList<Double> targetAtScore, boolean join) {
		// Jacard tJS=new Jacard(); tJS.initialize();
		if (redescriptions.size() < appset.numRetRed && tmp.JS >= appset.minJS) {
			int added = 0;
			for (int k = 0; k < redescriptions.size(); k++) {
				if (redescriptions.get(k).elements.size() == tmp.elements.size()) {
					tJS.computeRedescriptionElementJacard(tmp, redescriptions.get(k));
					if (tJS.JS == 1.0) {
						equalSubs++;
						if (tmp.JS > redescriptions.get(k).JS) {
							redescriptions.get(k).updateFreqTableOld(elemFreq, attrFreq, redScore, redScoreAt, tmp,
									this, dat);
							redescriptions.set(k, tmp);
							added = 1;
							break;
						} else {
							added = 1;
							break;
						}
					}
				}
			}

			if (added == 1) {
				return -1;
			}

			int index = redescriptions.size();
			redScoreAt.set(index, 0.0);
			redScore.set(index, 0.0);
			if (appset.attributeImportance != 0)
				targetAtScore.set(index, 0.0);
			redescriptions.add(tmp);

			TIntIterator it = tmp.elements.iterator();
			while (it.hasNext()) {
				int element = it.next();
				elemFreq[element] += 1;
			}

			ArrayList<Integer> attr = tmp.computeAttributesRed(tmp.viewElementsLists, dat);
			for (int itSet = 0; itSet < attr.size(); itSet++)
				attrFreq[attr.get(itSet)] += 1;

			if (redescriptions.size() == appset.numRetRed) {
				for (int k = 0; k < redescriptions.size(); k++) {
					redScore.set(k, redescriptions.get(k).computeScore(elemFreq, attrFreq, dat));
					redScoreAt.set(k, redescriptions.get(k).computeScoreAttr(elemFreq, attrFreq, dat));
					if (appset.attributeImportance != 0)
						targetAtScore.set(k, redescriptions.get(k).computeScoreTargeted(map, appset, dat));
				}
			}
		} else {

			if (join == true) {
				System.out.println(
						"Join procedure should not be used with optimization by exchange methodology! Under development...");
				int count = 0;
				for (int k = 0; k < redescriptions.size(); k++) {
					int quality = tmp.CompareQuality(redescriptions.get(k));

					if (quality == -1) {
						continue;
					} else if (quality == 2) {

						if (tmp.elements.size() == redescriptions.get(k).elements.size()) {
							redescriptions.get(k).join(tmp, map, dat);
							count++;
						} else {
							tmp.join(redescriptions.get(k), map, dat);
							count++;
						}
					} else if (quality == 1 && redescriptions.get(k).JS < 1.0) {
						redescriptions.get(k).join(tmp, map, dat);
						count++;
					}
				}
				System.out.println("num join: " + count);
			}

			if (tmp.JS < appset.minJS)
				return -1;

			int totalEF = 0, totalAF = 0;

			for (int tm = 0; tm < elemFreq.length; tm++)
				totalEF += elemFreq[tm];

			for (int tm = 0; tm < attrFreq.length; tm++)
				totalAF += attrFreq[tm];

			ArrayList<Integer> indexes = new ArrayList<>();
			ArrayList<Integer> potEq = new ArrayList<>();
			double score = 0.0;
			double maxDif = 0.0;

			TIntIterator it = tmp.elements.iterator();
			while (it.hasNext()) {
				int element = it.next();
				score += elemFreq[element];
			}

			double atrScore = 0.0;
			// atrScore=tmp.computeScoreAttr(elemFreq, attrFreq, dat);
			ArrayList<Integer> attr = tmp.computeAttributesRed(tmp.viewElementsLists, dat);
			for (int itSet = 0; itSet < attr.size(); itSet++) {
				/*
				 * it=attr.get(itSet).iterator(); while(it.hasNext()){ int at=it.next();
				 */
				int at = attr.get(itSet);
				atrScore += attrFreq[at];
				// }
			}

			int minInd = -1, minIndDJS = -1;

			double rSc = (/* 0.5 **/score / totalEF + /* 0.5 **/atrScore / totalAF), tRScG = 0.0;
			double targetedScore = 0.0;

			if (appset.attributeImportance != 0) {

				targetedScore = tmp.computeScoreTargeted(map, appset, dat);

			}

			if (appset.attributeImportance == 0)
				for (int k = 0; k < redescriptions.size(); k++) {
					double tRSc = (/* 0.5 **/(redScore.get(k) / totalEF) + /* 0.5 **/(redScoreAt.get(k) / totalAF));

					if (redescriptions.get(k).elements.size() == tmp.elements.size())
						potEq.add(k);
					if ((2.0 - tRSc + redescriptions.get(k).JS) < (2.0 - rSc + tmp.JS)
							&& tmp.pVal <= redescriptions.get(k).pVal && redescriptions.get(k).JS != 1.0) {
						// if((/*0.5**/1.0-0.5*tRSc+0.5*redescriptions.get(k).JS)<(/*0.5**/1.0-0.5*rSc+0.5*tmp.JS)
						// && tmp.pVal<=redescriptions.get(k).pVal && redescriptions.get(k).JS!=1.0){
						double d = ((2.0 - rSc + tmp.JS) / 3.0 - (2.0 - tRSc + redescriptions.get(k).JS) / 3.0);
						// double
						// d=(0.5*1.0-0.5*rSc+0.5*tmp.JS-0.5*1.0+0.5*tRSc-0.5*redescriptions.get(k).JS);
						if (d > maxDif) {
							minInd = k;
							maxDif = d;
							tRScG = tRSc;
						}
					}
				}
			else { // in case targeted RM is used
				for (int k = 0; k < redescriptions.size(); k++) {
					double tRSc = (/* 0.5 **/(redScore.get(k) / totalEF) + /* 0.5 **/(redScoreAt.get(k) / totalAF));

					if (redescriptions.get(k).elements.size() == tmp.elements.size())
						potEq.add(k);
					if ((2.0 - tRSc + redescriptions.get(k).JS + targetAtScore.get(k)) < (2.0 - rSc + tmp.JS
							+ targetedScore) && tmp.pVal <= redescriptions.get(k).pVal
							&& redescriptions.get(k).JS != 1.0) {
						// if((/*0.5**/1.0-0.5*tRSc+0.5*redescriptions.get(k).JS)<(/*0.5**/1.0-0.5*rSc+0.5*tmp.JS)
						// && tmp.pVal<=redescriptions.get(k).pVal && redescriptions.get(k).JS!=1.0){
						double d = ((2.0 - rSc + tmp.JS + targetedScore) / 4.0
								- (2.0 - tRSc + redescriptions.get(k).JS + targetAtScore.get(k)) / 4.0);
						// double
						// d=(0.5*1.0-0.5*rSc+0.5*tmp.JS-0.5*1.0+0.5*tRSc-0.5*redescriptions.get(k).JS);
						if (d > maxDif) {
							minInd = k;
							maxDif = d;
							tRScG = tRSc;
						}
					}
				}
			}

			if (minInd != -1)
				indexes.add(minInd);

			int added = 0;

			for (int k : potEq) {
				// tmp.
				tJS.computeRedescriptionElementJacard(tmp, redescriptions.get(k));
				if (tJS.JS == 1.0) {
					if (appset.attributeImportance == 0) {
						equalSubs++;
						tRScG = /* 0.5 **/(redScore.get(k) / totalEF) + /* 0.5 **/(redScoreAt.get(k) / totalAF);

						if (appset.attributeImportance == 0)
							tRScG += 1.0;
						else
							tRScG += (1.0 - targetAtScore.get(k));
						// tRScG=0.5*(redScore.get(k)/totalEF)+0.5*(redScoreAt.get(k)/totalAF);
						if ((rSc + 1 - targetedScore) < tRScG && tmp.JS >= redescriptions.get(k).JS) {
							redescriptions.get(k).updateScoreOld(elemFreq, attrFreq, redScore, redScoreAt, tmp, this,
									dat);
							System.out.println("update score equal supp");
							redescriptions.set(k, tmp);
							redScore.set(k, score);
							redScoreAt.set(k, atrScore);
							if (appset.attributeImportance != 0)
								targetAtScore.set(k, targetedScore);
						}
						added = 1;
						break;
					}
				}
			}

			if (added == 0) {

				for (int k : indexes) {
					// tRScG=0.5*(redScore.get(k)/totalEF)+0.5*(redScoreAt.get(k)/totalAF);
					// if(rSc<tRScG){

					/*
					 * if(tRScG>2.0){ System.out.println("Problem with old score...");
					 * System.out.println("element score: "+redScore.get(k)/totalEF);
					 * System.out.println("attribute score: "+redScoreAt.get(k)/totalAF); }
					 * if(rSc>2.0){ System.out.println("Problem with new score...");
					 * System.out.println("element score: "+score/totalEF);
					 * System.out.println("attribute score: "+atrScore/totalAF); }
					 */

					redescriptions.get(k).updateScoreOld(elemFreq, attrFreq, redScore, redScoreAt, tmp, this, dat);
					System.out.println("update score not equal exchange");
					System.out.println("Redescription removed.");
					System.out.println("Previous jaccard: " + redescriptions.get(k).JS);
					System.out.println("New jaccard: " + tmp.JS);
					System.out.println("Old score: " + tRScG);
					System.out.println("New score: " + rSc);
					redescriptions.set(k, tmp);

					TIntIterator el = tmp.elements.iterator();

					score = 0.0;
					while (el.hasNext()) {
						score += elemFreq[el.next()] - 1;
					}

					if (score < 0)
						System.out.println("Score<0 after update elements no equal supp");

					ArrayList<Integer> a = tmp.computeAttributesRed(tmp.viewElementsLists, dat);

					atrScore = 0.0;
					for (int iT = 0; iT < a.size(); iT++)
						atrScore += attrFreq[a.get(iT)] - 1;

					if (atrScore < 0)
						System.out.println("Score<0 after update atributes no equal supp");

					redScore.set(k, score);
					redScoreAt.set(k, atrScore);
					if (appset.attributeImportance != 0)
						targetAtScore.set(k, targetedScore/* tmp.computeScoreTargeted(map, appset, dat) */);

					added = 1;
					break;
				}
				// }
			}
		}
		return 1;
	}

	public int createGuidedNoJoinBasic(RuleReader rr, RuleReader rr1, Jacard js[], ApplicationSettings appset,
			int oldIndexRR, int oldIndexRR1, int RunInd, boolean outOfmemory[], Mappings map, DataSetCreator dat,
			int elemFreq[], int attrFreq[], ArrayList<Double> redScore, ArrayList<Double> redScoreAt,
			ArrayList<Double> redDistCoverage, ArrayList<Double> redDistCoverageAt, ArrayList<Double> redDistNetwork,
			ArrayList<Double> targetAtScore, double Statistics[], ArrayList<Double> maxDiffScoreDistribution,
			NHMCDistanceMatrix mat, int PreferenceRow) {

		System.out.println("Using guided expansion without join procedure constrained!");

		setParameters(null, null, js, appset, oldIndexRR, oldIndexRR1, RunInd, outOfmemory, map, dat, elemFreq, attrFreq,
				redScore, redScoreAt, redDistCoverage, redDistCoverageAt, redDistNetwork, targetAtScore, Statistics,
				maxDiffScoreDistribution, mat, PreferenceRow);

		boolean memoryCanbeOptimized = true;
		int maxNum = 0;

		ArrayList<Rule> negLeft = new ArrayList<>();
		ArrayList<Rule> negRight = new ArrayList<>();

		if (appset.leftNegation == true)
			for (int i = oldIndexRR; i < rr.rules.size(); i++) {
				Rule rtemp = new Rule(rr.rules.get(i), dat, map);
				negLeft.add(rtemp);
			}

		if (appset.rightNegation == true)
			for (int i = oldIndexRR1; i < rr1.rules.size(); i++) {
				Rule rtemp = new Rule(rr1.rules.get(i), dat, map);
				negRight.add(rtemp);
			}

		if (appset.unguidedExpansion) {
			if (RunInd == 0) {
				maxNum = (rr.rules.size() - oldIndexRR) * (rr1.rules.size() - oldIndexRR1);
			} else {
				maxNum = (rr.rules.size() - oldIndexRR) * (rr1.rules.size() - oldIndexRR1)
						- (rr.newRuleIndex - oldIndexRR) * (rr1.newRuleIndex - oldIndexRR1);
			}
		} else {
			maxNum = ((rr.newRuleIndex - oldIndexRR) * (rr1.rules.size() - rr1.newRuleIndex))
					+ ((rr.rules.size() - rr.newRuleIndex) * (rr1.newRuleIndex - oldIndexRR1));
		}

		System.out.println("max number of rules: " + maxNum);

		int numIt = 0;
		int step = maxNum / 100;

		if (step == 0)
			step = 1;

		int newRedescriptions = 0;
		Jacard tJS = new Jacard();
		tJS.initialize();

		for (int i = oldIndexRR; i < rr.rules.size(); i++) {
			if (appset.attributeImportanceGen.size() > 0) {
				if (appset.attributeImportanceGen.get(0) == 2
						&& rr.rules.get(i).checkConstraints(appset.importantAttributes, 0, 2, map) == 0)
					continue;
				else if (appset.attributeImportanceGen.get(0) == 1
						&& rr.rules.get(i).checkConstraints(appset.importantAttributes, 0, 1, map) == 0)
					continue;
			}
			for (int j = oldIndexRR1/* rr1.newRuleIndex-addon1 */; j < rr1.rules.size(); j++) {

				if (appset.attributeImportanceGen.size() > 0) {
					if (appset.attributeImportanceGen.get(1) == 2
							&& rr1.rules.get(j).checkConstraints(appset.importantAttributes, 1, 2, map) == 0)
						continue;
					else if (appset.attributeImportanceGen.get(1) == 1
							&& rr1.rules.get(j).checkConstraints(appset.importantAttributes, 1, 1, map) == 0)
						continue;
				}

				if (appset.unguidedExpansion == true) {
					if (RunInd != 0 && i < rr.newRuleIndex && j < rr1.newRuleIndex)
						continue;
				} else {
					if ((i < rr.newRuleIndex && j < rr1.newRuleIndex)
							|| (i >= rr.newRuleIndex && j >= rr1.newRuleIndex))
						continue;
				}

				for (int jinit = 0; jinit < js.length; jinit++)
					js[jinit].initialize();
				numIt++;

				double JSPos = 0.0, JSPosNeg = 0.0, JSNegPos = 0.0;
				if (appset.jsType == 0)
					JSPos = js[0].computeJacard(rr.rules.get(i), rr1.rules.get(j));
				else if (appset.jsType == 1)
					JSPos = js[0].computeJacardPess(rr.rules.get(i), rr1.rules.get(j), dat, map, 0);

				double RPval = js[0].computePval(rr.rules.get(i), rr1.rules.get(j), dat);

				if (JSPos >= appset.minJS)
					NumTests++;

				if (JSPos >= appset.minJS && RPval <= appset.maxPval && js[0].intersectSize >= appset.minSupport
						&& js[0].intersectSize <= appset.maxSupport) {
					Redescription tmp = new Redescription(rr.rules.get(i).rule, rr1.rules.get(j).rule, js[0].JS, map,
							dat, 0);
					tmp.computeElements(rr.rules.get(i), rr1.rules.get(j));
					// tmp.computeUnion(rr.rules.get(i), rr1.rules.get(j));
					if (appset.jsType == 0)
						tmp.computeUnion(rr.rules.get(i), rr1.rules.get(j));
					else if (appset.jsType == 1)
						tmp.computeUnionPess(rr.rules.get(i), rr1.rules.get(j), dat, map, 0);
					tmp.pVal = RPval;

					if (appset.attributeImportanceGen.size() > 0) {
						// for(int min=0;min<10;min++){
						// System.out.print("min: "+min+" ");
						// tmp.clearRuleMaps();
						tmp.closeInterval(dat, map);
						tmp.minimizeOptimal(dat, map, 0);
						// }
						// System.out.println();
						if (tmp.checkAttributes(appset, map, dat) == 0)
							continue;
					}

					if (tmp.elements.size() >= appset.minSupport && tmp.elements.size() <= appset.maxSupport) {// tmp.JS>0.4
																												// ||
																												// (tmp.elements.size()>1
																												// &&
																												// tmp.JS>0.2)
						// tryAdd(Redescription tmp, Jacard tJS,ApplicationSettings
						// appset,DataSetCreator dat ,int elemFreq[], int attrFreq[], ArrayList<Double>
						// redScore, ArrayList<Double> redScoreAt)
						// tryAdd(tmp, tJS, appset,
						// dat,map,elemFreq,attrFreq,redScore,redScoreAt,targetAtScore,false);
						tryAdd(tmp, tJS, false);
					}
				}
				if (appset.rightNegation == true && JSPos <= (1.0 - appset.minJS)) {
					if (appset.jsType == 0)
						JSPosNeg = js[1].computeJacard(rr.rules.get(i),
								negRight.get(j - oldIndexRR1)/* rr1.rules.get(j)*, dat, map, 1 */);
					else if (appset.jsType == 1)
						JSPosNeg = js[1].computeJacardPess(rr.rules.get(i), negRight.get(j - oldIndexRR1), dat, map, 1);
					if (JSPosNeg >= appset.minJS)
						NumTests++;
					if (JSPosNeg >= appset.minJS
							&& js[1].computePval(rr.rules.get(i), negRight.get(j - oldIndexRR1) /* rr1.rules.get(j) */,
									dat/* ,map,1 */) <= appset.maxPval
							&& js[1].intersectSize >= appset.minSupport && js[1].intersectSize <= appset.maxSupport) {
						Redescription tmp = new Redescription(rr.rules.get(i).rule, rr1.rules.get(j).rule, js[1].JS,
								map, dat, 1);
						// tmp.computeElements(rr.rules.get(i), negRight.get(j-oldIndexRR1)
						// /*rr1.rules.get(j)*//*,dat,map,1*/);
						tmp.computeElements(rr.rules.get(i), negRight.get(j - oldIndexRR1), rr.rules.get(i),
								rr1.rules.get(j)/* rr1.rules.get(j) *//* ,dat,map,1 */);
						// tmp.computeUnion(rr.rules.get(i),
						// negRight.get(j-oldIndexRR1)/*rr1.rules.get(j)*//*,dat,map,1*/);
						if (appset.jsType == 0)
							tmp.computeUnion(rr.rules.get(i), negRight.get(j - oldIndexRR1));
						else if (appset.jsType == 1)
							tmp.computeUnionPess(rr.rules.get(i), negRight.get(j - oldIndexRR1), dat, map, 1);

						if (appset.attributeImportanceGen.size() > 0) {
							tmp.closeInterval(dat, map);
							tmp.minimizeOptimal(dat, map, 0);
							if (tmp.checkAttributes(appset, map, dat) == 0)
								continue;
						}

						if (tmp.elements.size() >= appset.minSupport && tmp.elements.size() <= appset.maxSupport) {// tmp.JS>0.4
																													// ||
																													// (tmp.elements.size()>1
																													// &&
																													// tmp.JS>0.2)
							// tryAdd(tmp, tJS, appset,
							// dat,map,elemFreq,attrFreq,redScore,redScoreAt,targetAtScore,false);
							// //redescriptions.add(tmp);
							tryAdd(tmp, tJS, false);

							newRedescriptions++;
						}
					}
				}
				if (appset.leftNegation == true && JSPos <= (1.0 - appset.minJS)) {
					if (appset.jsType == 0)
						JSNegPos = js[2].computeJacard(negLeft.get(i - oldIndexRR)/* rr.rules.get(i) */,
								rr1.rules.get(j)/* , dat, map, 2 */);
					else if (appset.jsType == 1)
						JSNegPos = js[2].computeJacardPess(negLeft.get(i - oldIndexRR), rr1.rules.get(j), dat, map, 2);
					if (JSNegPos >= appset.minJS)
						NumTests++;
					if (JSNegPos >= appset.minJS
							&& js[2].computePval(negLeft.get(i - oldIndexRR)/* rr.rules.get(i) */, rr1.rules.get(j),
									dat/* ,map,2 */) <= appset.maxPval
							&& js[2].intersectSize >= appset.minSupport && js[2].intersectSize <= appset.maxSupport) {
						Redescription tmp = new Redescription(rr.rules.get(i).rule, rr1.rules.get(j).rule, js[2].JS,
								map, dat, 2);
						// tmp.computeElements(negLeft.get(i-oldIndexRR)/*rr.rules.get(i)*/,
						// rr1.rules.get(j)/*,dat,map,2*/);
						tmp.computeElements(negLeft.get(i - oldIndexRR), rr1.rules.get(j), rr.rules.get(i),
								rr1.rules.get(j)/* rr1.rules.get(j) *//* ,dat,map,1 */);
						// tmp.computeUnion(negLeft.get(i-oldIndexRR)/*rr.rules.get(i)*/,
						// rr1.rules.get(j)/*,dat,map,2*/);
						if (appset.jsType == 0)
							tmp.computeUnion(negLeft.get(i - oldIndexRR)/* rr.rules.get(i) */,
									rr1.rules.get(j)/* ,dat,map,2 */);
						else if (appset.jsType == 1)
							tmp.computeUnionPess(negLeft.get(i - oldIndexRR)/* rr.rules.get(i) */,
									rr1.rules.get(j)/* ,dat,map,2 */, dat, map, 2);

						if (appset.attributeImportanceGen.size() > 0) {
							tmp.closeInterval(dat, map);
							tmp.minimizeOptimal(dat, map, 0);
							if (tmp.checkAttributes(appset, map, dat) == 0)
								continue;
						}

						if (tmp.elements.size() >= appset.minSupport && tmp.elements.size() <= appset.maxSupport) {// tmp.JS>0.4
																													// ||
																													// (tmp.elements.size()>1
																													// &&
																													// tmp.JS>0.2)
							// tryAdd(tmp, tJS, appset,
							// dat,map,elemFreq,attrFreq,redScore,redScoreAt,targetAtScore,false);//redescriptions.add(tmp);
							tryAdd(tmp, tJS, false);

							newRedescriptions++;
						}
					}
				}
				if (numIt % step == 0) {
					System.out.println((((double) numIt / maxNum) * 100) + "% completed...");
					System.out.println("num redescriptions: " + redescriptions.size());
					// Runtime.getRuntime().gc();
				}
				if (numIt == maxNum)
					System.out.println("100% completed!");

				long maxMemory = Runtime.getRuntime().maxMemory() / (1024 * 1024);
				long freemem = Runtime.getRuntime().freeMemory() / (1024 * 1024);
				long totalMemory = Runtime.getRuntime().totalMemory() / (1024 * 1024);

				long minMemory = Math.max(10, (long) 0.15 * maxMemory);

				if ((maxMemory - (totalMemory - freemem) < minMemory) && memoryCanbeOptimized == true) {
					// this.filter(appset, rr, rr1, map, dat);

					System.out.println("Memory status: " + ((maxMemory - (totalMemory - freemem))));
					System.out.println("Min memory: " + minMemory);

					this.filter(appset, rr, rr1, map, dat);
					computePVal(dat, map);
					RedescriptionSet rTemp = new RedescriptionSet();
					double weights[] = { 0.2, 0.2, 0.2, 0.2, 0.2 };
					rTemp.createRedescriptionSet(this, weights, appset, dat, map);
					this.redescriptions.clear();

					for (int itm = 0; itm < rTemp.redescriptions.size(); itm++)
						this.redescriptions.add(rTemp.redescriptions.get(itm));

					for (int irt = 0; irt < redescriptions.size(); irt++)
						redescriptions.get(irt).clearRuleMaps();

					newRedescriptions = redescriptions.size();

					Runtime.getRuntime().gc();

					System.out.println("Memory status: " + ((maxMemory - (totalMemory - freemem))));
					System.out.println("Changing minJS level");
					System.out.println("New minJS level: " + appset.minJS);
				} else if ((maxMemory - (totalMemory - freemem) < minMemory) && memoryCanbeOptimized == false) {
					outOfmemory[0] = true;
					return newRedescriptions;
				}
				// }

			}
		}

		// this.remove(appset);

		if (appset.leftDisjunction == true || appset.rightDisjunction == true) {
			System.out.println("Computing disjunctive refinement...");

			step = redescriptions.size() / 100;
			if (redescriptions.size() < 100)
				step = 1;

			for (int k = 0; k < redescriptions.size(); k++) {
				if (redescriptions.get(k).JS == 1.0)
					continue;
				double joinJS = 0.0, maxJoinJS = 0.0, maxScoreGain = 0.0;
				int maxInd = 0;
				int negated = 0;
				int totalElFreq = 0, totalAtFreq = 0, rScoreEl = 0, rScoreAt = 0;

				for (int tm = 0; tm < elemFreq.length; tm++)
					totalElFreq += elemFreq[tm];

				for (int tm = 0; tm < attrFreq.length; tm++)
					totalAtFreq += attrFreq[tm];

				TIntIterator it = redescriptions.get(k).elements.iterator();
				while (it.hasNext()) {
					int element = it.next();
					rScoreEl += elemFreq[element] - 1;
				}

				double atrScore = 0.0;
				ArrayList<TIntHashSet> attr = redescriptions.get(k)
						.computeAttributes(redescriptions.get(k).viewElementsLists, dat);
				for (int itSet = 0; itSet < attr.size(); itSet++) {
					it = attr.get(itSet).iterator();
					while (it.hasNext()) {
						int at = it.next();
						rScoreAt += attrFreq[at] - 1;
					}
				}

				if (appset.rightDisjunction == true) {
					maxScoreGain = 0.0;
					// TIntHashSet
					// lRE=redescriptions.get(k).computeElements(redescriptions.get(k).viewElementsLists.get(0),
					// dat, map);
					// TIntHashSet
					// rRE=redescriptions.get(k).computeElements(redescriptions.get(k).viewElementsLists.get(1),
					// dat, map);
					ArrayList<TIntHashSet> sideElems = redescriptions.get(k).computeElementsGen(dat, map);// new
																											// ArrayList<>();//to
																											// make
																											// proper
																											// generalization
					int interCount = js[0].computeGenInterCount(redescriptions.get(k), sideElems, 1);
					// ArrayList<TIntHashSet> sideElems=new ArrayList<>();//to make proper
					// generalization
					// sideElems.add(lRE); sideElems.add(rRE);
					for (int j = oldIndexRR1; j < rr1.rules.size(); j++) {
						if (appset.jsType == 0)
							joinJS = js[0].computeRedescriptionRuleElementJacardGen(redescriptions.get(k),
									rr1.rules.get(j), sideElems, 1, 0, dat, map, appset, interCount);
						else if (appset.jsType == 1)
							joinJS = js[0].computeRedescriptionRuleElementJacardGenPess(redescriptions.get(k),
									rr1.rules.get(j), sideElems, 1, 0, dat, map, appset, interCount);

						double scoreGain = computeScoreGain(redescriptions.get(k), rr1.rules.get(j), appset, dat,
								elemFreq, attrFreq, redScore, redScoreAt, totalElFreq, totalAtFreq, rScoreEl, rScoreAt,
								joinJS);

						if (joinJS > 0.5) {
							if (scoreGain > maxScoreGain) {
								maxScoreGain = scoreGain;
								maxInd = j;
								negated = 0;
							}
						}

						if (appset.rightNegation == true) {
							if (appset.jsType == 0)
								joinJS = js[0].computeRedescriptionRuleElementJacardGen(redescriptions.get(k),
										rr1.rules.get(j), sideElems, 1, 1, dat, map, appset, interCount);
							else if (appset.jsType == 1)
								joinJS = js[0].computeRedescriptionRuleElementJacardGenPess(redescriptions.get(k),
										rr1.rules.get(j), sideElems, 1, 1, dat, map, appset, interCount);

							if (joinJS > 0.5) {
								if (scoreGain > maxScoreGain) {
									maxScoreGain = scoreGain;
									maxInd = j;
									negated = 1;
								}
							}
						}
					}
					// System.out.println("maxJoinJS: "+maxJoinJS);
					if (maxScoreGain > 0.1) {
						System.out.println("maxScoreGain: " + maxScoreGain);
						// System.out.println("max index: "+maxInd);
						// System.out.println("maxJoinJS: "+maxJoinJS);
						Redescription tmp = new Redescription(redescriptions.get(k), dat);
						// js[0].computePvalGen(tmp, null, dat, map, k)
						if (appset.jsType == 0)
							redescriptions.get(k).disjunctiveJoin(rr1.rules.get(maxInd), appset, dat, map, sideElems, 1,
									negated);
						else if (appset.jsType == 1)
							redescriptions.get(k).disjunctiveJoinPess(rr1.rules.get(maxInd), appset, dat, map,
									sideElems, 1, negated);
						redescriptions.get(k).computePVal(dat, map);

						if (appset.attributeImportanceGen.size() > 0) {
							redescriptions.get(k).closeInterval(dat, map);
							redescriptions.get(k).minimizeOptimal(dat, map, 0);
							if (redescriptions.get(k).checkAttributes(appset, map, dat) == 0) {
								redescriptions.set(k, tmp);
								continue;
							}
						}

						NumTests++;

						if (redescriptions.get(k).pVal > appset.maxPval
								|| redescriptions.get(k).elements.size() > appset.maxSupport)
							redescriptions.set(k, tmp);
						else
							// tmp.updateScore(elemFreq, attrFreq, redScore, redScoreAt,
							// redescriptions.get(k), this, dat);
							tmp.updateScore(elemFreq, attrFreq, redScore, redScoreAt, redDistCoverage, Statistics,
									redescriptions.get(k), this, dat);
					}
				}
				if (appset.leftDisjunction == true) {
					maxScoreGain = 0.0;
					// TIntHashSet
					// lRE=redescriptions.get(k).computeElements(redescriptions.get(k).viewElementsLists.get(0),
					// dat, map);
					// TIntHashSet
					// rRE=redescriptions.get(k).computeElements(redescriptions.get(k).viewElementsLists.get(1),
					// dat, map);
					// ArrayList<TIntHashSet> sideElems=new ArrayList<>();//to make proper
					// generalization
					// sideElems.add(lRE); sideElems.add(rRE);
					ArrayList<TIntHashSet> sideElems = redescriptions.get(k).computeElementsGen(dat, map);// new
																											// ArrayList<>();//to
																											// make
																											// proper
																											// generalization
					int interCount = js[0].computeGenInterCount(redescriptions.get(k), sideElems, 1);
					joinJS = 0.0;
					maxJoinJS = 0.0;
					maxInd = 0;
					negated = 0;
					for (int i = oldIndexRR; i < rr.rules.size(); i++) {
						if (appset.jsType == 0)
							joinJS = js[0].computeRedescriptionRuleElementJacardGen(redescriptions.get(k),
									rr.rules.get(i), sideElems, 0, 0, dat, map, appset, interCount);
						else if (appset.jsType == 1)
							joinJS = js[0].computeRedescriptionRuleElementJacardGenPess(redescriptions.get(k),
									rr.rules.get(i), sideElems, 0, 0, dat, map, appset, interCount);
						double scoreGain = computeScoreGain(redescriptions.get(k), rr.rules.get(i), appset, dat,
								elemFreq, attrFreq, redScore, redScoreAt, totalElFreq, totalAtFreq, rScoreEl, rScoreAt,
								joinJS);

						if (joinJS > 0.5) {
							if (scoreGain > maxScoreGain) {
								maxScoreGain = scoreGain;
								maxInd = i;
								negated = 0;
							}
						}

						if (appset.leftNegation == true) {
							if (appset.jsType == 0)
								joinJS = js[0].computeRedescriptionRuleElementJacardGen(redescriptions.get(k),
										rr.rules.get(i), sideElems, 0, 1, dat, map, appset, interCount);
							else if (appset.jsType == 1)
								joinJS = js[0].computeRedescriptionRuleElementJacardGenPess(redescriptions.get(k),
										rr.rules.get(i), sideElems, 0, 1, dat, map, appset, interCount);
							scoreGain = computeScoreGain(redescriptions.get(k), rr.rules.get(i), appset, dat, elemFreq,
									attrFreq, redScore, redScoreAt, totalElFreq, totalAtFreq, rScoreEl, rScoreAt,
									joinJS);

							if (joinJS > 0.5) {
								if (scoreGain > maxScoreGain) {
									maxScoreGain = scoreGain;
									maxInd = i;
									negated = 1;
								}
							}
						}
					}
					// System.out.println("maxJoinJS: "+maxJoinJS);
					if (maxScoreGain > 0.1) {
						System.out.println("maxScoreGain: " + maxScoreGain);
						Redescription tmp = new Redescription(redescriptions.get(k), dat);
						if (appset.jsType == 0)
							redescriptions.get(k).disjunctiveJoin(rr.rules.get(maxInd), appset, dat, map, sideElems, 0,
									negated);
						else if (appset.jsType == 1)
							redescriptions.get(k).disjunctiveJoinPess(rr.rules.get(maxInd), appset, dat, map, sideElems,
									0, negated);

						redescriptions.get(k).computePVal(dat, map);

						if (appset.attributeImportanceGen.size() > 0) {
							redescriptions.get(k).closeInterval(dat, map);
							redescriptions.get(k).minimizeOptimal(dat, map, 0);
							if (redescriptions.get(k).checkAttributes(appset, map, dat) == 0) {
								redescriptions.set(k, tmp);
								continue;
							}
						}

						NumTests++;

						if (redescriptions.get(k).pVal > appset.maxPval
								|| redescriptions.get(k).elements.size() > appset.maxSupport)
							redescriptions.set(k, tmp);
						else
							// tmp.updateScore(elemFreq, attrFreq, redScore, redScoreAt,
							// redescriptions.get(k), this, dat);
							tmp.updateScore(elemFreq, attrFreq, redScore, redScoreAt, redDistCoverage, Statistics,
									redescriptions.get(k), this, dat);
					}
				}

				if ((k + 1) % step == 0) {
					System.out.println((((double) (k + 1) / redescriptions.size()) * 100) + "% completed...");
					System.out.println("num redescriptions: " + redescriptions.size());
					// Runtime.getRuntime().gc();
				}
			}
		}

		/*
		 * for(int t=0;t<2;t++){ System.out.println("Validation"+t); this.adaptSet(dat,
		 * map); for(int i=0;i<redescriptions.size();i++)
		 * redescriptions.get(i).validate(dat, map);
		 * 
		 * for(int i=0;i<redescriptions.size();i++)
		 * redescriptions.get(i).clearRuleMaps(); }
		 */

		// return newRedescriptions;
		System.out.println("Num times join: " + equalSubs);
		return 1;
	}

	public int createGuidedNoJoin(RuleReader rr, RuleReader rr1, Jacard js[], ApplicationSettings appset,
			int oldIndexRR, int oldIndexRR1, int RunInd, boolean outOfmemory[], Mappings map, DataSetCreator dat) {

		System.out.println("Using guided expansion without join procedure!");
		boolean memoryCanbeOptimized = true;
		int maxNum = 0;

		ArrayList<Rule> negLeft = new ArrayList<>();
		ArrayList<Rule> negRight = new ArrayList<>();

		for (int i = oldIndexRR; i < rr.rules.size(); i++) {
			Rule rtemp = new Rule(rr.rules.get(i), dat, map);
			negLeft.add(rtemp);
		}

		for (int i = oldIndexRR1; i < rr1.rules.size(); i++) {
			Rule rtemp = new Rule(rr1.rules.get(i), dat, map);
			negRight.add(rtemp);
		}

		if (appset.unguidedExpansion) {
			if (RunInd == 0) {
				maxNum = (rr.rules.size() - oldIndexRR) * (rr1.rules.size() - oldIndexRR1);
			} else {
				maxNum = (rr.rules.size() - oldIndexRR) * (rr1.rules.size() - oldIndexRR1)
						- (rr.newRuleIndex - oldIndexRR) * (rr1.newRuleIndex - oldIndexRR1);
			}
		} else {
			maxNum = ((rr.newRuleIndex - oldIndexRR) * (rr1.rules.size() - rr1.newRuleIndex))
					+ ((rr.rules.size() - rr.newRuleIndex) * (rr1.newRuleIndex - oldIndexRR1));
		}

		System.out.println("max number of rules: " + maxNum);

		int numIt = 0;
		int step = maxNum / 100;

		if (step == 0)
			step = 1;

		int newRedescriptions = 0;

		for (int i = oldIndexRR/* rr.newRuleIndex-addon */; i < rr.rules.size(); i++) {
			// long startTime = System.currentTimeMillis();
			for (int j = oldIndexRR1/* rr1.newRuleIndex-addon1 */; j < rr1.rules.size(); j++) {

				if (appset.unguidedExpansion == true) {
					if (RunInd != 0 && i < rr.newRuleIndex && j < rr1.newRuleIndex)
						continue;
				} else {
					if ((i < rr.newRuleIndex && j < rr1.newRuleIndex)
							|| (i >= rr.newRuleIndex && j >= rr1.newRuleIndex))
						continue;
				}

				for (int jinit = 0; jinit < js.length; jinit++)
					js[jinit].initialize();
				numIt++;

				double JSPos = 0.0, JSPosNeg = 0.0, JSNegPos = 0.0;
				JSPos = js[0].computeJacard(rr.rules.get(i), rr1.rules.get(j));

				if (JSPos >= appset.minJS && js[0].computePval(rr.rules.get(i), rr1.rules.get(j), dat) <= appset.maxPval
						&& js[0].intersectSize >= appset.minSupport && js[0].intersectSize <= appset.maxSupport) {
					Redescription tmp = new Redescription(rr.rules.get(i).rule, rr1.rules.get(j).rule, js[0].JS, map,
							dat, 0);
					tmp.computeElements(rr.rules.get(i), rr1.rules.get(j));
					tmp.computeUnion(rr.rules.get(i), rr1.rules.get(j));

					if (tmp.elements.size() >= appset.minSupport && tmp.elements.size() <= appset.maxSupport) {// tmp.JS>0.4
																												// ||
																												// (tmp.elements.size()>1
																												// &&
																												// tmp.JS>0.2)
						redescriptions.add(tmp);
						newRedescriptions++;
					} // fix negation and validation!
				}
				if (appset.rightNegation == true && JSPos <= (1.0 - appset.minJS)) {
					JSPosNeg = js[1].computeJacard(rr.rules.get(i),
							negRight.get(j - oldIndexRR1)/* rr1.rules.get(j)*, dat, map, 1 */);
					if (JSPosNeg >= appset.minJS
							&& js[1].computePval(rr.rules.get(i), negRight.get(j - oldIndexRR1) /* rr1.rules.get(j) */,
									dat/* ,map,1 */) <= appset.maxPval
							&& js[1].intersectSize >= appset.minSupport && js[1].intersectSize <= appset.maxSupport) {
						Redescription tmp = new Redescription(rr.rules.get(i).rule, rr1.rules.get(j).rule, js[1].JS,
								map, dat, 1);
						// tmp.computeElements(rr.rules.get(i), negRight.get(j-oldIndexRR1)
						// /*rr1.rules.get(j)*//*,dat,map,1*/);
						tmp.computeElements(rr.rules.get(i), negRight.get(j - oldIndexRR1), rr.rules.get(i),
								rr1.rules.get(j)/* rr1.rules.get(j) *//* ,dat,map,1 */);
						tmp.computeUnion(rr.rules.get(i),
								negRight.get(j - oldIndexRR1)/* rr1.rules.get(j) *//* ,dat,map,1 */);
						if (tmp.elements.size() >= appset.minSupport && tmp.elements.size() <= appset.maxSupport) {// tmp.JS>0.4
																													// ||
																													// (tmp.elements.size()>1
																													// &&
																													// tmp.JS>0.2)
							redescriptions.add(tmp);
							newRedescriptions++;
						}
					}
				}
				if (appset.leftNegation == true && JSPos <= (1.0 - appset.minJS)) {
					JSNegPos = js[2].computeJacard(negLeft.get(i - oldIndexRR)/* rr.rules.get(i) */,
							rr1.rules.get(j)/* , dat, map, 2 */);
					if (JSNegPos >= appset.minJS
							&& js[2].computePval(negLeft.get(i - oldIndexRR)/* rr.rules.get(i) */, rr1.rules.get(j),
									dat/* ,map,2 */) <= appset.maxPval
							&& js[2].intersectSize >= appset.minSupport && js[2].intersectSize <= appset.maxSupport) {
						Redescription tmp = new Redescription(rr.rules.get(i).rule, rr1.rules.get(j).rule, js[2].JS,
								map, dat, 2);
						// tmp.computeElements(negLeft.get(i-oldIndexRR)/*rr.rules.get(i)*/,
						// rr1.rules.get(j)/*,dat,map,2*/);
						tmp.computeElements(negLeft.get(i - oldIndexRR), rr1.rules.get(j), rr.rules.get(i),
								rr1.rules.get(j)/* rr1.rules.get(j) *//* ,dat,map,1 */);
						tmp.computeUnion(negLeft.get(i - oldIndexRR)/* rr.rules.get(i) */,
								rr1.rules.get(j)/* ,dat,map,2 */);
						if (tmp.elements.size() >= appset.minSupport && tmp.elements.size() <= appset.maxSupport) {// tmp.JS>0.4
																													// ||
																													// (tmp.elements.size()>1
																													// &&
																													// tmp.JS>0.2)
							redescriptions.add(tmp);
							newRedescriptions++;
						}
					}
				}
				if (numIt % step == 0) {
					System.out.println((((double) numIt / maxNum) * 100) + "% completed...");
					System.out.println("num redescriptions: " + redescriptions.size());
					// Runtime.getRuntime().gc();
				}
				if (numIt == maxNum)
					System.out.println("100% completed!");

				long maxMemory = Runtime.getRuntime().maxMemory() / (1024 * 1024);
				long freemem = Runtime.getRuntime().freeMemory() / (1024 * 1024);
				long totalMemory = Runtime.getRuntime().totalMemory() / (1024 * 1024);

				long minMemory = Math.max(10, (long) 0.15 * maxMemory);

				if ((maxMemory - (totalMemory - freemem) < minMemory) && memoryCanbeOptimized == true) {
					// this.filter(appset, rr, rr1, map, dat);

					System.out.println("Memory status: " + ((maxMemory - (totalMemory - freemem))));
					System.out.println("Min memory: " + minMemory);

					this.filter(appset, rr, rr1, map, dat);
					computePVal(dat, map);
					RedescriptionSet rTemp = new RedescriptionSet();
					double weights[] = { 0.2, 0.2, 0.2, 0.2, 0.2 };
					rTemp.createRedescriptionSet(this, weights, appset, dat, map);
					this.redescriptions.clear();

					for (int itm = 0; itm < rTemp.redescriptions.size(); itm++)
						this.redescriptions.add(rTemp.redescriptions.get(itm));

					for (int irt = 0; irt < redescriptions.size(); irt++)
						redescriptions.get(irt).clearRuleMaps();

					newRedescriptions = redescriptions.size();

					Runtime.getRuntime().gc();

					System.out.println("Memory status: " + ((maxMemory - (totalMemory - freemem))));
					System.out.println("Changing minJS level");
					System.out.println("New minJS level: " + appset.minJS);
				} else if ((maxMemory - (totalMemory - freemem) < minMemory) && memoryCanbeOptimized == false) {
					outOfmemory[0] = true;
					return newRedescriptions;
				}
				// }

			}
		}

		// this.remove(appset);

		if (appset.leftDisjunction == true || appset.rightDisjunction == true) {
			System.out.println("Computing disjunctive refinement...");

			step = redescriptions.size() / 100;
			if (redescriptions.size() < 100)
				step = 1;

			for (int k = 0; k < redescriptions.size(); k++) {
				if (redescriptions.get(k).JS == 1.0)
					continue;
				double joinJS = 0.0, maxJoinJS = 0.0;
				int maxInd = 0;
				int negated = 0;

				if (appset.rightDisjunction == true) {
					// TIntHashSet
					// lRE=redescriptions.get(k).computeElements(redescriptions.get(k).viewElementsLists.get(0),
					// dat, map);
					// TIntHashSet
					// rRE=redescriptions.get(k).computeElements(redescriptions.get(k).viewElementsLists.get(1),
					// dat, map);
					ArrayList<TIntHashSet> sideElems = redescriptions.get(k).computeElementsGen(dat, map);// new
																											// ArrayList<>();//to
																											// make
																											// proper
																											// generalization
					int interCount = js[0].computeGenInterCount(redescriptions.get(k), sideElems, 1);
					// ArrayList<TIntHashSet> sideElems=new ArrayList<>();//to make proper
					// generalization
					// sideElems.add(lRE); sideElems.add(rRE);
					for (int j = oldIndexRR1; j < rr1.rules.size(); j++) {
						joinJS = js[0].computeRedescriptionRuleElementJacardGen(redescriptions.get(k), rr1.rules.get(j),
								sideElems, 1, 0, dat, map, appset, interCount);
						if (joinJS > maxJoinJS) {
							maxJoinJS = joinJS;
							maxInd = j;
							negated = 0;
						}

						if (appset.rightNegation == true) {
							joinJS = js[0].computeRedescriptionRuleElementJacardGen(redescriptions.get(k),
									rr1.rules.get(j), sideElems, 1, 1, dat, map, appset, interCount);
							if (joinJS > maxJoinJS) {
								maxJoinJS = joinJS;
								maxInd = j;
								negated = 1;
							}
						}
					}
					// System.out.println("maxJoinJS: "+maxJoinJS);
					if (maxJoinJS > 0.5) {
						// System.out.println("max index: "+maxInd);
						// System.out.println("maxJoinJS: "+maxJoinJS);
						redescriptions.get(k).disjunctiveJoin(rr1.rules.get(maxInd), appset, dat, map, sideElems, 1,
								negated);
					}
				}
				if (appset.leftDisjunction == true) {
					// TIntHashSet
					// lRE=redescriptions.get(k).computeElements(redescriptions.get(k).viewElementsLists.get(0),
					// dat, map);
					// TIntHashSet
					// rRE=redescriptions.get(k).computeElements(redescriptions.get(k).viewElementsLists.get(1),
					// dat, map);
					// ArrayList<TIntHashSet> sideElems=new ArrayList<>();//to make proper
					// generalization
					// sideElems.add(lRE); sideElems.add(rRE);
					ArrayList<TIntHashSet> sideElems = redescriptions.get(k).computeElementsGen(dat, map);// new
																											// ArrayList<>();//to
																											// make
																											// proper
																											// generalization
					int interCount = js[0].computeGenInterCount(redescriptions.get(k), sideElems, 1);
					joinJS = 0.0;
					maxJoinJS = 0.0;
					maxInd = 0;
					negated = 0;
					for (int i = oldIndexRR; i < rr.rules.size(); i++) {
						joinJS = js[0].computeRedescriptionRuleElementJacardGen(redescriptions.get(k), rr.rules.get(i),
								sideElems, 0, 0, dat, map, appset, interCount);
						if (joinJS > maxJoinJS) {
							maxJoinJS = joinJS;
							maxInd = i;
							negated = 0;
						}

						if (appset.leftNegation == true) {
							joinJS = js[0].computeRedescriptionRuleElementJacardGen(redescriptions.get(k),
									rr.rules.get(i), sideElems, 0, 1, dat, map, appset, interCount);
							if (joinJS > maxJoinJS) {
								maxJoinJS = joinJS;
								maxInd = i;
								negated = 1;
							}
						}
					}
					// System.out.println("maxJoinJS: "+maxJoinJS);
					if (maxJoinJS > 0.5) {
						redescriptions.get(k).disjunctiveJoin(rr.rules.get(maxInd), appset, dat, map, sideElems, 0,
								negated);
					}
				}

				if ((k + 1) % step == 0) {
					System.out.println((((double) (k + 1) / redescriptions.size()) * 100) + "% completed...");
					System.out.println("num redescriptions: " + redescriptions.size());
					// Runtime.getRuntime().gc();
				}
			}
		}

		for (int t = 0; t < 2; t++) {
			System.out.println("Validation" + t);
			this.adaptSet(dat, map, 1);
			for (int i = 0; i < redescriptions.size(); i++)
				redescriptions.get(i).validate(dat, map);

			for (int i = 0; i < redescriptions.size(); i++)
				redescriptions.get(i).clearRuleMaps();
		}

		// return newRedescriptions;
		return 1;
	}

	public int combineViewRules(RuleReader rr, Jacard js[], ApplicationSettings appset, int oldIndexRR, int RunInd,
			boolean outOfmemory[], Mappings map, DataSetCreator dat, int[] oldRIndex, int view) {

		System.out.println("Using guided expansion without join procedure, multi-view!");
		boolean memoryCanbeOptimized = true;
		int maxNum = 0;

		maxNum = (rr.rules.size() - oldIndexRR) * (redescriptions.size() - oldRIndex[0]);

		if (appset.unguidedExpansion == true)
			maxNum = (rr.rules.size() - oldIndexRR) * (redescriptions.size());

		System.out.println("max number of rules: " + maxNum);
		System.out.println("old index rules: " + oldIndexRR);

		int numIt = 0;
		int step = maxNum / 100;

		if (step == 0)
			step = 1;

		int newRedescriptions = 0;
		int sIndex = oldRIndex[0];

		if (appset.unguidedExpansion == true)
			sIndex = 0;

		int ROS = redescriptions.size();

		for (int j = sIndex; j < ROS; j++) {

			if (j >= redescriptions.size())
				break;

			if (redescriptions.get(j).viewElementsLists.get(view).size() > 0)
				continue;

			for (int i = oldIndexRR/* rr.newRuleIndex-addon */; i < rr.rules.size(); i++) {

				for (int jinit = 0; jinit < js.length; jinit++)
					js[jinit].initialize();
				numIt++;

				double JSPos = 0.0, JSPosNeg = 0.0, JSNegPos = 0.0;
				JSPos = js[0].computeJacardGen(redescriptions.get(j), rr.rules.get(i), dat, map, 0);
				// System.out.println("JSPos multi-view: "+JSPos);
				if (JSPos >= appset.minJS
						&& js[0].computePvalGen(redescriptions.get(j), rr.rules.get(i), dat, map, 0) <= appset.maxPval
						&& js[0].intersectSize >= appset.minSupport && js[0].intersectSize <= appset.maxSupport) {
					Redescription tmp = new Redescription(redescriptions.get(j), rr.rules.get(i).rule, js[0].JS, map,
							dat, 0, view);
					tmp.computeElements(redescriptions.get(j), rr.rules.get(i), view);
					tmp.computeUnion(redescriptions.get(j), rr.rules.get(i), dat, map, 0);

					if (tmp.elements.size() >= appset.minSupport && tmp.elements.size() <= appset.maxSupport) {// tmp.JS>0.4
																												// ||
																												// (tmp.elements.size()>1
																												// &&
																												// tmp.JS>0.2)
						redescriptions.add(tmp);
						newRedescriptions++;
					} // fix negation and validation!
				}
				if (appset.rightNegation == true) {
					JSPosNeg = js[1].computeJacardGen(redescriptions.get(j), rr.rules.get(i), dat, map, 1);
					if (JSPosNeg >= appset.minJS && js[1].computePvalGen(redescriptions.get(j), rr.rules.get(i), dat,
							map, 1) <= appset.maxPval) {
						Redescription tmp = new Redescription(redescriptions.get(j), rr.rules.get(i).rule, js[1].JS,
								map, dat, 1, view);
						tmp.computeElements(redescriptions.get(j), rr.rules.get(i), dat, map, 1, view);
						tmp.computeUnion(redescriptions.get(j), rr.rules.get(i), dat, map, 1);
						if (tmp.elements.size() >= appset.minSupport && tmp.elements.size() <= appset.maxSupport) {// tmp.JS>0.4
																													// ||
																													// (tmp.elements.size()>1
																													// &&
																													// tmp.JS>0.2)
							redescriptions.add(tmp);
							newRedescriptions++;
						}
					}
				}

				/*
				 * if(redescriptions.size()>100000){//test this.filter(appset, rr, rr,map,dat);
				 * }
				 */

				if (numIt % step == 0) {
					System.out.println((((double) numIt / maxNum) * 100) + "% completed...");
					System.out.println("num redescriptions: " + redescriptions.size());
					// Runtime.getRuntime().gc();
				}
				if (numIt == maxNum)
					System.out.println("100% completed!");

				long maxMemory = Runtime.getRuntime().maxMemory() / (1024 * 1024);
				long freemem = Runtime.getRuntime().freeMemory() / (1024 * 1024);
				long totalMemory = Runtime.getRuntime().totalMemory() / (1024 * 1024);

				long minMemory = Math.max(10, (long) 0.15 * maxMemory);

				/*
				 * System.out.println("Memory status: "+((maxMemory-(totalMemory-freemem))));
				 * System.out.println("Min memory: "+minMemory);
				 * System.out.println("15% memory: "+((long) (0.15*maxMemory)));
				 * System.out.println("Max memory: "+maxMemory);
				 * System.out.println("Total memory: "+totalMemory);
				 */

				if ((maxMemory - (totalMemory - freemem) < minMemory) && memoryCanbeOptimized == true) {
					// this.filter(appset, rr, rr1, map, dat);

					System.out.println("Memory status: " + ((maxMemory - (totalMemory - freemem))));
					System.out.println("Min memory: " + minMemory);

					this.filter(appset, rr, rr, map, dat);
					computePVal(dat, map);
					RedescriptionSet rTemp = new RedescriptionSet();
					double weights[] = { 0.2, 0.2, 0.2, 0.2, 0.2 };
					rTemp.createRedescriptionSet(this, weights, appset, dat, map);
					this.redescriptions.clear();

					for (int itm = 0; itm < rTemp.redescriptions.size(); itm++)
						this.redescriptions.add(rTemp.redescriptions.get(itm));

					for (int irt = 0; irt < redescriptions.size(); irt++)
						redescriptions.get(irt).clearRuleMaps();

					newRedescriptions = redescriptions.size();

					Runtime.getRuntime().gc();

					System.out.println("Memory status: " + ((maxMemory - (totalMemory - freemem))));
					System.out.println("Changing minJS level");
					System.out.println("New minJS level: " + appset.minJS);
				} else if ((maxMemory - (totalMemory - freemem) < minMemory) && memoryCanbeOptimized == false) {
					outOfmemory[0] = true;
					return newRedescriptions;
				}
			}
		}

		// this.filter(appset, rr, rr,map,dat);

		// this.remove(appset);

		if (appset.rightDisjunction == true) {
			System.out.println("Computing disjunctive refinement...");

			step = redescriptions.size() / 100;
			if (redescriptions.size() < 100)
				step = 1;

			for (int k = 0; k < redescriptions.size(); k++) {
				if (redescriptions.get(k).JS == 1.0)
					continue;
				double joinJS = 0.0, maxJoinJS = 0.0;
				int maxInd = 0;
				int negated = 0;

				if (appset.rightDisjunction == true) {
					// TIntHashSet
					// lRE=redescriptions.get(k).computeElements(redescriptions.get(k).viewElementsLists.get(0),
					// dat, map);
					// TIntHashSet
					// rRE=redescriptions.get(k).computeElements(redescriptions.get(k).viewElementsLists.get(1),
					// dat, map);
					// ArrayList<TIntHashSet> sideElems=new ArrayList<>();//to make proper
					// generalization
					// sideElems.add(lRE); sideElems.add(rRE);
					ArrayList<TIntHashSet> sideElems = redescriptions.get(k).computeElementsGen(dat, map);// new
																											// ArrayList<>();//to
																											// make
																											// proper
																											// generalization
					int interCount = js[0].computeGenInterCount(redescriptions.get(k), sideElems, view);
					joinJS = 0.0;
					maxJoinJS = 0.0;
					maxInd = 0;
					negated = 0;
					for (int i = oldIndexRR; i < rr.rules.size(); i++) {
						joinJS = js[0].computeRedescriptionRuleElementJacardGen(redescriptions.get(k), rr.rules.get(i),
								sideElems, view, 0, dat, map, appset, interCount);
						if (joinJS > maxJoinJS) {
							maxJoinJS = joinJS;
							maxInd = i;
							negated = 0;
						}

						if (appset.leftNegation == true) {
							joinJS = js[0].computeRedescriptionRuleElementJacardGen(redescriptions.get(k),
									rr.rules.get(i), sideElems, view, 1, dat, map, appset, interCount);
							if (joinJS > maxJoinJS) {
								maxJoinJS = joinJS;
								maxInd = i;
								negated = 1;
							}
						}
					}
					if (maxJoinJS > 0.5) {
						// System.out.println("maxJoinJS: "+maxJoinJS);
						redescriptions.get(k).disjunctiveJoin(rr.rules.get(maxInd), appset, dat, map, sideElems, view,
								negated);
					}
				}

				if ((k + 1) % step == 0) {
					System.out.println((((double) (k + 1) / redescriptions.size()) * 100) + "% completed...");
					System.out.println("num redescriptions: " + redescriptions.size());
					// Runtime.getRuntime().gc();
				}
			}
		}

		for (int t = 0; t < 2; t++) {
			System.out.println("Validation" + t);
			this.adaptSet(dat, map, 1);
			for (int i = 0; i < redescriptions.size(); i++) {
				// redescriptions.get(i).closeInterval(dat, map);
				// redescriptions.get(i).minimizeOptimal(dat, map, 1);
				redescriptions.get(i).validate(dat, map);
			}

			for (int i = 0; i < redescriptions.size(); i++)
				redescriptions.get(i).clearRuleMaps();
		}
		oldRIndex[0] = redescriptions.size();
		// return newRedescriptions;
		return 1;
	}

	public int combineViewRulesJoin(RuleReader rr, Jacard js[], ApplicationSettings appset, int oldIndexRR, int RunInd,
			boolean outOfmemory[], Mappings map, DataSetCreator dat, int[] oldRIndex, int view) {

		System.out.println("Using guided expansion with join procedure, multi-view!");
		boolean memoryCanbeOptimized = true;
		int maxNum = 0;

		maxNum = (rr.rules.size() - oldIndexRR) * (redescriptions.size() - oldRIndex[0]);

		if (appset.unguidedExpansion == true)
			maxNum = (rr.rules.size() - oldIndexRR) * (redescriptions.size());

		System.out.println("max number of rules: " + maxNum);
		System.out.println("old index rules: " + oldIndexRR);

		int numIt = 0;
		int step = maxNum / 100;

		if (step == 0)
			step = 1;

		int newRedescriptions = 0;
		int sIndex = oldRIndex[0];

		if (appset.unguidedExpansion == true)
			sIndex = 0;

		int ROS = redescriptions.size();

		for (int j = sIndex; j < ROS; j++) {

			if (j >= redescriptions.size())
				break;

			if (redescriptions.get(j).viewElementsLists.get(view).size() > 0)
				continue;
			for (int i = oldIndexRR/* rr.newRuleIndex-addon */; i < rr.rules.size(); i++) {
				int EmergencyExit = 0;
				for (int jinit = 0; jinit < js.length; jinit++)
					js[jinit].initialize();
				numIt++;

				double JSPos = 0.0, JSPosNeg = 0.0, JSNegPos = 0.0;
				JSPos = js[0].computeJacardGen(redescriptions.get(j), rr.rules.get(i), dat, map, 0);
				// System.out.println("JSPos multi-view: "+JSPos);
				if (JSPos >= appset.minAddRedJS && js[0].computePvalGen(redescriptions.get(j), rr.rules.get(i), dat,
						map, 0) <= appset.maxPval) {
					Redescription tmp = new Redescription(redescriptions.get(j), rr.rules.get(i).rule, js[0].JS, map,
							dat, 0, view);
					tmp.computeElements(redescriptions.get(j), rr.rules.get(i), view);
					tmp.computeUnion(redescriptions.get(j), rr.rules.get(i), dat, map, 0);

					Redescription tmp1 = new Redescription(tmp, dat);
					int found = 0;

					ArrayList<Redescription> toRemove = new ArrayList<>();
					ArrayList<Redescription> toAdd = new ArrayList<>();
					ArrayList<Redescription> toRefine = new ArrayList<>();
					int refInd = 0, refinementFound = 0;
					int refined = 0;
					for (int k = 0; k < redescriptions.size(); k++) {

						long maxMemory = Runtime.getRuntime().maxMemory() / (1024 * 1024);
						long freemem = Runtime.getRuntime().freeMemory() / (1024 * 1024);
						long totalMemory = Runtime.getRuntime().totalMemory() / (1024 * 1024);

						long minMemory = Math.max(10, (long) (0.15 * maxMemory));

						if ((maxMemory - (totalMemory - freemem) < minMemory) && memoryCanbeOptimized == true) {
							EmergencyExit = 1;
							break;
						}

						if (redescriptions.get(k).JS == 1.0) {
							if (redescriptions.get(k).CompareEqual(tmp) == 2 && refinementFound == 0) {
								toRefine.add(redescriptions.get(k));
								refInd = k;
								tmp = redescriptions.get(k);
								refinementFound = 1;
								found = 1;
								refined = 1;
								// break;
							}
							continue;
						}
						int quality = tmp.CompareQuality(redescriptions.get(k));
						found = 0;
						if (quality == -1) {
							continue;
						} else if (quality == 2 && refinementFound == 0) {

							if (tmp1.elements.size() == redescriptions.get(k).elements.size()) {
								redescriptions.get(k).join(tmp1, map, dat);
								if (redescriptions.get(k).JS == 1.0)
									refined = 1;
							} else {
								if (refined == 0) {// rafiniraj dok ne zadovoljiš uvjet
									tmp1.join(redescriptions.get(k), map, dat);
									if (tmp1.JS >= appset.minJS && tmp1.elements.size() <= appset.maxSupport) {
										toAdd.add(tmp1);
										refined = 1;
										// break;
									}
								}
							}

							found = 1;
						} else if (quality == 1 && redescriptions.get(k).JS < 1.0) {
							if (tmp.JS > tmp1.JS)
								redescriptions.get(k).join(tmp, map, dat);
							else
								redescriptions.get(k).join(tmp1, map, dat);
							// found=1;

							if (redescriptions.get(k).elements.size() < appset.minSupport)
								toRemove.add(redescriptions.get(k));

						}
					}

					for (int k = 0; k < toRemove.size(); k++)
						redescriptions.remove(toRemove.get(k));
					if (toRemove.size() > 0) {
						newRedescriptions -= toRemove.size();
						toRemove.clear();
					}

					for (int k = 0; k < toAdd.size(); k++)
						redescriptions.add(toAdd.get(k));
					if (toAdd.size() > 0) {
						newRedescriptions += toAdd.size();
						toAdd.clear();
					}

					if (found == 0) {
						if (tmp.elements.size() >= appset.minSupport && tmp.JS >= appset.minJS
								&& tmp.elements.size() <= appset.maxSupport) {// tmp.JS>0.4 || (tmp.elements.size()>1 &&
																				// tmp.JS>0.2)
							redescriptions.add(tmp);
							newRedescriptions++;
						}
					}

					if (toRefine.size() > 0) {

						ArrayList<Integer> rmInd = new ArrayList();
						int rUW = toRefine.get(0).numUsedViews();
						for (int k = redescriptions.size() - 1; k >= 0; k--) {
							if (k != refInd && redescriptions.get(k).CompareEqual(toRefine.get(0)) == 2
									&& rUW >= redescriptions.get(k).numUsedViews())
								rmInd.add(k);
						}
						toRefine.clear();
						refInd = 0;
						refinementFound = 0;
						newRedescriptions -= rmInd.size();
						for (int k = 0; k < rmInd.size(); k++)
							redescriptions.remove((int) rmInd.get(k));
						rmInd.clear();
					}
					// }

					/*
					 * if(tmp.elements.size()>=appset.minSupport &&
					 * tmp.elements.size()<=appset.maxSupport){//tmp.JS>0.4 ||
					 * (tmp.elements.size()>1 && tmp.JS>0.2) redescriptions.add(tmp);
					 * newRedescriptions++; }//fix negation and validation!
					 */
				}
				if (appset.rightNegation == true) {
					JSPosNeg = js[1].computeJacardGen(redescriptions.get(j), rr.rules.get(i), dat, map, 1);
					if (JSPosNeg >= appset.minJS && js[1].computePvalGen(redescriptions.get(j), rr.rules.get(i), dat,
							map, 1) <= appset.maxPval) {
						Redescription tmp = new Redescription(redescriptions.get(j), rr.rules.get(i).rule, js[1].JS,
								map, dat, 1, view);
						tmp.computeElements(redescriptions.get(j), rr.rules.get(i), dat, map, 1, view);
						tmp.computeUnion(redescriptions.get(j), rr.rules.get(i), dat, map, 1);
						if (tmp.elements.size() >= appset.minSupport && tmp.elements.size() <= appset.maxSupport) {// tmp.JS>0.4
																													// ||
																													// (tmp.elements.size()>1
																													// &&
																													// tmp.JS>0.2)
							redescriptions.add(tmp);
							newRedescriptions++;
						}
					}
				}

				/*
				 * if(redescriptions.size()>100000){//test this.filter(appset, rr, rr,map,dat);
				 * }
				 */

				if (numIt % step == 0) {
					System.out.println((((double) numIt / maxNum) * 100) + "% completed...");
					System.out.println("num redescriptions: " + redescriptions.size());
					// Runtime.getRuntime().gc();
				}
				if (numIt == maxNum)
					System.out.println("100% completed!");

				long maxMemory = Runtime.getRuntime().maxMemory() / (1024 * 1024);
				long freemem = Runtime.getRuntime().freeMemory() / (1024 * 1024);
				long totalMemory = Runtime.getRuntime().totalMemory() / (1024 * 1024);

				long minMemory = Math.max(10, (long) 0.15 * maxMemory);

				/*
				 * System.out.println("Memory status: "+((maxMemory-(totalMemory-freemem))));
				 * System.out.println("Min memory: "+minMemory);
				 * System.out.println("15% memory: "+((long) (0.15*maxMemory)));
				 * System.out.println("Max memory: "+maxMemory);
				 * System.out.println("Total memory: "+totalMemory);
				 */

				if ((maxMemory - (totalMemory - freemem) < minMemory) || EmergencyExit == 1) {
					// this.filter(appset, rr, rr1, map, dat);
					EmergencyExit = 0;
					System.out.println("Memory status: " + ((maxMemory - (totalMemory - freemem))));
					System.out.println("Min memory: " + minMemory);

					this.filter(appset, rr, rr, map, dat);
					computePVal(dat, map);
					RedescriptionSet rTemp = new RedescriptionSet();
					double weights[] = { 0.2, 0.2, 0.2, 0.2, 0.2 };
					rTemp.createRedescriptionSet(this, weights, appset, dat, map);
					this.redescriptions.clear();

					for (int itm = 0; itm < rTemp.redescriptions.size(); itm++)
						this.redescriptions.add(rTemp.redescriptions.get(itm));

					for (int irt = 0; irt < redescriptions.size(); irt++)
						redescriptions.get(irt).clearRuleMaps();

					newRedescriptions = redescriptions.size();

					Runtime.getRuntime().gc();

					System.out.println("Memory status: " + ((maxMemory - (totalMemory - freemem))));
					System.out.println("Changing minJS level");
					System.out.println("New minJS level: " + appset.minJS);
				} else if ((maxMemory - (totalMemory - freemem) < minMemory) && memoryCanbeOptimized == false) {
					outOfmemory[0] = true;
					return newRedescriptions;
				}
			}
		}

		// this.filter(appset, rr, rr,map,dat);

		// this.remove(appset);

		if (appset.rightDisjunction == true) {
			System.out.println("Computing disjunctive refinement...");

			step = redescriptions.size() / 100;
			if (redescriptions.size() < 100)
				step = 1;

			for (int k = 0; k < redescriptions.size(); k++) {
				if (redescriptions.get(k).JS == 1.0)
					continue;
				double joinJS = 0.0, maxJoinJS = 0.0;
				int maxInd = 0;
				int negated = 0;

				if (appset.rightDisjunction == true) {
					// TIntHashSet
					// lRE=redescriptions.get(k).computeElements(redescriptions.get(k).viewElementsLists.get(0),
					// dat, map);
					// TIntHashSet
					// rRE=redescriptions.get(k).computeElements(redescriptions.get(k).viewElementsLists.get(1),
					// dat, map);
					// ArrayList<TIntHashSet> sideElems=new ArrayList<>();//to make proper
					// generalization
					// sideElems.add(lRE); sideElems.add(rRE);
					ArrayList<TIntHashSet> sideElems = redescriptions.get(k).computeElementsGen(dat, map);// new
																											// ArrayList<>();//to
																											// make
																											// proper
																											// generalization
					int interCount = js[0].computeGenInterCount(redescriptions.get(k), sideElems, view);
					joinJS = 0.0;
					maxJoinJS = 0.0;
					maxInd = 0;
					negated = 0;
					for (int i = oldIndexRR; i < rr.rules.size(); i++) {
						joinJS = js[0].computeRedescriptionRuleElementJacardGen(redescriptions.get(k), rr.rules.get(i),
								sideElems, view, 0, dat, map, appset, interCount);
						if (joinJS > maxJoinJS) {
							maxJoinJS = joinJS;
							maxInd = i;
							negated = 0;
						}

						if (appset.leftNegation == true) {
							joinJS = js[0].computeRedescriptionRuleElementJacardGen(redescriptions.get(k),
									rr.rules.get(i), sideElems, view, 1, dat, map, appset, interCount);
							if (joinJS > maxJoinJS) {
								maxJoinJS = joinJS;
								maxInd = i;
								negated = 1;
							}
						}
					}
					if (maxJoinJS > 0.5) {
						// System.out.println("maxJoinJS: "+maxJoinJS);
						redescriptions.get(k).disjunctiveJoin(rr.rules.get(maxInd), appset, dat, map, sideElems, view,
								negated);
					}
				}

				if ((k + 1) % step == 0) {
					System.out.println((((double) (k + 1) / redescriptions.size()) * 100) + "% completed...");
					System.out.println("num redescriptions: " + redescriptions.size());
					// Runtime.getRuntime().gc();
				}
			}
		}

		for (int t = 0; t < 2; t++) {
			System.out.println("Validation" + t);
			this.adaptSet(dat, map, 1);
			for (int i = 0; i < redescriptions.size(); i++) {
				// redescriptions.get(i).closeInterval(dat, map);
				// redescriptions.get(i).minimizeOptimal(dat, map, 1);
				redescriptions.get(i).validate(dat, map);
			}

			for (int i = 0; i < redescriptions.size(); i++)
				redescriptions.get(i).clearRuleMaps();
		}
		oldRIndex[0] = redescriptions.size();
		// return newRedescriptions;
		return 1;
	}

	void remove(ApplicationSettings appset) {
		// removing all redescriptions with inadequate minSupport and minJS
		for (Iterator<Redescription> iteratorR = redescriptions.iterator(); iteratorR.hasNext();) {
			Redescription test = iteratorR.next();
			if (test.elements.size() < appset.minSupport || test.JS < appset.minJS
					|| test.elements.size() > appset.maxSupport) {
				iteratorR.remove();
			}
		}
	}

	void remove(ApplicationSettings appset, int num) {
		// removing all redescriptions with inadequate minSupport and minJS
		// but exactly count rules
		int count = redescriptions.size() - appset.numRetRed;
		for (Iterator<Redescription> iteratorR = redescriptions.iterator(); iteratorR.hasNext();) {
			Redescription test = iteratorR.next();
			if (test.elements.size() < appset.minSupport || test.JS < appset.minJS) {
				iteratorR.remove();
				count--;
				if (count == 0)
					return;
			}
		}
	}

	void removePVal(ApplicationSettings appset) {
		// removing all redescriptions with inadequate minPval
		for (Iterator<Redescription> iteratorR = redescriptions.iterator(); iteratorR.hasNext();) {
			Redescription test = iteratorR.next();
			if (test.pVal > appset.maxPval) {
				iteratorR.remove();
			}
		}
	}

	int countNumber(double JS) {
		int count = 0;

		for (int i = 0; i < redescriptions.size(); i++)
			if (redescriptions.get(i).JS < JS)
				count++;

		return count;
	}

	void filter(ApplicationSettings appset, RuleReader rr, RuleReader rr1, Mappings map, DataSetCreator dat) {
		if (redescriptions.size() == 0)
			return;
		System.out.println("Filtering redescriptions!");
		ArrayList<Redescription> toRemove = new ArrayList<>();
		int iterationCount = 0;
		Jacard js = new Jacard();
		js.initialize();
		while (true) {
			int ok = 0;
			System.out.println("Filtering iteration: " + iterationCount);
			System.out.println("Redescriptions size: " + redescriptions.size());
			for (int i = iterationCount; i < redescriptions.size(); i++) {
				ok = 1;
				int nuwi = redescriptions.get(i).numUsedViews();
				for (int j = i + 1; j < redescriptions.size(); j++) {
					int nuwj = redescriptions.get(j).numUsedViews();
					int cEq = redescriptions.get(i).CompareEqual(redescriptions.get(j));
					if (cEq == 2) {

						double attrJS = js.computeAttributeJacard(redescriptions.get(i), redescriptions.get(j), dat);

						if (appset.allowSERed == true && attrJS <= 0.2)
							continue;

						// add constraint on attributes
						if (redescriptions.get(i).JS < 1.0 && redescriptions.get(j).JS < 1.0 && nuwi == nuwj) {
							redescriptions.get(i).join(redescriptions.get(j), map, dat);
							toRemove.add(redescriptions.get(j));
						} else if (redescriptions.get(i).JS == 1.0 && redescriptions.get(j).JS < 1.0 && nuwi == nuwj)
							toRemove.add(redescriptions.get(j));
						else if (redescriptions.get(i).JS < 1.0 && redescriptions.get(j).JS == 1.0 && nuwi == nuwj)
							toRemove.add(redescriptions.get(i));
						else if (redescriptions.get(i).JS == 1.0 && redescriptions.get(j).JS == 1.0 && nuwi == nuwj) {
							if ((redescriptions.get(i).viewElementsLists.get(0).size()
									+ redescriptions.get(i).viewElementsLists.get(1)
											.size()) > (redescriptions.get(j).viewElementsLists.get(0).size()
													+ redescriptions.get(j).viewElementsLists.get(1).size()))// generalize
								toRemove.add(redescriptions.get(i));
							else
								toRemove.add(redescriptions.get(j));
						} else if (nuwi > nuwj) {
							if (redescriptions.get(i).JS >= redescriptions.get(j).JS
									&& redescriptions.get(i).pVal < redescriptions.get(j).pVal)
								toRemove.add(redescriptions.get(j));
							else {
								redescriptions.get(i).join(redescriptions.get(j), map, dat);
								toRemove.add(redescriptions.get(j));
							}
						} else if (nuwi < nuwj) {
							if (redescriptions.get(i).JS <= redescriptions.get(j).JS
									&& redescriptions.get(i).pVal >= redescriptions.get(j).pVal)
								toRemove.add(redescriptions.get(i));
							else {
								redescriptions.get(j).join(redescriptions.get(i), map, dat);
								toRemove.add(redescriptions.get(i));
							}
						}
					} else if (cEq == -2) {
						js.initialize();
						double elemJS = js.computeRedescriptionElementJacard(redescriptions.get(i),
								redescriptions.get(j));
						double attrJS = js.computeAttributeJacard(redescriptions.get(i), redescriptions.get(j), dat);
						if ((appset.allowSERed == false && elemJS == 1.0)
								|| (appset.allowSERed == true && elemJS == 1.0 && attrJS >= 0.2)) {
							if (nuwi > nuwj) {
								if (redescriptions.get(i).JS >= redescriptions.get(j).JS
										&& redescriptions.get(i).pVal < redescriptions.get(j).pVal)
									toRemove.add(redescriptions.get(j));
								// else
								// toRemove.add(redescriptions.get(i));
							} else if (nuwj > nuwi) {
								if (redescriptions.get(j).JS >= redescriptions.get(i).JS
										&& redescriptions.get(j).pVal < redescriptions.get(i).pVal)
									toRemove.add(redescriptions.get(i));
								// else
								// toRemove.add(redescriptions.get(j));
							} else if (redescriptions.get(i).containsNegation()) {
								if (redescriptions.get(i).JS >= redescriptions.get(j).JS)
									toRemove.add(redescriptions.get(j));
								else if (appset.allowSERed == false)
									toRemove.add(redescriptions.get(i));
								// toRemove.add(redescriptions.get(i));
							} else if (redescriptions.get(j).containsNegation())
								if (redescriptions.get(j).JS >= redescriptions.get(i).JS)
									toRemove.add(redescriptions.get(i));
								else if (appset.allowSERed == false)
									toRemove.add(redescriptions.get(j));
								// toRemove.add(redescriptions.get(j));
								else if (redescriptions.get(i).containsDisjunction())
									if (redescriptions.get(i).JS >= redescriptions.get(j).JS)
										toRemove.add(redescriptions.get(j));
									else if (appset.allowSERed == false)
										toRemove.add(redescriptions.get(i));
									else if (redescriptions.get(j).containsDisjunction())
										if (redescriptions.get(j).JS >= redescriptions.get(i).JS)
											toRemove.add(redescriptions.get(i));
										else if (appset.allowSERed == false)
											toRemove.add(redescriptions.get(j));
							/*
							 * else if(redescriptions.get(i).JS>=redescriptions.get(j).JS)
							 * toRemove.add(redescriptions.get(j)); else
							 * if(redescriptions.get(i).JS>=redescriptions.get(j).JS)
							 * toRemove.add(redescriptions.get(j)); else
							 * toRemove.add(redescriptions.get(i));
							 */
						}
						/*
						 * else if(appset.allowSERed==true && elemJS==1.0 && attrJS>=0.2){
						 * if(redescriptions.get(i).numUsedViews()>redescriptions.get(j).numUsedViews())
						 * toRemove.add(redescriptions.get(j)); else
						 * if(redescriptions.get(j).numUsedViews()>redescriptions.get(i).numUsedViews())
						 * toRemove.add(redescriptions.get(i)); else
						 * if(redescriptions.get(j).containsNegation())
						 * toRemove.add(redescriptions.get(j)); else
						 * if(redescriptions.get(i).containsNegation())
						 * toRemove.add(redescriptions.get(i)); else
						 * if(redescriptions.get(i).containsDisjunction())
						 * toRemove.add(redescriptions.get(i)); else
						 * if(redescriptions.get(j).containsDisjunction())
						 * toRemove.add(redescriptions.get(j)); else
						 * toRemove.add(redescriptions.get(j)); }
						 */
					}
				}
				if (toRemove.size() == 0)
					iterationCount = i;
				if (toRemove.size() > 0) {
					for (int j = 0; j < toRemove.size(); j++)
						redescriptions.remove(toRemove.get(j));
					ok = 0;
					toRemove.clear();
					break;
				}
			}
			if (ok == 1)
				break;
		}
	}

	int getNFRed() {
		int numFullRed = 0;
		for (int i = 0; i < redescriptions.size(); i++) {
			if (redescriptions.get(i).JS == 1.0)
				numFullRed++;
		}
		return numFullRed;
	}

	int computePVal(DataSetCreator dat, Mappings map) {
		int numFullRed = 0;
		for (int i = 0; i < redescriptions.size(); i++) {// remember only attributes without values, close at the end on
															// the fly
			if (redescriptions.get(i).pVal != 0.0) continue;
			ArrayList<TIntHashSet> elems = redescriptions.get(i).computeElementsGen(dat, map);

			double elemSize = 1.0, numExamples = 1.0;

			for (int k = 0; k < redescriptions.get(i).viewElementsLists.size(); k++) {
				if (redescriptions.get(i).viewElementsLists.get(k).size() > 0) {
					elemSize *= elems.get(k).size();
					numExamples *= dat.numExamples;
					// System.out.println("k: "+k);
				}
			}

			double prob = elemSize / numExamples;
			BinomialDistribution dist = new BinomialDistribution(dat.numExamples, prob);
			redescriptions.get(i).pVal = 1.0 - dist.cumulativeProbability(redescriptions.get(i).elements.size());
			/*
			 * double
			 * prob=((double)(redescriptions.get(i).supportsSides.get(0)*redescriptions.get(
			 * i).supportsSides.get(1)))/(dat.numExamples*dat.numExamples);//generalize
			 * BinomialDistribution dist=new BinomialDistribution(dat.numExamples,prob);
			 * redescriptions.get(i).pVal=1.0-dist.cumulativeProbability(redescriptions.get(
			 * i).elements.size());
			 */
			if (redescriptions.get(i).JS == 1.0)
				numFullRed++;
		}
		return numFullRed;
	}

	void sortRedescriptions() {
		Collections.sort(redescriptions, Collections.reverseOrder());
	}

	void writeToFile(String output, DataSetCreator dat, Mappings fid, long startTime, int numFullRed,
			ApplicationSettings appset, double score, double coverage[], boolean oom[]) {
		System.out.println("Java, output file: " + output);

		try {
			BufferedWriter bw;
			File file = new File(output);

			if (!file.exists()) {
				file.createNewFile();
			}
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			bw = new BufferedWriter(fw);

			System.out.println();
			System.out.println();
			System.out.println("Redescriptions: ");
			bw.write("Redescriptions: \n\n");
			int numIt = 0;
			int maxNum = redescriptions.size();
			int step = maxNum / 100;

			if (step == 0)
				step = 1;

			System.out.println("Number of redescriptions: " + (maxNum));
			System.out.println("\n\n\n\n");

			int numOK = 0, numNOK = 0;

			int numRedescriptions = redescriptions.size();

			if (appset.numRetRed == Integer.MAX_VALUE)
				numRedescriptions = redescriptions.size();
			else if (appset.numRetRed < redescriptions.size())
				numRedescriptions = appset.numRetRed;

			// close interval and validate redesc
			for (int i = 0; i < numRedescriptions; i++) {
				if (appset.optimizationType == 1)
					redescriptions.get(i).closeInterval(dat, fid);
				// redescriptions.get(i).closeInterval(dat, fid);
				/*
				 * redescriptions.get(i).closeInterval(dat, fid); if(appset.minimizeRules)
				 * redescriptions.get(i).minimize(dat,fid);
				 */
				if (redescriptions.get(i).validate(dat, fid) == 0) {
					numNOK++;
					System.out.println("Not OK: ");
					System.out.println(redescriptions.get(i).ruleStrings.get(0));// generalize
					System.out.println(redescriptions.get(i).ruleStrings.get(1));
				} else
					numOK++;
				// System.out.println("Left rule final before close:
				// "+redescriptions.get(i).LSrule);
				// redescriptions.get(i).createRuleString();
				// System.out.println("Left rule final after close:
				// "+redescriptions.get(i).LSrule);
				bw.write("Rules: \n\n");
				// System.out.println("LSR: "+redescriptions.get(i).LSrule);
				for (int z = 0; z < redescriptions.get(i).viewElementsLists.size(); z++) {
					if (redescriptions.get(i).viewElementsLists.size() > 0) {
						bw.write("W" + (z + 1) + "R: " + redescriptions.get(i).ruleStrings.get(z) + "\n");// generalize
						// System.out.println("RSR: "+redescriptions.get(i).RSrule);
						// bw.write("RSR: "+redescriptions.get(i).ruleStrings.get(1)+"\n");//generalize
					}
				}
				// System.out.println("JS: "+redescriptions.get(i).JS);
				bw.write("JS: " + redescriptions.get(i).JS + "\n");
				if (appset.useSplitTesting == true) {
					bw.write("JSPred: " + redescriptions.get(i).JSAll + "\n");
					bw.write("JSGen: " + redescriptions.get(i).JSTest + "\n");
				}
				bw.write("p-value :" + redescriptions.get(i).pVal + "\n");
				if (appset.useSplitTesting == true) {
					bw.write("p-valuePred :" + redescriptions.get(i).pValValidation + "\n");
					bw.write("p-valueTest :" + redescriptions.get(i).pValTest + "\n");
				}
				bw.write("Support intersection: " + redescriptions.get(i).elements.size() + "\n");
				bw.write("Support union: " + redescriptions.get(i).elementsUnion.size() + "\n\n");

//				if (appset.useSplitTesting == true) {
//					bw.write("Support intersection pred: " + redescriptions.get(i).elementsValidation.size() + "\n");
//					bw.write("Support union pred: " + redescriptions.get(i).elementsUnionValidation.size() + "\n\n");
//					bw.write("Support intersection test: " + redescriptions.get(i).elementsTest.size() + "\n");
//					bw.write("Support union test: " + redescriptions.get(i).elementsUnionTest.size() + "\n\n");
//				}
//				System.out.println("Covered examples: ");


				bw.write("Covered examples (intersection): \n");

				TIntIterator it = redescriptions.get(i).elements.iterator();

				while (it.hasNext()) {
					int s = it.next();
					bw.write(fid.idExample.get(s) + " ");
				}

				/*
				 * for(int s:redescriptions.get(i).elements){ // System.out.print(s+" ");
				 * bw.write(fid.idExample.get(s)+" "); //output+=s+" "; }
				 */
				// output+="\n";
				bw.write("\n");
				bw.write("Union elements: \n");
				// System.out.println("Union: ");

				it = redescriptions.get(i).elementsUnion.iterator();

				while (it.hasNext()) {
					int s = it.next();
					bw.write(fid.idExample.get(s) + " ");
				}

				/*
				 * for(int s:redescriptions.get(i).elementsUnion){ // System.out.print(s+" ");
				 * bw.write(fid.idExample.get(s)+" "); //output+=s+" "; }
				 */
				// System.out.println();
				// output+="\n\n";
				bw.write("\n\n");
				numIt++;
				if (numIt % step == 0)
					System.out.println((((double) numIt / maxNum) * 100) + "% completed...");
				if (numIt == maxNum)
					System.out.println("100% completed!");

			}
			// redescriptions.clear();
			System.out.println("Number of OK redescriptions: " + numOK);
			System.out.println("Number of not OK redescriptions: " + numNOK);
			bw.write("Number of tests: " + this.NumTests + "\n");
			bw.write("Found: " + numFullRed + " redescriptions with JS=1.0 and minsupport>" + appset.minSupport
					+ "\n\n");

			bw.write("Results value interval: [0,1]\n");
			bw.write("Elements coverage: " + coverage[0] + "\n");
			bw.write("Attributes coverage: " + coverage[1] + "\n");

			bw.write("Average JS: " + aJS + "\n");
			bw.write("Average PVal: " + aPvalSc + "\n");
			bw.write("Average entity JS: " + aEJS + "\n");
			bw.write("Average atribute JS: " + aAJS + "\n");
			bw.write("Average rule size: " + aRS + "\n");
			bw.write("Result set score: " + score + "\n\n");

			if (oom[0] == true)
				bw.write("Warning: insufficient memory to run all iterations, consider increasing java VM memory by using -Xmx flag!\n");

			long estimatedForestCreationTime = TargetRulesMap.getForestCreationTime();
			if (estimatedForestCreationTime > 0) {
				Map<String, Long> humanReadableForestCreationTime = millisToHumanReadable(estimatedForestCreationTime);
				bw.write("Supplementing forest creation time: " + humanReadableTimeToString(humanReadableForestCreationTime) + "\n\n");
			}

			long totalEstimatedTime = System.currentTimeMillis() - startTime;
			Map<String, Long> humanReadableTotalTime = millisToHumanReadable(totalEstimatedTime);
			bw.write("Elapsed time: " + humanReadableTimeToString(humanReadableTotalTime) + "\n");
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	void writeToFileBaseline(String output, long startTime, ApplicationSettings appset, Mappings fid, double score) {
		System.out.println("Java, output file: " + output);
		int numFullRed = 0;

		try {
			BufferedWriter bw;
			File file = new File(output);

			if (!file.exists()) {
				file.createNewFile();
			}
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			bw = new BufferedWriter(fw);

			System.out.println();
			System.out.println();
			System.out.println("Redescriptions: ");
			bw.write("Redescriptions: \n\n");
			int numIt = 0;
			int maxNum = redescriptions.size();
			int step = maxNum / 100;

			if (step == 0)
				step = 1;

			System.out.println("Number of redescriptions: " + (maxNum));
			System.out.println("\n\n\n\n");

			int numOK = 0, numNOK = 0;

			int numRedescriptions = redescriptions.size();

			if (appset.numRetRed == Integer.MAX_VALUE)
				numRedescriptions = redescriptions.size();
			else if (appset.numRetRed < redescriptions.size())
				numRedescriptions = appset.numRetRed;

			// close interval and validate redesc
			for (int i = 0; i < numRedescriptions; i++) {
				if (appset.optimizationType == 1)

					bw.write("Rules: \n\n");
				// System.out.println("LSR: "+redescriptions.get(i).LSrule);
				for (int z = 0; z < redescriptions.get(i).ruleStrings.size(); z++) {
					bw.write("W" + (z + 1) + "R: " + redescriptions.get(i).ruleStrings.get(z) + "\n");
				}
				// System.out.println("JS: "+redescriptions.get(i).JS);
				if (redescriptions.get(i).JS == 1.0) {
					numFullRed++;
				}

				bw.write("JS: " + redescriptions.get(i).JS + "\n");
				if (appset.useSplitTesting == true) {
					bw.write("JSPred: " + redescriptions.get(i).JSAll + "\n");
					bw.write("JSGen: " + redescriptions.get(i).JSTest + "\n");
				}
				bw.write("p-value :" + redescriptions.get(i).pVal + "\n");
				if (appset.useSplitTesting == true) {
					bw.write("p-valuePred :" + redescriptions.get(i).pValValidation + "\n");
					bw.write("p-valueTest :" + redescriptions.get(i).pValTest + "\n");
				}
				bw.write("Support intersection: " + redescriptions.get(i).elements.size() + "\n");
				bw.write("Support union: " + redescriptions.get(i).elementsUnion.size() + "\n\n");

				if (appset.useSplitTesting == true) {
					bw.write("Support intersection pred: " + redescriptions.get(i).elementsValidation.size() + "\n");
					bw.write("Support union pred: " + redescriptions.get(i).elementsUnionValidation.size() + "\n\n");
					bw.write("Support intersection test: " + redescriptions.get(i).elementsTest.size() + "\n");
					bw.write("Support union test: " + redescriptions.get(i).elementsUnionTest.size() + "\n\n");
				}
				// System.out.println("Covered examples: ");
				bw.write("Covered examples (intersection): \n");

				TIntIterator it = redescriptions.get(i).elements.iterator();

				while (it.hasNext()) {
					int s = it.next();
					bw.write(fid.idExample.get(s) + " ");
				}

				/*
				 * for(int s:redescriptions.get(i).elements){ // System.out.print(s+" ");
				 * bw.write(fid.idExample.get(s)+" "); //output+=s+" "; }
				 */
				// output+="\n";
				bw.write("\n");
				bw.write("Union elements: \n");
				// System.out.println("Union: ");

				it = redescriptions.get(i).elementsUnion.iterator();

				while (it.hasNext()) {
					int s = it.next();
					bw.write(fid.idExample.get(s) + " ");
				}

				/*
				 * for(int s:redescriptions.get(i).elementsUnion){ // System.out.print(s+" ");
				 * bw.write(fid.idExample.get(s)+" "); //output+=s+" "; }
				 */
				// System.out.println();
				// output+="\n\n";
				bw.write("\n\n");
				numIt++;
				if (numIt % step == 0)
					System.out.println((((double) numIt / maxNum) * 100) + "% completed...");
				if (numIt == maxNum)
					System.out.println("100% completed!");

			}
			// redescriptions.clear();
			System.out.println("Number of OK redescriptions: " + numOK);
			System.out.println("Number of not OK redescriptions: " + numNOK);
			bw.write("Number of tests: " + this.NumTests + "\n");
			bw.write("Found: " + numFullRed + " redescriptions with JS=1.0 and minsupport>" + appset.minSupport
					+ "\n\n");

			long totalEstimatedTime = System.currentTimeMillis() - startTime;
			Map<String, Long> humanReadableTotalTime = millisToHumanReadable(totalEstimatedTime);

			bw.write("Results value interval: [0,1]\n");
			bw.write("Result set score: " + score + "\n\n");

			bw.write("Elapsed time: " + humanReadableTimeToString(humanReadableTotalTime) + "\n");

			long estimatedForestCreationTime = TargetRulesMap.getForestCreationTime();
			if (estimatedForestCreationTime > 0) {
				Map<String, Long> humanReadableForestCreationTime = millisToHumanReadable(estimatedForestCreationTime);
				bw.write("Supplementing forest creation time: " + humanReadableTimeToString(humanReadableForestCreationTime) + "\n");
			}
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	private Map<String, Long> millisToHumanReadable(long estimatedTime) {
		long days = TimeUnit.MILLISECONDS.toDays(estimatedTime);
		estimatedTime -= TimeUnit.DAYS.toMillis(days);
		long hours = TimeUnit.MILLISECONDS.toHours(estimatedTime);
		estimatedTime -= TimeUnit.HOURS.toMillis(hours);
		long minutes = TimeUnit.MILLISECONDS.toMinutes(estimatedTime);
		estimatedTime -= TimeUnit.MINUTES.toMillis(minutes);
		long seconds = TimeUnit.MILLISECONDS.toSeconds(estimatedTime);
		estimatedTime -= TimeUnit.SECONDS.toMillis(seconds);

		Map<String, Long> humanReadableTime = new HashMap<>();
		humanReadableTime.put("days", days);
		humanReadableTime.put("hours", hours);
		humanReadableTime.put("minutes", minutes);
		humanReadableTime.put("seconds", seconds);
		humanReadableTime.put("millis", estimatedTime);

		return humanReadableTime;
	}

	private String humanReadableTimeToString(Map<String, Long> humanReadableTime) {
		return humanReadableTime.get("days") + " days, "
				+ humanReadableTime.get("hours") + " hours," + humanReadableTime.get("minutes")
				+ " minutes, " + humanReadableTime.get("seconds") + " seconds, "
				+ humanReadableTime.get("millis") + " milliseconds.";
	}

	void writeToFileTmp(String output, DataSetCreator dat, Mappings fid, ApplicationSettings appset) {
		try {
			BufferedWriter bw;
			File file = new File(output);

			if (!file.exists()) {
				file.createNewFile();
			}
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			bw = new BufferedWriter(fw);

			System.out.println();
			System.out.println();
			System.out.println("Redescriptions: ");
			bw.write("Redescriptions: \n\n");
			int numIt = 0;
			int maxNum = redescriptions.size();
			int step = maxNum / 100;

			if (step == 0)
				step = 1;

			System.out.println("Number of redescriptions: " + (maxNum));
			System.out.println("\n\n\n\n");

			int numOK = 0, numNOK = 0;

			int numRedescriptions = redescriptions.size();
			int numFullRed = 0;
			// close interval and validate redesc
			for (int i = 0; i < numRedescriptions; i++) {

				if (redescriptions.get(i).JS == 1.0)
					numFullRed++;
				/*
				 * redescriptions.get(i).closeInterval(dat, fid); if(appset.minimizeRules)
				 * redescriptions.get(i).minimize(dat,fid);
				 */
				if (redescriptions.get(i).validate(dat, fid) == 0) {
					numNOK++;
					System.out.println("Not OK: ");
					System.out.println(redescriptions.get(i).ruleStrings.get(0));// generalize
					System.out.println(redescriptions.get(i).ruleStrings.get(1));
				} else
					numOK++;
				// System.out.println("Left rule final before close:
				// "+redescriptions.get(i).LSrule);
				// redescriptions.get(i).createRuleString();
				// System.out.println("Left rule final after close:
				// "+redescriptions.get(i).LSrule);
				bw.write("Rules: \n\n");
				// System.out.println("LSR: "+redescriptions.get(i).LSrule);
				bw.write("LSR: " + redescriptions.get(i).ruleStrings.get(0) + "\n");// generalizes
				// System.out.println("RSR: "+redescriptions.get(i).RSrule);
				bw.write("RSR: " + redescriptions.get(i).ruleStrings.get(1) + "\n");
				// System.out.println("JS: "+redescriptions.get(i).JS);
				bw.write("JS: " + redescriptions.get(i).JS + "\n");
				bw.write("p-value :" + redescriptions.get(i).pVal + "\n");
				bw.write("Support intersection: " + redescriptions.get(i).elements.size() + "\n");
				bw.write("Support union: " + redescriptions.get(i).elementsUnion.size() + "\n\n");
				// System.out.println("Covered examples: ");
				bw.write("Covered examples (intersection): \n");

				TIntIterator it = redescriptions.get(i).elements.iterator();

				while (it.hasNext()) {
					int s = it.next();
					bw.write(fid.idExample.get(s) + " ");
				}

				/*
				 * for(int s:redescriptions.get(i).elements){ // System.out.print(s+" ");
				 * bw.write(fid.idExample.get(s)+" "); //output+=s+" "; }
				 */
				// output+="\n";
				bw.write("\n");
				bw.write("Union elements: \n");
				// System.out.println("Union: ");

				it = redescriptions.get(i).elementsUnion.iterator();

				while (it.hasNext()) {
					int s = it.next();
					bw.write(fid.idExample.get(s) + " ");
				}

				/*
				 * for(int s:redescriptions.get(i).elementsUnion){ // System.out.print(s+" ");
				 * bw.write(fid.idExample.get(s)+" "); //output+=s+" "; }
				 */
				// System.out.println();
				// output+="\n\n";
				bw.write("\n\n");
				numIt++;
				if (numIt % step == 0)
					System.out.println((((double) numIt / maxNum) * 100) + "% completed...");
				if (numIt == maxNum)
					System.out.println("100% completed!");

			}
			// redescriptions.clear();
			System.out.println("Number of OK redescriptions: " + numOK);
			System.out.println("Number of not OK redescriptions: " + numNOK);

			bw.write("Found: " + numFullRed + " redescriptions with JS=1.0 and minsupport>" + appset.minSupport
					+ "\n\n");

			bw.write(
					"Warning: low memory triggered this rule dump (<15%), consider increasing java VM memory by using -Xmx flag!\n");
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	void writePlots(String output, ApplicationSettings appset, DataSetCreator dat, Mappings map) {

		try {

			BufferedWriter bw;
			File file = new File(output);

			if (!file.exists()) {
				file.createNewFile();
			}
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			bw = new BufferedWriter(fw);

			System.out.println("Final number of iterations: " + SupplementingRandomForest.getNumIter());
			System.out.println("writing plot data:....");
			System.out.println("redescription set size: " + redescriptions.size());

			for (int i = 0; i < redescriptions.size(); i++) {
				ArrayList<Double> sc = RedescriptionScoreFinal(redescriptions.get(i), dat, map);
				int ruleSize = redescriptions.get(i).computeAttributesDuplicateGen(dat);
				double finRSS = ruleSize / appset.ruleSizeNormalization;
				if (finRSS > 1.0)
					finRSS = 1.0;
				if (appset.useSplitTesting)
					bw.write(redescriptions.get(i).JS + "\t" + redescriptions.get(i).JSAll + "\t"
							+ redescriptions.get(i).JSTest + "\t" + redescriptions.get(i).elements.size() + "\t"
							+ redescriptions.get(i).pVal + "\t" + redescriptions.get(i).pValValidation + "\t"
							+ sc.get(0) + "\t" + sc.get(1) + "\t" + finRSS + "\n");
				else
					bw.write(redescriptions.get(i).JS + "\t" + redescriptions.get(i).elements.size() + "\t"
							+ redescriptions.get(i).pVal + "\t" + sc.get(0) + "\t" + sc.get(1) + "\t" + finRSS + "\n");

			}
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	void writePlotsBaseline(String output, ApplicationSettings appset, DataSetCreator dat) {

		try {

			BufferedWriter bw;
			File file = new File(output);

			if (!file.exists()) {
				file.createNewFile();
			}
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			bw = new BufferedWriter(fw);

			System.out.println("Final number of iterations: " + SupplementingRandomForest.getNumIter());
			System.out.println("writing plot data:....");
			System.out.println("redescription set size: " + redescriptions.size());

			for (int i = 0; i < redescriptions.size(); i++) {
				ArrayList<Double> sc = RedescriptionScoreFinalBaseline(redescriptions.get(i), dat);
				int ruleSize = redescriptions.get(i).computeAttributesDuplicateGenBaseline();
				// System.out.println("Rule size: "+ruleSize);
				double finRSS = ruleSize / (double) appset.ruleSizeNormalization;
				if (finRSS > 1.0)
					finRSS = 1.0;
				if (appset.useSplitTesting)
					bw.write(redescriptions.get(i).JS + "\t" + redescriptions.get(i).JSAll + "\t"
							+ redescriptions.get(i).JSTest + "\t" + redescriptions.get(i).elements.size() + "\t"
							+ redescriptions.get(i).pVal + "\t" + redescriptions.get(i).pValValidation + "\t"
							+ sc.get(0) + "\t" + sc.get(1) + "\t" + finRSS + "\n");
				else
					bw.write(redescriptions.get(i).JS + "\t" + redescriptions.get(i).elements.size() + "\t"
							+ redescriptions.get(i).pVal + "\t" + sc.get(0) + "\t" + sc.get(1) + "\t" + finRSS + "\n");
				System.out.println("Written: " + (i + 1) + "/" + redescriptions.size());
			}
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	double RedescriptionScore(double weights[], Redescription R, double minPval, DataSetCreator dat, Mappings map,
			int setSize) {
		double res = 1.0;
		Jacard js = new Jacard();
		double AEJ = 0.0, AAJ = 0.0;

		for (int i = 0; i < redescriptions.size(); i++) {
			AEJ += js.computeRedescriptionElementJacard(R, redescriptions.get(i));
			AAJ += js.computeAttributeJacard(R, redescriptions.get(i), dat);
		}
		AEJ /= redescriptions.size();
		AAJ /= redescriptions.size();

		// System.out.println("Element-attribute divergence computed...");

		double pv = Math.log10(R.pVal) / minPval + 1.0;

		if (R.pVal == 0.0)
			pv = 0.0;

		double elemConstraint = (double) redescriptions.size() / (double) setSize;

		res = weights[0] * (1.0 - R.JS)
				+ weights[1] * (elemConstraint * pv + (1 - elemConstraint) * (R.elements.size()) / dat.numExamples)
				+ weights[2] * AEJ + weights[3] * AAJ
				+ weights[4] * ((double) R.computeAttributesDuplicateGen(dat) / dat.schema.getNbAttributes());
		// (((double)(R.computeAttributesDuplicate(R.viewElementsLists.get(0),
		// dat)+R.computeAttributesDuplicate(R.viewElementsLists.get(1),
		// dat)))/dat.schema.getNbAttributes());//generalize
		// System.out.println("Result computed");
		return res;
	}

	ArrayList<Double> RedescriptionScoreFinal(Redescription R, DataSetCreator dat, Mappings map) {
		ArrayList<Double> res = new ArrayList<>();
		Jacard js = new Jacard();
		double AEJ = 0.0, AAJ = 0.0;

		for (int i = 0; i < redescriptions.size(); i++) {
			if (redescriptions.get(i).equals(R))
				continue;
			AEJ += js.computeRedescriptionElementJacard(R, redescriptions.get(i));
			AAJ += js.computeAttributeJacard(R, redescriptions.get(i), dat);
		}
		AEJ /= redescriptions.size();
		AAJ /= redescriptions.size();

		res.add(AEJ);
		res.add(AAJ);

		return res;
	}

	ArrayList<Double> RedescriptionScoreFinalBaseline(Redescription R, DataSetCreator dat) {
		ArrayList<Double> res = new ArrayList<>();
		Jacard js = new Jacard();
		double AEJ = 0.0, AAJ = 0.0;

		for (int i = 0; i < redescriptions.size(); i++) {
			if (redescriptions.get(i).equals(R))
				continue;
			AEJ += js.computeRedescriptionElementJacard(R, redescriptions.get(i));
			AAJ += js.computeAttributeJacardBaseline(R, redescriptions.get(i));
		}
		AEJ /= redescriptions.size();
		AAJ /= redescriptions.size();

		res.add(AEJ);
		res.add(AAJ);

		return res;
	}

	double evaluateRedescription(Redescription R, double weights[], DataSetCreator dat, Mappings map, double minPval) {

		double pval = Math.log10(R.pVal) / minPval + 1.0;

		if (R.pVal == 0)
			pval = 0.0;

		return (weights[0] * (1.0 - R.JS) + weights[1] * pval
				+ weights[2] * (1 - (double) R.elements.size() / dat.numExamples)
				+ weights[4] * ((double) R.computeAttributesDuplicateGen(dat) / dat.schema.getNbAttributes()));
		// (((double)(R.computeAttributesDuplicate(R.viewElementsLists.get(0),
		// dat)+R.computeAttributesDuplicate(R.viewElementsLists.get(1),
		// dat)))/dat.schema.getNbAttributes()));//generalize
	}

	double evaluateRedescriptionCooc(Redescription R, int numRed, double weights[], DataSetCreator dat, Mappings map,
			double minPval, CoocurenceMatrix cooc) {

		double pval = Math.log10(R.pVal) / minPval + 1.0;

		if (R.pVal == 0)
			pval = 0.0;

		int jointOccurence = 0;

		TIntIterator it = R.elements.iterator();

		while (it.hasNext()) {
			int elem = it.next();
			jointOccurence += cooc.matrix[elem][elem];
		}

		int maxOcc = 0;// Integer.MIN_VALUE;

		for (int i = 0; i < dat.numExamples; i++)
			// if(cooc.matrix[i][i]>maxOcc)
			maxOcc += cooc.matrix[i][i];

		System.out.println("Coocurence score: " + (weights[2] * (((double) jointOccurence) / ((double) maxOcc))));
		System.out.println("Attributes score: "
				+ (weights[4] * (((double) (R.computeAttributesDuplicate(R.viewElementsLists.get(0), dat)
						+ R.computeAttributesDuplicate(R.viewElementsLists.get(1), dat)))
						/ dat.schema.getNbAttributes())));
		// jointOccurence/R.elements.size()*maxOcc
		return (weights[0] * (1.0 - R.JS) + weights[1] * pval
				+ weights[2] * (((double) jointOccurence) / ((double) maxOcc))
				+ weights[4] * ((double) R.computeAttributesDuplicateGen(dat) / dat.schema.getNbAttributes()));
		// (((double)(R.computeAttributesDuplicate(R.viewElementsLists.get(0),
		// dat)+R.computeAttributesDuplicate(R.viewElementsLists.get(1),
		// dat)))/dat.schema.getNbAttributes()));
	}

	// rs assumed to be sorted with computed JS, pval, shortened.
	void createRedescriptionSet(RedescriptionSet rs, double weights[], ApplicationSettings appset, DataSetCreator dat,
			Mappings map) {

		if (rs.redescriptions.size() == 0)
			return;

		int returnRedSize = 0;
		double minPval = 17.0;

		if (appset.numRetRed < rs.redescriptions.size())
			returnRedSize = appset.numRetRed;
		else
			returnRedSize = rs.redescriptions.size();

		int firstInd = 0;
		double minScore = Double.POSITIVE_INFINITY;
		for (int i = 0; i < redescriptions.size(); i++) {
			double tmp = evaluateRedescription(redescriptions.get(i), weights, dat, map, minPval);

			if (tmp < minScore) {
				minScore = tmp;
				firstInd = i;
			}
		}

		redescriptions.add(rs.redescriptions.get(firstInd));

		System.out.println("First redescription found!");

		double res = 0.0, resMin = 1.0;
		int indMin = 0, numIt = 0, step = returnRedSize / 100;
		if (step == 0)
			step = 1;
		HashSet<Integer> addedIndex = new HashSet<>();
		addedIndex.add(firstInd);
		for (int k = 0; k < returnRedSize - 1; k++) {
			for (int i = 0; i < rs.redescriptions.size(); i++) {
				if (addedIndex.contains(i))
					continue;
				// System.out.println("computing redescription score: "+numIt);
				res = RedescriptionScore(weights, rs.redescriptions.get(i), minPval, dat, map, returnRedSize);
				// System.out.println("redescription score computed...");
				if (res < resMin) {
					resMin = res;
					indMin = i;
				}
			}
			redescriptions.add(rs.redescriptions.get(indMin));
			addedIndex.add(indMin);
			resMin = 1.0;
			numIt++;
			System.out.println("NumIt: " + numIt);
			if (numIt % step == 0)
				System.out.println((((double) numIt / returnRedSize) * 100) + "% completed...");
			if (numIt == returnRedSize)
				System.out.println("100% completed!");
		}
	}

	void createRedescriptionSetCooc(RedescriptionSet rs, double weights[], ApplicationSettings appset,
			DataSetCreator dat, Mappings map, CoocurenceMatrix cooc) {

		if (rs.redescriptions.size() == 0)
			return;

		int returnRedSize = 0;
		double minPval = 17.0;

		if (appset.numRetRed < rs.redescriptions.size())
			returnRedSize = appset.numRetRed;
		else
			returnRedSize = rs.redescriptions.size();

		int firstInd = 0;
		double minScore = Double.POSITIVE_INFINITY;
		for (int i = 0; i < rs.redescriptions.size(); i++) {
			System.out.println("Evaluate coocurence CooC");
			double tmp = evaluateRedescriptionCooc(rs.redescriptions.get(i), rs.redescriptions.size(), weights, dat,
					map, minPval, cooc);

			if (tmp < minScore) {
				minScore = tmp;
				firstInd = i;
			}
		}

		redescriptions.add(rs.redescriptions.get(firstInd));

		System.out.println("First redescription found!");

		double res = 0.0, resMin = 1.0;
		int indMin = 0, numIt = 0, step = returnRedSize / 100;
		if (step == 0)
			step = 1;
		HashSet<Integer> addedIndex = new HashSet<>();
		addedIndex.add(firstInd);
		for (int k = 0; k < returnRedSize - 1; k++) {
			for (int i = 0; i < rs.redescriptions.size(); i++) {
				if (addedIndex.contains(i))
					continue;
				// System.out.println("computing redescription score: "+numIt);
				res = RedescriptionScore(weights, rs.redescriptions.get(i), minPval, dat, map, returnRedSize);
				// System.out.println("redescription score computed...");
				if (res < resMin) {
					resMin = res;
					indMin = i;
				}
			}
			redescriptions.add(rs.redescriptions.get(indMin));
			addedIndex.add(indMin);
			resMin = 1.0;
			numIt++;
			System.out.println("NumIt: " + numIt);
			if (numIt % step == 0)
				System.out.println((((double) numIt / returnRedSize) * 100) + "% completed...");
			if (numIt == returnRedSize)
				System.out.println("100% completed!");
		}
	}

	double computeRedescriptionSetScore(double weights[], double coverage[], DataSetCreator dat, Mappings map) {

		if (redescriptions.size() == 0)
			return 1.0;

		double score = 0.0, minPval = 17.0, ruleSize = 0.0;
		Jacard js = new Jacard();
		double AEJ = 0.0, AAJ = 0.0, avrgJS = 0.0, avrPval = 0.0;
		int numIt = 0, step = (redescriptions.size() * (redescriptions.size() - 1)) / 100;
		if (step == 0)
			step = 1;
		HashSet<Integer> elements = new HashSet<>();
		HashSet<Integer> attributes = new HashSet<>();

		for (int i = 0; i < redescriptions.size() - 1; i++) {
			for (int j = i + 1; j < redescriptions.size(); j++) {
				AEJ += js.computeRedescriptionElementJacard(redescriptions.get(i), redescriptions.get(j));
				AAJ += js.computeAttributeJacard(redescriptions.get(i), redescriptions.get(j), dat);
				numIt++;
			}
			avrgJS += (1.0 - redescriptions.get(i).JS);
			double scorePV = Math.log10(redescriptions.get(i).pVal) / minPval + 1.0;
			if (redescriptions.get(i).pVal == 0.0)
				scorePV = 0.0;
			avrPval += scorePV;

			ArrayList<TIntHashSet> attrL = redescriptions.get(i)
					.computeAttributes(redescriptions.get(i).viewElementsLists, dat);// generalize
			// TIntHashSet
			// attrR=redescriptions.get(i).computeAttributes(redescriptions.get(i).viewElementsLists.get(1),
			// dat);

			int attrS = 0;

			for (int k = 0; k < attrL.size(); k++)
				attrS += attrL.get(k).size();

			// ruleSize+=((double)(attrL.size()+attrR.size()))/dat.schema.getNbAttributes();
			ruleSize += ((double) (attrS)) / dat.schema.getNbAttributes();

			if (numIt % step == 0)
				System.out.println((((double) numIt / (redescriptions.size() * (redescriptions.size() - 1))) * 100)
						+ "% completed...");
			if (numIt == (redescriptions.size() * (redescriptions.size() - 1)))
				System.out.println("100% completed!");

			TIntIterator it = redescriptions.get(i).elements.iterator();

			while (it.hasNext()) {
				int s = it.next();
				elements.add(s);
			}

			for (int k = 0; k < attrL.size(); k++) {
				it = attrL.get(k).iterator();

				while (it.hasNext()) {
					int attr = it.next();
					attributes.add(attr);
				}

			}
		}

		avrgJS += (1.0 - redescriptions.get(redescriptions.size() - 1).JS);
		double scorePV = Math.log10(redescriptions.get(redescriptions.size() - 1).pVal) / minPval + 1.0;
		if (redescriptions.get(redescriptions.size() - 1).pVal == 0.0)
			scorePV = 0.0;
		avrPval += scorePV;

		ArrayList<TIntHashSet> attrL = redescriptions.get(redescriptions.size() - 1)
				.computeAttributes(redescriptions.get(redescriptions.size() - 1).viewElementsLists, dat);// generalize
		// TIntHashSet
		// attrR=redescriptions.get(redescriptions.size()-1).computeAttributes(redescriptions.get(redescriptions.size()-1).rightRuleElements,
		// dat);

		int attrS = 0;

		for (int k = 0; k < attrL.size(); k++)
			attrS += attrL.get(k).size();

		ruleSize += ((double) (attrS)) / dat.schema.getNbAttributes();

		TIntIterator it = redescriptions.get(redescriptions.size() - 1).elements.iterator();

		while (it.hasNext()) {
			int s = it.next();
			elements.add(s);
		}

		/*
		 * for(int s:redescriptions.get(redescriptions.size()-1).elements)
		 * elements.add(s);
		 */
		for (int k = 0; k < attrL.size(); k++)
			it = attrL.get(k).iterator();
		while (it.hasNext()) {
			attributes.add(it.next());
		}

		coverage[0] = (double) elements.size() / dat.numExamples;
		coverage[1] = (double) attributes.size() / dat.schema.getNbAttributes();

		AEJ /= ((redescriptions.size() * (redescriptions.size() - 1)) / 2);
		AAJ /= ((redescriptions.size() * (redescriptions.size() - 1)) / 2);
		avrgJS /= redescriptions.size();
		avrPval /= redescriptions.size();
		ruleSize /= redescriptions.size();

		System.out.println("avrgJS: " + avrgJS);
		System.out.println("avrPval: " + avrPval);
		System.out.println("AEJ: " + AEJ);
		System.out.println("AAJ: " + AAJ);
		System.out.println("Rule size: " + ruleSize);
		score = weights[0] * avrgJS + weights[1] * avrPval + weights[2] * AEJ + weights[3] * AAJ
				+ weights[4] * ruleSize;

		aJS = avrgJS;
		aEJS = AEJ;
		aAJS = AAJ;
		aPvalSc = avrPval;
		aRS = ruleSize;
		return score;
	}

	void adaptSet(DataSetCreator dat, Mappings map, int minimize) {// minimize, reduce attributes and remove to long
																	// formulas

		// ArrayList<Redescription> toRemove=new ArrayList<>();

		for (int i = 0; i < redescriptions.size(); i++) {
			redescriptions.get(i).closeInterval(dat, map);
			// long startTime = System.currentTimeMillis();
			redescriptions.get(i).createRuleString(map);
			// System.out.println("Before");
			// System.out.println(redescriptions.get(i).ruleStrings.get(0));
			// System.out.println(redescriptions.get(i).ruleStrings.get(1));

			if (minimize == 1)
				redescriptions.get(i).minimizeOptimal(dat, map, 1);
			// redescriptions.get(i).createRuleString(map);
			// System.out.println("After");
			// System.out.println(redescriptions.get(i).ruleStrings.get(0));
			// System.out.println(redescriptions.get(i).ruleStrings.get(1));
			// redescriptions.get(i).createRuleString(map);
			// redescriptions.get(i).minimize(dat, map);
			// long estimatedTime = System.currentTimeMillis() - startTime;

			// System.out.println("Minimization time: "+estimatedTime);

			/*
			 * if(redescriptions.get(i).leftRule.keySet().size()>6 ||
			 * redescriptions.get(i).rightRule.keySet().size()>6)
			 * toRemove.add(redescriptions.get(i));
			 */
		}

		/*
		 * for(int i=0;i<toRemove.size();i++) redescriptions.remove(toRemove.get(i));
		 * 
		 * toRemove.clear();
		 */
	}

	// keep number of rules <=1600
	int findCutoff(int numExamples, double startPercentage, double endPercentage, int[] minCovElem, int[] maxCovElem,
			int[] oldIndex, int minSupp, int maxSupp, int numRules) {
		int nelMin = (int) (numExamples * startPercentage);
		int nelMax = (int) (numExamples * endPercentage);

		if (nelMax > maxSupp)
			nelMax = maxSupp;

		int ruleCount = 0;
		int minCovElemT = minSupp;
		int maxCovElemT = maxSupp;
		int step = (nelMax - nelMin) / 20;

		if (nelMin < minSupp)
			nelMin = minSupp;

		if (nelMin > minSupp)
			minCovElemT = nelMin;

		if (nelMax < maxSupp)
			maxCovElemT = nelMax;

		if (nelMax < minSupp)
			return -1;

		while (true) {
			for (int i = oldIndex[0]; i < redescriptions.size(); i++) {
				if (redescriptions.get(i).elements.size() >= startPercentage * numExamples
						&& redescriptions.get(i).elements.size() <= endPercentage * numExamples
						&& redescriptions.get(i).elements.size() >= minCovElemT
						&& redescriptions.get(i).elements.size() <= maxCovElemT)
					ruleCount++;

			}

			if (ruleCount == 0)
				return -1;

			if (ruleCount > numRules) {
				ruleCount = 0;
				minCovElemT += step;
			} else
				break;
		}
		minCovElem[0] = minCovElemT;
		maxCovElem[0] = maxCovElemT;

		return 0;
	}

	double evaluateRedescriptionCoocGen(Redescription R, int jsType, int numRed, double weights[], DataSetCreator dat,
			Mappings map, ApplicationSettings appset, double minPval, CoocurenceMatrix cooc) {

		double pval = Math.log10(R.pVal) / minPval + 1.0;

		if (R.pVal == 0)
			pval = 0.0;

		int jointOccurence = 0;

		TIntIterator it = R.elements.iterator();

		while (it.hasNext()) {
			int elem = it.next();
			jointOccurence += cooc.matrix[elem][elem];
		}

		int maxOcc = 0;// Integer.MIN_VALUE;
		double jointOccurenceAt = 0.0;

		for (int i = 0; i < dat.numExamples; i++)
			// if(cooc.matrix[i][i]>maxOcc)
			maxOcc += cooc.matrix[i][i];

		ArrayList<TIntHashSet> attributes = R.computeAttributes(R.viewElementsLists, dat);

		for (int j = 0; j < attributes.size(); j++) {
			TIntIterator itt = attributes.get(j).iterator();

			while (itt.hasNext()) {
				int at = itt.next();
				jointOccurenceAt += cooc.attributeMatrix[at][at];
			}
		}

		double totalAt = 0.0;
		for (int i = 0; i < map.attId.keySet().size(); i++) {
			totalAt += cooc.attributeMatrix[i][i];
		}

		jointOccurenceAt /= totalAt;

		System.out.println("Coocurence score: " + (weights[2] * (((double) jointOccurence) / ((double) maxOcc))));
		System.out.println("Attributes score: "
				+ (weights[4] * (((double) (R.computeAttributesDuplicate(R.viewElementsLists.get(0), dat)
						+ R.computeAttributesDuplicate(R.viewElementsLists.get(1), dat)))
						/ appset.ruleSizeNormalization))) /* dat.schema.getNbAttributes()))) */;
		// jointOccurence/R.elements.size()*maxOcc

		if (appset.RedStabilityWeight > 0.0) {
			R.computeAllJSMeasures(dat, map);
		}

		double resTmp = 0.0;

		for (String s : appset.preferenceHeader.keySet()) {
			if (s.equals("JSImp") && jsType == 0)
				resTmp += weights[appset.preferenceHeader.get(s)] * (1.0 - R.JS);
			else if (s.equals("JSImp") && jsType == 1) {
				if (R.JSPes < appset.minJS)
					return Double.POSITIVE_INFINITY;
				resTmp += weights[appset.preferenceHeader.get(s)] * (1.0 - R.JSPes);
			} else if (s.equals("JSImp") && jsType == 2)
				resTmp += weights[appset.preferenceHeader.get(s)] * (1.0 - R.JSOpt);
			else if (s.equals("JSImp") && jsType == 3)
				resTmp += weights[appset.preferenceHeader.get(s)] * (1.0 - R.JSGR);
			else if (s.equals("PValImp"))
				resTmp += weights[appset.preferenceHeader.get(s)] * pval;
			else if (s.equals("AttDivImp"))
				resTmp += weights[appset.preferenceHeader.get(s)] * jointOccurenceAt;
			else if (s.equals("ElemDivImp"))
				resTmp += weights[appset.preferenceHeader.get(s)] * (((double) jointOccurence) / ((double) maxOcc));
			else if (s.equals("RuleSizeImp")) {
				double rs = ((double) R.computeAttributesDuplicateGen(dat) / appset.ruleSizeNormalization);
				if (rs > 1.0)
					rs = 1.0;
				resTmp += weights[appset.preferenceHeader.get(s)] * rs;

			} else if (s.equals("RedStability"))
				resTmp += weights[appset.preferenceHeader.get(s)] * (R.JSGR - R.JSPes);
		}

		return resTmp;

		// return
		// (weights[0]*(1.0-R.JS)+weights[1]*pval+weights[2]*(((double)jointOccurence)/((double)maxOcc))+weights[4]*((double)R.computeAttributesDuplicateGen(dat)/dat.schema.getNbAttributes()))+weights[5]*(R.JSGR-R.JSPes);
		// (((double)(R.computeAttributesDuplicate(R.viewElementsLists.get(0),
		// dat)+R.computeAttributesDuplicate(R.viewElementsLists.get(1),
		// dat)))/dat.schema.getNbAttributes()));
	}

	ArrayList<RedescriptionSet> createRedescriptionSetsCoocGen(RedescriptionSet rs, ArrayList<double[]> weights,
			int jsType, ApplicationSettings appset, DataSetCreator dat, Mappings map, CoocurenceMatrix cooc) {

		ArrayList<RedescriptionSet> sets = new ArrayList<>();

		for (int it = 0; it < weights.size(); it++) {
			sets.add(new RedescriptionSet());
		}

		for (int it = 0; it < weights.size(); it++) {
			if (rs.redescriptions.size() == 0)
				return sets;

			int returnRedSize = 0;
			double minPval = 17.0;

			if (appset.numRetRed < rs.redescriptions.size())
				returnRedSize = appset.numRetRed;
			else
				returnRedSize = rs.redescriptions.size();

			int firstInd = -1;
			double minScore = Double.POSITIVE_INFINITY;
			for (int i = 0; i < rs.redescriptions.size(); i++) {
				System.out.println("Evaluate coocurence CooC");
				double tmp = evaluateRedescriptionCoocGen(rs.redescriptions.get(i), jsType, rs.redescriptions.size(),
						weights.get(it), dat, map, appset, minPval, cooc);

				if (jsType == 1 && rs.redescriptions.get(i).JSPes < appset.minJS)
					continue;

				if (rs.redescriptions.get(i).elements.size() < appset.minSupport
						|| rs.redescriptions.get(i).JS < appset.minJS)
					continue;

				if (tmp < minScore) {
					minScore = tmp;
					firstInd = i;
				}
			}

			if (firstInd == -1)
				continue;
			sets.get(it).redescriptions.add(rs.redescriptions.get(firstInd));
			// redescriptions.add(rs.redescriptions.get(firstInd));

			System.out.println("First redescription found!");

			double res = 0.0, resMin = 1.0;
			int indMin = 0, numIt = 0, step = returnRedSize / 100;
			if (step == 0)
				step = 1;
			HashSet<Integer> addedIndex = new HashSet<>();
			addedIndex.add(firstInd);
			for (int k = 0; k < returnRedSize - 1; k++) {
				for (int i = 0; i < rs.redescriptions.size(); i++) {
					if (addedIndex.contains(i))
						continue;

					if (jsType == 1 && rs.redescriptions.get(i).JSPes < appset.minJS)
						continue;

					if (rs.redescriptions.get(i).elements.size() < appset.minSupport
							|| rs.redescriptions.get(i).JS < appset.minJS)
						continue;

					res = sets.get(it).RedescriptionScoreGen(weights.get(it), rs.redescriptions.get(i), jsType, minPval,
							dat, map, appset, returnRedSize);

					if (res > 1.0)
						continue;

					if (res < resMin) {
						resMin = res;
						indMin = i;
					}
				}

				if (!addedIndex.contains(indMin))
					sets.get(it).redescriptions.add(rs.redescriptions.get(indMin));
				addedIndex.add(indMin);
				resMin = 1.0;
				numIt++;
				System.out.println("NumIt: " + numIt);
				if (numIt % step == 0)
					System.out.println((((double) numIt / returnRedSize) * 100) + "% completed...");
				if (numIt == returnRedSize)
					System.out.println("100% completed!");
			}
		}
		return sets;
	}

	ArrayList<RedescriptionSet> createRedescriptionSetsRandGen(RedescriptionSet rs, ArrayList<double[]> weights,
			int jsType, ApplicationSettings appset, DataSetCreator dat, Mappings map, CoocurenceMatrix cooc) {

		ArrayList<RedescriptionSet> sets = new ArrayList<>();
		ArrayList<Integer> maxIndexes = new ArrayList<>();
		double max = 0.0, minsupp = dat.numExamples;

		for (int it = 0; it < weights.size(); it++) {
			sets.add(new RedescriptionSet());
		}

		if (rs.redescriptions.size() == 0)
			return sets;

		for (int i = 0; i < rs.redescriptions.size(); i++) {
			if (jsType == 0) {
				if (rs.redescriptions.get(i).JS > max)
					max = rs.redescriptions.get(i).JS;
			} else if (jsType == 1) {
				if (rs.redescriptions.get(i).JSPes > max)
					max = rs.redescriptions.get(i).JSPes;
			} else if (jsType == 2) {
				if (rs.redescriptions.get(i).JSOpt > max)
					max = rs.redescriptions.get(i).JSOpt;
			} else if (jsType == 3) {
				if (rs.redescriptions.get(i).JSGR > max)
					max = rs.redescriptions.get(i).JSGR;
			}
		}

		for (int i = 0; i < rs.redescriptions.size(); i++) {
			if (jsType == 0) {
				if (rs.redescriptions.get(i).JS == max)
					if (rs.redescriptions.get(i).elements.size() < minsupp)
						minsupp = rs.redescriptions.get(i).elements.size();
			} else if (jsType == 1) {
				if (rs.redescriptions.get(i).JSPes == max)
					if (rs.redescriptions.get(i).elements.size() < minsupp)
						minsupp = rs.redescriptions.get(i).elements.size();
			} else if (jsType == 2) {
				if (rs.redescriptions.get(i).JSOpt == max)
					if (rs.redescriptions.get(i).elements.size() < minsupp)
						minsupp = rs.redescriptions.get(i).elements.size();
			} else if (jsType == 3) {
				if (rs.redescriptions.get(i).JSGR == max)
					if (rs.redescriptions.get(i).elements.size() < minsupp)
						minsupp = rs.redescriptions.get(i).elements.size();
			}
		}

		for (int i = 0; i < rs.redescriptions.size(); i++) {
			if (jsType == 0) {
				if ((rs.redescriptions.get(i).JS == max) && (rs.redescriptions.get(i).elements.size() == minsupp))
					maxIndexes.add(i);
			} else if (jsType == 1) {
				if ((rs.redescriptions.get(i).JS == max) && (rs.redescriptions.get(i).elements.size() == minsupp))
					maxIndexes.add(i);
			} else if (jsType == 2) {
				if ((rs.redescriptions.get(i).JS == max) && (rs.redescriptions.get(i).elements.size() == minsupp))
					maxIndexes.add(i);
			} else if (jsType == 3) {
				if ((rs.redescriptions.get(i).JS == max) && (rs.redescriptions.get(i).elements.size() == minsupp))
					maxIndexes.add(i);
			}
		}

		int rnd = new Random().nextInt(maxIndexes.size());

		for (int it = 0; it < weights.size(); it++) {
			// sets.add(new RedescriptionSet());

			/*
			 * if(rs.redescriptions.size()==0) return null;
			 */

			int returnRedSize = 0;
			double minPval = 17.0;

			if (appset.numRetRed < rs.redescriptions.size())
				returnRedSize = appset.numRetRed;
			else
				returnRedSize = rs.redescriptions.size();

			int firstInd = rnd;

			double minScore = Double.POSITIVE_INFINITY;

			sets.get(it).redescriptions.add(rs.redescriptions.get(firstInd));
			// redescriptions.add(rs.redescriptions.get(firstInd));

			System.out.println("First redescription found!");

			double res = 0.0, resMin = 1.0;
			int indMin = 0, numIt = 0, step = returnRedSize / 100;
			if (step == 0)
				step = 1;
			HashSet<Integer> addedIndex = new HashSet<>();
			addedIndex.add(firstInd);
			for (int k = 0; k < returnRedSize - 1; k++) {
				for (int i = 0; i < rs.redescriptions.size(); i++) {
					if (addedIndex.contains(i))
						continue;

					if (jsType == 1 && rs.redescriptions.get(i).JSPes < appset.minJS)
						continue;

					if (rs.redescriptions.get(i).elements.size() < appset.minSupport
							|| rs.redescriptions.get(i).JS < appset.minJS)
						continue;

					res = sets.get(it).RedescriptionScoreGen(weights.get(it), rs.redescriptions.get(i), jsType, minPval,
							dat, map, appset, returnRedSize);

					if (res > 1.0)
						continue;

					if (res < resMin) {
						resMin = res;
						indMin = i;
					}
				}

				if (!addedIndex.contains(indMin))
					sets.get(it).redescriptions.add(rs.redescriptions.get(indMin));
				addedIndex.add(indMin);
				resMin = 1.0;
				numIt++;
				System.out.println("NumIt: " + numIt);
				if (numIt % step == 0)
					System.out.println((((double) numIt / returnRedSize) * 100) + "% completed...");
				if (numIt == returnRedSize)
					System.out.println("100% completed!");
			}
		}
		return sets;
	}

	void computeAllMeasureFS(DataSetCreator dat, ApplicationSettings appset, Mappings map) {
		if (appset.preferenceHeader.containsKey("RedStability"))
			appset.RedStabilityWeight = appset.preferences.get(0)[appset.preferenceHeader.get("RedStability")];

		if (appset.RedStabilityWeight <= 0)
			return;

		for (int i = 0; i < redescriptions.size(); i++) {
			redescriptions.get(i).computeAllJSMeasures(dat, map);
		}
	}

	double RedescriptionScoreGen(double weights[], Redescription R, int jsType, double minPval, DataSetCreator dat,
			Mappings map, ApplicationSettings appset, int setSize) {
		double res = 1.0;
		Jacard js = new Jacard();
		double AEJ = 0.0, AAJ = 0.0;

		if (appset.preferenceHeader.containsKey("RedStability"))
			appset.RedStabilityWeight = weights[appset.preferenceHeader.get("RedStability")];

		double tmpEJS = 0.0, tmpAJS = 0.0;
		for (int i = 0; i < redescriptions.size(); i++) {
			tmpEJS = js.computeRedescriptionElementJacard(R, redescriptions.get(i));
			tmpAJS = js.computeAttributeJacard(R, redescriptions.get(i), dat);
			if (tmpEJS == 1.0 && tmpAJS == 1.0)
				return Double.POSITIVE_INFINITY;

			if (tmpEJS > AEJ)
				AEJ = tmpEJS;
			if (tmpAJS > AAJ)
				AAJ = tmpAJS;
		}

		double pv = Math.log10(R.pVal) / minPval + 1.0;

		if (R.pVal == 0.0)
			pv = 0.0;

		double elemConstraint = (double) redescriptions.size() / (double) setSize;

		if (appset.RedStabilityWeight > 0.0) {
			R.computeAllJSMeasures(dat, map);
		}

		double resTmp = 0.0;

		for (String s : appset.preferenceHeader.keySet()) {
			if (s.equals("JSImp") && jsType == 0)
				resTmp += weights[appset.preferenceHeader.get(s)] * (1.0 - R.JS);
			else if (s.equals("JSImp") && jsType == 1) {
				if (R.JSPes < appset.minJS)
					return 1.0;
				resTmp += weights[appset.preferenceHeader.get(s)] * (1.0 - R.JSPes);
			}
			if (s.equals("JSImp") && jsType == 2)
				resTmp += weights[appset.preferenceHeader.get(s)] * (1.0 - R.JSOpt);
			if (s.equals("JSImp") && jsType == 3)
				resTmp += weights[appset.preferenceHeader.get(s)] * (1.0 - R.JSGR);
			else if (s.equals("PValImp"))
				resTmp += weights[appset.preferenceHeader.get(s)]
						* (elemConstraint * pv + (1 - elemConstraint) * (R.elements.size()) / dat.numExamples);
			else if (s.equals("AttDivImp"))
				resTmp += weights[appset.preferenceHeader.get(s)] * AAJ;
			else if (s.equals("ElemDivImp"))
				resTmp += weights[appset.preferenceHeader.get(s)] * AEJ;
			else if (s.equals("RuleSizeImp")) {
				double rs = ((double) R.computeAttributesDuplicateGen(dat) / appset.ruleSizeNormalization);
				if (rs > 1.0)
					rs = 1.0;
				resTmp += weights[appset.preferenceHeader.get(s)] * rs;
			} else if (s.equals("RedStability"))
				resTmp += weights[appset.preferenceHeader.get(s)] * (R.JSGR - R.JSPes);
		}

		res = resTmp;

		return res;
	}

}
