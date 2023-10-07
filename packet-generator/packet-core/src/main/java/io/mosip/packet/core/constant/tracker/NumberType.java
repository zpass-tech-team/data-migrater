package io.mosip.packet.core.constant.tracker;

public enum NumberType {
    MSSQL("NUMERIC"),
    ORACLE("NUMERIC"),
    MYSQL("NUMERIC"),
    POSTGRESQL("NUMERIC");

    private final String type;

    NumberType(String type) {
        this.type = type;
    }

    public String getValue(String val, String precision) {
        return type + String.format("(%s, %s)", new Object[] {val, precision});
    }
}
