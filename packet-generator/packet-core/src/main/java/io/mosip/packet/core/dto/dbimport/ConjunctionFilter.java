package io.mosip.packet.core.dto.dbimport;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@Setter
@Getter
public class ConjunctionFilter {
    private ConjuctionType conjuctionType;
    private List<QueryFilter> filters;
}
