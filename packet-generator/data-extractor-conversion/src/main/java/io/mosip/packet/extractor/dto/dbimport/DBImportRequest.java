package io.mosip.packet.extractor.dto.dbimport;

import io.mosip.packet.extractor.constant.DBTypes;
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
    private String tableName;
    private String process;
    private List<QueryFilter> filters;
    private List<FieldFormatRequest> columnDetails;
    private List<String> ignoreIdSchemaFields;
}
