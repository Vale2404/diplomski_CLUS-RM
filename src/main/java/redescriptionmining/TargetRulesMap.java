package redescriptionmining;

import java.util.List;
import java.util.Map;

public class TargetRulesMap {
	
	private static final Map<Integer, Integer> targetSupportNegatedMap = new ConcurrentTHashMap<>();
	private static final Map<Integer, Integer> targetSupportNegatedMap1 = new ConcurrentTHashMap<>();
	
	private static final Map<Integer, Rule > targetSupportMap = new ConcurrentTHashMap<>();
	private static final Map<Integer, Rule > targetSupportMap1 = new ConcurrentTHashMap<>();

	private static final Map<Integer, List<Integer>> nodeTargets = new ConcurrentTHashMap<>();
	private static final Map<Integer, List<Integer>> nodeTargets1 = new ConcurrentTHashMap<>();
	
	private static final Map<Integer, List<Double>> mapaJaccardova = new ConcurrentTHashMap<>();
	private static final Map<Integer, List<Double>> mapaJaccardova1 = new ConcurrentTHashMap<>();
	
	private static final Map<Integer, List<Double>> mapaPVrijednosti = new ConcurrentTHashMap<>();
	private static final Map<Integer, List<Double>> mapaPVrijednosti1 = new ConcurrentTHashMap<>();

	private static final Map<Integer, Rule> idRuleMap = new ConcurrentTHashMap<>();
	private static final Map<Integer, Rule> idRuleMap1 = new ConcurrentTHashMap<>();

	private static long forestCreationTime;
	
	private static Cycle cycle = Cycle.ONE;
	private static Boolean isForest = false;

	private TargetRulesMap() {
	}

	public static void setCycle(Cycle cycleToSet) {
		cycle = cycleToSet;
	}

	public static Boolean getIsForest() {
		return isForest;
	}

	public static void setIsForest(Boolean isForestToSet) {
		isForest = isForestToSet;
	}

	public static Map<Integer, Rule> getIdRuleMap() {
		return cycle.equals(Cycle.ONE) ? idRuleMap : idRuleMap1;
	}
	
	public static Map<Integer, Rule> getIdRuleMap(Cycle cycle) {
		return cycle.equals(Cycle.ONE) ? idRuleMap : idRuleMap1;
	}
	
	public static Map<Integer, List<Double>> getmapaPVrijednosti() {
		return cycle.equals(Cycle.ONE) ? mapaPVrijednosti : mapaPVrijednosti1;
	}
 	
	public static Map<Integer, List<Double>> getmapaPVrijednosti(Cycle cycle) {
		return cycle.equals(Cycle.ONE) ? mapaPVrijednosti : mapaPVrijednosti1;
	}
	
	public static Map<Integer, List<Double>> getmapaJaccardova() {
		return cycle.equals(Cycle.ONE) ? mapaJaccardova : mapaJaccardova1;
	}
 	
	public static Map<Integer, List<Double>> getmapaJaccardova(Cycle cycle) {
		return cycle.equals(Cycle.ONE) ? mapaJaccardova : mapaJaccardova1;
	}
	
	public static Map<Integer, Integer> getTargetSupportNegatedMap(Cycle cycle) {
		return cycle.equals(Cycle.ONE) ? targetSupportNegatedMap : targetSupportNegatedMap1;
	}
	
	public static Map<Integer, List<Integer>> getNodeTargets(Cycle cycle) {
		return cycle.equals(Cycle.ONE) ? nodeTargets : nodeTargets1;
	}
	
	public static Map<Integer, Rule> getTargetSupportMap() {
		return cycle.equals(Cycle.ONE) ? targetSupportMap : targetSupportMap1;
	}
 	
	public static Map<Integer, Rule> getTargetSupportMap(Cycle cycle) {
		return cycle.equals(Cycle.ONE) ? targetSupportMap : targetSupportMap1;
	}
	
	public static Map<Integer, Integer> getTargetSupportNegatedMap() {
		return cycle.equals(Cycle.ONE) ? targetSupportNegatedMap : targetSupportNegatedMap1;
	}
	
	public static Map<Integer, List<Integer>> getNodeTargets() {
		return cycle.equals(Cycle.ONE) ? nodeTargets : nodeTargets1;
	}

	public static long getForestCreationTime() {
		return forestCreationTime;
	}

	public static void setForestCreationTime(long time) {
		forestCreationTime = time;
	}
	
	public static void clearTargetSupportNegatedMap() {
		targetSupportNegatedMap.clear();
		targetSupportNegatedMap1.clear();
	}
	
	public static void clearNodeTargets() {
		nodeTargets.clear();
		nodeTargets1.clear();
	}
	
	public static void clearTargetNumberOfOnesMap() {
		targetSupportMap.clear();
		targetSupportMap1.clear();
	}
	
	public static void clearAll() {
		targetSupportNegatedMap.clear();
		targetSupportNegatedMap1.clear();
		nodeTargets.clear();
		nodeTargets1.clear();
		targetSupportMap.clear();
		targetSupportMap1.clear();
		mapaJaccardova.clear();
		mapaJaccardova1.clear();
		idRuleMap.clear();
		idRuleMap1.clear();
		mapaPVrijednosti.clear();
		mapaPVrijednosti1.clear();
	}
}
