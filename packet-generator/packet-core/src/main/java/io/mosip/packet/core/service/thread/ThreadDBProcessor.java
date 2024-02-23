package io.mosip.packet.core.service.thread;

import java.util.Map;

public interface ThreadDBProcessor {
    public void processData(ResultSetter setter, Map<String, Object> resultMap) throws Exception;
}
