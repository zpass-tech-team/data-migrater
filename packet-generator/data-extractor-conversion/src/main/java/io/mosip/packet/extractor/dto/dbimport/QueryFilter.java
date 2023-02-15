package io.mosip.packet.extractor.dto.dbimport;

import io.mosip.packet.extractor.constant.FieldType;
import io.mosip.packet.extractor.constant.FilterCondition;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class QueryFilter {
    private String filterField;
    private FieldType fieldType;
    private String fromValue;
    private String toValue;
    private FilterCondition filterCondition;

}
