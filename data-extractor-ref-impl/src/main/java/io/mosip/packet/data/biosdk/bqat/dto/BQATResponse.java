package io.mosip.packet.data.biosdk.bqat.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BQATResponse {
    private Object results;
    private String engine;
    private String modality;
    private String id;
    private String timestamp;
}
