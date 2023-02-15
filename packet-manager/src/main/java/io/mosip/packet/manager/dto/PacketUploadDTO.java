package io.mosip.packet.manager.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class PacketUploadDTO {
    private String name;
    private String phone;
    private String email;
    private String langCode;
    private String registrationType;
    private String packetId;
    private String registrationId;
    private String packetPath;
}
