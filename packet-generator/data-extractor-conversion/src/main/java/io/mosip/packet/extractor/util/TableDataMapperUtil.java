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
import io.mosip.packet.core.spi.BioDocApiFactory;
import io.mosip.packet.core.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

    @Autowired
    private ObjectStoreHelper objectStoreHelper;

    @Value("${mosip.packet.objectstore.fetch.enabled:false}")
    private boolean objectStoreFetchEnabled;

    @Autowired
    private BioDocApiFactory bioDocApiFactory;

    private String VALUE_SPLITTER = " ";

    @Override
    public void dataMapper(FieldFormatRequest fieldFormatRequest, Map<String, Object> resultSet, Map<FieldCategory, LinkedHashMap<String, Object>> dataMap2, String tableName, Map<String, HashSet<String>> fieldsCategoryMap, Boolean localStoreRequired) throws Exception {
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
                                    demoValue += VALUE_SPLITTER + resultSet.get(field.getFieldName());
                            }
                        else
                            demoValue += " <" + field.getFieldName() + ">";

                        dataMap2.get(fieldFormatRequest.getFieldCategory()).put(field.getTableName() + "." + field.getFieldName(), resultSet.get(field.getFieldName()));
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

                    String[] fieldMapArray = fieldMap.split(",");
                    if(fieldMapArray.length > 0) {
                        int arrayLength = fieldMapArray.length;
                        String[] mapArray = demoValue.toString().split(VALUE_SPLITTER);
                        int maplength = mapArray.length;

                        if(arrayLength >= maplength) {
                            for(int i = 0; i < arrayLength; i++)
                                dataMap2.get(fieldFormatRequest.getFieldCategory()).put(fieldMapArray[i], (mapArray.length < i+1) ? null : mapArray[i]);
                        } else if(arrayLength < maplength) {
                            int difference = Double.valueOf(Math.ceil(Float.valueOf(maplength) / Float.valueOf(arrayLength))).intValue();

                            String[] newArray = new String[arrayLength];
                            int i = 0;
                            int k = 0;
                            do {
                                String val = "";
                                for(int j = 0; j < difference; j++)
                                    val+= (mapArray.length < k+1) ? "" : mapArray[k++] + VALUE_SPLITTER;
                                newArray[i] = val;
                            } while(++i < arrayLength);

                            for(int z = 0; z < arrayLength; z++)
                                dataMap2.get(fieldFormatRequest.getFieldCategory()).put(fieldMapArray[z], (newArray.length < z+1) ? null : newArray[z]);
                        }
                    } else {
                        dataMap2.get(fieldFormatRequest.getFieldCategory()).put(fieldMap, demoValue);
                    }

                    dataMap2.get(fieldFormatRequest.getFieldCategory()).put(originalField, demoValue);
                }
            } else if (fieldFormatRequest.getFieldCategory().equals(FieldCategory.BIO)) {
                String fieldName = fieldFormatRequest.getFieldList().get(0).getFieldName();
                if(fieldsCategoryMap.get(tableName).contains(fieldName))  {
                    byte[] byteVal = convertObjectToByteArray(resultSet.get(fieldName));
                    if(objectStoreFetchEnabled)
                        byteVal = objectStoreHelper.getBiometricObject(new String(byteVal, StandardCharsets.UTF_8));
                    byteVal = bioDocApiFactory.getBioData(byteVal, fieldMap);
                    byte[] convertedImageData = convertBiometric(dataMap2.get(FieldCategory.DEMO).get(fieldFormatRequest.getPrimaryField()).toString(), fieldFormatRequest, byteVal, localStoreRequired);
                    dataMap2.get(fieldFormatRequest.getFieldCategory()).put(fieldMap + (fieldFormatRequest.getSrcFieldForQualityScore() != null ? "_" + resultSet.get(fieldFormatRequest.getFieldNameWithoutSchema(fieldFormatRequest.getSrcFieldForQualityScore())) : ""), convertedImageData);
                    dataMap2.get(fieldFormatRequest.getFieldCategory()).put(originalField, "");
                }
            } else if (fieldFormatRequest.getFieldCategory().equals(FieldCategory.DOC)) {
                String fieldName = fieldFormatRequest.getFieldList().get(0).getFieldName();

                if(fieldsCategoryMap.get(tableName).contains(fieldName))  {
                    Document document = new Document();
                    byte[] byteVal = convertObjectToByteArray(resultSet.get(fieldName));
                    if(objectStoreFetchEnabled)
                        byteVal = objectStoreHelper.getBiometricObject(new String(byteVal, StandardCharsets.UTF_8));
                    byteVal = bioDocApiFactory.getDocData(byteVal, fieldMap);
                    document.setDocument(byteVal);
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
            bioConversion.writeFile(fileNamePrefix + "-" + fieldFormatRequest.getFieldList().get(0).getFieldName() , bioValue, fieldFormatRequest.getSrcFormat());
            return bioConversion.writeFile(fileNamePrefix + "-" + fieldFormatRequest.getFieldList().get(0).getFieldName(), bioConversion.convertImage(fieldFormatRequest, bioValue), fieldFormatRequest.getDestFormat());
        } else {
            return bioConversion.convertImage(fieldFormatRequest, bioValue);
        }
    }
}
