package io.mosip.packet.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TableData {
    private Object schema;
    private Object tableName;
    private boolean selected;
}
