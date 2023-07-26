package io.mosip.packet.data.biosdk.bqat.dto;

import io.mosip.packet.data.biosdk.bqat.constant.BQATFileType;
import io.mosip.packet.data.biosdk.bqat.constant.BQATModalityType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BQATRequest {
    private String modality;
    private String type;
    private String data;
    private String id;
    private String timestamp;
}
