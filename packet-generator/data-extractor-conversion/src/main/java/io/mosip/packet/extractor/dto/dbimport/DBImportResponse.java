package io.mosip.packet.extractor.dto.dbimport;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;

@Data
@Getter
@Setter
public class DBImportResponse {
    private String message;
    private LinkedHashMap<String, Object> convertedBioValues;
}
