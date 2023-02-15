package io.mosip.packet.extractor.dto.biosdk;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Data
@Getter
@Setter
public class SegmentDto implements Serializable {
    private List<Object> segments;
    private OtherDto others;
}
