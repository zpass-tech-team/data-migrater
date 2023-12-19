package io.mosip.packet.client.dto;

import io.mosip.packet.core.constant.FieldType;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class ColumnData {
    private String schema;
    private String tableName;
    private String columnName;
    private FieldType dataType;
    private String length;
}
