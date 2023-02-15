package io.mosip.packet.extractor.dto.request;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Setter
@Getter
public class SecretKeyRequest {
	public String clientId;
	public String secretKey;
	public String appId;
}