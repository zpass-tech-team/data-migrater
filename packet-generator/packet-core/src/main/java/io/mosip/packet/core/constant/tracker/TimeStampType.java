package io.mosip.packet.core.constant.tracker;

public enum TimeStampType {

    MSSQL("DATETIME"),
    ORACLE("TIMESTAMP"),
    MYSQL("TIMESTAMP"),
    POSTGRESQL("TIMESTAMP");


    private final String type;

    TimeStampType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
