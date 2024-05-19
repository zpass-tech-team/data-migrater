package io.mosip.packet.core.dto.masterdata;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class MachineFilter {
    private String value;
    private String fromValue;
    private String toValue;
    private String columnName;
    private String type;
}
