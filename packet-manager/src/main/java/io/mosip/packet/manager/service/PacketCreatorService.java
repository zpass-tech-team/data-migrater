package io.mosip.packet.manager.service;

import io.mosip.commons.packet.dto.PacketInfo;
import io.mosip.commons.packet.dto.packet.PacketDto;
import io.mosip.commons.packet.exception.TagCreationException;
import io.mosip.commons.packet.facade.PacketWriter;
import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.exception.BaseUncheckedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;

@Component
public class PacketCreatorService {

    @Autowired
    private PacketWriter packetWriter;

    public  List<PacketInfo> persistPacket(PacketDto packetDto) {
        try {

            List<PacketInfo> resultField = packetWriter.createPacket(packetDto);
            return resultField;

        } catch (Exception e) {
            if (e instanceof BaseCheckedException) {
                BaseCheckedException ex = (BaseCheckedException) e;
                throw new TagCreationException(ex.getErrorCode(), ex.getMessage());
            } else if (e instanceof BaseUncheckedException) {
                BaseUncheckedException ex = (BaseUncheckedException) e;
                throw new TagCreationException(ex.getErrorCode(), ex.getMessage());
            }
            throw new TagCreationException(e.getMessage());

        }
    }

}
