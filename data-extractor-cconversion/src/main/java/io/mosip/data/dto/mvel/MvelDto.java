package io.mosip.data.dto.mvel;

import io.mosip.data.dto.mvel.MvelParameter;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@Getter
@Setter
public class MvelDto {
    private String mvelFile;
    private List<MvelParameter> parameters;
}
