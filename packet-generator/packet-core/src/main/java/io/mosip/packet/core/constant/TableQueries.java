package io.mosip.packet.core.constant;

import org.springframework.stereotype.Component;

import java.util.HashMap;

@Component
public class TableQueries {;
    private static HashMap<String, HashMap<DBTypes, String>> queryMap;

    public TableQueries() {
        HashMap<DBTypes, String> inserQuerytMap = new HashMap<>();
        inserQuerytMap.put(DBTypes.MYSQL, "INSERT INTO <TABLE_NAME> (SESSION_KEY, OFFSET_VALUE ) VALUES ('<SESSION_ID>', '<VALUE>') ON DUPLICATE KEY UPDATE SESSION_KEY = '<SESSION_ID>', OFFSET_VALUE = '<VALUE>'");
        inserQuerytMap.put(DBTypes.POSTGRESQL, "INSERT INTO <TABLE_NAME> ( SESSION_KEY, OFFSET_VALUE ) VALUES ('<SESSION_ID>', '<VALUE>') ON CONFLICT(SESSION_KEY) DO UPDATE SET SESSION_KEY = '<SESSION_ID>', OFFSET_VALUE = '<VALUE>'");
        inserQuerytMap.put(DBTypes.ORACLE, "MERGE INTO <TABLE_NAME> T USING (SELECT 1 FROM DUAL) C ON (T.SESSION_KEY = '<SESSION_ID>') WHEN NOT MATCHED THEN INSERT (SESSION_KEY, OFFSET_VALUE) values('<SESSION_ID>', <VALUE>) WHEN MATCHED THEN UPDATE SET OFFSET_VALUE = '<VALUE>'");
        inserQuerytMap.put(DBTypes.MSSQL, "IF EXISTS (SELECT * FROM <TABLE_NAME> WHERE SESSION_KEY = '<SESSION_ID>') UPDATE <TABLE_NAME> SET SESSION_KEY = '<SESSION_ID>', OFFSET_VALUE = '<VALUE>' WHERE SESSION_KEY = '<SESSION_ID>' ELSE INSERT INTO <TABLE_NAME> (SESSION_KEY, OFFSET_VALUE) VALUES('<SESSION_ID>', <VALUE>)");
        queryMap = new HashMap<>();
        queryMap.put(RegistrationConstants.OFFSET_TRACKER_TABLE_NAME, inserQuerytMap);
    }

    public static String getInsertQueries(String  tableName, DBTypes dbType) {
        return queryMap.get(tableName).get(dbType);
    }
}
