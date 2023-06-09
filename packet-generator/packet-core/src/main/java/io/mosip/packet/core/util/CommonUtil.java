package io.mosip.packet.core.util;

import io.mosip.kernel.core.idgenerator.spi.RidGenerator;
import io.mosip.packet.core.constant.ApiName;
import io.mosip.packet.core.constant.DataFormat;
import io.mosip.packet.core.constant.FieldCategory;
import io.mosip.packet.core.dto.ResponseWrapper;
import io.mosip.packet.core.dto.dbimport.DBImportRequest;
import io.mosip.packet.core.dto.dbimport.FieldFormatRequest;
import io.mosip.packet.core.exception.ApisResourceAccessException;
import io.mosip.packet.core.service.DataRestClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class CommonUtil {

    @Autowired
    private RidGenerator ridGenerator;

    @Autowired
    private DataRestClientService restApiClient;

    private LinkedHashMap<String, Object> latestIdSchemaMap;

    public String generateRegistrationId(String centerId, String machineId) {
        return (String) ridGenerator.generateId(centerId, machineId);
    }

    public String getDocumentAttributeStaticValue(String val) {
        return val.substring(val.indexOf(":")+1).trim();
    }

    public LinkedHashMap<String, Object> getLatestIdSchema() throws ApisResourceAccessException {
        if (latestIdSchemaMap == null) {
            ResponseWrapper response= (ResponseWrapper) restApiClient.getApi(ApiName.LATEST_ID_SCHEMA, null, "", "", ResponseWrapper.class);
            latestIdSchemaMap = (LinkedHashMap<String, Object> ) response.getResponse();
        }
        return latestIdSchemaMap;
    }

    public void updateFieldCategory(DBImportRequest dbImportRequest) throws ApisResourceAccessException {
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
            if(request.getFieldCategory() == null)
                request.setFieldCategory(fieldMap.get(request.getFieldToMap()) == null ? FieldCategory.DEMO : fieldMap.get(request.getFieldToMap()));
    }

    public void updateBioDestFormat(DBImportRequest dbImportRequest) throws ApisResourceAccessException {
        LinkedHashMap<String, Object> idSchema = getLatestIdSchema();
        LinkedHashMap<String, DataFormat> fieldMap = new LinkedHashMap<>();

        for(Object obj : (List)idSchema.get("schema")) {
            Map<String, Object> map = (Map<String, Object>) obj;
            String id = map.get("id").toString();
            String type = map.get("type").toString();

            if(type.equalsIgnoreCase("biometricsType")) {
                List<String> bioAttributes = (List<String>) map.get("bioAttributes");
                for(String attribute : bioAttributes)
                    fieldMap.put(id + "_" + attribute, DataFormat.ISO);
            }
        }

        for(FieldFormatRequest request : dbImportRequest.getColumnDetails())
            if(request.getDestFormat() == null)
                request.setDestFormat(fieldMap.get(request.getFieldToMap()));
    }

    public Object[] getBioAttributesforAll() throws ApisResourceAccessException {
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
