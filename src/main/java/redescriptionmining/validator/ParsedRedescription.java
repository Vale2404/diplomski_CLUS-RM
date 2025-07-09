package redescriptionmining.validator;

import si.ijs.kt.clus.util.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

public class ParsedRedescription {
    String[] supportInstances;
    String[] unionInstances;
    List<List<ParsedRule>> parsedRulesW1;
    List<List<ParsedRule>> parsedRulesW2;

    ParsedRedescription(String[] supportInstances, String[] unionInstances, List<List<ParsedRule>> parsedRulesW1,
                        List<List<ParsedRule>> parsedRulesW2) {
        this.supportInstances = supportInstances;
        this.unionInstances = unionInstances;
        this.parsedRulesW1 = parsedRulesW1;
        this.parsedRulesW2 = parsedRulesW2;
    }
}
