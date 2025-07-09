package redescriptionmining.validator;

import java.util.ArrayList;
import java.util.List;

public class ParsedRule {
    public String attributeName;
    public List<String> categories;
    public List<String> negativeCategories;
    public Float upperBound;
    public Float lowerBound;
    public Boolean isNegative = false;

    public ParsedRule(String attributeName, List<String> categories, Float upperBound, Float lowerBound) {
        this.attributeName = attributeName;
        this.categories = categories;
        this.upperBound = upperBound;
        this.lowerBound = lowerBound;
    }

    public String toString() {
        String categories = this.categories != null ? String.join(", ", this.categories) : "null";
        return "Attribute: " + attributeName + "(" + lowerBound + ", " + upperBound
                + ", [" + categories + "], " + "isNegative: " + isNegative + ")";
    }

    private static final String lessThanSymbol = "<=";
    private static final String biggerThanSymbol = ">=";
    private static final String isOfCategorySymbol = "=";

    public static ParsedRule parseRule(String[] ruleString) {
        String attributeName;
        Float biggerThan = null;
        Float lessThan = null;
        List<String> isOfCategory = new ArrayList<>();

        attributeName = ruleString[0];
        for (int index = 1; index < ruleString.length; index+=1) {
            if (ruleString[index] == null) break;
            switch (ruleString[index]) {
                case lessThanSymbol: {
                    lessThan = Float.parseFloat(ruleString[index+1]);
                    break;
                }
                case biggerThanSymbol: {
                    biggerThan = Float.parseFloat(ruleString[index+1]);
                    break;
                }
                case isOfCategorySymbol: {
                    isOfCategory.add(ruleString[index+1]);
                    break;
                }
                default: {
                    break;
                }
            }
        }

        return new ParsedRule(attributeName, isOfCategory, lessThan, biggerThan);
    }

    public static ParsedRule parseNegativeRule(String[] ruleString) {
        ParsedRule parsedRule = parseRule(ruleString);
        if (!parsedRule.categories.isEmpty()) {
            parsedRule.negativeCategories = parsedRule.categories;
        }

        parsedRule.isNegative = true;

        return parsedRule;
    }
}
