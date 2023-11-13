package io.mosip.packet.core.service.thread;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.packet.core.constant.FieldCategory;
import io.mosip.packet.core.logger.DataProcessLogger;
import lombok.Setter;
import lombok.SneakyThrows;

import java.util.LinkedHashMap;
import java.util.Map;

@Setter
public class BaseThreadDBController implements Runnable {
    protected static final Logger LOGGER = DataProcessLogger.getLogger(BaseThreadDBController.class);
    private ResultSetter setter;
    private ThreadDBProcessor processor;
    private Map<String, Object> resultMap;

    @SneakyThrows
    @Override
    public void run() {
        processor.processData(setter, resultMap);
    }
}
