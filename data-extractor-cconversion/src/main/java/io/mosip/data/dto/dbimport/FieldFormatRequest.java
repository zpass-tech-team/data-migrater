package io.mosip.data.dto.dbimport;

import io.mosip.data.constant.ImageFormat;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;

@Data
@Getter
@Setter
public class FieldFormatRequest {
    private String FieldName;
    private String displayName;
    private ImageFormat fromFormat;
    private ImageFormat toFormat;
}
