package io.mosip.packet.extractor.validator;

import io.mosip.packet.core.dto.dbimport.DBImportRequest;

public interface Validator {
    public Boolean validate(DBImportRequest dbImportRequest ) throws Exception;
}
