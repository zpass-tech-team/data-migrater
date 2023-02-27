package io.mosip.packet.extractor.validator.impl;

import io.mosip.packet.core.constant.FieldCategory;
import io.mosip.packet.core.dto.dbimport.DBImportRequest;
import io.mosip.packet.core.dto.dbimport.FieldFormatRequest;
import io.mosip.packet.core.dto.dbimport.TableRequestDto;
import io.mosip.packet.core.exception.ApisResourceAccessException;
import io.mosip.packet.extractor.util.PacketCreator;
import io.mosip.packet.extractor.validator.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class IdSchemaFieldValidator implements Validator {

    @Autowired
    private PacketCreator packetCreator;


    List<String> idFields;

    Map<FieldCategory, List<String>> nonIdSchemaFieldsMap;

    private List<String> getIdFields() throws ApisResourceAccessException {
        if (idFields == null) {
            idFields = new ArrayList<>();
            LinkedHashMap<String, Object> idSchema = packetCreator.getLatestIdSchema();

            for(Object obj : (List)idSchema.get("schema")) {
                Map<String, Object> map = (Map<String, Object>) obj;
                String id = map.get("id").toString();
                String type = map.get("type").toString();

                if (type.equals("biometricsType")) {
                    List<String> bioAttributes = (List<String>) map.get("bioAttributes");
                    for(String attribute : bioAttributes)
                        idFields.add(id + "_" + attribute);
                } else {
                    idFields.add(id);
                }
            }
        }
        return idFields;
    }

    public void populateNonIdSchemaFields(DBImportRequest dbImportRequest) {
        nonIdSchemaFieldsMap = new HashMap<>();

        for (TableRequestDto tableRequestDto: dbImportRequest.getTableDetails()) {
            List<FieldCategory> fieldCategoryList = Arrays.asList(tableRequestDto.getFieldCategory());
            if(tableRequestDto.getNonIdSchemaFields() != null) {
                List<String> nonIdSchemaFieldList = Arrays.asList(tableRequestDto.getNonIdSchemaFields());

                for (FieldCategory fieldCategory : fieldCategoryList)
                    nonIdSchemaFieldsMap.put(fieldCategory, nonIdSchemaFieldList);
            }
        }
    }
    @Override
    public Boolean validate(DBImportRequest dbImportRequest) throws Exception {
        List<String> idFieldsList = getIdFields();
        populateNonIdSchemaFields(dbImportRequest);

        for(FieldFormatRequest fieldFormatRequest : dbImportRequest.getColumnDetails()) {
            if(nonIdSchemaFieldsMap == null || !nonIdSchemaFieldsMap.containsKey(fieldFormatRequest.getFieldCategory()) || !nonIdSchemaFieldsMap.get(fieldFormatRequest.getFieldCategory()).contains(fieldFormatRequest.getFieldToMap()))
                if(!idFieldsList.contains(fieldFormatRequest.getFieldToMap()))
                    throw new Exception(fieldFormatRequest.getFieldToMap() + " is not found in Id Schema.");
        }
        return true;
    }
}
