package io.mosip.data.dto.dbimport;

import io.mosip.data.constant.FieldType;
import io.mosip.data.constant.FilterCondition;
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
