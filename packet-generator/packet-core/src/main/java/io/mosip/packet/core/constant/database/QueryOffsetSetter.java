package io.mosip.packet.core.constant.database;

public enum QueryOffsetSetter {
    MSSQL("OFFSET %s ROWS"),
    ORACLE("OFFSET %s ROWS"),
    MYSQL("OFFSET %s"),
    POSTGRESQL("OFFSET %s");

    private final String type;

    QueryOffsetSetter(String type) {
        this.type = type;
    }

    public String getValue(Long val) {
        return String.format(type, val);
    }
}
