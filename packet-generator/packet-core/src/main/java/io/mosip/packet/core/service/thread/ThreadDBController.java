package io.mosip.packet.core.service.thread;

import lombok.Setter;

import java.util.Map;

@Setter
public class ThreadDBController extends BaseThreadController {
    private Map<String, Object> resultMap;
    private ThreadDBProcessor processor;

    @Override
    public void execute() throws Exception {
        processor.processData(setter, resultMap);
    }
}
