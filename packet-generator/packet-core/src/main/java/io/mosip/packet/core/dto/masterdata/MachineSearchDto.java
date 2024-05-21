package io.mosip.packet.core.dto.masterdata;

import io.mosip.packet.core.dto.request.Metadata;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@Getter
@Setter
public class MachineSearchDto<T> {
	private List<MachineFilter> filters;
	private List<MachineSort> sort;
	private Pagination pagination;
	private String languageCode;
}
