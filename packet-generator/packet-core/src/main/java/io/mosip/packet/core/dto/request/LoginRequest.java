package io.mosip.packet.core.dto.request;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Setter
@Getter
public class LoginRequest {
	public String clientId;
	public String appId;
	public String userName;
	public String password;
}