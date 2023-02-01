package io.mosip.data.service.impl;

import io.mosip.data.constant.ApiName;
import io.mosip.data.constant.DBTypes;
import io.mosip.data.constant.FieldCategory;
import io.mosip.data.dto.ResponseWrapper;
import io.mosip.data.dto.dbimport.DBImportRequest;
import io.mosip.data.dto.dbimport.FieldFormatRequest;
import io.mosip.data.dto.packet.DocumentDto;
import io.mosip.data.dto.packet.PacketResponse;
import io.mosip.data.exception.ApisResourceAccessException;
import io.mosip.data.service.DataExtractionService;
import io.mosip.data.service.DataRestClientService;
import io.mosip.data.util.BioConversion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.*;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DataExtraationServiceImpl  implements DataExtractionService {

    @Autowired
    private BioConversion bioConversion;

    @Autowired
    private DataRestClientService restApiClient;

    @Autowired
    private Environment env;

    @Override
    public LinkedHashMap<String, String> extractBioDataFromDB(DBImportRequest dbImportRequest, Boolean localStoreRequired) throws Exception {
        Connection conn = null;
        LinkedHashMap<String, String> biodata = new LinkedHashMap<>();

        try {
            ResultSet resultSet = readDataFromDatabase(dbImportRequest, conn);

            while (resultSet.next()) {
                for (FieldFormatRequest fieldFormatRequest : dbImportRequest.getColumnDetails()) {
                    byte[] byteVal = resultSet.getBinaryStream(fieldFormatRequest.getFieldName()).readAllBytes();
                    String convertedImageData = convertBiometric(resultSet.getString(fieldFormatRequest.getPrimaryField()), fieldFormatRequest, byteVal, localStoreRequired);
                    biodata.put(resultSet.getString(fieldFormatRequest.getPrimaryField())+ "-" + fieldFormatRequest.getFieldName(),  convertedImageData);
                }
            }
        } finally {
            if (conn != null)
                conn.close();
        }


        return biodata;
    }

    @Override
    public LinkedHashMap<String, String> extractBioDataFromDBAsBytes(DBImportRequest dbImportRequest) throws Exception {
        LinkedHashMap<String, String> bioData = extractBioDataFromDB(dbImportRequest, false);
        LinkedHashMap<String, String> convertedData = new LinkedHashMap<>();

        for(Map.Entry<String, String> entry : bioData.entrySet()) {
            byte[] data = Base64.getDecoder().decode(entry.getValue());
            StringBuffer byteStringData = new StringBuffer();

            for (byte b : data)
                if (byteStringData.length() > 0)
                    byteStringData.append("," + b);
                else
                    byteStringData.append(b);

            convertedData.put(entry.getKey(), byteStringData.toString());
        }
        return convertedData;
    }

    @Override
    public PacketResponse createPacketFromDataBase(DBImportRequest dbImportRequest) throws Exception {
        Connection conn = null;
        PacketResponse packetResponse = new PacketResponse();

        try {
            ResultSet resultSet = readDataFromDatabase(dbImportRequest, conn);
            LinkedHashMap<String, String> DemoDetails = new LinkedHashMap<>();
            LinkedHashMap<String, String> bioDetails = new LinkedHashMap<>();
            LinkedHashMap<String, DocumentDto> docDetails = new LinkedHashMap<>();

            while (resultSet.next()) {
                for (FieldFormatRequest fieldFormatRequest : dbImportRequest.getColumnDetails()) {
                    String fieldName = fieldFormatRequest.getFieldName().contains(",") ? fieldFormatRequest.getFieldName().replace(",", "") : fieldFormatRequest.getFieldName();
                    String fieldMap = fieldFormatRequest.getFieldToMap() != null ? fieldFormatRequest.getFieldToMap() : fieldFormatRequest.getFieldName();
                    if (fieldFormatRequest.getFieldCategory().equals(FieldCategory.DEMO)) {
                        DemoDetails.put(fieldMap, resultSet.getString(fieldName));
                    } else if (fieldFormatRequest.getFieldCategory().equals(FieldCategory.BIO)) {
                        byte[] byteVal = resultSet.getBinaryStream(fieldFormatRequest.getFieldName()).readAllBytes();
                        String convertedImageData = convertBiometric(null, fieldFormatRequest, byteVal, false);
                        bioDetails.put(fieldMap, convertedImageData);
                    } else if (fieldFormatRequest.getFieldCategory().equals(FieldCategory.DOC)) {
 /*                       DocumentDto document = new DocumentDto();
                        document.setOwner();
                        document.setCategory();
                        document.setDocument(resultSet.getBinaryStream(fieldFormatRequest.getFieldName()).readAllBytes());
                        document.setFormat();
                        document.setRefNumber();
                        document.setType();
                        document.setValue(fieldMap);
                        docDetails.put(fieldMap, document);

                        DocumentMetaInfoDTO documentMetaInfoDTO = new DocumentMetaInfoDTO();
                        documentMetaInfoDTO.setDocumentCategory(document.getCategory());
                        documentMetaInfoDTO.setDocumentName(document.getValue());
                        documentMetaInfoDTO.setDocumentOwner(document.getOwner());
                        documentMetaInfoDTO.setDocumentType(document.getType());
                        documentMetaInfoDTO.setRefNumber(document.getRefNumber());
*/
                   //     docDetails.put(fieldMap, Base64.getEncoder().encodeToString(byteVal));
                    }
                }

    /*            PacketDto packetDto = new PacketDto();
                packetDto.setId();
                packetDto.setProcess(dbImportRequest.getProcess());
                packetDto.setSource("REGISTRATION_CLIENT");
                packetDto.setSchemaVersion("0.1");
                packetDto.setAdditionalInfoReqId();
                packetDto.setMetaInfo(null);
                packetDto.setOfflineMode();
                packetDto.setRefId();


                PacketWriter packetWriter = new PacketWriter();
                packetWriter.createPacket()*/
            }

            packetResponse.setDemoDetails(DemoDetails);
            packetResponse.setBioDetails(bioDetails);
       //     packetResponse.setDocDetails(docDetails);
        } finally {
            if (conn != null)
                conn.close();
        }

        return packetResponse;
    }

    private String convertBiometric(String fileNamePrefix, FieldFormatRequest fieldFormatRequest, byte[] bioValue, Boolean localStoreRequired) throws IOException {
        if (localStoreRequired) {
            bioConversion.writeFile(fileNamePrefix + "-" + fieldFormatRequest.getFieldName() , bioValue, fieldFormatRequest.getFromFormat());
            return bioConversion.writeFile(fileNamePrefix + "-" + fieldFormatRequest.getFieldName(), bioConversion.convertImage(fieldFormatRequest.getFromFormat(), fieldFormatRequest.getToFormat(), bioValue), fieldFormatRequest.getToFormat());
        } else {
            return Base64.getEncoder().encodeToString(bioConversion.convertImage(fieldFormatRequest.getFromFormat(), fieldFormatRequest.getToFormat(), bioValue));
        }
    }

    private ResultSet readDataFromDatabase(DBImportRequest dbImportRequest, Connection conn) throws Exception {
        loadMasterData();
        if (dbImportRequest.getDbType().equals(DBTypes.MSSQL)) {
            DriverManager.registerDriver(new com.microsoft.sqlserver.jdbc.SQLServerDriver());
            conn = DriverManager.getConnection("jdbc:sqlserver://" + dbImportRequest.getUrl() + ";sslProtocol=TLSv1.2;databaseName=" + dbImportRequest.getDatabaseName()+ ";Trusted_Connection=True;", dbImportRequest.getUserId(), dbImportRequest.getPassword());
        }

        if(conn!=null) {
            String columnNames = null;
            HashSet<String> uniqueCloumns = new HashSet<>();
            for (FieldFormatRequest fieldFormatRequest : dbImportRequest.getColumnDetails()) {
                String field = null;
                if (fieldFormatRequest.getFieldName().contains(",")) {
                    switch(dbImportRequest.getDbType().toString()) {
                        case "MSSQL":
                            for (String column : fieldFormatRequest.getFieldName().split(","))
                                if (field == null)
                                    field = column;
                                else
                                    field += ", " + column;
                            field = "CONCAT(" + field + ") AS " +  fieldFormatRequest.getFieldName().replace(",", "");
                            break;

                        default:
                            throw new Exception("Implementation missing for Database to Read Data DBType :" +  dbImportRequest.getDbType().toString());
                    }
                } else {
                    field = fieldFormatRequest.getFieldName();
                }

                uniqueCloumns.add(field);
                uniqueCloumns.add(fieldFormatRequest.getPrimaryField());
            }

            for (String column : uniqueCloumns) {
                if (columnNames == null)
                    columnNames = column;
                else
                    columnNames += "," + column;
            }

            System.out.println("Database Successfully connected");
            Statement statement = conn.createStatement();

            String selectSql = "SELECT " + columnNames + "  from " + dbImportRequest.getTableName();
            return statement.executeQuery(selectSql);
        } else
            throw new SQLException("Unable to Connect With Database. Please check the Configuration");
    }

    private void loadMasterData() throws ApisResourceAccessException {
        ResponseWrapper response= (ResponseWrapper) restApiClient.getApi(ApiName.DOCUMENT_CATEGORY, null, "", "", ResponseWrapper.class);
        System.out.println(response);
    }

}
