package io.mosip.packet.core.dto.dbimport;

import io.mosip.packet.core.constant.DBTypes;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@Getter
@Setter
public class DBImportRequest {
    private DBTypes dbType;
    private String url;
    private String port;
    private String databaseName;
    private String userId;
    private String password;
    private List<TableRequestDto> tableDetails;
    private String process;
    private List<FieldFormatRequest> columnDetails;
    private List<String> ignoreIdSchemaFields;
    private TrackerInfo trackerInfo;
}
