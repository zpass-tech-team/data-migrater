package io.mosip.packet.core.service.thread;

import io.mosip.packet.core.constant.FieldCategory;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Setter
public class ThreadDataProcessController extends BaseThreadController {
    private Map<FieldCategory, HashMap<String, Object>> dataHashMap;
    private ThreadProcessor processor;
    private String registrationId;
    private String trackerColumn;

    @Override
    public void execute() throws Exception {
        processor.processData(setter, dataHashMap, registrationId, trackerColumn);
    }
}
