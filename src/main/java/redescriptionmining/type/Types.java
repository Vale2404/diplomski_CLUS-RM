package redescriptionmining.type;

public enum Types {

    FOREST("-forest"),
    RULES("-rules");

    private final String code;

    private Types(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }
}
