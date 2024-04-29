package io.mosip.packet.core.service.thread;

import io.mosip.packet.core.dto.upload.PacketUploadDTO;

public interface ThreadUploadProcessor {
    public void processData(ResultSetter setter, PacketUploadDTO result) throws Exception;
}
