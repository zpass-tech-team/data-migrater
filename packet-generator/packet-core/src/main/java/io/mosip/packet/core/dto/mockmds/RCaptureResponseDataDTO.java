package io.mosip.packet.core.dto.mockmds;

import java.util.Base64;

import io.mosip.kernel.core.util.CryptoUtil;
import lombok.Data;

@Data
public class RCaptureResponseDataDTO {

	private String deviceCode;
	private String digitalId;
	private String deviceServiceVersion;
	private String bioType;
	private String bioSubType;
	private String purpose;
	private String env;
	private String bioValue;
	private String bioExtract;
	private String registrationId;
	private String timestamp;
	private String requestedScore;
	private String qualityScore;
	private String transactionId;

	public byte[] getDecodedBioValue() {
		return CryptoUtil.decodeURLSafeBase64(bioExtract);
	}

}
