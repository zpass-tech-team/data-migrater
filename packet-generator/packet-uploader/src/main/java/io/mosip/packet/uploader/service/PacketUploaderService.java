package io.mosip.packet.uploader.service;


import io.mosip.packet.core.dto.upload.PacketUploadDTO;
import io.mosip.packet.core.dto.upload.PacketUploadResponseDTO;

import java.util.LinkedHashMap;
import java.util.List;

public interface PacketUploaderService {
    public void syncPacket(List<PacketUploadDTO> packets, String centerId, String machineId, LinkedHashMap<String, PacketUploadResponseDTO> response) throws Exception;
    public void uploadSyncedPacket(List<PacketUploadDTO> packets, LinkedHashMap<String, PacketUploadResponseDTO> response) throws Exception;
}
