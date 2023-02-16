package io.mosip.packet.uploader.service;


import io.mosip.packet.core.dto.PacketUploadDTO;

import java.util.List;

public interface PacketUploaderService {
    public void syncPacket(List<PacketUploadDTO> packets) throws Exception;
}
