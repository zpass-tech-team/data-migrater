package io.mosip.packet.core.service.thread;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.packet.core.logger.DataProcessLogger;
import lombok.Setter;

import java.util.Map;

import static io.mosip.packet.core.constant.GlobalConfig.FAILED_RECORDS;

@Setter
public class BaseThreadDBController implements Runnable {
    protected static final Logger LOGGER = DataProcessLogger.getLogger(BaseThreadDBController.class);
    private ResultSetter setter;
    private ThreadDBProcessor processor;
    private Map<String, Object> resultMap;

    @Override
    public void run() {
        try {
            processor.processData(setter, resultMap);
        } catch (Exception e) {
            FAILED_RECORDS++;
            System.out.println("FAILED Record Count " + FAILED_RECORDS);
            e.printStackTrace();
        }
    }
}
