package io.mosip.packet.extractor.service;

import io.mosip.commons.packet.dto.packet.PacketDto;
import io.mosip.packet.extractor.dto.dbimport.DBImportRequest;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;

public interface DataExtractionService {

    public LinkedHashMap<String, Object> extractBioDataFromDB(DBImportRequest dbImportRequest, Boolean localStoreRequired) throws Exception;
    public LinkedHashMap<String, Object> extractBioDataFromDBAsBytes(DBImportRequest dbImportRequest, Boolean localStoreRequired) throws Exception;
    public PacketDto createPacketFromDataBase(DBImportRequest dbImportRequest) throws SQLException, IOException, Exception;
}
