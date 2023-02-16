package io.mosip.packet.core.dto.dbimport;

import io.mosip.packet.core.dto.mvel.MvelDto;
import io.mosip.packet.core.constant.FieldCategory;
import io.mosip.packet.core.constant.ImageFormat;
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
    private ImageFormat srcFormat;
    private ImageFormat destFormat;
    private String fieldToMap;
    private String srcFieldForQualityScore;
    private DocumentAttributes documentAttributes;
    private MvelDto mvelExpressions;
}
