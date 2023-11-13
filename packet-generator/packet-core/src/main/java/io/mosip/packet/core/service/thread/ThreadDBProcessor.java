package io.mosip.packet.core.service.thread;

import io.mosip.packet.core.constant.FieldCategory;

import java.util.LinkedHashMap;
import java.util.Map;

public interface ThreadDBProcessor {
    public void processData(ResultSetter setter, Map<String, Object> resultMap) throws Exception;
}
