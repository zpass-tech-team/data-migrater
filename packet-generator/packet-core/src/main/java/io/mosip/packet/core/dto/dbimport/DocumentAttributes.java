package io.mosip.packet.core.dto.dbimport;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class DocumentAttributes {
    private String documentRefNoField;
    private String documentFormatField;
    private String documentCodeField;
    private DocumentValueMap documentValueMap;
}
