package io.mosip.packet.extractor.dto.packet.metadata;

import lombok.Data;

@Data
public class DocumentMetaInfoDTO {

	private String documentType;
	private String documentCategory;
	private String documentOwner;
	private String documentName;
	private String refNumber;
}
