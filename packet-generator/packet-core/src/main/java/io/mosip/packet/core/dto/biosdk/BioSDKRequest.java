package io.mosip.packet.core.dto.biosdk;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class BioSDKRequest {
    private String version;
    private String request;
}
