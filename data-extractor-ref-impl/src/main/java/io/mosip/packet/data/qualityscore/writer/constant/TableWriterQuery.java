package io.mosip.packet.data.qualityscore.writer.constant;

import io.mosip.packet.core.constant.DBTypes;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;

@Component
@Getter
public class TableWriterQuery {

    private static LinkedHashMap<TableWriterConstant, LinkedHashMap<String, String>> analysisMap;
    private static LinkedHashMap<String, LinkedHashMap<DBTypes, String>> insertMap;
    private static String WRITER_TABLE_NAME = "TB_T_QUALITY_SCORE";


    public TableWriterQuery() {
        analysisMap = new LinkedHashMap<>();
        insertMap=new LinkedHashMap<>();

        LinkedHashMap<String, String> rowQueryMap = new LinkedHashMap<>();
        rowQueryMap.put("Excellent Quality Biometrics", "SELECT COUNT(*) FROM <$TABLE_NAME> WHERE <$COLUMN_NAME> >= 81");
        rowQueryMap.put("Very Good Quality Biometrics", "SELECT COUNT(*) FROM <$TABLE_NAME> WHERE <$COLUMN_NAME> >= 61 AND <$COLUMN_NAME> < 81");
        rowQueryMap.put("Good Quality Biometrics", "SELECT COUNT(*) FROM <$TABLE_NAME> WHERE <$COLUMN_NAME> >= 41 AND <$COLUMN_NAME> < 61");
        rowQueryMap.put("Average Quality Biometrics", "SELECT COUNT(*) FROM <$TABLE_NAME> WHERE <$COLUMN_NAME> >= 21 AND <$COLUMN_NAME> < 41");
        rowQueryMap.put("Bad Quality Biometrics", "SELECT COUNT(*) FROM <$TABLE_NAME> WHERE <$COLUMN_NAME> < 21");
        rowQueryMap.put("No Biometric Captured", "SELECT COUNT(*) FROM <$TABLE_NAME> WHERE <$COLUMN_NAME> IS NULL");
        rowQueryMap.put("Min Quality of Biometric Captured", "SELECT MIN(<$COLUMN_NAME>) FROM <$TABLE_NAME>");
        rowQueryMap.put("Max Quality of Biometric Captured", "SELECT MAX(<$COLUMN_NAME>) FROM <$TABLE_NAME>");
        rowQueryMap.put("Average Quality of Biometric Captured", "SELECT AVG(<$COLUMN_NAME>) FROM <$TABLE_NAME>");

        analysisMap.put(TableWriterConstant.ROW_ANALYSER_QUERIES, rowQueryMap);

        LinkedHashMap<DBTypes, String> inserQuerytMap = new LinkedHashMap<>();
        inserQuerytMap.put(DBTypes.MYSQL, "INSERT INTO <TABLE_NAME> (REF_ID, <COLUMN_NAME> ) VALUES (<REF_ID>, <VALUES>) ON DUPLICATE KEY UPDATE REF_ID = <REF_ID>, <UPDATE_COLUMN_NAME>");
        inserQuerytMap.put(DBTypes.POSTGRESQL, "INSERT INTO <TABLE_NAME> ( REF_ID, <COLUMN_NAME> ) VALUES (<REF_ID>, <VALUES>) ON CONFLICT(REF_ID) DO UPDATE SET REF_ID = <REF_ID>, <UPDATE_COLUMN_NAME>");
        inserQuerytMap.put(DBTypes.ORACLE, "MERGE INTO <TABLE_NAME> T USING (SELECT 1 FROM DUAL) C ON (T.REF_ID = '<REF_ID>') WHEN NOT MATCHED THEN INSERT (REF_ID, <COLUMN_NAME>) values(<REF_ID>, <VALUES>) WHEN MATCHED THEN UPDATE SET REF_ID = <REF_ID>, <UPDATE_COLUMN_NAME>");
        inserQuerytMap.put(DBTypes.MSSQL, "IF EXISTS (SELECT * FROM <TABLE_NAME> WHERE REF_ID = <REF_ID>) UPDATE <TABLE_NAME> SET REF_ID = <REF_ID>, <UPDATE_COLUMN_NAME> WHERE REF_ID = <REF_ID> ELSE INSERT INTO <TABLE_NAME> (REF_ID, <COLUMN_NAME>) VALUES(<REF_ID>, <VALUES>)");
        insertMap.put(WRITER_TABLE_NAME, inserQuerytMap);
    }

    public static LinkedHashMap<String, String> getQueries(TableWriterConstant constant) {
        return analysisMap.get(constant);
    }

    public static LinkedHashMap<DBTypes, String> getInsertQueries(String  tableName) {
        return insertMap.get(tableName);
    }
}
