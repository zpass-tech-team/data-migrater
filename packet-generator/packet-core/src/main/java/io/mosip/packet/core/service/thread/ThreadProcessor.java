package io.mosip.packet.core.service.thread;

import io.mosip.packet.core.constant.FieldCategory;

import java.util.HashMap;
import java.util.Map;

public interface ThreadProcessor {
    public void processData(ResultSetter setter, Map<FieldCategory, HashMap<String, Object>> dataHashMap, String registrationId, String trackerColumn) throws Exception;
}
