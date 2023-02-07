package io.mosip.data.dto.dbimport;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.List;

@Data
@Getter
@Setter
public class MvelDto {
    private String mvelFile;
    private List<MvelParameter> parameters;
}
