package io.mosip.packet.data.convertion;

import io.mosip.packet.core.spi.BioDocApiFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class BioDocDataConverter implements BioDocApiFactory {
    @Override
    public Map<String, byte[]> getBioData(byte[] byteval, String fieldName) {
        Map<String, byte[]> map = new HashMap<>();
        map.put(fieldName, byteval);
        return map;
    }

    @Override
    public Map<String, byte[]> getDocData(byte[] byteval, String fieldName) {
        Map<String, byte[]> map = new HashMap<>();
        map.put(fieldName, byteval);
        return map;
    }
}
