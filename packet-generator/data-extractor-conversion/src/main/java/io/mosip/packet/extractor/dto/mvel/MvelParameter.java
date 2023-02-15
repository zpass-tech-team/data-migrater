package io.mosip.packet.extractor.dto.mvel;

import io.mosip.packet.extractor.constant.mvel.ParameterType;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class MvelParameter {
    private String parameterName;
    private ParameterType parameterType;
    private String parameterValue;
}
