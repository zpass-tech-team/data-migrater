package io.mosip.packet.core.logger;


import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.logger.logback.factory.Logfactory;

public final class DataProcessLogger {
    private DataProcessLogger(){
    }

    public static Logger getLogger(Class<?> clazz) {
        return Logfactory.getSlf4jLogger(clazz);
    }
}
