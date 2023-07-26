package io.mosip.packet.core.dto.packet;

import io.mosip.packet.core.constant.DataFormat;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BioData {
    private String qualityScore;
    private byte[] bioData;
    private DataFormat format;
}
