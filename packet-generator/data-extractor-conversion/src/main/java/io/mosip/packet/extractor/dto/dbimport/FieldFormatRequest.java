package io.mosip.packet.extractor.dto.dbimport;

import io.mosip.packet.extractor.constant.FieldCategory;
import io.mosip.packet.extractor.constant.ImageFormat;
import io.mosip.packet.extractor.dto.mvel.MvelDto;
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
