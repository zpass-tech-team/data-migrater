package io.mosip.packet.core.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.commons.packet.dto.Document;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.packet.core.constant.DBTypes;
import io.mosip.packet.core.constant.DataFormat;
import io.mosip.packet.core.constant.FieldCategory;
import io.mosip.packet.core.constant.QuerySelection;
import io.mosip.packet.core.constant.mvel.ParameterType;
import io.mosip.packet.core.dto.dbimport.*;
import io.mosip.packet.core.dto.mvel.MvelParameter;
import io.mosip.packet.core.logger.DataProcessLogger;
import io.mosip.packet.core.service.CustomNativeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.Date;

import static io.mosip.packet.core.constant.RegistrationConstants.*;
import static io.mosip.packet.core.constant.RegistrationConstants.DEFAULT_TABLE;

@Component
public class DataBaseUtil {
    private static final Logger LOGGER = DataProcessLogger.getLogger(DataBaseUtil.class);
    private Connection conn = null;
    private Statement statement = null;
    private boolean isTrackerSameHost = false;
    private String trackColumn = null;

    @Autowired
    private QueryFormatter formatter;

    @Autowired
    private DataMapperUtil dataMapperUtil;

    public void connectDatabase(DBImportRequest dbImportRequest, Boolean isOnlyForQualityCheck) throws SQLException {
        try {
            if(conn == null) {
                DBTypes dbType = dbImportRequest.getDbType();
                Class driverClass = Class.forName(dbType.getDriver());
                DriverManager.registerDriver((Driver) driverClass.newInstance());
                String connectionHost = String.format(dbType.getDriverUrl(), dbImportRequest.getUrl(), dbImportRequest.getPort(), dbImportRequest.getDatabaseName());
                conn = DriverManager.getConnection(connectionHost, dbImportRequest.getUserId(), dbImportRequest.getPassword());
                if(!isOnlyForQualityCheck)
                    if(isTrackerSameHost = TrackerUtil.isTrackerHostSame(connectionHost, dbImportRequest.getDatabaseName()))
                        trackColumn = dbImportRequest.getTrackerInfo().getTrackerColumn();

                LOGGER.info("SESSION_ID", APPLICATION_NAME, APPLICATION_ID, "External DataBase" + dbImportRequest.getUrl() +  "Database Successfully connected");
                System.out.println("External DataBase " + dbImportRequest.getUrl() + " Successfully connected");
            }
        } catch (Exception e) {
            LOGGER.error("SESSION_ID", APPLICATION_NAME, APPLICATION_ID, " Error While Connecting Database " + ExceptionUtils.getStackTrace(e));
            System.exit(1);
        }

    }

    public ResultSet readDataFromDatabase(TableRequestDto tableRequestDto, Map<FieldCategory, LinkedHashMap<String, Object>> dataHashMap, Map<String, HashSet<String>> fieldsCategoryMap) throws Exception {
        if(conn != null) {
            return getResult(tableRequestDto, dataHashMap, fieldsCategoryMap);
        } else
            throw new SQLException("Unable to Connect With Database. Please check the Configuration");
    }

    private ResultSet getResult(TableRequestDto tableRequestDto, Map<FieldCategory, LinkedHashMap<String, Object>> dataMap, Map<String, HashSet<String>> fieldsCategoryMap) throws Exception {
        if (tableRequestDto.getQueryType().equals(QuerySelection.TABLE)) {
            String tableName = tableRequestDto.getTableNameWithOutSchema();

            String columnNames = null;

            for(String column : fieldsCategoryMap.get(tableName)) {
                if (columnNames == null)
                    columnNames = column;
                else
                    columnNames += "," + column;
            }

            if(fieldsCategoryMap.containsKey(DEFAULT_TABLE))
                for(String column : fieldsCategoryMap.get(DEFAULT_TABLE)) {
                    if (columnNames == null)
                        columnNames = column;
                    else
                        columnNames += "," + column;
                }

            if(statement != null)
                statement.close();

            statement = conn.createStatement();

            String filterCondition = null;
            boolean whereCondition= false;

            String selectSql = "SELECT " + columnNames + "  from " + tableRequestDto.getTableName();

            if(tableRequestDto.getFilters() != null) {
                for (QueryFilter queryFilter : tableRequestDto.getFilters()) {
                    if (!whereCondition) {
                        filterCondition = " WHERE ";
                        whereCondition=true;
                    } else {
                        filterCondition += " AND ";
                    }

                    filterCondition += queryFilter.getFilterField() + " " + queryFilter.getFilterCondition().format(queryFilter.getFromValue(), queryFilter.getToValue(), queryFilter.getFieldType());
                }

                selectSql += filterCondition;
            }

            filterCondition = "";
            if(isTrackerSameHost && tableRequestDto.getExecutionOrderSequence().equals(1)) {
                if (!whereCondition) {
                    filterCondition = " WHERE ";
                    whereCondition=true;
                } else {
                    filterCondition += " AND ";
                }

                filterCondition += trackColumn + String.format(" NOT IN (SELECT REF_ID FROM %s) ", TRACKER_TABLE_NAME);
                selectSql += filterCondition;
            }

            return statement.executeQuery(formatter.replaceColumntoDataIfAny(selectSql, dataMap));
        } else if (tableRequestDto.getQueryType().equals(QuerySelection.SQL_QUERY)) {
            if(statement != null)
                statement.close();

            statement = conn.createStatement();

            return statement.executeQuery(formatter.replaceColumntoDataIfAny(tableRequestDto.getSqlQuery(), dataMap));
        } else
            return null;
    }

    public void closeConnection() {
        if (conn != null) {
            try {
                conn.close();
                conn = null;
            } catch (SQLException e) {
                LOGGER.error("SESSION_ID", APPLICATION_NAME, APPLICATION_ID, " Error While Closing Database Connection " + e.getMessage());
            }
        }
    }

    public void populateDataFromResultSet(TableRequestDto tableRequestDto, List<FieldFormatRequest> columnDetails, ResultSet resultSet, Map<FieldCategory, LinkedHashMap<String, Object>> dataMap2, List<Map<FieldCategory, LinkedHashMap<String, Object>>> dataMap, Map<String, HashSet<String>> fieldsCategoryMap, Boolean localStoreRequired, Boolean isOnlyForQualityCheck) throws Exception {
        List<Map<String, Object>> resultData = extractResultSet(resultSet);

        for(Map<String, Object> result : resultData) {
            Map<FieldCategory, LinkedHashMap<String, Object>> dataMap1 = new HashMap<>();

            if (dataMap != null && dataMap.size() > 0 && dataMap2 != null) {
                dataMap1 = dataMap2;
            } else {
                dataMap1.put(FieldCategory.DEMO, new LinkedHashMap<>());
                dataMap1.put(FieldCategory.BIO, new LinkedHashMap<>());
                dataMap1.put(FieldCategory.DOC, new LinkedHashMap<>());
            }

            for (FieldFormatRequest fieldFormatRequest : columnDetails) {
                dataMapperUtil.dataMapper(fieldFormatRequest, result, dataMap1, tableRequestDto.getTableNameWithOutSchema(), fieldsCategoryMap, localStoreRequired);
            }
            if(dataMap2 == null)
                dataMap.add(dataMap1);
        }
    }

    public List<Map<String, Object>> extractResultSet(ResultSet resultSet) throws SQLException {
        List<Map<String, Object>> mapList = new ArrayList<>();
        LinkedHashMap<String, Object> resultData = new LinkedHashMap<>();
        ResultSetMetaData metadata = resultSet.getMetaData();
        int columnCount = metadata.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            resultData.put(metadata.getColumnName(i), null);
        }
        while (resultSet.next()) {
            Map<String, Object> resultMap = (LinkedHashMap<String, Object>) resultData.clone();

            for (Map.Entry<String, Object> entry : resultMap.entrySet()) {
                resultMap.put(entry.getKey(), resultSet.getObject(entry.getKey()));
            }
            mapList.add(resultMap);
        }
        return mapList;
    }
}
