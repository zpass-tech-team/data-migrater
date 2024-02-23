package io.mosip.packet.core.service.thread;

import io.mosip.packet.core.entity.PacketTracker;

public interface ThreadReprocessor {
    public void processData(PacketTracker packetTracker) throws Exception;
}
