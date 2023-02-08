package io.mosip.data.dto.dbimport;

import io.mosip.data.constant.FieldCategory;
import io.mosip.data.constant.ImageFormat;
import io.mosip.data.dto.mvel.MvelDto;
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
    private MvelDto mvelExpressions;
}
