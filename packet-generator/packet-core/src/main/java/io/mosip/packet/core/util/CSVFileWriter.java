package io.mosip.packet.core.util;

import com.opencsv.CSVWriter;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.packet.core.exception.ApisResourceAccessException;
import io.mosip.packet.core.logger.DataProcessLogger;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class CSVFileWriter {
    private static File csvFile;
    private static Boolean isHeaderWritten = false;
    private static LinkedHashMap<String, String> map = new LinkedHashMap<>();
    private static Logger LOGGER = DataProcessLogger.getLogger(CSVFileWriter.class);

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

    public static synchronized void  writeCSVData(HashMap<String, String> csvMap) throws IOException, InterruptedException {
        CSVWriter WRITER = null;
        try {
            WRITER = new CSVWriter(new FileWriter(csvFile, true));

            if(!isHeaderWritten) {
                WRITER.writeNext(csvMap.keySet().toArray(new String[0]));
                isHeaderWritten = true;
            }

            if(!map.keySet().containsAll(csvMap.keySet())) {
                for(String key : csvMap.keySet())
                    if(!map.containsKey(key))
                        map.put(key, null);
            }

            List<String> valuesList = new ArrayList<>();
            for(String key : map.keySet())
                valuesList.add(csvMap.get(key));

            WRITER.writeNext(valuesList.toArray(new String[0]));
            WRITER.flush();
            csvMap.clear();
        } catch (FileNotFoundException e) {
            LOGGER.warn("File : " + csvFile.getAbsolutePath() + " not found or Opened by someother Program. Request you to close and continue");
            System.out.println("File : " + csvFile.getAbsolutePath() + " not found or Opened by someother Program. Request you to close and continue");
            Thread.sleep(10000);
            writeCSVData(csvMap);
        } finally {
            if(WRITER != null)
                WRITER.close();
        }
;

    }

    @PreDestroy
    public void destroy() {
        CSVWriter WRITER = null;
        try {
            WRITER = new CSVWriter(new FileWriter(csvFile, true));
            WRITER.writeNext(map.keySet().toArray(new String[0]));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(WRITER != null) {
                try {
                    WRITER.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
