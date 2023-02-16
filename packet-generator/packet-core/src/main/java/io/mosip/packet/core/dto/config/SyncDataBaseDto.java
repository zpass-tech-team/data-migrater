package io.mosip.packet.core.dto.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SyncDataBaseDto {
	
	private String entityName;
	private String entityType;
	private String data;

}
