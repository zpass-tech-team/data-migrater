package io.mosip.packet.core.dto.dbimport;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;

@Data
@Getter
@Setter
public class DataResult implements Comparable {
    private LinkedHashMap<String, Object> demoDetails;
    private LinkedHashMap<String, Object> bioDetails;
    private LinkedHashMap<String, Object> docDetails;

    @Override
    public int compareTo(Object o) {
        return 0;
    }
}
