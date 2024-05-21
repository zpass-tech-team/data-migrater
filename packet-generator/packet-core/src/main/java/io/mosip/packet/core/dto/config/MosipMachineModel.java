package io.mosip.packet.core.dto.config;

import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MosipMachineModel{
	private String id;
    private String	ipAddress;
    private Boolean isActive;
    private String 	langCode;
    private String	macAddress;
    private String	machineSpecId;
    private String	name;
    private String	publicKey;
    private String	regCenterId;
    private String	serialNum;
    private String	signPublicKey;
    private String	validityDateTime;
    private String	zoneCode;
}

