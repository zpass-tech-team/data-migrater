package io.mosip.packet.core.dto.masterdata;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class Pagination {
    private Integer pageStart;
    private Integer pageFetch;
}
