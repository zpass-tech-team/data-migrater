package io.mosip.packet.core.util;

import com.opencsv.CSVWriter;
import io.mosip.packet.core.exception.ApisResourceAccessException;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;

@Component
public class CSVFileWriter {
    private static File csvFile;
    private static Boolean isHeaderWritten = false;
    private static LinkedHashMap<String, String> map = new LinkedHashMap<>();

    @Autowired
    private CommonUtil commonUtil;

    static {
        try {
            csvFile = new File("./CSVData.csv");

            if(!csvFile.exists())
                csvFile.createNewFile();
            else {
                csvFile.renameTo(new File("./CSVData_" + (new SimpleDateFormat("yyyyMMddhhmmss")).format(new Date()) +".csv"));
                csvFile.createNewFile();
            }

            isHeaderWritten = false;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public HashMap getCSVDataMap() throws ApisResourceAccessException, IOException, ParseException {
        if(map.size() <= 2) {
            map.put("ref_id", null);
            map.put("reg_no", null);

            for(Object ob : commonUtil.getBioAttributesforAll())
                map.put(ob.toString(), null);
        }

        return (HashMap) map.clone();
    }

    public static synchronized void  writeCSVData(HashMap<String, String> csvMap) throws IOException {
        CSVWriter WRITER = null;
        try {
            WRITER = new CSVWriter(new FileWriter(csvFile));

            if(!isHeaderWritten) {
                WRITER.writeNext(csvMap.keySet().toArray(new String[0]));
                isHeaderWritten = true;
            }

            if(!map.keySet().containsAll(csvMap.keySet())) {
                map.clear();
                for(String key : csvMap.keySet())
                    map.put(key, null);
            }

            WRITER.writeNext(csvMap.values().toArray(new String[0]));
            WRITER.flush();
            csvMap.clear();
        } finally {
            if(WRITER != null)
                WRITER.close();
        }
;

    }
}
