package io.mosip.packet.core.util;

import io.mosip.packet.core.constant.FieldCategory;
import io.mosip.packet.core.dto.dbimport.FieldFormatRequest;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public interface DataMapperUtil {
    void dataMapper(FieldFormatRequest fieldFormatRequest, Map<String, Object> resultSet, Map<FieldCategory, LinkedHashMap<String, Object>> dataMap2, String tableName, Map<String, HashMap<String, String>> fieldsCategoryMap, Boolean localStoreRequired) throws Exception;
}
