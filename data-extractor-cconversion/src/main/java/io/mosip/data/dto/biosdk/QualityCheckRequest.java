package io.mosip.data.dto.biosdk;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Data
@Getter
@Setter
public class QualityCheckRequest implements Serializable {
    private SegmentDto sample;
    private List<String> modalitiesToCheck;
}
