package io.mosip.data.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mosip.data.constant.ApiName;
import io.mosip.data.constant.DBTypes;
import io.mosip.data.constant.FieldCategory;
import io.mosip.data.dto.ResponseWrapper;
import io.mosip.data.dto.dbimport.DBImportRequest;
import io.mosip.data.dto.dbimport.FieldFormatRequest;
import io.mosip.data.dto.masterdata.DocumentCategoryDto;
import io.mosip.data.dto.masterdata.DocumentCategoryResponseDto;
import io.mosip.data.dto.masterdata.DocumentTypeExtnDto;
import io.mosip.data.dto.masterdata.PageDto;
import io.mosip.data.dto.packet.DocumentDto;
import io.mosip.data.dto.packet.PacketResponse;
import io.mosip.data.dto.packet.type.IndividualBiometricType;
import io.mosip.data.dto.packet.type.SimpleType;
import io.mosip.data.exception.ApisResourceAccessException;
import io.mosip.data.service.DataExtractionService;
import io.mosip.data.service.DataRestClientService;
import io.mosip.data.util.BioConversion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.*;
import java.util.*;

@Service
public class DataExtraationServiceImpl  implements DataExtractionService {

    @Autowired
    private BioConversion bioConversion;

    @Autowired
    private DataRestClientService restApiClient;

    @Autowired
    private Environment env;

    @Value("${mosip.id.schema.version:0.1}")
    private Float version;

    @Value("${mosip.primary.language}")
    private String primaryLamguage;

    private LinkedHashMap<String, DocumentCategoryDto> documentCategory = new LinkedHashMap<>();
    private LinkedHashMap<String, DocumentTypeExtnDto> documentType = new LinkedHashMap<>();

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
            LinkedHashMap<String, Object> demoDetails = new LinkedHashMap<>();
            LinkedHashMap<String, String> bioDetails = new LinkedHashMap<>();
            LinkedHashMap<String, String> docDetails = new LinkedHashMap<>();

            while (resultSet.next()) {
                for (FieldFormatRequest fieldFormatRequest : dbImportRequest.getColumnDetails()) {
                    String fieldName = fieldFormatRequest.getFieldName().contains(",") ? fieldFormatRequest.getFieldName().replace(",", "") : fieldFormatRequest.getFieldName();
                    String fieldMap = fieldFormatRequest.getFieldToMap() != null ? fieldFormatRequest.getFieldToMap().toLowerCase() : fieldFormatRequest.getFieldName().toLowerCase();
                    if (fieldFormatRequest.getFieldCategory().equals(FieldCategory.DEMO)) {
                        demoDetails.put(fieldMap, resultSet.getObject(fieldName));
                    } else if (fieldFormatRequest.getFieldCategory().equals(FieldCategory.BIO)) {
                        byte[] byteVal = resultSet.getBinaryStream(fieldFormatRequest.getFieldName()).readAllBytes();
                        String convertedImageData = convertBiometric(null, fieldFormatRequest, byteVal, false);
                        bioDetails.put(fieldMap, convertedImageData);
                    } else if (fieldFormatRequest.getFieldCategory().equals(FieldCategory.DOC)) {
                        byte[] byteVal = resultSet.getBinaryStream(fieldFormatRequest.getFieldName()).readAllBytes();
                        docDetails.put(fieldMap, Base64.getEncoder().encodeToString(byteVal));
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



            if (demoDetails.size() > 0) {
                ObjectMapper mapper = new ObjectMapper();
                LinkedHashMap<String, Object> demoMap = new LinkedHashMap<>();
                ResponseWrapper response= (ResponseWrapper) restApiClient.getApi(ApiName.LATEST_ID_SCHEMA, null, "", "", ResponseWrapper.class);
                LinkedHashMap<String, Object> idSchemaMap = (LinkedHashMap<String, Object>) response.getResponse();

                for(Object obj : (List)idSchemaMap.get("schema")) {
                    Map<String, Object> map = (Map<String, Object>) obj;
                    String id = map.get("id").toString();
                    String type = map.get("type").toString();
                    Boolean required = (Boolean) map.get("required");

                    if (id.equals("IDSchemaVersion")) {
                        demoMap.put(id, version);
                    } else if (type.equals("biometricsType")) {
                        if (id.equals("individualBiometrics")) {
                            if (bioDetails.size() > 0) {
                                IndividualBiometricType indiBiotype = new IndividualBiometricType();
                                indiBiotype.setFormat("cbeff");
                                indiBiotype.setVersion(1.0);
                                indiBiotype.setValue("individualBiometrics_bio_CBEFF");
                                demoMap.put("individualBiometrics", mapper.writeValueAsString(indiBiotype));
                            }
                        }

                    } else if (type.equals("documentType")) {

                    } else if (demoDetails.containsKey(id.toLowerCase()) && demoDetails.get(id.toLowerCase()) != null) {
                        switch (type) {
                            case "simpleType":
                                List<SimpleType> valList = new ArrayList<>();
                                SimpleType simpleType = new SimpleType(primaryLamguage, demoDetails.get(id.toLowerCase()) == null ? "":demoDetails.get(id.toLowerCase()).toString());
                                valList.add(simpleType);
                                demoMap.put(id, mapper.writeValueAsString(valList));
                                break;

                            case "number":

                            case "string" :
                                demoMap.put(id, demoDetails.get(id.toLowerCase()) == null ? "" : demoDetails.get(id.toLowerCase()));
                                break;
                        }
                    } else if (required && !dbImportRequest.getIgnoreIdSchemaFields().contains(id)) {
                        throw new Exception("Mandatory Field '" + id + "' value missing");
                    }
                }
                packetResponse.setDemoDetails(demoMap);
            }


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
        ObjectMapper mapper = new ObjectMapper();

        ResponseWrapper response =  (ResponseWrapper) restApiClient.getApi(ApiName.DOCUMENT_CATEGORY, null, "", "", ResponseWrapper.class);
        DocumentCategoryResponseDto documentCategoryResponse = mapper.convertValue(response.getResponse(), DocumentCategoryResponseDto.class);
        for (DocumentCategoryDto dto : documentCategoryResponse.getDocumentcategories())
            documentCategory.put(dto.getCode(), dto);

        response= (ResponseWrapper) restApiClient.getApi(ApiName.DOCUMENT_TYPES, null, "", "", ResponseWrapper.class);
 //       PageDto pageDto = mapper.convertValue(response.getResponse(), PageDto.class);
 //       for (DocumentTypeExtnDto dto : ()pageDto.getData())
 //           documentType.put(dto.getCode(), dto);

            System.out.println(response);

    }

}
