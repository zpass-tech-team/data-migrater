package io.mosip.packet.core.dto.mockmds;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RCaptureResponse {

	 public List<BioMetricsDto> biometrics;
}
