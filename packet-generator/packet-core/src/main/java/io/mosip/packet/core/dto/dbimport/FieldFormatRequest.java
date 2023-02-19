package io.mosip.packet.core.dto.dbimport;

import io.mosip.packet.core.dto.mvel.MvelDto;
import io.mosip.packet.core.constant.FieldCategory;
import io.mosip.packet.core.constant.DataFormat;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class FieldFormatRequest {
    private String FieldName;
    private FieldCategory fieldCategory;
    private String primaryField;
    private DataFormat srcFormat;
    private DataFormat destFormat;
    private String fieldToMap;
    private String srcFieldForQualityScore;
    private DocumentAttributes documentAttributes;
    private MvelDto mvelExpressions;
}
