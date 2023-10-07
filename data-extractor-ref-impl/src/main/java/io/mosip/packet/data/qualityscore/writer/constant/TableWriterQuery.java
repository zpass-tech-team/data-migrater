package io.mosip.packet.data.qualityscore.writer.constant;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;

@Component
@Getter
public class TableWriterQuery {

    private static LinkedHashMap<TableWriterConstant, LinkedHashMap<String, String>> analysisMap;

    public TableWriterQuery() {
        analysisMap = new LinkedHashMap<>();

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
    }

    public static LinkedHashMap<String, String> getQueries(TableWriterConstant constant) {
        return analysisMap.get(constant);
    }
}
