package io.mosip.data.service;

import io.mosip.data.dto.dbimport.DBImportRequest;
import io.mosip.data.dto.dbimport.FieldFormatRequest;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;

public interface DataExtractionService {

    public LinkedHashMap<String, String> extractBioDataFromDB(DBImportRequest dbImportRequest) throws SQLException, IOException;
    public LinkedHashMap<String, String> extractBioDataFromDBAsBytes(DBImportRequest dbImportRequest) throws SQLException, IOException;
}
