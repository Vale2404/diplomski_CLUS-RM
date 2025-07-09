/*************************************************************************
 * Clus - Software for Predictive Clustering *
 * Copyright (C) 2007 *
 * Katholieke Universiteit Leuven, Leuven, Belgium *
 * Jozef Stefan Institute, Ljubljana, Slovenia *
 * *
 * This program is free software: you can redistribute it and/or modify *
 * it under the terms of the GNU General Public License as published by *
 * the Free Software Foundation, either version 3 of the License, or *
 * (at your option) any later version. *
 * *
 * This program is distributed in the hope that it will be useful, *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the *
 * GNU General Public License for more details. *
 * *
 * You should have received a copy of the GNU General Public License *
 * along with this program. If not, see <http://www.gnu.org/licenses/>. *
 * *
 * Contact information: <http://www.cs.kuleuven.be/~dtai/clus/>. *
 *************************************************************************/

package si.ijs.kt.clus.algo.tdidt;

import gnu.trove.iterator.TIntIterator;
import org.apache.commons.math3.distribution.BinomialDistribution;
import redescriptionmining.*;
import si.ijs.kt.clus.algo.ClusInductionAlgorithm;
import si.ijs.kt.clus.algo.split.CurrentBestTestAndHeuristic;
import si.ijs.kt.clus.algo.split.FindBestTest;
import si.ijs.kt.clus.algo.split.NominalSplit;
import si.ijs.kt.clus.data.ClusSchema;
import si.ijs.kt.clus.data.rows.DataTuple;
import si.ijs.kt.clus.data.rows.RowData;
import si.ijs.kt.clus.data.type.ClusAttrType;
import si.ijs.kt.clus.data.type.primitive.NominalAttrType;
import si.ijs.kt.clus.data.type.primitive.NumericAttrType;
import si.ijs.kt.clus.ext.ensemble.ClusEnsembleInduce;
import si.ijs.kt.clus.ext.ensemble.ClusEnsembleInduce.ParallelTrap;
import si.ijs.kt.clus.ext.ensemble.ros.ClusROSHelpers;
import si.ijs.kt.clus.ext.ensemble.ros.ClusROSModelInfo;
import si.ijs.kt.clus.main.ClusRun;
import si.ijs.kt.clus.main.ClusStatManager;
import si.ijs.kt.clus.main.settings.Settings;
import si.ijs.kt.clus.main.settings.section.SettingsEnsemble.EnsembleMethod;
import si.ijs.kt.clus.main.settings.section.SettingsEnsemble.EnsembleROSAlgorithmType;
import si.ijs.kt.clus.main.settings.section.SettingsGeneric;
import si.ijs.kt.clus.main.settings.section.SettingsTree.MissingClusteringAttributeHandlingType;
import si.ijs.kt.clus.main.settings.section.SettingsTree.MissingTargetAttributeHandlingType;
import si.ijs.kt.clus.main.settings.section.SettingsTree.TreeOptimizeValues;
import si.ijs.kt.clus.model.ClusModel;
import si.ijs.kt.clus.model.test.NodeTest;
import si.ijs.kt.clus.model.test.NumericTest;
import si.ijs.kt.clus.model.test.SubsetTest;
import si.ijs.kt.clus.statistic.*;
import si.ijs.kt.clus.util.ClusLogger;
import si.ijs.kt.clus.util.ClusRandom;
import si.ijs.kt.clus.util.ClusRandomNonstatic;
import si.ijs.kt.clus.util.exception.ClusException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


public class DepthFirstInduce extends ClusInductionAlgorithm {

    protected FindBestTest m_FindBestTest;
    protected FindBestTest m_Find_MinMax; // daniela
    protected ClusNode m_Root;

    private List<List<ClusNode>> levelNodesList = new ArrayList<>();
	private RowData fullDataSet;
	private boolean hasMissing; 
    
    protected static int SHOW_INDUCE_PROGRESS = 2;
    private static AtomicInteger idCounter = new AtomicInteger(1);
    private ClusAttrType[] m_RandomSubspaces; // TODO: this field should be removed in the future
    private double jaccard;
    private int minSupport;
    private int maxSupport;

    private ClusAttrType[] getRandomSubspaces() {
        return m_RandomSubspaces;
    }
    private void setRandomSubspaces(ClusAttrType[] attrs) {
        m_RandomSubspaces = attrs;
    }
    private ClusAttrType[] selectRandomSubspaces(ClusAttrType[] attrs, int select, int randomizerVersion, ClusRandomNonstatic rand) {
        int origsize = attrs.length;
        int[] samples = new int[origsize];
        int rnd;
        boolean randomize = true;
        int i = 0;
        if (rand == null) {
            while (randomize) {
                rnd = ClusRandom.nextInt(randomizerVersion, origsize);
                if (samples[rnd] == 0) {
                    samples[rnd]++;
                    i++;
                }
                randomize = i != select;
            }
        }
        else {
            while (randomize) {
                rnd = rand.nextInt(randomizerVersion, origsize);
                if (samples[rnd] == 0) {
                    samples[rnd]++;
                    i++;
                }
                randomize = i != select;
            }
        }

        ClusAttrType[] result = new ClusAttrType[select];
        int res = 0;
        for (int k = 0; k < origsize; k++) {
            if (samples[k] != 0) {
                result[res] = attrs[k];
                res++;
            }
        }
        return result;
    }

    public DepthFirstInduce(ClusSchema schema, Settings sett) throws ClusException, IOException {
        super(schema, sett);
        m_FindBestTest = new FindBestTest(getStatManager());
        m_Find_MinMax = new FindBestTest(getStatManager()); // daniela
    }


    public DepthFirstInduce(ClusInductionAlgorithm other) {
        super(other);
        m_FindBestTest = new FindBestTest(getStatManager());
        m_Find_MinMax = new FindBestTest(getStatManager()); // daniela
    }


    /**
     * Used in parallelisation.
     * 
     * @param other
     * @param mgr
     * @param parallelism
     *        Used only to distinguish between this constructor and
     *        {@code DepthFirstInduce(ClusInductionAlgorithm, NominalSplit)},
     *        when the second argument is {@code null}.
     */
    public DepthFirstInduce(ClusInductionAlgorithm other, ClusStatManager mgr, boolean parallelism) {
        super(other, mgr);
        m_FindBestTest = new FindBestTest(getStatManager());
        m_Find_MinMax = new FindBestTest(getStatManager()); // daniela

    }


    public DepthFirstInduce(ClusInductionAlgorithm other, NominalSplit split) {
        super(other);
        m_FindBestTest = new FindBestTest(getStatManager(), split);
        m_Find_MinMax = new FindBestTest(getStatManager()); // daniela
    }


    @Override
    public void initialize() throws ClusException, IOException {
        super.initialize();
    }


    public FindBestTest getFindBestTest() {
        return m_FindBestTest;
    }


    public CurrentBestTestAndHeuristic getBestTest() {
        return m_FindBestTest.getBestTest();
    }


    public boolean initSelectorAndStopCrit(ClusNode node, RowData data) {
        int max = getSettings().getConstraints().getTreeMaxDepth();
        if (max != -1 && node.getLevel() >= max) { return true; }
        m_Find_MinMax.initSelectorAndStopCrit(node.getClusteringStat(), data); // daniela
        if (node.getTargetStat().getTargetSumWeights() < 2 * getSettings().getModel().getMinimalWeight()) { // FIXME:
                                                                                                            // not sure
                                                                                                            // how to
                                                                                                            // deal with
                                                                                                            // partially
                                                                                                            // labeled
                                                                                                            // data,
                                                                                                            // should we
                                                                                                            // allow
                                                                                                            // split if,
                                                                                                            // for
                                                                                                            // example,
                                                                                                            // only one
                                                                                                            // target
                                                                                                            // has
                                                                                                            // labels?
            return true;
        }
        return m_FindBestTest.initSelectorAndStopCrit(node.getClusteringStat(), data);
    }


    public ClusAttrType[] getDescriptiveAttributes(ClusRandomNonstatic rnd) {
        ClusSchema schema = getSchema();
        Settings sett = getSettings();
        if (!sett.getEnsemble().isEnsembleMode()) {
            return schema.getDescriptiveAttributes();
        }
        else {
            ClusAttrType[] selected;
            // boolean shouldSet = false;
            switch (sett.getEnsemble().getEnsembleMethod()) {
                case RForest:
                case ExtraTrees:// same as for Random Forests
                    ClusAttrType[] attrsAll = schema.getDescriptiveAttributes();
                    selected = selectRandomSubspaces(attrsAll, schema.getSettings().getEnsemble().getNbRandomAttrSelected(), ClusRandomNonstatic.RANDOM_SELECTION, rnd);
                    // shouldSet = true; //ClusEnsembleInduce.setRandomSubspaces(attrsAll,
                    // schema.getSettings().getNbRandomAttrSelected(), rnd);
                    break;
                // ClusEnsembleInduce.setRandomSubspacesProportionalToSparsity(attrsAll,
                // schema.getSettings().getNbRandomAttrSelected());
                case RSubspaces:
                    selected = getRandomSubspaces();
                    ClusEnsembleInduce.giveParallelisationWarning(ParallelTrap.DepthFirst_getDescriptiveAttributes);
                    break;
                case BagSubspaces:
                    ClusEnsembleInduce.giveParallelisationWarning(ParallelTrap.DepthFirst_getDescriptiveAttributes);
                    selected = getRandomSubspaces();
                    break;
                case RFeatSelection: // SettingsEnsemble.ENSEMBLE_METHOD_RFOREST_NO_BOOTSTRAP:
                    ClusEnsembleInduce.giveParallelisationWarning(ParallelTrap.DepthFirst_getDescriptiveAttributes);
                    ClusAttrType[] attrsAll1 = schema.getDescriptiveAttributes();
                    selected = selectRandomSubspaces(attrsAll1, schema.getSettings().getEnsemble().getNbRandomAttrSelected(), ClusRandomNonstatic.RANDOM_SELECTION, rnd);
                    // shouldSet = true;// ClusEnsembleInduce.setRandomSubspaces(attrsAll1,
                    // schema.getSettings().getNbRandomAttrSelected(), rnd);
                    break; // setRandomSubspaces(ClusAttrType[] attrs, int select,
                // ClusRandomNonstatic rnd)
                case Bagging:
                    // shouldSet = true; // ClusEnsembleInduce.setRandomSubspaces(attrs_all,
                    // schema.getSettings().getNbRandomAttrSelected(), rnd);
                    // ClusEnsembleInduce.setRandomSubspacesProportionalToSparsity(attrsAll,
                    // schema.getSettings().getNbRandomAttrSelected());
                default:
                    selected = schema.getDescriptiveAttributes();
                    break;
            }
            // Current references of the method do not need this
            // if (shouldSet){
            // ClusEnsembleInduce.setRandomSubspaces(selected);
            // }
            return selected;
        }
    }


    public void filterAlternativeSplits(ClusNode node, RowData data, RowData[] subsets) {
        // boolean removed = false;
        CurrentBestTestAndHeuristic best = m_FindBestTest.getBestTest();
        int arity = node.getTest().updateArity();
        ArrayList<NodeTest> alternatives = best.getAlternativeBest(); // alternatives: all tests that result in same
                                                                      // heuristic value, in the end this will contain
                                                                      // all true alternatives
        ArrayList<NodeTest> oppositeAlternatives = new ArrayList<NodeTest>(); // this will contain all tests that are
                                                                              // alternatives, but where the left and
                                                                              // right branches are switched
        String alternativeString = new String(); // this will contain the string of alternative tests (regular and
                                                 // opposite), sorted according to position
        for (int k = 0; k < alternatives.size(); k++) {
            NodeTest nt = alternatives.get(k);
            int altarity = nt.updateArity();
            // remove alternatives that have different arity than besttest
            if (altarity != arity) {
                alternatives.remove(k);
                k--;
                ClusLogger.info("Alternative split with different arity: " + nt.getString());
                // removed = true;
            }
            else {
                // we assume the arity is 2 here
                // exampleindices of one branch are stored
                int nbsubset0 = subsets[0].getNbRows();
                int indices[] = new int[nbsubset0];
                for (int m = 0; m < nbsubset0; m++) {
                    indices[m] = subsets[0].getTuple(m).getIndex();
                }
                // check for all (=2) alternative branches one of them contains the same indices
                boolean same = false;
                for (int l = 0; l < altarity; l++) {
                    RowData altrd = data.applyWeighted(nt, l);
                    if (altrd.getNbRows() == nbsubset0) {
                        int nbsame = 0;
                        for (int m = 0; m < nbsubset0; m++) {
                            if (altrd.getTuple(m).getIndex() == indices[m]) {
                                nbsame++;
                            }
                        }
                        if (nbsame == nbsubset0) {
                            // same subsets found
                            same = true;
                            if (l != 0) {
                                // the same subsets, but the opposite split, hence we add the test to the
                                // opposite
                                // alternatives
                                alternativeString = alternativeString + " and not(" + alternatives.get(k).toString() + ")";
                                alternatives.remove(k);
                                k--;
                                oppositeAlternatives.add(nt);
                            }
                            else {
                                // the same subsets, and the same split
                                alternativeString = alternativeString + " and " + alternatives.get(k).toString();
                            }
                        }
                    }
                }
                if (!same) {
                    alternatives.remove(k);
                    k--;
                    ClusLogger.info("Alternative split with different ex in subsets: " + nt.getString());
                    // removed = true;
                }

            }
        }
        node.setAlternatives(alternatives);
        node.setOppositeAlternatives(oppositeAlternatives);
        node.setAlternativesString(alternativeString);
        // if (removed) ClusLogger.info("Alternative splits were possible");
    }


    public void makeLeaf(ClusNode node) {
        if (getSettings().getGeneral().getVerbose() >= SHOW_INDUCE_PROGRESS) {
            ClusLogger.info("Creating a leaf ...");
        }
        node.makeLeaf();
        if (getSettings().getTree().hasTreeOptimize(TreeOptimizeValues.NoClusteringStats)) {
            node.setClusteringStat(null);
        }
    }

    // iste redeskripcije moraju biti
    private boolean isJaccardValid(double sumValue, double sumWeight, int targetSupport) {
    	ApplicationSettings appset = ApplicationSettings.getInstance();
    	if(sumValue < minSupport || sumValue > maxSupport) {
    		return false;
    	}

    	// od sumValue (i imaju ciljnu labelu 1) i sumWeight oduzeti m_MissIndex (vidjeti kako to funkcionira)
    	// elements contains missingValue koji imaju target = 1 - to oduzimamo od sumValue
    	// od sumWeight oduzimamo sve missing
    	return sumValue / (sumWeight + targetSupport - sumValue) >= jaccard;
    	
    }
    
    private boolean isPValueValid(double sumValue, double sumWeight, int targetSupport) {
    	ApplicationSettings appset = ApplicationSettings.getInstance();
    	
    	return calculatePValue(sumValue, sumWeight, targetSupport) <= appset.maxPval;
    }
    
    private double calculatePValue(double sumValue, double sumWeight, int targetSupport) {
    	int numExamples = SupplementingRandomForest.getDatJ().numExamples;
    	
		double prob = ((double) (sumWeight * targetSupport)) / (numExamples * numExamples);
		BinomialDistribution dist = new BinomialDistribution(numExamples, prob);
		return 1.0 - dist.cumulativeProbability((int) sumValue);
    }
    
    
    private void calculateActualMissingForTarget(RowData data, ClusNode node, boolean isLeaf) {
//    	if(SupplementingRandomForest.getNumIter() == 0) return;
//		if(node.getParent() != null && m_Root.getMissingSet().isEmpty()) {
//			return;
//		}
    	if(!hasMissing) {
    		return;
    	}
    	
		ClusAttrType splitAttribute;
		
		if(isLeaf) {
			splitAttribute = ((ClusNode) node.getParent()).getTest().getType();
		} else {
    		splitAttribute = node.getParent() == null ? node.getTest().getType() : ((ClusNode) node.getParent()).getTest().getType();
		}
		
		RegressionStat missingStat = new RegressionStat(node.m_TargetStat.getSettings(), ((RegressionStatBase)(node.m_TargetStat)).getAttributes());
		if(splitAttribute.isNumeric()) {
    		calculateActualMissingNumeric(data, (NumericAttrType) splitAttribute, node, missingStat);
		} else if(splitAttribute.isNominal()) {
			calculateActualMissingNominal(data, (NominalAttrType) splitAttribute, node, missingStat);
		}
    }
    
    private void calculateActualMissingNominal(RowData data, NominalAttrType at, ClusNode node, RegressionStat missingStat) {
    	for(DataTuple tuple : data.getData()) {
    		if(tuple.getDatasetIndex() >= SupplementingRandomForest.getDatJ().numExamples) continue; 
			if(at.isMissing(tuple)) {
    			missingStat.updateWeightedSelected(tuple);
    			node.getMissingSet().add(tuple.getDatasetIndex());
    		} else {
    			ClusNode tempNode = node;
				
				while(tempNode.getParent() != null) {
					tempNode = (ClusNode) tempNode.getParent();
					if(tempNode.getMissingSet().contains(tuple.getDatasetIndex())) {
						node.getMissingSet().add(tuple.getDatasetIndex());
						missingStat.updateWeightedSelected(tuple);
						break;
					}
				}
    		}
    	}
    	node.m_TargetStat.subtractActualFromThis(missingStat);
    }
    
    // actual missing
    private void calculateActualMissingNumeric(RowData data, NumericAttrType at, ClusNode node, RegressionStat missingStat) {
        DataTuple tuple;
        int pos = 0;
        Integer[] indicesSorted = data.smartSort(at);
        int nb_rows = data.getNbRows();

        if (at.isMissing(data.getTuple(indicesSorted[pos]))) {
            while (pos < nb_rows && at.isMissing(tuple = data.getTuple(indicesSorted[pos]))) {
                if (tuple.getDatasetIndex() == -1) {
                    pos++;
                    continue;
                }
                if (tuple.getDatasetIndex() < SupplementingRandomForest.getDatJ().numExamples) {
                    missingStat.updateWeightedSelected(tuple);
                    node.getMissingSet().add(tuple.getDatasetIndex());
                }


                pos++;
            }
        }

        if (node.getParent() != null) {
            for (int i = pos; i < nb_rows; i++) {
                tuple = data.getTuple(indicesSorted[i]);
                if (tuple.getDatasetIndex() >= SupplementingRandomForest.getDatJ().numExamples || tuple.getDatasetIndex() == -1)
                    continue;
                ClusNode tempNode = node;

                while (tempNode.getParent() != null) {
                    tempNode = (ClusNode) tempNode.getParent();
                    if (tempNode.getMissingSet().contains(tuple.getDatasetIndex())) {
                        node.getMissingSet().add(tuple.getDatasetIndex());
                        missingStat.updateWeightedSelected(tuple);
                        break;
                    }
                }
            }
        }
        node.m_TargetStat.subtractActualFromThis(missingStat);

    }

    public static long startMs;
    
    public void induce(ClusNode node, RowData data, ClusRandomNonstatic rnd) throws Exception {
        // ROS
        if (getSettings().getEnsemble().getEnsembleROSAlgorithmType().equals(EnsembleROSAlgorithmType.DynamicSubspaces)) {
            ClusROSModelInfo info = m_FindBestTest.getStatManager().getHeuristic().getClusteringAttributeWeights().getROSModelInfo();
            ClusLogger.fine(String.format("  ROS dynamic (Bag %s): %s => Node level: %s", info.getTreeNumber() + 1, info.getSubspaceString(), node.getLevel()));
            node.setROSModelInfo(info);
        }

        if (getSettings().getGeneral().getVerbose() >= SHOW_INDUCE_PROGRESS) {
            ClusLogger.fine("Depth " + node.getLevel() + ": inducing new node: " + data.getNbRows() + " examples");
        }
        if (rnd == null) {
            // rnd may be null due to some calls of induce that do not support parallelization yet
            ClusEnsembleInduce.giveParallelisationWarning(ParallelTrap.StaticRandom);
        }

        // ClusLogger.info("nonsparse induce");

        // Initialize selector and perform various stopping criteria
        if (initSelectorAndStopCrit(node, data)) {
            
        	calculateActualMissingForTarget(data, node, true);
        	
        	calculateMissingSetNegated(node);
        	
        	calculateRuleTargetCombinations(node, data);
        	
        	makeLeaf(node);

            return;
        }
        // Find best test

        // long start_time = System.currentTimeMillis();
        // if all values are missing for some attribute, statistic of parent node are
        // used for estimation of heuristic
        // score and prototype calculation. Needed for SSL-PCTs
        if (
        /* */
        getSettings().getSSL().isSemiSupervisedMode() &&
        /* */
                getSettings().getTree().getMissingClusteringAttrHandling().equals(MissingClusteringAttributeHandlingType.EstimateFromParentNode)) {
            m_FindBestTest.setParentStatsToChildren();
        }

        ClusAttrType[] attrs = getDescriptiveAttributes(rnd);
        boolean isGIS = !getSettings().getAttribute().isNullGIS();
        if (isGIS) {
            // daniela
            // only for every binary attribute or, as some say, samo za site binarni atr
            double globalMax = Double.NEGATIVE_INFINITY, globalMin = Double.POSITIVE_INFINITY;
            m_Find_MinMax.resetMinMax();
            for (int i = 0; i < attrs.length; i++) {
                ClusAttrType at = attrs[i];
                if (at instanceof NominalAttrType && ((NominalAttrType) at).getNbValues() == 2) {
                    m_Find_MinMax.rememberMinMax((NominalAttrType) at, data);
                    if (m_Find_MinMax.hMaxB > globalMax)
                        globalMax = m_Find_MinMax.hMaxB;
                    if (m_Find_MinMax.hMinB < globalMin)
                        globalMin = m_Find_MinMax.hMinB;
                }
            }
            // daniela end
        }

        boolean isExtraTreesEnsemble = getSettings().getEnsemble().isEnsembleMode() && getSettings().getEnsemble().getEnsembleMethod().equals(EnsembleMethod.ExtraTrees);
        for (int i = 0; i < attrs.length; i++) {
            ClusAttrType at = attrs[i];
            if (isGIS) {
                // daniela
                ClassificationStat.INITIALIZEPARTIALSUM = true; // attribute is evaluated, the corresponding partial
                                                                // sums of
                WHTDStatistic.INITIALIZEPARTIALSUM = true; // Optimized Moran I would be computed
                // daniela end
            }
            if (isExtraTreesEnsemble) {
                if (at.isNominal()) { // at instanceof NominalAttrType
                    m_FindBestTest.findNominalExtraTree((NominalAttrType) at, data, rnd);
                }
                else {
                    m_FindBestTest.findNumericExtraTree((NumericAttrType) at, data, rnd);
                }
            }
            else if (at.isNominal()) { // at instanceof NominalAttrType
                m_FindBestTest.findNominal((NominalAttrType) at, data, rnd, node);
            }
            else {
                m_FindBestTest.findNumeric((NumericAttrType) at, data, rnd, node);
            }
        }

        /*
         * long stop_time = System.currentTimeMillis(); long elapsed = stop_time -
         * start_time; m_Time += elapsed;
         */

        // Partition data + recursive calls
        CurrentBestTestAndHeuristic best = m_FindBestTest.getBestTest();
        
        // za najbolji split napravi kao findNumeric
        
//        node.m_MissingStat = new RegressionStat((RegressionStat) m_FindBestTest.m_BestTest.m_MissingStat); 
//        node.m_TestStat = new RegressionStat[best.m_TestStat.length];
//        for(int i = 0; i < best.m_TestStat.length; i++) {
//        	node.m_TestStat[i] = new RegressionStat((RegressionStat) best.m_TestStat[i]);
//        }
        
//        node.m_TotCorrStat = new RegressionStat((RegressionStat) m_FindBestTest.m_BestTest.m_TotCorrStat);
//        node.m_TotStat = new RegressionStat((RegressionStat) m_FindBestTest.m_BestTest.m_TotStat);
//        node.m_TestStat = new RegressionStat((RegressionStat) m_FindBestTest.m_BestTest.m_TestStat); 
        if (best.hasBestTest()) {
            // start_time = System.currentTimeMillis();

            // if all values are missing for some attribute, statistic of parent node are
            // used for estimation of
            // heuristic score and prototype calculation. Needed for SSL-PCTs
        	
            if (
            /* */
            getSettings().getSSL().isSemiSupervisedMode() &&
            /* */
                    getSettings().getTree().getMissingClusteringAttrHandling().equals(MissingClusteringAttributeHandlingType.EstimateFromParentNode)) {
                m_FindBestTest.setParentStatsToThis(node.getClusteringStat());
            }
            
            node.testToNode(best);

            // Output best test
            if (getSettings().getGeneral().getVerbose() >= SHOW_INDUCE_PROGRESS)
                ClusLogger.info("Test: " + node.getTestString() + " -> " + best.getHeuristicValue());
     
            // Create children
            int arity = node.updateArity();
            NodeTest test = node.getTest();
            RowData[] subsets = new RowData[arity];
            for (int j = 0; j < arity; j++) {
                subsets[j] = data.applyWeighted(test, j);
            }
            if (getSettings().getTree().showAlternativeSplits()) {
                filterAlternativeSplits(node, data, subsets);
            }
            if (node != m_Root && getSettings().getTree().hasTreeOptimize(TreeOptimizeValues.NoInodeStats)) {
                // Don't remove statistics of root node; code below depends on them
                node.setClusteringStat(null);
                node.setTargetStat(null);
            }

            calculateActualMissingForTarget(data, node, false);
            
            calculateMissingSetNegated(node);
            
            calculateRuleTargetCombinations(node, data);
            
            for (int j = 0; j < arity; j++) {
                ClusNode child = new ClusNode();
                node.setChild(child, j);
                child.initClusteringStat(m_StatManager, m_Root.getClusteringStat(), subsets[j]);
                child.initTargetStat(m_StatManager, m_Root.getTargetStat(), subsets[j]);

                child.m_ID = idCounter.incrementAndGet();
                child.setDepth(node.getDepth() + 1);
                
                 // added by Jurica Levatic, JSI. Needed for SSL-PCTs
                if (getSettings().getSSL().isSemiSupervisedMode() && (getSettings().getTree().getMissingClusteringAttrHandling().equals(MissingClusteringAttributeHandlingType.EstimateFromParentNode) || getSettings().getTree().getMissingTargetAttrHandling().equals(MissingTargetAttributeHandlingType.ParentNode))) {
                    child.getClusteringStat().setParentStat(node.getClusteringStat());
                    child.getTargetStat().setParentStat(node.getTargetStat());
                }

                // ROS: create new subspace if ROS enabled and subspaces should be calculated dynamically
                if (getSettings().getEnsemble().isEnsembleROSEnabled() && getSettings().getEnsemble().getEnsembleROSAlgorithmType().equals(EnsembleROSAlgorithmType.DynamicSubspaces)) {

                    ClusROSModelInfo nodeROSModelInfo = node.getROSModelInfo();

                    int sizeOfSubspace = nodeROSModelInfo.getSizeOfSubspace(); // take as many attributes in child node
                                                                               // as in current node

                    if (nodeROSModelInfo.isRandom() && !nodeROSModelInfo.isRandomPerTree()) {
                        sizeOfSubspace = -1; // random number of attributes at child node! (this overrides the default
                                             // behavior of parent subspace size)
                    }

                    HashMap<Integer, Integer> newSubspace = generateSubspace(
                            /* */
                            getStatManager().getSchema(),
                            /* */
                            sizeOfSubspace,
                            /* */
                            getSettings().getEnsemble().getEnsembleROSAlgorithmType(),
                            /* */
                            nodeROSModelInfo.getTreeNumber());

                    ClusROSModelInfo childROSModelInfo = nodeROSModelInfo.initWithNewSubspace(newSubspace);

                    nodeROSModelInfo.addChild(childROSModelInfo);
                    child.setROSModelInfo(childROSModelInfo);

                    m_FindBestTest.getStatManager().getHeuristic().getClusteringAttributeWeights().setROSModelInfo(childROSModelInfo);
                }
                
//                if(!ApplicationSettings.getInstance().leftNegation && !ApplicationSettings.getInstance().rightNegation) {
//                	RegressionStat regressionStat = ((RegressionStat) child.m_TargetStat);
//                    
//                    if(SupplementingRandomForest.getNumIter() != 0) {
//                    	                	
//                    	List<Integer> targetList = new ArrayList<>();
//                    	List<Double> listaJaccardova = new ArrayList<>();
//                    	
//                    	for(int targetId = 1; targetId <= regressionStat.getSumValues().length; targetId++) {
//                    		// 1. POZITIVNA PRAVILA
//                    		int targetPosition = targetId - 1;
//                    		
//                    		int targetSupport = TargetRulesMap.getTargetSupportMap().get(targetId).elements.size();
//                    		
//                        	// pozitivni targeti
//                    		if(
//                    			isJaccardValid(regressionStat.getSumValues(targetPosition), regressionStat.getSumWeights(targetPosition), targetSupport) &&
//                    			isPValueValid(regressionStat.getSumValues(targetPosition), regressionStat.getSumWeights(targetPosition), targetSupport)
//                        	) {
//                        		targetList.add(targetId);
//                        		listaJaccardova.add(regressionStat.getSumValues(targetPosition) / (regressionStat.getSumWeights(targetPosition) + targetSupport - regressionStat.getSumValues(targetPosition)));
//                        	}
//                        }
//                    	
//                    	if(!targetList.isEmpty()) {
//                        	TargetRulesMap.getNodeTargets().put(child.getID(), targetList);
//                        	TargetRulesMap.getmapaJaccardova().put(child.getID(), listaJaccardova);
//                    	}
//                    	
//                    }
//                }
//
                induce(child, subsets[j], rnd);
            }
        }
        else {
        	
        	calculateActualMissingForTarget(data, node, true);
        	
        	calculateMissingSetNegated(node);
        	
        	calculateRuleTargetCombinations(node, data);
        	
            makeLeaf(node);
        }
        if (getSettings().getGeneral().getVerbose() >= SHOW_INDUCE_PROGRESS) {
            ClusLogger.finer("Depth " + node.getLevel() + ": node finished.");
        }
    }

    public HashMap<Integer, Integer> generateSubspace(ClusSchema schema, int sizeOfSubspace, EnsembleROSAlgorithmType rosAT, int bagNumber) {
        if (rosAT.equals(EnsembleROSAlgorithmType.FixedSubspaces) && bagNumber == 0) {
            // first bag contains all attributes
            return ClusROSHelpers.populateMap(schema.getClusteringAttributes(), schema);
        }
        else {
            // randomly select subspace
            return generateSubspace(schema, sizeOfSubspace);
        }
    }

    private HashMap<Integer, Integer> generateSubspace(ClusSchema schema, int sizeOfSubspace) {
        boolean isRandom = sizeOfSubspace <= 0;
        int subspaceCount = sizeOfSubspace;

        ClusAttrType[] clustering = schema.getClusteringAttributes();

        if (isRandom) {
            // if number of randomly selected targets should also be randomized
            // use a separate randomizer for randomized target subspace size selection
            subspaceCount = ClusRandom.nextInt(ClusRandom.RANDOM_ENSEMBLE_ROS_SUBSPACE_SIZE_SELECTION, 1, clustering.length);
        }

        // Randomly select targets with ClusRandom.RANDOM_ENSEMBLE_TARGET_SUBSPACING randomizer
        ClusAttrType[] selected = selectRandomSubspaces(clustering, subspaceCount, ClusRandom.RANDOM_ENSEMBLE_ROS, null);

        return ClusROSHelpers.populateMap(selected, schema);
    }

    /*
     * public void inducePert(ClusNode node, RowData data) {
     * //ClusLogger.info("nonsparse inducePert"); // Initialize selector and
     * perform various stopping criteria if (initSelectorAndStopCrit(node, data)) {
     * makeLeaf(node); return; } // Find best test // ClusLogger.info("Schema: "
     * + getSchema().toString()); ArrayList<Integer> tuplelist =
     * data.getPertTuples(); if (tuplelist.size()<2) { makeLeaf(node); return; } //
     * we only check the first two tuples. In case of multi-class classification,
     * this corresponds to two random(?) classes to split. int tuple1index =
     * tuplelist.get(0); DataTuple tuple1 = data.getTuple(tuple1index); int
     * tuple2index = tuplelist.get(1); DataTuple tuple2 =
     * data.getTuple(tuple2index); // ClusLogger.info("tuples chosen: " +
     * tuple1index + " " + tuple1.m_Index + " and " + tuple2index + " " +
     * tuple2.m_Index); ClusAttrType attr =
     * tuple1.findDiscriminatingAttribute(tuple2); //
     * ClusLogger.info("attribute chosen: " + attr.toString()); if (attr != null)
     * { m_FindBestTest.findPert(attr, tuple1, tuple2); } else { // no
     * discriminating attribute can be found, should not occur System.out.
     * println("No discriminating attribute found for the two selected tuples. Making leaf..."
     * ); makeLeaf(node); return; } // Partition data + recursive calls
     * CurrentBestTestAndHeuristic best = m_FindBestTest.getBestTest(); if
     * (best.hasBestTest()) { // start_time = System.currentTimeMillis();
     * node.testToNode(best); // Output best test if
     * (getSettings().getGeneral().getVerbose() > 0)
     * ClusLogger.info("Test: "+node.getTestString()+" -> "
     * +best.getHeuristicValue()); // Create children int arity =
     * node.updateArity(); NodeTest test = node.getTest(); RowData[] subsets = new
     * RowData[arity]; for (int j = 0; j < arity; j++) { subsets[j] =
     * data.applyWeighted(test, j); } if (getSettings().showAlternativeSplits()) {
     * filterAlternativeSplits(node, data, subsets); } if (node != m_Root &&
     * getSettings().hasTreeOptimize(Settings.TREE_OPTIMIZE_NO_INODE_STATS)) { //
     * Don't remove statistics of root node; code below depends on them
     * node.setClusteringStat(null); node.setTargetStat(null); } for (int j = 0; j <
     * arity; j++) { ClusNode child = new ClusNode(); node.setChild(child, j);
     * child.initClusteringStat(m_StatManager, m_Root.getClusteringStat(),
     * subsets[j]); child.initTargetStat(m_StatManager, m_Root.getTargetStat(),
     * subsets[j]); inducePert(child, subsets[j]); } } else { makeLeaf(node); } }
     */

    @Deprecated
    public void rankFeatures(ClusNode node, RowData data, ClusRandomNonstatic rnd) throws Exception {
        // Find best test
        PrintWriter wrt = new PrintWriter(new OutputStreamWriter(new FileOutputStream("ranking.csv")));
        ClusAttrType[] attrs = getDescriptiveAttributes(rnd);
        for (int i = 0; i < attrs.length; i++) {
            ClusAttrType at = attrs[i];
            initSelectorAndStopCrit(node, data);
            if (at instanceof NominalAttrType)
                m_FindBestTest.findNominal((NominalAttrType) at, data, null, node);
            else
                m_FindBestTest.findNumeric((NumericAttrType) at, data, null, node);
            CurrentBestTestAndHeuristic cbt = m_FindBestTest.getBestTest();
            if (cbt.hasBestTest()) {
                NodeTest test = cbt.updateTest();
                wrt.print(cbt.m_BestHeur);
                wrt.print(",\"" + at.getName() + "\"");
                wrt.println(",\"" + test + "\"");
            }
        }
        wrt.close();
    }


    public void initSelectorAndSplit(ClusStatistic stat) throws ClusException {
        m_FindBestTest.initSelectorAndSplit(stat);
        m_Find_MinMax.initSelectorAndSplit(stat); // daniela
    }


    public void setInitialData(ClusStatistic stat, RowData data) throws ClusException {
        m_FindBestTest.setInitialData(stat, data);
        m_Find_MinMax.setInitialData(stat, data); // daniela
    }


    public void cleanSplit() {
        m_FindBestTest.cleanSplit();
    }

    public ClusNode induceSingleUnpruned(RowData data, ClusRandomNonstatic rnd) throws Exception {
        m_Root = null;
        while (true) {
            // Init root node
        	this.fullDataSet = data;
            m_Root = new ClusNode();
            m_Root.initClusteringStat(m_StatManager, data);
            m_Root.initTargetStat(m_StatManager, data);
            /*
             * if ensembles mode is used we don't write root info (i.e., hierarchy.txt
             * file), otherwise this file is written for every tree, which is not convenient
             * because the file can be huge, root info is instead written only once in
             * <code>ClusEnsembleInduce</code> added by Jurica Levatic, June, 2014
             */
            if (!m_Schema.getSettings().getEnsemble().isEnsembleMode()) {
                m_Root.getClusteringStat().showRootInfo();
            }

            initSelectorAndSplit(m_Root.getClusteringStat());
            setInitialData(m_Root.getClusteringStat(), data);

            // Induce the tree
            data.addIndices();
            /*
             * if (getSettings().isEnsembleMode() && getSettings().getEnsembleMethod() ==
             * getSettings().ENSEMBLE_PERT) { inducePert(m_Root, data); } else {
             */

            this.hasMissing = m_Schema.hasMissing();

            m_Root.m_ID = 1;
            induce(m_Root, data, rnd);
            idCounter.set(1);
//            resetCounter();
//            m_Root. printTree();
            // skipping initialization phase
//            if(SupplementingRandomForest.getNumIter() != 0 /*&& (ApplicationSettings.getInstance().leftNegation || ApplicationSettings.getInstance().rightNegation)*/) {
//            	calculateRuleTargetCombinationsNegations();
//            }


            /* } */
            // rankFeatures(m_Root, data);

            // Refinement finished
            if (SettingsGeneric.EXACT_TIME == false) // TODO: where is this used? martinb
                break;
        }

        m_Root.afterInduce(m_StatManager);
        cleanSplit();
        return m_Root;
    }

    public ClusNode induceSingleUnpruned(RowData data, ClusRandomNonstatic rnd, ClusRun cr) throws Exception {
        setRandomSubspaces(
                selectRandomSubspaces(
                        cr.getStatManager().getSchema().getDescriptiveAttributes(),
                        cr.getStatManager().getSettings().getEnsemble().getNbRandomAttrSelected(),
                        ClusRandomNonstatic.RANDOM_SELECTION,
                        rnd
                )
        );
        ApplicationSettings appset = ApplicationSettings.getInstance();
        jaccard = appset.useJoin ? appset.minAddRedJS : appset.minJS;
        minSupport = appset.minSupport;
        maxSupport = appset.maxSupport;

        m_Root = null;
        // TODO: where is this used? martinb
        do {
            // Init root node
            this.fullDataSet = data;
            m_Root = new ClusNode();
            m_Root.initClusteringStat(m_StatManager, data);
            m_Root.initTargetStat(m_StatManager, data);
            /*
             * if ensembles mode is used we don't write root info (i.e., hierarchy.txt
             * file), otherwise this file is written for every tree, which is not convenient
             * because the file can be huge, root info is instead written only once in
             * <code>ClusEnsembleInduce</code> added by Jurica Levatic, June, 2014
             */
            if (!m_Schema.getSettings().getEnsemble().isEnsembleMode()) {
                m_Root.getClusteringStat().showRootInfo();
            }

            initSelectorAndSplit(m_Root.getClusteringStat());
            setInitialData(m_Root.getClusteringStat(), data);

            // Induce the tree
            data.addIndices();
            /*
             * if (getSettings().isEnsembleMode() && getSettings().getEnsembleMethod() ==
             * getSettings().ENSEMBLE_PERT) { inducePert(m_Root, data); } else {
             */

            this.hasMissing = m_Schema.hasMissing();

            m_Root.m_ID = TargetRulesMap.getIsForest() ? idCounter.incrementAndGet() : 1;
            if (m_Root.m_ID == 1) idCounter.set(1);
            induce(m_Root, data, rnd);
//            resetCounter();
//            m_Root. printTree();
            // skipping initialization phase
//            if(SupplementingRandomForest.getNumIter() != 0 /*&& (ApplicationSettings.getInstance().leftNegation || ApplicationSettings.getInstance().rightNegation)*/) {
//            	calculateRuleTargetCombinationsNegations();
//            }


            /* } */
            // rankFeatures(m_Root, data);

            // Refinement finished
        } while (SettingsGeneric.EXACT_TIME != false);

        m_Root.afterInduce(m_StatManager);
        cleanSplit();
        return m_Root;
    }

    private boolean isCriteriaValidNumeric(NodeTest test, DataTuple tuple) {
    	double bound = ((NumericTest) test).getBound();
		double attributeValue = tuple.getDoubleVal(test.getType().getArrayIndex());
		if(attributeValue > bound) {
			return true;
		}
		return false;
    }
    
    private boolean isCriteriaValidNominal(NodeTest test, DataTuple tuple) {
    	SubsetTest subsetTest = (SubsetTest) test;
		// vrijednost u tom retku
		String attributeValue = test.getType().getString(tuple);
		if(attributeValue.equals("?")) {
			return true;
		}
		for(int i = 0; i < subsetTest.getNbValues(); i++) {
			// iz type dohvaćam String, a iz subsetTest točno index vrijednosti splita
			if(((NominalAttrType) subsetTest.getType()).getValue(subsetTest.getValue(i)).equals(attributeValue)) {
				return true;
			}
		}
		return false;
    }
    
    private boolean isCriteriaValid(NodeTest test, DataTuple tuple) {
    	// numeričke vrijednosti
    	if(test instanceof NumericTest) {
    		double bound = ((NumericTest) test).getBound();
    		double attributeValue = tuple.getDoubleVal(test.getType().getArrayIndex());
    		if(attributeValue > bound) {
    			return true;
    		}
    		// kategorijske vrijednosti
    	} else if(test instanceof SubsetTest) {
    		SubsetTest subsetTest = (SubsetTest) test;
    		// vrijednost u tom retku
    		String attributeValue = test.getType().getString(tuple);
    		if(attributeValue.equals("?")) {
    			return true;
    		}
			for(int i = 0; i < subsetTest.getNbValues(); i++) {
				// iz type dohvaćam String, a iz subsetTest točno index vrijednosti splita
				if(((NominalAttrType) subsetTest.getType()).getValue(subsetTest.getValue(i)).equals(attributeValue)) {
					return true;
				}
			}
    	}
		return false;
    }
    
    private void calculateMissingSetNegated(ClusNode node) {
//    	if(SupplementingRandomForest.getNumIter() == 0) return;
    	TIntIterator iterator = node.getMissingSet().iterator();

    	while(iterator.hasNext()) {
    		int tupleIndex = iterator.next();
    		if(tupleIndex >= SupplementingRandomForest.getDatJ().numExamples || tupleIndex == -1) continue; 

    		DataTuple tuple = fullDataSet.getTuple(tupleIndex);
    		
    		if(node.getParent() == null) {
    			NodeTest test = ((ClusNode) node).getTest();
    			if(test.getType().isNumeric()) {
    				if(!test.getType().isMissing(tuple) || !isCriteriaValidNumeric(test, tuple)) {
        				node.getMissingSetNegated().add(tuple.getDatasetIndex());
        			}
    			} else if(test.getType().isNominal()) {
    				if(!isCriteriaValidNominal(test, tuple)) {
        				node.getMissingSetNegated().add(tuple.getDatasetIndex());
        			}
    			}
    		} else {
    			ClusNode tempNode = node;

        		while(tempNode.getParent() != null) {
        			NodeTest test = ((ClusNode) tempNode.getParent()).getTest();
        			if(test.getType().isNumeric()) {
        				if(!test.getType().isMissing(tuple) || !isCriteriaValidNumeric(test, tuple)) {
            				node.getMissingSetNegated().add(tuple.getDatasetIndex());
            			}
        			} else if(test.getType().isNominal()) {
        				if(!isCriteriaValidNominal(test, tuple)) {
            				node.getMissingSetNegated().add(tuple.getDatasetIndex());
            			}
        			}
        			
        			tempNode = (ClusNode) tempNode.getParent();
        		}
    		}	
    	}
    	// TODO datJ().numExamples ili fullDataSet.getNbRows()?
    	double sumWeightNegacije = SupplementingRandomForest.getDatJ().numExamples - ((RegressionStat)node.m_TargetStat).m_SumWeightsActual[0] - (node.getMissingSet().size() - node.getMissingSetNegated().size());
    	node.setSumWeightNegacije(sumWeightNegacije);
    	node.setMissingSetDifferenceMissingSetNegated(Jacard.getSetDifference(node.getMissingSet(), node.getMissingSetNegated()));
    }
    
    private void calculateRuleTargetCombinations(ClusNode node, RowData data) {
    	
    	if(SupplementingRandomForest.getNumIter() == 0) return;

    	ApplicationSettings appset = ApplicationSettings.getInstance();

    	RegressionStat regressionStat = ((RegressionStat) node.m_TargetStat);
    	
		List<Integer> targetList = new ArrayList<>();
        List<Integer> targetListNegative = new ArrayList<>();
        List<Double> listaJaccardova = new ArrayList<>();
    	List<Double> listaJaccardovaNegative = new ArrayList<>();
    	List<Double> listaPVrijednosti = new ArrayList<>();
    	List<Double> listaPVrijednostiNegative = new ArrayList<>();

    	for(int targetId = 1; targetId <= regressionStat.m_SumValues.length; targetId++) {
			int targetPosition = targetId - 1;
    		
    		// 1. POZITIVNA PRAVILA
			Rule target = TargetRulesMap.getTargetSupportMap().get(targetId);
    		int targetSupport = target.elements.size();
    		int targetSupportNegated = TargetRulesMap.getTargetSupportNegatedMap().get(-targetId);

    		double sumValueActual = regressionStat.m_SumValuesActual[targetPosition];
    		double sumWeightActual = regressionStat.m_SumWeightsActual[targetPosition];
    		
    		if(sumValueActual >= minSupport && sumValueActual <= maxSupport) {
    			double jac = sumValueActual / (sumWeightActual + targetSupport - sumValueActual);
    			if(
    	    			jac >= jaccard && isPValueValid(sumValueActual, sumWeightActual, targetSupport)
    	        	) {
    	        		targetList.add(targetId);
//    	        		listaJaccardova.add(sumValueActual);
    	        		listaJaccardova.add(jac);
//    	        		listaJaccardova.add((regressionStat.getSumValues(targetPosition) / (regressionStat.getSumWeights(targetPosition) + targetSupport - regressionStat.getSumValues(targetPosition))));
//    	        		listaPVrijednosti.add(calculatePValue(sumValue, sumWeight, targetSupport));
//    	        		listaPVrijednosti.add(sumWeightActual + targetSupport - sumValueActual);
//    	        		listaPVrijednosti.add(sumWeight);
    	    		}
    		}
    		
    		
    		// 2. NEGIRANI TARGETI
    			   		// TODO
    		int counter1 = 0;
    		TIntIterator iterator;
    		double presjek;
    		double unija;
    		
			iterator = target.getMissingSetDifferenceMissingSetNegated().iterator(); 
    		while(iterator.hasNext()) {
        		int rowIndex = iterator.next();
        		if(rowIndex == -1) continue;
        		if(rowIndex < SupplementingRandomForest.getDatJ().numExamples) {
        			DataTuple tuple = fullDataSet.getTuple(rowIndex);
            		if(!node.getMissingSet().contains(rowIndex) && (node.getParent() == null || data.containsIndex(rowIndex)) && tuple.getDoubleVal(regressionStat.getAttribute(targetPosition).getArrayIndex()) == 0) {
            			counter1++;
            		}
        		}
        	}

    		presjek = sumWeightActual - sumValueActual - counter1;
    		
    		if(presjek >= minSupport && presjek <= maxSupport) {
    			unija = sumWeightActual - presjek + (int) target.getSumWeightNegacije();
    			double jac = presjek / unija;
            	if(
        			jac >= jaccard &&
        			isPValueValid(presjek, sumWeightActual, (int) target.getSumWeightNegacije())
                ) {
                	targetList.add(-targetId);
//            		listaJaccardova.add((presjek)/(regressionStat.getSumWeights(targetPosition) + targetSupportNegated - presjek));
            		listaJaccardova.add(jac);
//            		listaPVrijednosti.add(unija);
                }
    		}
    		
    		
    		
			// 3. NEGIRANA PRAVILA
    		
        	int counter2 = 0;
        	
    		iterator = node.getMissingSetDifferenceMissingSetNegated().iterator();
        	while(iterator.hasNext()) {
        		int rowIndex = iterator.next();
        		DataTuple tuple = fullDataSet.getTuple(rowIndex);
        		if(tuple.getDoubleVal(regressionStat.getAttribute(targetPosition).getArrayIndex()) == 1) {
        			counter2++;
        		}
        	}
        	
        	
        	presjek = targetSupport - sumValueActual - counter2;

        	if(presjek >= minSupport && presjek <= maxSupport) {
        		double jac = presjek / (node.getSumWeightNegacije() + targetSupport - presjek);
        		if(jac >= jaccard && isPValueValid(presjek, node.getSumWeightNegacije(), targetSupport)) {
            		targetListNegative.add(targetId);
            		listaJaccardovaNegative.add(jac);
//            		listaPVrijednostiNegative.add(node.getSumWeightNegacije() + targetSupport - presjek);
            	}
        	}
        	
        	
    		
		}
		if(!targetList.isEmpty()) {
            TargetRulesMap.getNodeTargets().put(node.getID(), targetList);
        	TargetRulesMap.getmapaJaccardova().put(node.getID(), listaJaccardova);
        	TargetRulesMap.getmapaPVrijednosti().put(node.getID(), listaPVrijednosti);
    	}
    	
    	if(!targetListNegative.isEmpty()) {
            TargetRulesMap.getNodeTargets().put(-node.getID(), targetListNegative);
        	TargetRulesMap.getmapaJaccardova().put(-node.getID(), listaJaccardovaNegative);
        	TargetRulesMap.getmapaPVrijednosti().put(-node.getID(), listaPVrijednostiNegative);
    	}
    }
    
    private void calculateRuleTargetCombinationsNegations() {
    	List<ClusNode> leafList = new ArrayList<>();
    	
    	for(int depth = 0; depth < levelNodesList.size(); depth++) {
    		// privremeno spremamo liste trenutne dubine stabla tako da ih ne dodajemo duplo za trenutnu dubinu
    		// nakon iteracije pravila dodaje se u leafList
    		List<ClusNode> leafListTemp = new ArrayList<>();
    		List<ClusNode> nodeList = levelNodesList.get(depth);
    		
        	for(int i = 0; i < nodeList.size(); i++) {
        		
        		RegressionStat regressionStat = ((RegressionStat) nodeList.get(i).m_TargetStat);
	
        		List<Integer> targetList = new ArrayList<>();
                List<Integer> targetListNegative = new ArrayList<>();
                List<Double> listaJaccardova = new ArrayList<>();
            	List<Double> listaJaccardovaNegative = new ArrayList<>();
            	List<Double> listaPVrijednosti = new ArrayList<>();
            	List<Double> listaPVrijednostiNegative = new ArrayList<>();
            	
        		for(int targetId = 1; targetId <= regressionStat.m_SumValues.length; targetId++) {
        			int targetPosition = targetId - 1;
            		
            		// 1. POZITIVNA PRAVILA
        			Rule target = TargetRulesMap.getTargetSupportMap().get(targetId);
            		int targetSupport = target.elements.size();
            		int targetSupportNegated = TargetRulesMap.getTargetSupportNegatedMap().get(-targetId);
            		
            		double sumValue;
            		double sumWeight;
            		
            		// pozitivni targeti
            		
            		sumValue = regressionStat.m_SumValuesActual[targetPosition];
            		sumWeight = regressionStat.m_SumWeightsActual[targetPosition];
            		
            		if(
            			isJaccardValid(sumValue, sumWeight, targetSupport) && isPValueValid(sumValue, sumWeight, targetSupport)
                	) {
                		targetList.add(targetId);
                		listaJaccardova.add(sumValue);
//                		listaJaccardova.add(sumValue / (sumWeight + targetSupport - sumValue));
//                		listaJaccardova.add((regressionStat.getSumValues(targetPosition) / (regressionStat.getSumWeights(targetPosition) + targetSupport - regressionStat.getSumValues(targetPosition))));
//                		listaPVrijednosti.add(calculatePValue(sumValue, sumWeight, targetSupport));
                		listaPVrijednosti.add(sumWeight + targetSupport - sumValue);
//                		listaPVrijednosti.add(sumWeight);
            		}
            		
//            		for(DataTuple missingTuple : nodeList.get(i).getMissingSet()) {
//            			
//            		}
            		
            		double presjek = regressionStat.m_SumWeights[targetPosition] - regressionStat.m_SumValues[targetPosition];
                	if(
            			isJaccardValid(presjek, regressionStat.getSumWeights(targetPosition), targetSupportNegated) &&
            			isPValueValid(presjek, regressionStat.getSumWeights(targetPosition), targetSupportNegated)
                    ) {
//                    	targetList.add(-targetId);
//                		listaJaccardova.add((presjek)/(regressionStat.getSumWeights(targetPosition) + targetSupportNegated - presjek));
//                		listaPVrijednosti.add(calculatePValue(presjek, regressionStat.getSumWeights(targetPosition), targetSupportNegated));
                    }
            		
            		// za korijen je negacija prazan skup pa ne provodimo ovu računicu
        			// 2. NEGIRANA PRAVILA - sumWeight i sumValue od braće (aproksimacija)
//                	double sumValue = 0;
//        			double sumWeight = 0;
        			
        			
        			// m_TargetStat -> razlika = sumWeight - (m_NbExamples - m_NbMissing) (u m_NbMissing imam broj elemenata)
        	        //  sumWeight = sumWeight - razlika
//                	for(int childNum = 0; childNum < nodeList.size(); childNum++) {
//                		if(childNum != i) {
//                			RegressionStat regressionStatChild = ((RegressionStat) nodeList.get(childNum).m_TargetStat);
//                			sumValue += regressionStatChild.getSumValues(targetPosition);
//                			sumWeight += regressionStatChild.getSumWeights(targetPosition);
//                		}
//                	}
//                	
//            		for(ClusNode leaf : leafList) {
//            			RegressionStat regressionStatChild = ((RegressionStat) leaf.m_TargetStat);
//            			sumValue += regressionStatChild.getSumValues(targetPosition);
//            			sumWeight += regressionStatChild.getSumWeights(targetPosition);
//            		}
                	
            		// OVO RADI BEZ BRAĆE!
//            		double p1 = sumValue / (sumWeight + targetSupport - sumValue);
            		double presjekProvjera = targetSupport - regressionStat.getSumValues(targetPosition);
            		double p2 = (presjekProvjera) / (targetSupport + SupplementingRandomForest.getDatJ().numExamples - regressionStat.getSumWeights(targetPosition) - presjekProvjera);
            		
                	// pozitivni targeti + svi listovi iznad njega
                	if(isJaccardValid(presjekProvjera, SupplementingRandomForest.getDatJ().numExamples - regressionStat.getSumWeights(targetPosition), targetSupport) && isPValueValid(presjekProvjera, SupplementingRandomForest.getDatJ().numExamples - regressionStat.getSumWeights(targetPosition), targetSupport)) {
//                		targetListNegative.add(targetId);
////                    		listaJaccardovaNegative.add(sumValue);
//                		listaJaccardovaNegative.add(p2);
//                		
//                		listaPVrijednostiNegative.add(calculatePValue(presjekProvjera, SupplementingRandomForest.getDatJ().numExamples - regressionStat.getSumWeights(targetPosition), targetSupport));
                	}
            		
            		// negirani targeti
//                		presjek = sumWeight - sumValue;
//                		if(isJaccardValid(presjek, sumWeight, targetSupportNegated) && isPValueValid(presjek, sumWeight, targetSupportNegated)) {
//                    		targetListNegative.add(-targetId);
//                    		listaJaccardovaNegative.add(presjek / (sumWeight + targetSupportNegated - presjek));
//                    		listaJaccardovaNegative.add(presjek);
//                    	}
            		
        		}
        		if(!targetList.isEmpty()) {
                	TargetRulesMap.getNodeTargets().put(nodeList.get(i).getID(), targetList);
                	TargetRulesMap.getmapaJaccardova().put(nodeList.get(i).getID(), listaJaccardova);
                	TargetRulesMap.getmapaPVrijednosti().put(nodeList.get(i).getID(), listaPVrijednosti);
            	}
            	
            	if(!targetListNegative.isEmpty()) {
                    TargetRulesMap.getNodeTargets().put(-nodeList.get(i).getID(), targetListNegative);
                	TargetRulesMap.getmapaJaccardova().put(-nodeList.get(i).getID(), listaJaccardovaNegative);
                	TargetRulesMap.getmapaPVrijednosti().put(nodeList.get(i).getID(), listaPVrijednostiNegative);
            	}
                	
                if(nodeList.get(i).atBottomLevel()) {
                	leafListTemp.add(nodeList.get(i));
                }
        	} 
        	leafList.addAll(leafListTemp);
        }
    }
    

    public ClusModel induceSingleUnpruned(ClusRun cr, ClusRandomNonstatic rnd) throws Exception {
        return induceSingleUnpruned((RowData) cr.getTrainingSet(), rnd, cr);
    }


    @Override
    public ClusModel induceSingleUnpruned(ClusRun cr) throws Exception {
        if (getSettings().getEnsemble().isEnsembleMode() && getSettings().getEnsemble().getNumberOfThreads() != -1) {
            ClusLogger.info(String.format("Potential WARNING:\n" + "It seems that you are trying to build an ensemble in parallel.\n If this is not the case, ignore this message. Otherwise: The chosen number of threads (%d) is not equal to 1, and the method\ninduceSingleUnpruned(ClusRun cr) is not appropriate for parallelism (the results might not be reproducible).\nThe method induceSingleUnpruned(RowData data, ClusRandomNonstatic rnd) should be used instead.", getSettings().getEnsemble().getNumberOfThreads()));
        }
        return induceSingleUnpruned((RowData) cr.getTrainingSet(), null, cr);
    }

}
