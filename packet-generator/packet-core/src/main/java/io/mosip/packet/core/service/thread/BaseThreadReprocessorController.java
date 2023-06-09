package io.mosip.packet.core.service.thread;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.packet.core.constant.FieldCategory;
import io.mosip.packet.core.entity.PacketTracker;
import io.mosip.packet.core.logger.DataProcessLogger;
import lombok.Setter;
import lombok.SneakyThrows;

import java.util.LinkedHashMap;
import java.util.Map;

@Setter
public class BaseThreadReprocessorController implements Runnable {
    protected static final Logger LOGGER = DataProcessLogger.getLogger(BaseThreadReprocessorController.class);
    private ThreadReprocessor processor;
    private PacketTracker packetTracker;
    private ResultSetter setter;

    @SneakyThrows
    @Override
    public void run() {
        processor.processData(packetTracker);
    }
}
