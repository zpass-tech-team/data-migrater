package io.mosip.packet.extractor.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.commons.packet.dto.Document;
import io.mosip.packet.core.constant.DataFormat;
import io.mosip.packet.core.constant.FieldCategory;
import io.mosip.packet.core.constant.mvel.ParameterType;
import io.mosip.packet.core.dto.dbimport.DocumentAttributes;
import io.mosip.packet.core.dto.dbimport.FieldFormatRequest;
import io.mosip.packet.core.dto.dbimport.FieldName;
import io.mosip.packet.core.dto.mvel.MvelParameter;
import io.mosip.packet.core.service.CustomNativeRepository;
import io.mosip.packet.core.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Component
public class TableDataMapperUtil implements DataMapperUtil {

    @Autowired
    private CustomNativeRepository customNativeRepository;

    @Autowired
    private MvelUtil mvelUtil;

    @Autowired
    private BioConversion bioConversion;

    @Autowired
    private CommonUtil commonUtil;

    @Override
    public void dataMapper(FieldFormatRequest fieldFormatRequest, Map<String, Object> resultSet, Map<FieldCategory, LinkedHashMap<String, Object>> dataMap2, String tableName, Map<String, HashSet<String>> fieldsCategoryMap) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        List<FieldName> fieldNames = fieldFormatRequest.getFieldList();
        String fieldMap = fieldFormatRequest.getFieldToMap() != null ? fieldFormatRequest.getFieldToMap() : fieldNames.get(0).getFieldName().toLowerCase();
        String originalField = fieldFormatRequest.getFieldName();

        if(!dataMap2.get(fieldFormatRequest.getFieldCategory()).containsKey(originalField))
            if (fieldFormatRequest.getFieldCategory().equals(FieldCategory.DEMO)) {
                Object demoValue = null;
                if (fieldFormatRequest.getMvelExpressions() != null) {
                    Map map = new HashMap();
                    for (MvelParameter parameter : fieldFormatRequest.getMvelExpressions().getParameters()) {
                        if (parameter.getParameterType().equals(ParameterType.STRING))
                            if(parameter.getParameterValue().contains("${")) {
                                String param = parameter.getParameterValue().replace("${", "").replace("}", "");
                                map.put(parameter.getParameterName(), resultSet.get(param));
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
                    demoValue = null;
                    boolean initialEntry = true;

                    for(FieldName field : fieldNames) {
                        if(initialEntry)
                            demoValue = dataMap2.get(fieldFormatRequest.getFieldCategory()).get(fieldMap);
                        initialEntry=false;

                        if(fieldsCategoryMap.get(tableName).contains(field.getFieldName()))
                            if(demoValue == null)
                                demoValue = resultSet.get(field.getFieldName());
                            else {
                                if(demoValue.toString().contains("<" + field.getFieldName() + ">"))
                                    demoValue = demoValue.toString().replace("<" + field.getFieldName() + ">", resultSet.get(field.getFieldName()).toString());
                                else
                                    demoValue += " " + resultSet.get(field.getFieldName());
                            }
                        else
                            demoValue += " <" + field.getFieldName() + ">";
                    }
                }

                if(demoValue != null) {
                    if (fieldFormatRequest.getDestFormat() != null) {
                        if (fieldFormatRequest.getDestFormat().equals(DataFormat.DMY) || fieldFormatRequest.getDestFormat().equals(DataFormat.YMD)) {
                            Date dateVal = DateUtils.findDateFormat(demoValue.toString());
                            demoValue = DateUtils.parseDate(dateVal, fieldFormatRequest.getDestFormat().getFormat());
                        } else {
                            throw new Exception("Invalid Format for Conversion for Demo Details for Field : " + fieldFormatRequest.getFieldName());
                        }
                    }

                    dataMap2.get(fieldFormatRequest.getFieldCategory()).put(fieldMap, demoValue);
                    dataMap2.get(fieldFormatRequest.getFieldCategory()).put(originalField, demoValue);
                }
            } else if (fieldFormatRequest.getFieldCategory().equals(FieldCategory.BIO)) {
                String fieldName = fieldFormatRequest.getFieldList().get(0).getFieldName();
                if(fieldsCategoryMap.get(tableName).contains(fieldName))  {
                    byte[] byteVal = convertObjectToByteArray(resultSet.get(fieldName));
                    byte[] convertedImageData = convertBiometric(null, fieldFormatRequest, byteVal, false);
                    dataMap2.get(fieldFormatRequest.getFieldCategory()).put(fieldMap + (fieldFormatRequest.getSrcFieldForQualityScore() != null ? "_" + resultSet.get(fieldFormatRequest.getFieldNameWithoutSchema(fieldFormatRequest.getSrcFieldForQualityScore())) : ""), convertedImageData);
                    dataMap2.get(fieldFormatRequest.getFieldCategory()).put(originalField, "");
                }
            } else if (fieldFormatRequest.getFieldCategory().equals(FieldCategory.DOC)) {
                String fieldName = fieldFormatRequest.getFieldList().get(0).getFieldName();

                if(fieldsCategoryMap.get(tableName).contains(fieldName))  {
                    Document document = new Document();
                    document.setDocument(convertObjectToByteArray(resultSet.get(fieldName)));
                    if(fieldFormatRequest.getDocumentAttributes() != null) {
                        DocumentAttributes documentAttributes = fieldFormatRequest.getDocumentAttributes();
                        String refField = documentAttributes.getDocumentRefNoField().contains("STATIC") ? "STATIC_" +  commonUtil.getDocumentAttributeStaticValue(documentAttributes.getDocumentRefNoField())
                                :  fieldFormatRequest.getFieldNameWithoutSchema(documentAttributes.getDocumentRefNoField());
                        document.setRefNumber(String.valueOf(resultSet.get(refField)));
                        dataMap2.get(fieldFormatRequest.getFieldCategory()).put(fieldMap + ":" + refField, document.getRefNumber());

                        String formatField = documentAttributes.getDocumentFormatField().contains("STATIC") ? "STATIC_" + commonUtil.getDocumentAttributeStaticValue(documentAttributes.getDocumentFormatField())
                                :  fieldFormatRequest.getFieldNameWithoutSchema(documentAttributes.getDocumentFormatField());
                        document.setFormat(String.valueOf(resultSet.get(formatField)));
                        dataMap2.get(fieldFormatRequest.getFieldCategory()).put(fieldMap + ":" + formatField, document.getFormat());

                        String codeField = documentAttributes.getDocumentCodeField().contains("STATIC") ? "STATIC_" + commonUtil.getDocumentAttributeStaticValue(documentAttributes.getDocumentCodeField())
                                :  fieldFormatRequest.getFieldNameWithoutSchema(documentAttributes.getDocumentCodeField());
                        document.setType(String.valueOf(resultSet.get(codeField)));
                        dataMap2.get(fieldFormatRequest.getFieldCategory()).put(fieldMap + ":" + codeField, document.getType());
                    }

                    dataMap2.get(fieldFormatRequest.getFieldCategory()).put(fieldMap, mapper.writeValueAsString(document));
                    dataMap2.get(fieldFormatRequest.getFieldCategory()).put(originalField, "");
                }
            }
    }

    private byte[] convertObjectToByteArray(Object obj) throws IOException {
        return (byte[]) obj;
    }

    public byte[] convertBiometric(String fileNamePrefix, FieldFormatRequest fieldFormatRequest, byte[] bioValue, Boolean localStoreRequired) throws Exception {
        if (localStoreRequired) {
            bioConversion.writeFile(fileNamePrefix + "-" + fieldFormatRequest.getFieldList().get(0) , bioValue, fieldFormatRequest.getSrcFormat());
            return bioConversion.writeFile(fileNamePrefix + "-" + fieldFormatRequest.getFieldList().get(0), bioConversion.convertImage(fieldFormatRequest, bioValue), fieldFormatRequest.getDestFormat());
        } else {
            return bioConversion.convertImage(fieldFormatRequest, bioValue);
        }
    }
}
