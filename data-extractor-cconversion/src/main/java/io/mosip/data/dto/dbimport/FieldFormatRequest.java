package io.mosip.data.dto.dbimport;

import io.mosip.data.constant.FieldCategory;
import io.mosip.data.constant.ImageFormat;
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
    private ImageFormat fromFormat;
    private ImageFormat toFormat;
    private String fieldToMap;
    private String fieldToQualityScore;
}
