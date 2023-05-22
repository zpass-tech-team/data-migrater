package io.mosip.packet.core.constant;

public enum DBTypes {
    MSSQL("com.microsoft.sqlserver.jdbc.SQLServerDriver", "jdbc:sqlserver://%s:%s;sslProtocol=TLSv1.2;databaseName=%s;Trusted_Connection=True;"),
    ORACLE("oracle.jdbc.driver.OracleDriver", "jdbc:oracle:thin:@%s:%s:%s"),
    MYSQL("com.mysql.jdbc.Driver", "jdbc:mysql://%s:%s/%s"),
    POSTGRESQL("org.postgresql.Driver", "jdbc:postgresql://%s:%s/%s?useSSL=false");

    private final String driver;
    private final String driverUrl;

    DBTypes(String driver, String driverUrl) {
        this.driver = driver;
        this.driverUrl = driverUrl;
    }

    public String getDriver() {
        return driver;
    }

    public String getDriverUrl() {
        return driverUrl;
    }
}
