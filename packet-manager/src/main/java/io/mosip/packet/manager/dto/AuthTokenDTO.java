package io.mosip.packet.manager.dto;

import lombok.Data;

/**
 * The Class AuthTokenDTO is to store the token response upon invoking the rest
 * API.
 * 
 * @author Mahesh Kumar
 */
@Data
public class AuthTokenDTO {

	private String loginMode;
	private String cookie;

}
