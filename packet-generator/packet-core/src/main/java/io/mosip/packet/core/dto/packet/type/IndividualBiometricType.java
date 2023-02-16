package io.mosip.packet.core.dto.packet.type;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class IndividualBiometricType {
    private String format;
    private Object version;
    private String value;
}
