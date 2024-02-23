package io.mosip.packet.core.dto.dbimport;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;

@Data
@Getter
@Setter
public class DBImportResponse {
    private String message;
    private HashMap<String, Object> convertedBioValues;
}
