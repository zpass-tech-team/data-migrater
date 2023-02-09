package io.mosip.data.validator;

import io.mosip.data.dto.dbimport.DBImportRequest;
import io.mosip.data.dto.dbimport.FieldFormatRequest;
import io.mosip.data.exception.ApisResourceAccessException;
import io.mosip.data.util.PacketCreator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class IdSchemaFieldValidator {

    @Autowired
    private PacketCreator packetCreator;


    List<String> idFields;

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

    public Boolean validate(DBImportRequest dbImportRequest) throws Exception {
        List<String> idFieldsList = getIdFields();

        for(FieldFormatRequest fieldFormatRequest : dbImportRequest.getColumnDetails()) {
            if(!idFieldsList.contains(fieldFormatRequest.getFieldToMap()))
                throw new Exception(fieldFormatRequest.getFieldToMap() + " is not found in Id Schema.");
        }
        return true;
    }
}
