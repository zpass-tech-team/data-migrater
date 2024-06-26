package io.mosip.packet.data.convertion;

import io.mosip.packet.core.spi.BioDocApiFactory;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Component
@ConditionalOnProperty(value = "mosip.packet.bio.doc.data.converter.classname", havingValue = "BioDocDataFetchFromFile")
public class BioDocDataFetchFromLocal implements BioDocApiFactory {

    private static final Logger logger = LoggerFactory.getLogger(BioDocDataFetchFromLocal.class);

    @Override
    public Map<String, byte[]> getBioData(byte[] byteval, String fieldName) throws Exception {

        Map<String, byte[]> map = new HashMap<>();
        map.put(fieldName, getFileByteArray(byteval));
        return map;
    }

    @Override
    public Map<String, byte[]> getDocData(byte[] byteval, String fieldName) throws Exception {

        Map<String, byte[]> map = new HashMap<>();
        map.put(fieldName, getFileByteArray(byteval));
        return map;
    }

    private byte[] getFileByteArray(byte[] byteval) throws Exception {

        if (byteval != null) {
            String filepath = new String(byteval, StandardCharsets.UTF_8);
            filepath = filepath.replaceAll("'","");
            File inputFile = new File(filepath);
            if (inputFile == null || !inputFile.exists() || !inputFile.isFile()) {
                logger.error("File not found or not a file: {}", filepath);
                throw new RuntimeException("File not found: " + filepath);
            }
            return new FileInputStream(inputFile).readAllBytes();
        }
        return null;
    }
}