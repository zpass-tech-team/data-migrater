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
import io.mosip.packet.core.dto.packet.BioData;
import io.mosip.packet.core.service.CustomNativeRepository;
import io.mosip.packet.core.spi.BioConvertorApiFactory;
import io.mosip.packet.core.spi.BioDocApiFactory;
import io.mosip.packet.core.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.*;

@Component
public class TableDataMapperUtil implements DataMapperUtil {

    @Autowired
    private CustomNativeRepository customNativeRepository;

    @Autowired
    private MvelUtil mvelUtil;

    @Autowired
    private BioConvertorApiFactory bioConvertorApiFactory;

    @Autowired
    private CommonUtil commonUtil;

    @Autowired
    private ObjectStoreHelper objectStoreHelper;

    @Value("${mosip.packet.objectstore.fetch.enabled:false}")
    private boolean objectStoreFetchEnabled;

    @Value("${mosip.packet.bio.doc.data.converter.classname:io.mosip.packet.data.convertion.BioDocDataConverter}")
    private String bioDocApiClassName;

    @Autowired
    private List<BioDocApiFactory> bioDocApiFactoryList;

    private BioDocApiFactory bioDocApiFactory;

    private String VALUE_SPLITTER = " ";

    @Autowired
    private QueryFormatter formatter;

    @PostConstruct
    public void loadConfiguration() {
        for(BioDocApiFactory factory : bioDocApiFactoryList) {
            if(factory.getClass().getName().equals(bioDocApiClassName))
                bioDocApiFactory = factory;
        }
    }

    @Override
    public void dataMapper(FieldFormatRequest fieldFormatRequest, Map<String, Object> resultSet, Map<FieldCategory, LinkedHashMap<String, Object>> dataMap2, String tableName, Map<String, HashSet<String>> fieldsCategoryMap, Boolean localStoreRequired) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        DataFormat destFormat = fieldFormatRequest.getDestFormat() != null ? fieldFormatRequest.getDestFormat().get(fieldFormatRequest.getDestFormat().size()-1) : null;
        List<FieldName> fieldNames = fieldFormatRequest.getFieldList();
        String fieldMap = fieldFormatRequest.getFieldToMap() != null ? fieldFormatRequest.getFieldToMap() : fieldNames.get(0).getFieldName().toLowerCase();
        String originalField = fieldFormatRequest.getFieldName();

        if(!dataMap2.get(fieldFormatRequest.getFieldCategory()).containsKey(originalField) && fieldsCategoryMap.get(tableName).contains(fieldNames.get(0).getFieldName())) {
            String mvelValue = null;
            if (fieldFormatRequest.getMvelExpressions() != null) {
                Map map = new HashMap();
                for (MvelParameter parameter : fieldFormatRequest.getMvelExpressions().getParameters()) {
                    if (parameter.getParameterType().equals(ParameterType.STRING))
                        if(parameter.getParameterValue().contains("${")) {
                            try {
                                map.put(parameter.getParameterName(), formatter.replaceColumntoDataIfAny(parameter.getParameterValue(), dataMap2));
                            } catch (Exception e) {
                                String param = parameter.getParameterValue().replace("${", "").replace("}", "");
                                map.put(parameter.getParameterName(), resultSet.get(param));
                            }
                        } else {
                            map.put(parameter.getParameterName(), parameter.getParameterValue());
                        }
                    else if (parameter.getParameterType().equals(ParameterType.SQL)){
                        List<Object> list = (List<Object>) customNativeRepository.runNativeQuery(parameter.getParameterValue());
                        map.put(parameter.getParameterName(), list);
                    }
                }

                mvelValue = mvelUtil.processViaMVEL(fieldFormatRequest.getMvelExpressions().getMvelFile(), map);
            }

            if (fieldFormatRequest.getFieldCategory().equals(FieldCategory.DEMO)) {
                Object demoValue = null;
                if(fieldFormatRequest.getMvelExpressions() != null) {
                    demoValue = mvelValue;
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
                    if (destFormat != null) {
                        if (destFormat.equals(DataFormat.DMY) || destFormat.equals(DataFormat.YMD)) {
                            Date dateVal = DateUtils.findDateFormat(demoValue.toString());
                            demoValue = DateUtils.parseDate(dateVal, destFormat.getFormat());
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
                Map<String, byte[]> map = new HashMap<>();

                if(fieldsCategoryMap.get(tableName).contains(fieldName))  {
                    byte[] convertedImageData = null;
                    byte[] byteVal = null;
                    if(fieldFormatRequest.getMvelExpressions() != null && mvelValue != null) {
                        byteVal = convertObjectToByteArray(mvelValue);;
                    } else {
                        if(resultSet.get(fieldName) != null) {
                            byteVal = convertObjectToByteArray(resultSet.get(fieldName));
                        }
                    }

                    if(byteVal != null) {
                        if(objectStoreFetchEnabled)
                            byteVal = objectStoreHelper.getBiometricObject(new String(byteVal, StandardCharsets.UTF_8));
                        map = bioDocApiFactory.getBioData(byteVal, fieldMap);
                    }

                    for(String field : fieldMap.split(",")) {
                        byte[] bytes = map.get(field);
                        if(bytes != null) {
                            convertedImageData = convertBiometric(dataMap2.get(FieldCategory.DEMO).get(fieldFormatRequest.getPrimaryField()).toString(), fieldFormatRequest, bytes, localStoreRequired, field);
                        }
                        BioData bioData = new BioData();
                        bioData.setBioData(convertedImageData);
                        bioData.setFormat(fieldFormatRequest.getDestFormat().get(fieldFormatRequest.getDestFormat().size()-1));
                        bioData.setQualityScore(fieldFormatRequest.getSrcFieldForQualityScore() != null ? resultSet.get(fieldFormatRequest.getFieldNameWithoutSchema(fieldFormatRequest.getSrcFieldForQualityScore())).toString() : "");
                        dataMap2.get(fieldFormatRequest.getFieldCategory()).put(field, bioData);
                    }
                    dataMap2.get(fieldFormatRequest.getFieldCategory()).put(originalField, "");
                }
            } else if (fieldFormatRequest.getFieldCategory().equals(FieldCategory.DOC)) {
                String fieldName = fieldFormatRequest.getFieldList().get(0).getFieldName();

                if(fieldsCategoryMap.get(tableName).contains(fieldName))  {
                    Document document = new Document();
                    byte[] byteVal = convertObjectToByteArray(resultSet.get(fieldName));
                    if(objectStoreFetchEnabled)
                        byteVal = objectStoreHelper.getBiometricObject(new String(byteVal, StandardCharsets.UTF_8));
                    byteVal = bioDocApiFactory.getDocData(byteVal, fieldMap).get(fieldMap);
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
    }

    private byte[] convertObjectToByteArray(Object obj) throws IOException, SQLException {
        if (obj instanceof String)
            return ((String) obj).getBytes(StandardCharsets.UTF_8);

        if (obj instanceof Blob) {
            Blob blobObj = (Blob) obj;
            return blobObj.getBytes(1, (int) blobObj.length());
        }

        return (byte[]) obj;
    }

    public byte[] convertBiometric(String fileNamePrefix, FieldFormatRequest fieldFormatRequest, byte[] bioValue, Boolean localStoreRequired, String fieldName) throws Exception {
        if (localStoreRequired) {
            bioConvertorApiFactory.writeFile(fileNamePrefix + "-" + fieldFormatRequest.getFieldList().get(0).getFieldName() , bioValue, fieldFormatRequest.getSrcFormat());
            return bioConvertorApiFactory.writeFile(fileNamePrefix + "-" + fieldFormatRequest.getFieldList().get(0).getFieldName(), bioConvertorApiFactory.convertImage(fieldFormatRequest, bioValue, fieldName), fieldFormatRequest.getDestFormat().get(fieldFormatRequest.getDestFormat().size()-1));
        } else {
            return bioConvertorApiFactory.convertImage(fieldFormatRequest, bioValue, fieldName);
        }
    }
}
