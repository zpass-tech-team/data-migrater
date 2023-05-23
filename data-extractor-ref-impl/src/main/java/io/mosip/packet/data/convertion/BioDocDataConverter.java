package io.mosip.packet.data.convertion;

import io.mosip.packet.core.spi.BioDocApiFactory;
import org.springframework.stereotype.Component;

@Component
public class BioDocDataConverter implements BioDocApiFactory {
    @Override
    public byte[] getBioData(byte[] byteval, String fieldName) {
        return byteval;
    }

    @Override
    public byte[] getDocData(byte[] byteval, String fieldName) {
        return byteval;
    }
}
