package io.mosip.packet.core.service.thread;

import io.mosip.packet.core.constant.tracker.TrackerStatus;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class ResultDto {
    private String refId;
    private String regNo;
    private String comments;
    private TrackerStatus status;
}
