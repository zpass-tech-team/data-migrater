package io.mosip.packet.core.dto.upload;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Data
@Getter
@Setter
public class PacketUploadDTO implements Serializable {
    public enum PacketUploadDTOEnum {
        name,
        phone,
        email,
        langCode
    }

    private String name;
    private String phone;
    private String email;
    private String langCode;
    private String registrationType;
    private String packetId;
    private String registrationId;
    private String packetPath;

    public void setValue(String type, Object val) {
        switch (type) {
            case "name" :
                this.name = (String) val;
                break;
            case "phone" :
                this.phone = (String) val;
                break;
            case "email" :
                this.email = (String) val;
                break;
            case "langCode" :
                this.langCode = (String) val;
                break;
        }
    }
}
