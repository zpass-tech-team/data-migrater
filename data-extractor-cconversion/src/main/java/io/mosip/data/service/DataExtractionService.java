package io.mosip.data.service;

import io.mosip.data.dto.dbimport.DBImportRequest;
import io.mosip.data.dto.dbimport.FieldFormatRequest;

import java.io.IOException;
import java.sql.SQLException;

public interface DataExtractionService {

    public String extractBioDataFromDB(DBImportRequest dbImportRequest) throws SQLException, IOException;
    public void convertBiometric(String fileNamePrefix, FieldFormatRequest fieldFormatRequest, byte[] bioValue) throws IOException;
}
