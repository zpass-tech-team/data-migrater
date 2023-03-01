package io.mosip.packet.extractor.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import io.mosip.commons.packet.dto.Document;
import io.mosip.commons.packet.dto.PacketInfo;
import io.mosip.commons.packet.dto.packet.PacketDto;
import io.mosip.packet.core.constant.*;
import io.mosip.packet.core.constant.mvel.ParameterType;
import io.mosip.packet.core.dto.dbimport.*;
import io.mosip.packet.core.dto.upload.PacketUploadDTO;
import io.mosip.packet.core.dto.mvel.MvelParameter;
import io.mosip.packet.core.dto.masterdata.DocumentCategoryDto;
import io.mosip.packet.core.dto.masterdata.DocumentTypeExtnDto;
import io.mosip.packet.core.dto.upload.PacketUploadResponseDTO;
import io.mosip.packet.core.util.DateUtils;
import io.mosip.packet.core.util.QueryFormatter;
import io.mosip.packet.extractor.service.CustomNativeRepository;
import io.mosip.packet.extractor.service.DataExtractionService;
import io.mosip.packet.extractor.util.*;
import io.mosip.kernel.core.idgenerator.spi.RidGenerator;
import io.mosip.packet.manager.service.PacketCreatorService;
//import io.mosip.packet.uploader.service.PacketUploaderService;
import io.mosip.packet.uploader.service.PacketUploaderService;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
import java.util.Date;

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

    @Autowired
    ValidationUtil validationUtil;

    @Autowired
    PacketCreatorService packetCreatorService;

    @Autowired
    PacketUploaderService packetUploaderService;

    @Value("${packet.manager.account.name}")
    private String packetUploadPath;

    @Value("${mosip.selected.languages}")
    private String primaryLanguage;

    @Autowired
    private QueryFormatter formatter;

    private Connection conn = null;

    private Statement statement = null;

    private Map<FieldCategory, LinkedHashMap<String, Object>> dataMap = new HashMap<>();

    private LinkedHashMap<String, DocumentCategoryDto> documentCategory = new LinkedHashMap<>();
    private LinkedHashMap<String, DocumentTypeExtnDto> documentType = new LinkedHashMap<>();
    private Map<FieldCategory, HashSet<String>> fieldsCategoryMap = new HashMap<>();

    @Override
    public LinkedHashMap<String, Object> extractBioDataFromDBAsBytes(DBImportRequest dbImportRequest, Boolean localStoreRequired) throws Exception {
        LinkedHashMap<String, Object> biodata = new LinkedHashMap<>();

        try {
/*            connectDatabase(dbImportRequest);
            ResultSet resultSet = readDataFromDatabase(dbImportRequest.);

            while (resultSet.next()) {
                for (FieldFormatRequest fieldFormatRequest : dbImportRequest.getColumnDetails()) {
                    byte[] byteVal = resultSet.getBinaryStream(fieldFormatRequest.getFieldName()).readAllBytes();
                    byte[] convertedImageData = convertBiometric(resultSet.getString(fieldFormatRequest.getPrimaryField()), fieldFormatRequest, byteVal, localStoreRequired);
                    biodata.put(resultSet.getString(fieldFormatRequest.getPrimaryField())+ "-" + fieldFormatRequest.getFieldName(),  convertedImageData);
                }
            }*/
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
        PacketDto packetDto = null;
        dataMap.clear();

        try {
            List<ValidatorEnum> enumList = new ArrayList<>();
            enumList.add(ValidatorEnum.ID_SCHEMA_VALIDATOR);
            enumList.add(ValidatorEnum.FILTER_VALIDATOR);
            validationUtil.validateRequest(dbImportRequest, enumList);
            populateTableFields(dbImportRequest);
            connectDatabase(dbImportRequest);

            dataMap.put(FieldCategory.DEMO, new LinkedHashMap<>());
            dataMap.put(FieldCategory.BIO, new LinkedHashMap<>());
            dataMap.put(FieldCategory.DOC, new LinkedHashMap<>());

            List<TableRequestDto> tableRequestDtoList = dbImportRequest.getTableDetails();
            Collections.sort(tableRequestDtoList);
            TableRequestDto tableRequestDto  = tableRequestDtoList.get(0);
            ResultSet resultSet = null;
            resultSet = readDataFromDatabase(tableRequestDto);

            while (resultSet != null && resultSet.next()) {
                populateDataFromResultSet(tableRequestDto, dbImportRequest.getColumnDetails(), resultSet);

                for (int i = 1; i < tableRequestDtoList.size(); i++) {
                    tableRequestDto  = tableRequestDtoList.get(i);
                    resultSet = null;
                    resultSet = readDataFromDatabase(tableRequestDto);

                    while (resultSet != null && resultSet.next()) {
                        populateDataFromResultSet(tableRequestDto, dbImportRequest.getColumnDetails(), resultSet);
                    }
                }

                packetDto = new PacketDto();
                packetDto.setProcess(dbImportRequest.getProcess());
                packetDto.setSource("DATAMIGRATOR");
                packetDto.setSchemaVersion(String.valueOf(packetCreator.getLatestIdSchema().get("idVersion")));
                packetDto.setAdditionalInfoReqId(null);
                packetDto.setMetaInfo(null);
                packetDto.setOfflineMode(false);

                LinkedHashMap<String, Object> demoDetails = dataMap.get(FieldCategory.DEMO);
                LinkedHashMap<String, Object> bioDetails = dataMap.get(FieldCategory.BIO);
                LinkedHashMap<String, Object> docDetails = dataMap.get(FieldCategory.DOC);
                LinkedHashMap<String, String> metaInfo = new LinkedHashMap<>();

                if (docDetails.size()>0) {
                    packetDto.setDocuments(packetCreator.setDocuments(docDetails, dbImportRequest.getIgnoreIdSchemaFields(), metaInfo, demoDetails));
                }

                if (demoDetails.size() > 0) {
                    packetDto.setFields(packetCreator.setDemographic(demoDetails, (bioDetails.size()>0), dbImportRequest.getIgnoreIdSchemaFields()));
                }

                if (bioDetails.size()>0) {
                    packetDto.setBiometrics(packetCreator.setBiometrics(bioDetails, metaInfo));
                }

                packetDto.setId(generateRegistrationId(ConfigUtil.getConfigUtil().getCenterId(), ConfigUtil.getConfigUtil().getMachineId()));
                packetDto.setRefId(ConfigUtil.getConfigUtil().getCenterId()+ "_" + ConfigUtil.getConfigUtil().getMachineId());
                packetCreator.setMetaData(metaInfo, packetDto, dbImportRequest);
                packetDto.setMetaInfo(metaInfo);
                packetDto.setAudits(packetCreator.setAudits(packetDto.getId()));

                LinkedHashMap<String, Object> idSchema = packetCreator.getLatestIdSchema();
                packetDto.setSchemaJson(idSchema.get("schemaJson").toString());
                packetDto.setOfflineMode(true);

                // TODO Remove this break while  integrate with production // This is Testing purpose only
                List<PacketInfo> infoList = packetCreatorService.persistPacket(packetDto);

                Path identityFile = Paths.get(System.getProperty("user.dir"), "identity.json");
                if (identityFile.toFile().exists()) {
                    PacketUploadDTO uploadDTO = new PacketUploadDTO();

                    JSONParser parser = new JSONParser();
                    JSONObject jsonObject = (JSONObject) parser.parse(IOUtils.toString(new FileInputStream(identityFile.toFile()), StandardCharsets.UTF_8));
                    JSONObject identityJsonObject = (JSONObject) jsonObject.get("identity");
                    for (Object entry : identityJsonObject.keySet()) {
                        String val = (String) ((JSONObject)identityJsonObject.get(entry)).get("value");
                        if (val.contains(",")) {
                            String[] valList = val.split(",");
                            String fullVal = null;

                            for (String val2 : valList) {
                                if(fullVal == null) {
                                    fullVal= (String) demoDetails.get(val2);
                                } else {
                                    fullVal += " " + demoDetails.get(val2);
                                }
                            }
                            uploadDTO.setValue(entry.toString(), fullVal);
                        } else {
                            uploadDTO.setValue(entry.toString(), demoDetails.get(entry));
                        }
                    }

                    PacketInfo info = infoList.get(0);
                    Path path = Paths.get(System.getProperty("user.dir"), "home/" + packetUploadPath);
                    uploadDTO.setPacketPath(path.toAbsolutePath().toString());
                    uploadDTO.setRegistrationType(dbImportRequest.getProcess());
                    uploadDTO.setPacketId(info.getId());
                    uploadDTO.setRegistrationId(info.getId().split("-")[0]);
                    uploadDTO.setLangCode(primaryLanguage);

                    List<PacketUploadDTO> uploadList = new ArrayList<>();
                    uploadList.add(uploadDTO);
                    LinkedHashMap<String, PacketUploadResponseDTO> response = new LinkedHashMap<>();
                    packetUploaderService.syncPacket(uploadList, ConfigUtil.getConfigUtil().getCenterId(), ConfigUtil.getConfigUtil().getMachineId(), response);
                    packetUploaderService.uploadSyncedPacket(uploadList, response);

                    System.out.println((new Gson()).toJson(response));
                } else {
                    throw new Exception("Identity Mapping JSON File missing");
                }
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

    private void connectDatabase(DBImportRequest dbImportRequest) throws SQLException {
        if(conn != null)
            conn.close();

        if (dbImportRequest.getDbType().equals(DBTypes.MSSQL)) {
            DriverManager.registerDriver(new com.microsoft.sqlserver.jdbc.SQLServerDriver());
            conn = DriverManager.getConnection("jdbc:sqlserver://" + dbImportRequest.getUrl() + ";sslProtocol=TLSv1.2;databaseName=" + dbImportRequest.getDatabaseName()+ ";Trusted_Connection=True;", dbImportRequest.getUserId(), dbImportRequest.getPassword());
        }

        System.out.println("Database Successfully connected");
    }

    private void populateTableFields(DBImportRequest dbImportRequest) throws Exception {
        fieldsCategoryMap.clear();
        fieldsCategoryMap.put(FieldCategory.DEMO, new HashSet<>());
        fieldsCategoryMap.put(FieldCategory.BIO, new HashSet<>());
        fieldsCategoryMap.put(FieldCategory.DOC, new HashSet<>());

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

            fieldsCategoryMap.get(fieldFormatRequest.getFieldCategory()).add(field);
            if(fieldFormatRequest.getPrimaryField() != null)
                fieldsCategoryMap.get(fieldFormatRequest.getFieldCategory()).add(fieldFormatRequest.getPrimaryField());
            if(fieldFormatRequest.getSrcFieldForQualityScore() != null)
                fieldsCategoryMap.get(fieldFormatRequest.getFieldCategory()).add(fieldFormatRequest.getSrcFieldForQualityScore());

            if(fieldFormatRequest.getDocumentAttributes() != null) {
                DocumentAttributes documentAttributes = fieldFormatRequest.getDocumentAttributes();
                fieldsCategoryMap.get(fieldFormatRequest.getFieldCategory()).add(documentAttributes.getDocumentRefNoField().contains("STATIC") ? "'" + getDocumentAttributeStaticValue(documentAttributes.getDocumentRefNoField()) + "' AS STATIC_" +  getDocumentAttributeStaticValue(documentAttributes.getDocumentRefNoField())
                        :  documentAttributes.getDocumentRefNoField());
                fieldsCategoryMap.get(fieldFormatRequest.getFieldCategory()).add(documentAttributes.getDocumentFormatField().contains("STATIC") ? "'" + getDocumentAttributeStaticValue(documentAttributes.getDocumentFormatField()) + "' AS STATIC_" + getDocumentAttributeStaticValue(documentAttributes.getDocumentFormatField())
                        :  documentAttributes.getDocumentFormatField());
                fieldsCategoryMap.get(fieldFormatRequest.getFieldCategory()).add(documentAttributes.getDocumentCodeField().contains("STATIC") ? "'" + getDocumentAttributeStaticValue(documentAttributes.getDocumentCodeField()) + "' AS STATIC_" + getDocumentAttributeStaticValue(documentAttributes.getDocumentCodeField())
                        :  documentAttributes.getDocumentCodeField());
            }
        }
    }
    private ResultSet readDataFromDatabase(TableRequestDto tableRequestDto) throws Exception {
        if(conn != null) {
            return getResult(tableRequestDto);
        } else
            throw new SQLException("Unable to Connect With Database. Please check the Configuration");
    }

    private String generateRegistrationId(String centerId, String machineId) {
        return (String) ridGenerator.generateId(centerId, machineId);
    }

    private String getDocumentAttributeStaticValue(String val) {
        return val.substring(val.indexOf(":")+1).trim();
    }

    private ResultSet getResult(TableRequestDto tableRequestDto) throws Exception {
        if (tableRequestDto.getQueryType().equals(QuerySelection.TABLE)) {

            List<FieldCategory> tableMap = Arrays.asList(tableRequestDto.getFieldCategory());

            String columnNames = null;

            for(FieldCategory category : tableMap) {
                for(String column : fieldsCategoryMap.get(category)) {
                    if (columnNames == null)
                        columnNames = column;
                    else
                        columnNames += "," + column;
                }
            }

/*            if (tableRequestDto.getNonIdSchemaFields() != null && tableRequestDto.getNonIdSchemaFields().length > 0) {
                List<String> nonIdSchemaFields = Arrays.asList(tableRequestDto.getNonIdSchemaFields());
                for(String column : nonIdSchemaFields) {
                    if (columnNames == null)
                        columnNames = column;
                    else
                        columnNames += "," + column;
                }
            }*/

            if(statement != null)
                statement.close();

            statement = conn.createStatement();

            String filterCondition = null;
            boolean whereCondition= false;

            for (QueryFilter queryFilter : tableRequestDto.getFilters()) {
                if (!whereCondition) {
                    filterCondition = " WHERE ";
                    whereCondition=true;
                } else {
                    filterCondition += " AND ";
                }

                filterCondition += queryFilter.getFilterField() + " " + queryFilter.getFilterCondition().format(queryFilter.getFromValue(), queryFilter.getToValue(), queryFilter.getFieldType());
            }

            String selectSql = "SELECT " + columnNames + "  from " + tableRequestDto.getTableName();
            selectSql += filterCondition;
            return statement.executeQuery(formatter.replaceColumntoDataIfAny(selectSql, dataMap));
        } else if (tableRequestDto.getQueryType().equals(QuerySelection.SQL_QUERY)) {
            if(statement != null)
                statement.close();

            statement = conn.createStatement();

            return statement.executeQuery(formatter.replaceColumntoDataIfAny(tableRequestDto.getSqlQuery(), dataMap));
        } else
            return null;
    }

    private void populateDataFromResultSet(TableRequestDto tableRequestDto, List<FieldFormatRequest> columnDetails, ResultSet resultSet) throws Exception {
        List<FieldCategory> availableCategory = Arrays.asList(tableRequestDto.getFieldCategory());

        for (FieldFormatRequest fieldFormatRequest : columnDetails) {
            dataMapper(availableCategory, fieldFormatRequest, resultSet);
        }
    }

    private void dataMapper(List<FieldCategory> availableCategory, FieldFormatRequest fieldFormatRequest, ResultSet resultSet) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        if (availableCategory.contains(fieldFormatRequest.getFieldCategory())) {
            String fieldName = fieldFormatRequest.getFieldName().contains(",") ? fieldFormatRequest.getFieldName().replace(",", "") : fieldFormatRequest.getFieldName();
            String fieldMap = fieldFormatRequest.getFieldToMap() != null ? fieldFormatRequest.getFieldToMap() : fieldFormatRequest.getFieldName().toLowerCase();
            String originalField = fieldFormatRequest.getFieldName();

            if (fieldFormatRequest.getFieldCategory().equals(FieldCategory.DEMO)) {
                Object demoValue = null;
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

                    demoValue = mvelUtil.processViaMVEL(fieldFormatRequest.getMvelExpressions().getMvelFile(), map);
                } else {
                    demoValue = resultSet.getObject(fieldName);
                }

                if (fieldFormatRequest.getDestFormat() != null) {
                    if (fieldFormatRequest.getDestFormat().equals(DataFormat.DMY) || fieldFormatRequest.getDestFormat().equals(DataFormat.YMD)) {
                        Date dateVal = DateUtils.findDateFormat(demoValue.toString());
                        demoValue = DateUtils.parseDate(dateVal, fieldFormatRequest.getDestFormat().getFormat());
                    } else {
                        throw new Exception("Invalid Format for Conversion for Demo Details for Field : " + fieldFormatRequest.getFieldName());
                    }
                }

                dataMap.get(fieldFormatRequest.getFieldCategory()).put(fieldMap, demoValue);
                dataMap.get(fieldFormatRequest.getFieldCategory()).put(originalField, demoValue);

            } else if (fieldFormatRequest.getFieldCategory().equals(FieldCategory.BIO)) {
                byte[] byteVal = resultSet.getBinaryStream(fieldFormatRequest.getFieldName()).readAllBytes();
                byte[] convertedImageData = convertBiometric(null, fieldFormatRequest, byteVal, false);
                dataMap.get(fieldFormatRequest.getFieldCategory()).put(fieldMap + (fieldFormatRequest.getSrcFieldForQualityScore() != null ? "_" + resultSet.getString(fieldFormatRequest.getSrcFieldForQualityScore()) : ""), convertedImageData);
            } else if (fieldFormatRequest.getFieldCategory().equals(FieldCategory.DOC)) {
                Document document = new Document();
                document.setDocument(resultSet.getBinaryStream(fieldFormatRequest.getFieldName()).readAllBytes());
                if(fieldFormatRequest.getDocumentAttributes() != null) {
                    DocumentAttributes documentAttributes = fieldFormatRequest.getDocumentAttributes();
                    String refField = documentAttributes.getDocumentRefNoField().contains("STATIC") ? "STATIC_" +  getDocumentAttributeStaticValue(documentAttributes.getDocumentRefNoField())
                            :  documentAttributes.getDocumentRefNoField();
                    document.setRefNumber(resultSet.getString(refField));
                    dataMap.get(fieldFormatRequest.getFieldCategory()).put(fieldMap + ":" + refField, document.getRefNumber());

                    String formatField = documentAttributes.getDocumentFormatField().contains("STATIC") ? "STATIC_" + getDocumentAttributeStaticValue(documentAttributes.getDocumentFormatField())
                            :  documentAttributes.getDocumentFormatField();
                    document.setFormat(resultSet.getString(formatField));
                    dataMap.get(fieldFormatRequest.getFieldCategory()).put(fieldMap + ":" + formatField, document.getFormat());

                    String codeField = documentAttributes.getDocumentCodeField().contains("STATIC") ? "STATIC_" + getDocumentAttributeStaticValue(documentAttributes.getDocumentCodeField())
                            :  documentAttributes.getDocumentCodeField();
                    document.setType(resultSet.getString(codeField));
                    dataMap.get(fieldFormatRequest.getFieldCategory()).put(fieldMap + ":" + codeField, document.getType());
                }

                dataMap.get(fieldFormatRequest.getFieldCategory()).put(fieldMap, mapper.writeValueAsString(document));
            }
        }
    }
}
