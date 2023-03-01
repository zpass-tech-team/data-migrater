package io.mosip.packet.core.dto.mockmds;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties
public class ErrorInfo {
	public String errorCode;
    public String errorInfo;
    
	public ErrorInfo(String errorCode, String errorInfo) {
		super();
		this.errorCode = errorCode;
		this.errorInfo = errorInfo;
	}    
}
