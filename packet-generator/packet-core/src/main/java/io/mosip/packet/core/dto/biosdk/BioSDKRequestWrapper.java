package io.mosip.packet.core.dto.biosdk;

import io.mosip.kernel.biometrics.entities.BIR;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BioSDKRequestWrapper {
    private List<BIR> segments;
    private String biometricType;
    private String format;
    private Object inputObject;
    private Boolean isOnlyForQualityCheck;
    private String biometricField;
}
