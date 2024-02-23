package io.mosip.packet.uploader.service;


import io.mosip.packet.core.dto.upload.PacketUploadDTO;
import io.mosip.packet.core.dto.upload.PacketUploadResponseDTO;

import java.util.HashMap;
import java.util.List;

public interface PacketUploaderService {
    public void syncPacket(List<PacketUploadDTO> packets, String centerId, String machineId, HashMap<String, PacketUploadResponseDTO> response) throws Exception;
    public void uploadSyncedPacket(List<PacketUploadDTO> packets, HashMap<String, PacketUploadResponseDTO> response) throws Exception;
}
