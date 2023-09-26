package io.mosip.packet.core.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.kernel.core.idgenerator.spi.RidGenerator;
import io.mosip.packet.core.constant.ApiName;
import io.mosip.packet.core.constant.DataFormat;
import io.mosip.packet.core.constant.FieldCategory;
import io.mosip.packet.core.dto.ResponseWrapper;
import io.mosip.packet.core.dto.dbimport.DBImportRequest;
import io.mosip.packet.core.dto.dbimport.FieldFormatRequest;
import io.mosip.packet.core.exception.ApisResourceAccessException;
import io.mosip.packet.core.service.DataRestClientService;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Component
public class CommonUtil {

    @Autowired
    private RidGenerator ridGenerator;

    @Autowired
    private DataRestClientService restApiClient;

    private LinkedHashMap<String, Object> latestIdSchemaMap;

    @Value("${mosip.extractor.load.local.idschema:false}")
    private Boolean loadLocalIdSchema;

    @Value("${mosip.extractor.common.bio.default.formats:JP2,ISO}")
    private String defaultBioConvertFormat;

    private ObjectMapper objectMapper = new ObjectMapper();

    public String generateRegistrationId(String centerId, String machineId) {
        return (String) ridGenerator.generateId(centerId, machineId);
    }

    public String getDocumentAttributeStaticValue(String val) {
        return val.substring(val.indexOf(":")+1).trim();
    }

    public LinkedHashMap<String, Object> getLatestIdSchema() throws ApisResourceAccessException, IOException, ParseException {
        if (latestIdSchemaMap == null) {
            ResponseWrapper response= null;

            if(loadLocalIdSchema) {
                Path identityFile = Paths.get(System.getProperty("user.dir"), "idschema.json");
                if (identityFile.toFile().exists()) {
                    JSONParser parser = new JSONParser();
                    JSONObject jsonObject = (JSONObject) parser.parse(IOUtils.toString(new FileInputStream(identityFile.toFile()), StandardCharsets.UTF_8));
                    latestIdSchemaMap = objectMapper.readValue(jsonObject.get("response").toString(), new TypeReference<LinkedHashMap<String, Object>>() {});
                }
            } else {
                response= (ResponseWrapper) restApiClient.getApi(ApiName.LATEST_ID_SCHEMA, null, "", "", ResponseWrapper.class);
                latestIdSchemaMap = (LinkedHashMap<String, Object> ) response.getResponse();
            }
        }
        return latestIdSchemaMap;
    }

    public void updateFieldCategory(DBImportRequest dbImportRequest) throws ApisResourceAccessException, IOException, ParseException {
        LinkedHashMap<String, Object> idSchema = getLatestIdSchema();
        LinkedHashMap<String, FieldCategory> fieldMap = new LinkedHashMap<>();

        for(Object obj : (List)idSchema.get("schema")) {
            Map<String, Object> map = (Map<String, Object>) obj;
            String id = map.get("id").toString();
            String type = map.get("type").toString();

            if(type.equalsIgnoreCase("documentType"))
                fieldMap.put(id, FieldCategory.DOC);
            else if(type.equalsIgnoreCase("biometricsType")) {
                List<String> bioAttributes = (List<String>) map.get("bioAttributes");
                for(String attribute : bioAttributes)
                    fieldMap.put(id + "_" + attribute, FieldCategory.BIO);
            } else
                fieldMap.put(id, FieldCategory.DEMO);
        }

        for(FieldFormatRequest request : dbImportRequest.getColumnDetails())
            if(request.getFieldCategory() == null) {
                HashSet<FieldCategory> availableCategory = new HashSet<>();
                String[] fields = request.getFieldToMap().split(",");

                for(String field : fields)
                    availableCategory.add(fieldMap.get(field) == null ? FieldCategory.DEMO : fieldMap.get(field));

                if(availableCategory.contains(FieldCategory.BIO))
                    request.setFieldCategory(FieldCategory.BIO);
                else if(availableCategory.contains(FieldCategory.DOC))
                    request.setFieldCategory(FieldCategory.DOC);
                else if(availableCategory.contains(FieldCategory.DEMO))
                    request.setFieldCategory(FieldCategory.DEMO);
            }
    }

    public void updateBioDestFormat(DBImportRequest dbImportRequest) throws ApisResourceAccessException, IOException, ParseException {
        LinkedHashMap<String, Object> idSchema = getLatestIdSchema();
        LinkedHashMap<String, List<DataFormat>> fieldMap = new LinkedHashMap<>();

        for(Object obj : (List)idSchema.get("schema")) {
            Map<String, Object> map = (Map<String, Object>) obj;
            String id = map.get("id").toString();
            String type = map.get("type").toString();

            if(type.equalsIgnoreCase("biometricsType")) {
                List<String> bioAttributes = (List<String>) map.get("bioAttributes");

                List<DataFormat> formatList = new ArrayList<>();
                for(String format : defaultBioConvertFormat.split(","))
                    formatList.add(DataFormat.valueOf(format));

                for(String attribute : bioAttributes) {
                    fieldMap.put(id + "_" + attribute, formatList);
                }
            }
        }

        for(FieldFormatRequest request : dbImportRequest.getColumnDetails())
            if(request.getDestFormat() == null)
                request.setDestFormat(fieldMap.get(request.getFieldToMap().split(",")[0]));
    }

    public Object[] getBioAttributesforAll() throws ApisResourceAccessException, IOException, ParseException {
        LinkedHashMap<String, Object> idSchema = getLatestIdSchema();
        List<String> attributes = new ArrayList<>();

        for(Object obj : (List)idSchema.get("schema")) {
            Map<String, Object> map = (Map<String, Object>) obj;
            String id = map.get("id").toString();
            String type = map.get("type").toString();

            if(type.equalsIgnoreCase("biometricsType")) {
                List<String> bioAttributes = (List<String>) map.get("bioAttributes");
                for(String attribute : bioAttributes)
                    attributes.add(id + "_" + attribute);
            }
        }
        return attributes.toArray();
    }
}
