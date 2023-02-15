package io.mosip.packet.extractor.validator;

import io.mosip.packet.extractor.dto.dbimport.DBImportRequest;

public interface Validator {
    public Boolean validate(DBImportRequest dbImportRequest ) throws Exception;
}
