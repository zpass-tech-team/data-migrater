package io.mosip.packet.core.service.thread;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.packet.core.constant.FieldCategory;
import io.mosip.packet.core.logger.DataProcessLogger;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Setter
public class BaseThreadController implements Runnable {
    protected static final Logger LOGGER = DataProcessLogger.getLogger(BaseThreadController.class);
    private Map<FieldCategory, LinkedHashMap<String, Object>> dataHashMap;
    private ResultSetter setter;
    private ThreadProcessor processor;
    private String registrationId;

    @Override
    public void run() {
        processor.processData(setter, dataHashMap, registrationId);
    }
}
