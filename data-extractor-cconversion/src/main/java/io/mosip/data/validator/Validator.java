package io.mosip.data.validator;

import io.mosip.data.dto.dbimport.DBImportRequest;

public interface Validator {
    public Boolean validate(DBImportRequest dbImportRequest ) throws Exception;
}
