package io.mosip.data.dto.mvel;

import io.mosip.data.constant.mvel.ParameterType;
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
