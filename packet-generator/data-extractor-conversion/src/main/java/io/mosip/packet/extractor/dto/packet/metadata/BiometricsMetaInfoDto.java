package io.mosip.packet.extractor.dto.packet.metadata;

import lombok.Data;

@Data
public class BiometricsMetaInfoDto {

	private int numRetry;
	private boolean forceCaptured;
	private String BIRIndex;

	public BiometricsMetaInfoDto(int numRetry, boolean forceCaptured, String bIRIndex) {
		super();
		this.numRetry = numRetry;
		this.forceCaptured = forceCaptured;
		BIRIndex = bIRIndex;
	}

}
