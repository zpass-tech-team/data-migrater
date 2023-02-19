package io.mosip.packet.core.dto.upload;

import io.mosip.packet.core.constant.StatusConstant;
import io.mosip.packet.core.exception.ServiceError;

import java.util.ArrayList;
import java.util.List;

public class PacketUploadResponseDTO {
    private String packetId;
    private StatusConstant status;
    private List<ServiceError> errors = new ArrayList();
}
