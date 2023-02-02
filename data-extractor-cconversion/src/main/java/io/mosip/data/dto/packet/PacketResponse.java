package io.mosip.data.dto.packet;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;

@Data
@Getter
@Setter
public class PacketResponse {
    private String message;
    private LinkedHashMap<String, Object> DemoDetails;
    private LinkedHashMap<String, String> bioDetails;
    private LinkedHashMap<String, String> docDetails;
}
