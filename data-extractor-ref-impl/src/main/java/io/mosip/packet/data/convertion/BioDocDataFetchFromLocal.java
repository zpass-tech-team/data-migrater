package io.mosip.packet.data.convertion;

import io.mosip.packet.core.spi.BioDocApiFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Component
@ConditionalOnProperty(value = "mosip.packet.bio.doc.data.converter.classname", havingValue = "BioDocDataFetchFromLocal")
public class BioDocDataFetchFromLocal implements BioDocApiFactory {
    @Override
    public Map<String, byte[]> getBioData(byte[] byteval, String fieldName) throws IOException {
        File file = new File(new String(byteval, StandardCharsets.UTF_8));
        FileInputStream is = new FileInputStream(file);
        Map<String, byte[]> map = new HashMap<>();
        map.put(fieldName, is.readAllBytes());
        return map;
    }

    @Override
    public Map<String, byte[]> getDocData(byte[] byteval, String fieldName) {
        Map<String, byte[]> map = new HashMap<>();
        map.put(fieldName, byteval);
        return map;
    }
}
