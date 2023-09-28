package io.mosip.packet.core.dto.tracker;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class TrackerRequestDto {
    private String refId;
    private String regNo;
    private String status;
    private String process;
    private String sessionKey;
    private String activity;
    private String comments;
}
