package io.mosip.packet.core.service.thread;

import io.mosip.packet.core.constant.FieldCategory;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

public interface ThreadProcessor {
    public void processData(ResultSetter setter, Map<FieldCategory, LinkedHashMap<String, Object>> dataHashMap, String registrationId, String trackerColumn);
}
