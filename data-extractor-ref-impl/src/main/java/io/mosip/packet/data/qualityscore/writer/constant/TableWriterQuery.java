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
        rowQueryMap.put("Excellent Quality Biometrics", "SELECT COUNT(*) FROM <$TABLE_NAME> WHERE <$COLUMN_NAME> >= 80");
        rowQueryMap.put("Very Good Quality Biometrics", "SELECT COUNT(*) FROM <$TABLE_NAME> WHERE <$COLUMN_NAME> > 60 AND <$COLUMN_NAME> <= 80");
        rowQueryMap.put("Good Quality Biometrics", "SELECT COUNT(*) FROM <$TABLE_NAME> WHERE <$COLUMN_NAME> > 40 AND <$COLUMN_NAME> <= 60");
        rowQueryMap.put("Average Quality Biometrics", "SELECT COUNT(*) FROM <$TABLE_NAME> WHERE <$COLUMN_NAME> > 20 AND <$COLUMN_NAME> <= 40");
        rowQueryMap.put("Bad Quality Biometrics", "SELECT COUNT(*) FROM <$TABLE_NAME> WHERE <$COLUMN_NAME> <=  20");
        rowQueryMap.put("No Biometric Captured", "SELECT COUNT(*) FROM <$TABLE_NAME> WHERE <$COLUMN_NAME> IS NULL");
        rowQueryMap.put("Min Quality of Biometric Captured", "SELECT MIN(<$COLUMN_NAME>) FROM <$TABLE_NAME>");
        rowQueryMap.put("Max Quality of Biometric Captured", "SELECT MAX(<$COLUMN_NAME>) FROM <$TABLE_NAME>");
        rowQueryMap.put("Average Quality of Biometric Captured", "SELECT AVG(<$COLUMN_NAME>) FROM <$TABLE_NAME>");

        analysisMap.put(TableWriterConstant.ROW_ANALYSER_QUERIES, rowQueryMap);

        LinkedHashMap<DBTypes, String> inserQuerytMap = new LinkedHashMap<>();
        inserQuerytMap.put(DBTypes.MYSQL, "INSERT INTO <TABLE_NAME> ( <COLUMN_NAME> ) VALUES ( <VALUES>) ON DUPLICATE KEY UPDATE <UPDATE_COLUMN_NAME>");
        inserQuerytMap.put(DBTypes.POSTGRESQL, "INSERT INTO <TABLE_NAME> ( <COLUMN_NAME> ) VALUES ( <VALUES>) ON CONFLICT(REF_ID) DO UPDATE SET <UPDATE_COLUMN_NAME>");
        inserQuerytMap.put(DBTypes.ORACLE, "UPSERT INTO <TABLE_NAME> ( <COLUMN_NAME> ) VALUES ( <VALUES>)");
        inserQuerytMap.put(DBTypes.MSSQL, "IF EXISTS (SELECT * FROM <TABLE_NAME> WHERE REF_ID = <REF_ID>) UPDATE <TABLE_NAME> SET <UPDATE_COLUMN_NAME> WHERE REF_ID = <REF_ID> ELSE INSERT INTO <TABLE_NAME> (<COLUMN_NAME>) VALUES(<VALUES>)");
        insertMap.put(WRITER_TABLE_NAME, inserQuerytMap);
    }

    public static LinkedHashMap<String, String> getQueries(TableWriterConstant constant) {
        return analysisMap.get(constant);
    }

    public static LinkedHashMap<DBTypes, String> getInsertQueries(String  tableName) {
        return insertMap.get(tableName);
    }
}
