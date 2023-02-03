package io.mosip.data.service;

import io.mosip.data.dto.dbimport.DBImportRequest;
import io.mosip.data.dto.packet.PacketDto;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;

public interface DataExtractionService {

    public LinkedHashMap<String, Object> extractBioDataFromDB(DBImportRequest dbImportRequest, Boolean localStoreRequired) throws Exception;
    public LinkedHashMap<String, Object> extractBioDataFromDBAsBytes(DBImportRequest dbImportRequest, Boolean localStoreRequired) throws Exception;
    public PacketDto createPacketFromDataBase(DBImportRequest dbImportRequest) throws SQLException, IOException, Exception;
}
