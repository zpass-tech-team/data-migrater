package io.mosip.data.service;

import io.mosip.data.dto.dbimport.DBImportRequest;
import io.mosip.data.dto.packet.PacketResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;

public interface DataExtractionService {

    public LinkedHashMap<String, String> extractBioDataFromDB(DBImportRequest dbImportRequest, Boolean localStoreRequired) throws Exception;
    public LinkedHashMap<String, String> extractBioDataFromDBAsBytes(DBImportRequest dbImportRequest) throws Exception;
    public PacketResponse createPacketFromDataBase(DBImportRequest dbImportRequest) throws SQLException, IOException, Exception;
}
