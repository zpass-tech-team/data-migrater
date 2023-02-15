package io.mosip.packet.uploader.service.impl;

import io.mosip.packet.uploader.service.PacketUploaderService;
import org.springframework.stereotype.Service;

@Service
public class PacketUploaderServiceImpl  implements PacketUploaderService {
    @Override
    public void uploadPacket() {
        System.out.println("Packet has been Uploaded");
    }
}
