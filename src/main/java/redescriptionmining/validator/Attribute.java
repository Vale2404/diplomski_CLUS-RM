package redescriptionmining.validator;

public class Attribute {
    String name;
    int position;
    AttributeType type;
    String[] categories;

    Attribute(String name, int position, String typeString) {
        this.name = name;
        this.position = position;
        this.type = getType(typeString);
    }

    private AttributeType getType(String type) {
        if (type.equals("numeric")) return AttributeType.NUMERIC;

        type = type.replace("{", "").replace("}", "");
        this.categories = type.split(",");
        return AttributeType.CATEGORIC;
    }

    public enum AttributeType {
        NUMERIC,
        CATEGORIC
    }
}
