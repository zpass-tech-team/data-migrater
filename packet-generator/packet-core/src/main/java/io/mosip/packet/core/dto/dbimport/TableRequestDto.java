package io.mosip.packet.core.dto.dbimport;

import io.mosip.packet.core.constant.FieldCategory;
import io.mosip.packet.core.constant.QuerySelection;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@Getter
@Setter
public class TableRequestDto implements Comparable<TableRequestDto> {
    private QuerySelection queryType;
    private Integer executionOrderSequence;
    private String tableName;
    private FieldCategory[] fieldCategory;
    private String sqlQuery;
    private String[] nonIdSchemaFields;
    private List<QueryFilter> filters;

    @Override
    public int compareTo(TableRequestDto o) {
        return this.executionOrderSequence.compareTo(o.executionOrderSequence);
    }
}
