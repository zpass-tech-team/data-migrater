package io.mosip.data.dto.packet;

import io.mosip.kernel.biometrics.entities.BIR;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.List;

@Data
@Getter
@Setter
public class PacketResponse {
    private String message;
    private LinkedHashMap<String, Object> DemoDetails;
    private LinkedHashMap<String, List<BIR>> bioDetails;
    private LinkedHashMap<String, String> docDetails;
    private LinkedHashMap<String, String> metaInfo;
}
