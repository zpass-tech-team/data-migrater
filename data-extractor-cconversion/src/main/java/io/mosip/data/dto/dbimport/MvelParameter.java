package io.mosip.data.dto.dbimport;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class MvelParameter {
    private String parameterName;
    private String parameterType;
    private String parameterValue;
}
