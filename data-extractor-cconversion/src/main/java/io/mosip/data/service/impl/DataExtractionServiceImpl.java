package io.mosip.data.service.impl;

import io.mosip.data.constant.DBTypes;
import io.mosip.data.constant.FieldCategory;
import io.mosip.data.constant.mvel.ParameterType;
import io.mosip.data.dto.dbimport.DBImportRequest;
import io.mosip.data.dto.dbimport.FieldFormatRequest;
import io.mosip.data.dto.mvel.MvelParameter;
import io.mosip.data.dto.masterdata.DocumentCategoryDto;
import io.mosip.data.dto.masterdata.DocumentTypeExtnDto;
import io.mosip.data.dto.packet.PacketDto;
import io.mosip.data.repository.BlocklistedWordsRepository;
import io.mosip.data.service.CustomNativeRepository;
import io.mosip.data.service.DataExtractionService;
import io.mosip.data.util.BioConversion;
import io.mosip.data.util.ConfigUtil;
import io.mosip.data.util.MvelUtil;
import io.mosip.data.util.PacketCreator;
import io.mosip.kernel.core.idgenerator.spi.RidGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;

@Service
public class DataExtractionServiceImpl implements DataExtractionService {

    @Autowired
    private BioConversion bioConversion;

    @Autowired
    private PacketCreator packetCreator;

    @Autowired
    private RidGenerator ridGenerator;

    @Autowired
    private MvelUtil mvelUtil;

    @Autowired
    CustomNativeRepository customNativeRepository;

    private LinkedHashMap<String, DocumentCategoryDto> documentCategory = new LinkedHashMap<>();
    private LinkedHashMap<String, DocumentTypeExtnDto> documentType = new LinkedHashMap<>();

    @Override
    public LinkedHashMap<String, Object> extractBioDataFromDBAsBytes(DBImportRequest dbImportRequest, Boolean localStoreRequired) throws Exception {
        Connection conn = null;
        LinkedHashMap<String, Object> biodata = new LinkedHashMap<>();

        try {
            ResultSet resultSet = readDataFromDatabase(dbImportRequest, conn);

            while (resultSet.next()) {
                for (FieldFormatRequest fieldFormatRequest : dbImportRequest.getColumnDetails()) {
                    byte[] byteVal = resultSet.getBinaryStream(fieldFormatRequest.getFieldName()).readAllBytes();
                    byte[] convertedImageData = convertBiometric(resultSet.getString(fieldFormatRequest.getPrimaryField()), fieldFormatRequest, byteVal, localStoreRequired);
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
    public LinkedHashMap<String, Object> extractBioDataFromDB(DBImportRequest dbImportRequest, Boolean localStoreRequired) throws Exception {
        LinkedHashMap<String, Object> bioData = extractBioDataFromDBAsBytes(dbImportRequest, localStoreRequired);
        LinkedHashMap<String, Object> convertedData = new LinkedHashMap<>();

        for(Map.Entry<String, Object> entry : bioData.entrySet()) {
            String data = Base64.getEncoder().encodeToString((byte[])entry.getValue());
             convertedData.put(entry.getKey(), data);
        }
        return convertedData;
    }

    @Override
    public PacketDto createPacketFromDataBase(DBImportRequest dbImportRequest) throws Exception {
        Connection conn = null;
        PacketDto packetDto = null;

        try {
            ResultSet resultSet = readDataFromDatabase(dbImportRequest, conn);

            while (resultSet.next()) {
                packetDto = new PacketDto();
                packetDto.setProcess(dbImportRequest.getProcess());
                packetDto.setSource("REGISTRATION_CLIENT");
                packetDto.setSchemaVersion("0.1");
                packetDto.setAdditionalInfoReqId(null);
                packetDto.setMetaInfo(null);
                packetDto.setOfflineMode(false);

                LinkedHashMap<String, Object> demoDetails = new LinkedHashMap<>();
                LinkedHashMap<String, Object> bioDetails = new LinkedHashMap<>();
                LinkedHashMap<String, String> docDetails = new LinkedHashMap<>();
                LinkedHashMap<String, String> metaInfo = new LinkedHashMap<>();

                for (FieldFormatRequest fieldFormatRequest : dbImportRequest.getColumnDetails()) {
                    String fieldName = fieldFormatRequest.getFieldName().contains(",") ? fieldFormatRequest.getFieldName().replace(",", "") : fieldFormatRequest.getFieldName();
                    String fieldMap = fieldFormatRequest.getFieldToMap() != null ? fieldFormatRequest.getFieldToMap() : fieldFormatRequest.getFieldName().toLowerCase();
                    if (fieldFormatRequest.getFieldCategory().equals(FieldCategory.DEMO)) {
                        if (fieldFormatRequest.getMvelExpressions() != null) {
                            Map map = new HashMap();
                            for (MvelParameter parameter : fieldFormatRequest.getMvelExpressions().getParameters()) {
                                if (parameter.getParameterType().equals(ParameterType.STRING))
                                    if(parameter.getParameterValue().contains("${")) {
                                        String param = parameter.getParameterValue().replace("${", "").replace("}", "");
                                        map.put(parameter.getParameterName(), resultSet.getObject(param));
                                    } else {
                                        map.put(parameter.getParameterName(), parameter.getParameterValue());
                                    }
                                else if (parameter.getParameterType().equals(ParameterType.SQL)){
                                    List<Object> list = (List<Object>) customNativeRepository.runNativeQuery(parameter.getParameterValue());
                                    map.put(parameter.getParameterName(), list);
                                }
                            }

                            demoDetails.put(fieldMap, mvelUtil.processViaMVEL(fieldFormatRequest.getMvelExpressions().getMvelFile(), map));
                        } else {
                            demoDetails.put(fieldMap, resultSet.getObject(fieldName));
                        }
                    } else if (fieldFormatRequest.getFieldCategory().equals(FieldCategory.BIO)) {
                        byte[] byteVal = resultSet.getBinaryStream(fieldFormatRequest.getFieldName()).readAllBytes();
                        byte[] convertedImageData = convertBiometric(null, fieldFormatRequest, byteVal, false);
                        bioDetails.put(fieldMap + (fieldFormatRequest.getSrcFieldForQualityScore() != null ? "_" + resultSet.getString(fieldFormatRequest.getSrcFieldForQualityScore()) : ""), convertedImageData);
                    } else if (fieldFormatRequest.getFieldCategory().equals(FieldCategory.DOC)) {
                        byte[] byteVal = resultSet.getBinaryStream(fieldFormatRequest.getFieldName()).readAllBytes();
                        docDetails.put(fieldMap, Base64.getEncoder().encodeToString(byteVal));
                    }
                }

                if (demoDetails.size() > 0) {
                    packetDto.setFields(packetCreator.setDemographic(demoDetails, (bioDetails.size()>0), dbImportRequest.getIgnoreIdSchemaFields()));
                }

                if (bioDetails.size()>0) {
                    packetDto.setBiometrics(packetCreator.setBiometrics(bioDetails, metaInfo));
                }

                packetDto.setId(generateRegistrationId(ConfigUtil.getConfigUtil().getCenterId(), ConfigUtil.getConfigUtil().getMachineId()));
                packetDto.setRefId(packetDto.getId());
                packetCreator.setMetaData(metaInfo, packetDto, dbImportRequest);
                packetDto.setMetaInfo(metaInfo);
                packetDto.setAudits(packetCreator.setAudits(packetDto.getId()));

                LinkedHashMap<String, Object> idSchema = packetCreator.getLatestIdSchema();
                packetDto.setSchemaJson(idSchema.get("schemaJson").toString());
            }
        } finally {
            if (conn != null)
                conn.close();
        }

        return packetDto;
    }

    private byte[] convertBiometric(String fileNamePrefix, FieldFormatRequest fieldFormatRequest, byte[] bioValue, Boolean localStoreRequired) throws Exception {
        if (localStoreRequired) {
            bioConversion.writeFile(fileNamePrefix + "-" + fieldFormatRequest.getFieldName() , bioValue, fieldFormatRequest.getSrcFormat());
            return bioConversion.writeFile(fileNamePrefix + "-" + fieldFormatRequest.getFieldName(), bioConversion.convertImage(fieldFormatRequest, bioValue), fieldFormatRequest.getDestFormat());
        } else {
            return bioConversion.convertImage(fieldFormatRequest, bioValue);
        }
    }

    private ResultSet readDataFromDatabase(DBImportRequest dbImportRequest, Connection conn) throws Exception {
 //       loadMasterData();
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
                if(fieldFormatRequest.getPrimaryField() != null)
                    uniqueCloumns.add(fieldFormatRequest.getPrimaryField());
                if(fieldFormatRequest.getSrcFieldForQualityScore() != null)
                    uniqueCloumns.add(fieldFormatRequest.getSrcFieldForQualityScore());
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

 /*   private void loadMasterData() throws ApisResourceAccessException {
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

    }*/

    private String generateRegistrationId(String centerId, String machineId) {
        return (String) ridGenerator.generateId(centerId, machineId);
    }
}
