package io.mosip.packet.extractor.dto.mvel;

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
