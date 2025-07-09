package redescriptionmining;

import java.util.ArrayList;

public class ParemetersForGuidedJoinBasic {
	
	private RuleReader rr;
	private RuleReader rr1;
	private Jacard[] js;
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
	private double statistics[];
	private ArrayList<Double> maxDiffScoreDistribution;
	private NHMCDistanceMatrix mat;
	private int PreferenceRow;
	
	public ParemetersForGuidedJoinBasic(RuleReader rr, RuleReader rr1, Jacard[] js, ApplicationSettings appset,
			int oldIndexRR, int oldIndexRR1, int runInd, boolean[] outOfmemory, Mappings map, DataSetCreator dat,
			int[] elemFreq, int[] attrFreq, ArrayList<Double> redScore, ArrayList<Double> redScoreAt,
			ArrayList<Double> redDistCoverage, ArrayList<Double> redDistCoverageAt, ArrayList<Double> redDistNetwork,
			ArrayList<Double> targetAtScore, double[] statistics, ArrayList<Double> maxDiffScoreDistribution,
			NHMCDistanceMatrix mat, int preferenceRow) {
		super();
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
		PreferenceRow = preferenceRow;
	}

	public RuleReader getRr() {
		return rr;
	}

	public void setRr(RuleReader rr) {
		this.rr = rr;
	}

	public RuleReader getRr1() {
		return rr1;
	}

	public void setRr1(RuleReader rr1) {
		this.rr1 = rr1;
	}

	public Jacard[] getJs() {
		return js;
	}

	public void setJs(Jacard[] js) {
		this.js = js;
	}

	public ApplicationSettings getAppset() {
		return appset;
	}

	public void setAppset(ApplicationSettings appset) {
		this.appset = appset;
	}

	public int getOldIndexRR() {
		return oldIndexRR;
	}

	public void setOldIndexRR(int oldIndexRR) {
		this.oldIndexRR = oldIndexRR;
	}

	public int getOldIndexRR1() {
		return oldIndexRR1;
	}

	public void setOldIndexRR1(int oldIndexRR1) {
		this.oldIndexRR1 = oldIndexRR1;
	}

	public int getRunInd() {
		return runInd;
	}

	public void setRunInd(int runInd) {
		this.runInd = runInd;
	}

	public boolean[] getOutOfmemory() {
		return outOfmemory;
	}

	public void setOutOfmemory(boolean[] outOfmemory) {
		this.outOfmemory = outOfmemory;
	}

	public Mappings getMap() {
		return map;
	}

	public void setMap(Mappings map) {
		this.map = map;
	}

	public DataSetCreator getDat() {
		return dat;
	}

	public void setDat(DataSetCreator dat) {
		this.dat = dat;
	}

	public int[] getElemFreq() {
		return elemFreq;
	}

	public void setElemFreq(int[] elemFreq) {
		this.elemFreq = elemFreq;
	}

	public int[] getAttrFreq() {
		return attrFreq;
	}

	public void setAttrFreq(int[] attrFreq) {
		this.attrFreq = attrFreq;
	}

	public ArrayList<Double> getRedScore() {
		return redScore;
	}

	public void setRedScore(ArrayList<Double> redScore) {
		this.redScore = redScore;
	}

	public ArrayList<Double> getRedScoreAt() {
		return redScoreAt;
	}

	public void setRedScoreAt(ArrayList<Double> redScoreAt) {
		this.redScoreAt = redScoreAt;
	}

	public ArrayList<Double> getRedDistCoverage() {
		return redDistCoverage;
	}

	public void setRedDistCoverage(ArrayList<Double> redDistCoverage) {
		this.redDistCoverage = redDistCoverage;
	}

	public ArrayList<Double> getRedDistCoverageAt() {
		return redDistCoverageAt;
	}

	public void setRedDistCoverageAt(ArrayList<Double> redDistCoverageAt) {
		this.redDistCoverageAt = redDistCoverageAt;
	}

	public ArrayList<Double> getRedDistNetwork() {
		return redDistNetwork;
	}

	public void setRedDistNetwork(ArrayList<Double> redDistNetwork) {
		this.redDistNetwork = redDistNetwork;
	}

	public ArrayList<Double> getTargetAtScore() {
		return targetAtScore;
	}

	public void setTargetAtScore(ArrayList<Double> targetAtScore) {
		this.targetAtScore = targetAtScore;
	}

	public double[] getStatistics() {
		return statistics;
	}

	public void setStatistics(double[] statistics) {
		this.statistics = statistics;
	}

	public ArrayList<Double> getMaxDiffScoreDistribution() {
		return maxDiffScoreDistribution;
	}

	public void setMaxDiffScoreDistribution(ArrayList<Double> maxDiffScoreDistribution) {
		this.maxDiffScoreDistribution = maxDiffScoreDistribution;
	}

	public NHMCDistanceMatrix getMat() {
		return mat;
	}

	public void setMat(NHMCDistanceMatrix mat) {
		this.mat = mat;
	}

	public int getPreferenceRow() {
		return PreferenceRow;
	}

	public void setPreferenceRow(int preferenceRow) {
		PreferenceRow = preferenceRow;
	}
	
	
	
}
