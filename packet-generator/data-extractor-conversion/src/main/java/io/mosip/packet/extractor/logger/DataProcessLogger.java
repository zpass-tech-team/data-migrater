package io.mosip.packet.extractor.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DataProcessLogger {
    private DataProcessLogger(){
    }

    public static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }
}
