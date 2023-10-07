package io.mosip.packet.core.spi;

import java.util.HashMap;

public interface QualityWriterFactory {
    HashMap<String, String> getDataMap() throws Exception;
    void writeQualityData(HashMap<String, String> csvMap) throws Exception;
}
