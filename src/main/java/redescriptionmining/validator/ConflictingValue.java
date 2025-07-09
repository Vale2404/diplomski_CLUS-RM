package redescriptionmining.validator;

public class ConflictingValue {
    public String instance;
    public String value;
    public ParsedRule conflictingRule;
    public Reason reason;

    ConflictingValue(String instance, String value, ParsedRule foundRule) {
        this.instance = instance;
        this.value = value;
        this.conflictingRule = foundRule;
    }

    public String toString() {
        return "Conflicting value found (" + reason.name() + "): \"" + instance.replace("\"", "") + "\"(" + value + ")\n"
                + conflictingRule.toString();
    }

    public enum Reason {
        SUPPORT,
        UNION,
        COMPLEMENT,
        UNION_COMPLEMENT
    }
}
