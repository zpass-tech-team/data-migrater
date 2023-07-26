package io.mosip.packet.core.util;

import com.opencsv.CSVWriter;
import io.mosip.packet.core.exception.ApisResourceAccessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;

@Component
public class CSVFileWriter {
    private static CSVWriter WRITER;
    private static Boolean isHeaderWritten = false;
    private LinkedHashMap<String, String> map = new LinkedHashMap<>();

    @Autowired
    private CommonUtil commonUtil;

    static {
        try {
            File file = new File("./CSVData.csv");

            if(!file.exists())
                file.createNewFile();
            else {
                file.renameTo(new File("./CSVData_" + (new SimpleDateFormat("yyyyMMddhhmmss")).format(new Date()) +".csv"));
                file.createNewFile();
            }

            WRITER = new CSVWriter(new FileWriter(file));
            isHeaderWritten = false;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public HashMap getCSVDataMap() throws ApisResourceAccessException {
        if(map.size() <= 2) {
            map.put("ref_id", null);
            map.put("reg_no", null);

            for(Object ob : commonUtil.getBioAttributesforAll())
                map.put(ob.toString(), null);
        }

        return (HashMap) map.clone();
    }

    public static synchronized void  writeCSVData(HashMap<String, String> csvMap) throws IOException {
        if(!isHeaderWritten) {
            WRITER.writeNext(csvMap.keySet().toArray(new String[0]));
            isHeaderWritten = true;
        }
        WRITER.writeNext(csvMap.values().toArray(new String[0]));
        WRITER.flush();
        csvMap.clear();
    }
}
