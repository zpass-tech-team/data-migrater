package io.mosip.packet.core.dto.request;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Setter
@Getter
public class ClientSecretKeyRequest extends LoginRequest {
	public String clientSecret;
}