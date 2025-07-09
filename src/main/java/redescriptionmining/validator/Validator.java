package redescriptionmining.validator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Validator {
    private static final Pattern instancePattern = Pattern.compile("\"[^\"]+\"");
    private static final Map<String, Attribute> allAttributes = new HashMap<>();
    private static final List<String> allInstances = new ArrayList<>();
    private static int numElemsView1;
    private static int view = 0;
    private static int totalRedescriptions = 0;
    private static boolean unknown = false;


    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.err.println("Usage: java Validator <view1> <view2> <redescriptions>");
            System.exit(1);
        }

        List<String> linesW1 = Files.readAllLines(Paths.get(args[0]));
        List<String> linesW2 = Files.readAllLines(Paths.get(args[1]));
        List<String> linesRedescriptions = Files.readAllLines(Paths.get(args[2]));
        getAllInstances(linesW1);

        List<ParsedRedescription> parsedRedescriptions = parseRedescriptionsFile(linesRedescriptions);
        parseDataFiles(linesW1, linesW2);
        ConflictingValue notValidRule = checkAllRules(linesW1, linesW2, parsedRedescriptions);

        if (notValidRule != null) {
            System.out.println("ERROR! Not all rules are valid.\n"
                    + "Example:\n"
                    + notValidRule
            );
        } else {
            System.out.println("SUCCESS! All rules are valid!");
        }
    }


    public static List<ParsedRedescription> parseRedescriptionsFile(List<String> lines) {
        List<ParsedRedescription> parsedRedescriptions = new ArrayList<>();

        String[] supportInstances = new String[0];
        String[] unionInstances = new String[0];
        List<List<ParsedRule>> parsedRulesW1 = new ArrayList<>();
        List<List<ParsedRule>> parsedRulesW2 = new ArrayList<>();
        boolean isSupportLine = false;
        boolean isUnionLine = false;

        for (String line : lines) {
            line = line.trim();

            if (line.startsWith("W1R:")) {
                parsedRulesW1 = extractRules(line.substring(5));
            } else if (line.startsWith("W2R:")) {
                parsedRulesW2 = extractRules(line.substring(5));
            } else if (line.startsWith("Covered") || isSupportLine) {
                if (isSupportLine) {
                    supportInstances = extractInstances(line);
                    isSupportLine = false;
                } else {
                    isSupportLine = true;
                }
            } else if (line.startsWith("Union") || isUnionLine) {
                if (isUnionLine) {
                    unionInstances = extractInstances(line);
                    isUnionLine = false;
                } else {
                    isUnionLine = true;
                }
            } else if (line.startsWith("Rules:")) {
                if (supportInstances.length == 0) continue;
                totalRedescriptions += 1;
                parsedRedescriptions.add(
                        new ParsedRedescription(supportInstances, unionInstances, parsedRulesW1, parsedRulesW2)
                );
            }
        }

        return parsedRedescriptions;
    }


    private static List<List<ParsedRule>> extractRules(String line) {
        String[] lineParts = line.split("\\s+");
        boolean isPositive = true;
        boolean isContinuous = false;
        String[] ruleStringParts = new String[5];
        int i = 0;
        List<ParsedRule> andRules = new ArrayList<>();
        List<List<ParsedRule>> orRules = new ArrayList<>();

        for (String linePart : lineParts) {
            if (linePart.equals("NOT")) {
                isPositive = false;
                if (i == 0) continue;
                i = 0;
            } else if (linePart.equals("(")) {
                i = 0;
                isContinuous = true;
            } else if (linePart.equals(")")) {
                i = 0;
                andRules.add(parseRuleStringParts(ruleStringParts, isPositive));
                orRules.add(andRules);
                andRules = new ArrayList<>();
                isContinuous = false;
                isPositive = true;
            } else if (linePart.equals("AND")) {
                if (i == 0) continue;
                i = 0;
                andRules.add(parseRuleStringParts(ruleStringParts, isPositive));
                if (!isContinuous) {
                    isPositive = true;
                } else {
                    if (!isPositive) {
                        orRules.add(andRules);
                        andRules = new ArrayList<>();
                    }
                }
            } else if (linePart.equals("OR")) {
                if (i == 0) continue;
                i = 0;
                andRules.add(parseRuleStringParts(ruleStringParts, isPositive));
                if (!isContinuous) {
                    orRules.add(andRules);
                    andRules = new ArrayList<>();
                    isPositive = true;
                }
            } else {
                ruleStringParts[i] = linePart;
                i += 1;
            }
        }

        if (i != 0) andRules.add(parseRuleStringParts(ruleStringParts, isPositive));
        if (!andRules.isEmpty()) orRules.add(andRules);
        return orRules;
    }


    private static ParsedRule parseRuleStringParts(String[] ruleStringParts, boolean isPositive) {
        if (isPositive) {
            return ParsedRule.parseRule(ruleStringParts);
        } else {
            return ParsedRule.parseNegativeRule(ruleStringParts);
        }
    }



    private static void getAllInstances(List<String> linesView) {
        for (String line : linesView) {
            if (!line.startsWith("\"")) continue;
            Matcher matcher = instancePattern.matcher(line);
            while (matcher.find()) {
                allInstances.add(matcher.group());
            }
        }
    }


    private static String[] extractInstances(String line) {
        line = line.replaceAll("\"", "");
        return line.split(" ");
    }


    private static void parseDataFiles(List<String> linesW1, List<String> linesW2) {
        for (String line : linesW1) {
            if(!parseDataFileLine(line)) break;
        }

        numElemsView1 = allAttributes.size();
        view = 1;
        for (String line : linesW2) {
            if(!parseDataFileLine(line)) break;
        }
    }



    private static boolean parseDataFileLine(String line) {
        line = line.trim();

        if (line.startsWith("@attribute") || line.startsWith("@ATTRIBUTE")) {
            String preprocessedLine = line.replaceAll("\\s+", " ");
            String attributeName = preprocessedLine.split("\\s")[1];
            String typeString = preprocessedLine.split("\\s")[2];
            allAttributes.put(attributeName, new Attribute(attributeName, allAttributes.size() + view, typeString));
        } else if (line.startsWith("@data") || line.startsWith("@DATA")) {
            return false;
        }

        return true;
    }


    private static ConflictingValue checkAllRules(List<String> linesW1, List<String> linesW2,
                                                                    List<ParsedRedescription> parsedRedescriptions) {
        ConflictingValue conflictingValue;
        float percent = totalRedescriptions / 100.0f;
        int i = 1;
        for (ParsedRedescription redescription : parsedRedescriptions) {
            if (i % percent < 1) System.out.println((i / percent) + "%");
            i += 1;
            conflictingValue = checkRedescription(linesW1, linesW2, redescription);
            if (conflictingValue != null) return conflictingValue;
        }

        return null;
    }


    private static ConflictingValue checkRedescription(List<String> linesW1, List<String> linesW2, ParsedRedescription redescription) {
        ConflictingValue conflictingValue = checkSupportRules(linesW1, linesW2, redescription);
        if (conflictingValue != null) return conflictingValue;

        conflictingValue = checkUnionRules(linesW1, linesW2, redescription);
        if (conflictingValue != null) return conflictingValue;

        return checkComplementRules(linesW1, linesW2, redescription);
    }


    private static ConflictingValue checkSupportRules(List<String> linesW1, List<String> linesW2, ParsedRedescription redescription) {
        ConflictingValue conflictingValue;

        for (String instance : redescription.supportInstances) {
            conflictingValue = checkOrRules(instance, redescription.parsedRulesW1, linesW1, 0);

            if (conflictingValue != null) {
                conflictingValue.reason = ConflictingValue.Reason.SUPPORT;
                return conflictingValue;
            }

            conflictingValue = checkOrRules(instance, redescription.parsedRulesW2, linesW2, 1);

            if (conflictingValue != null) {
                conflictingValue.reason = ConflictingValue.Reason.SUPPORT;
                return conflictingValue;
            }
        }

        return null;
    }


    private static ConflictingValue checkUnionRules(List<String> linesW1, List<String> linesW2, ParsedRedescription redescription) {
        for (String instance : redescription.unionInstances) {
            boolean isSupportInstance = false;
            for (String supportInstance : redescription.supportInstances) {
                if (instance.equals(supportInstance)) {
                    isSupportInstance = true;
                    break;
                }
            }
            if (isSupportInstance) continue;

            ConflictingValue conflictingValue;
            conflictingValue = checkOrRules(instance, redescription.parsedRulesW1, linesW1, 0);

            if (conflictingValue != null) {
                conflictingValue = checkOrRules(instance, redescription.parsedRulesW2, linesW2, 1);
                if (conflictingValue != null) {
                    conflictingValue.reason = ConflictingValue.Reason.UNION;
                    return conflictingValue;
                }
            } else {
                conflictingValue = checkOrRulesNegative(instance, redescription.parsedRulesW2, linesW2, 1);
                if (conflictingValue != null) {
                    conflictingValue.reason = ConflictingValue.Reason.UNION_COMPLEMENT;
                    return conflictingValue;
                }
            }
        }

        return null;
    }


    private static ConflictingValue checkComplementRules(List<String> linesW1, List<String> linesW2, ParsedRedescription redescription) {
        boolean isUnionInstance;
        for (String instance : allInstances) {
            isUnionInstance = false;
            for (String unionInstance : redescription.unionInstances) {
                if (instance.equals("\"" + unionInstance + "\"")) {
                    isUnionInstance = true;
                    break;
                }
            }
            if (isUnionInstance) continue;

            ConflictingValue conflictingValue;
            conflictingValue = checkOrRulesNegative(instance, redescription.parsedRulesW1, linesW1, 0);
            if (conflictingValue == null) {
                conflictingValue = checkOrRulesNegative(instance, redescription.parsedRulesW2, linesW2, 1);
            }

            if (conflictingValue != null) {
                conflictingValue.reason = ConflictingValue.Reason.COMPLEMENT;
                return conflictingValue;
            }
        }

        return null;
    }


    private static ConflictingValue checkOrRules(String instance, List<List<ParsedRule>> orRules, List<String> linesView, int view) {
        Integer firstRuleIndex = view == 0 ? 0 : numElemsView1;
        ConflictingValue conflictingValue = null;
        int size = orRules.size();
        for (String line : linesView) {
            if (!line.startsWith("\"" + instance + "\"")) continue;
            for (List<ParsedRule> andRules : orRules) {
                Map<Integer, ParsedRule> parsedRulePositionMap = resolveRulePositions(andRules);
                conflictingValue = checkAndRules(line, instance, parsedRulePositionMap, firstRuleIndex);
                if (unknown) {
                    unknown = false;
                    size -= 1;
                    continue;
                }
                if (conflictingValue == null) break;
            }
            break;
        }
        if (size < orRules.size()) {
            if (size == 0) return conflictingValue;
            else return null;
        }
        return conflictingValue;
    }


    private static ConflictingValue checkOrRulesNegative(String instance, List<List<ParsedRule>> orRules, List<String> linesView, int view) {
        Integer firstRuleIndex = view == 0 ? 0 : numElemsView1;
        ConflictingValue conflictingValue = null;
        for (String line : linesView) {
            if (!line.startsWith("\"" + instance.replace("\"", "") + "\"")) continue;
            for (List<ParsedRule> andRules : orRules) {
                Map<Integer, ParsedRule> parsedRulePositionMap = resolveRulePositions(andRules);
                conflictingValue = checkAndRulesNegative(line, instance, parsedRulePositionMap, firstRuleIndex);
                if (conflictingValue != null) break;
            }
            break;
        }
        return conflictingValue;
    }


    private static Map<Integer, ParsedRule> resolveRulePositions(List<ParsedRule> rulesList) {
        Map<Integer, ParsedRule> parsedRulePositionMap = new HashMap<>();
        for (ParsedRule rule : rulesList) {
            int position;
            try {
                position = allAttributes.get(rule.attributeName).position;
            } catch (Exception e) {
                System.out.println(rule.attributeName);
                throw e;
            }
            parsedRulePositionMap.put(position, rule);
        }
        return parsedRulePositionMap;
    }


    private static ConflictingValue checkAndRules(String line, String instance, Map<Integer, ParsedRule> parsedRulePositionMap, Integer firstRuleIndex) {
        String[] lineElements = line.split(",");
        Integer lastRulePosition = firstRuleIndex < numElemsView1 ? numElemsView1 : Integer.MAX_VALUE;
        Boolean isValid;
        int size = parsedRulePositionMap.size();
        for (Integer foundRulePosition : parsedRulePositionMap.keySet()) {
            if (foundRulePosition >= lastRulePosition || foundRulePosition < firstRuleIndex) continue;

            ParsedRule foundRule = parsedRulePositionMap.get(foundRulePosition);
            String foundValue = lineElements[foundRulePosition - firstRuleIndex].trim();
            isValid = isRuleValid(foundRule, foundValue);

            if (isValid == null) {
                size -= 1;
                if (size == 0) unknown = true;
                return new ConflictingValue(instance, foundValue, foundRule);
            }
            else if (!isValid) return new ConflictingValue(instance, foundValue, foundRule);
        }

        return null;
    }


    private static ConflictingValue checkAndRulesNegative(String line, String instance, Map<Integer, ParsedRule> parsedRulePositionMap, Integer firstRuleIndex) {
        String[] lineElements = line.split(",");
        Integer lastRulePosition = firstRuleIndex < numElemsView1 ? numElemsView1 : Integer.MAX_VALUE;
        ConflictingValue conflictingValue = null;
        for (Integer foundRulePosition : parsedRulePositionMap.keySet()) {
            if (foundRulePosition >= lastRulePosition || foundRulePosition < firstRuleIndex) continue;

            ParsedRule foundRule = parsedRulePositionMap.get(foundRulePosition);
            String foundValue = lineElements[foundRulePosition - firstRuleIndex].trim();
            Boolean isValid = isRuleValid(foundRule, foundValue);

            if (isValid == null) return null;
            if (isValid) {
                conflictingValue = new ConflictingValue(instance, foundValue, foundRule);
            } else return null;
        }

        return conflictingValue;
    }


    private static Boolean isRuleValid(ParsedRule rule, String value) {
        if (rule.isNegative && value.equals("?")) return null;
        else if (value.equals("?")) return false;
        Attribute attribute = allAttributes.get(rule.attributeName);

        if (attribute.type == Attribute.AttributeType.NUMERIC) {
            Float resolvedValue = Float.valueOf(value);
            if (rule.isNegative) {
                return rule.lowerBound > resolvedValue || rule.upperBound < resolvedValue;
            } else {
                return rule.lowerBound <= resolvedValue && rule.upperBound >= resolvedValue;
            }
        } else if (attribute.type == Attribute.AttributeType.CATEGORIC) {
            if (rule.isNegative)
                return !rule.categories.contains(value);
            else return rule.categories.contains(value);
        }

        return true;
    }
}
