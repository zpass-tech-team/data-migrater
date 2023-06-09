package io.mosip.packet.core.service.thread;

import io.mosip.packet.core.constant.FieldCategory;
import io.mosip.packet.core.entity.PacketTracker;
import org.json.simple.parser.ParseException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public interface ThreadReprocessor {
    public void processData(PacketTracker packetTracker) throws Exception;
}
