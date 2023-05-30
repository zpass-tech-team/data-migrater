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

            WRITER.writeNext(map.keySet().toArray(new String[0]));
        }

        return (HashMap) map.clone();
    }

    public static synchronized void  writeCSVData(String[] list) throws IOException {
        WRITER.writeNext(list);
        WRITER.flush();
    }
}
