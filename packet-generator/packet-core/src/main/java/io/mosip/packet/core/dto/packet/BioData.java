package io.mosip.packet.core.dto.packet;

import io.mosip.packet.core.constant.DataFormat;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class BioData implements Serializable {
    private String qualityScore;
    private byte[] bioData;
    private DataFormat format;
}
