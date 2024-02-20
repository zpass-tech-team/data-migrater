package io.mosip.packet.client.dto;

import io.mosip.packet.core.dto.dbimport.QueryFilter;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class FilterTableViewData extends QueryFilter {
    private Boolean select;
}
