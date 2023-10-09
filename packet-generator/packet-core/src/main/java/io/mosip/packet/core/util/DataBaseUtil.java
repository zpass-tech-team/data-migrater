package io.mosip.packet.core.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.packet.core.constant.DBTypes;
import io.mosip.packet.core.constant.FieldCategory;
import io.mosip.packet.core.constant.QuerySelection;
import io.mosip.packet.core.dto.dbimport.*;
import io.mosip.packet.core.logger.DataProcessLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;

import static io.mosip.packet.core.constant.GlobalConfig.IS_ONLY_FOR_QUALITY_CHECK;
import static io.mosip.packet.core.constant.RegistrationConstants.*;

@Component
public class DataBaseUtil {
    private static final Logger LOGGER = DataProcessLogger.getLogger(DataBaseUtil.class);
    private Connection conn = null;
    private boolean isTrackerSameHost = false;
    private String trackColumn = null;
    private PriorityBlockingQueue<DataResult> syncronizedQueue = new PriorityBlockingQueue<>();
    private ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private QueryFormatter formatter;

    @Autowired
    private DataMapperUtil dataMapperUtil;

    @Autowired
    private TrackerUtil trackerUtil;

    public PriorityBlockingQueue<DataResult> getSyncronizedQueue() {
        return syncronizedQueue;
    }

    public void setSyncronizedQueue(PriorityBlockingQueue<DataResult> syncronizedQueue) {
        this.syncronizedQueue = syncronizedQueue;
    }

    public void connectDatabase(DBImportRequest dbImportRequest) throws SQLException {
        try {
            if(conn == null) {
                DBTypes dbType = dbImportRequest.getDbType();
                Class driverClass = Class.forName(dbType.getDriver());
                DriverManager.registerDriver((Driver) driverClass.newInstance());
                String connectionHost = String.format(dbType.getDriverUrl(), dbImportRequest.getUrl(), dbImportRequest.getPort(), dbImportRequest.getDatabaseName());
                conn = DriverManager.getConnection(connectionHost, dbImportRequest.getUserId(), dbImportRequest.getPassword());
                if(!IS_ONLY_FOR_QUALITY_CHECK)
                    if(isTrackerSameHost = trackerUtil.isTrackerHostSame(connectionHost, dbImportRequest.getDatabaseName()))
                        trackColumn = dbImportRequest.getTrackerInfo().getTrackerColumn();

                LOGGER.info("SESSION_ID", APPLICATION_NAME, APPLICATION_ID, "External DataBase" + dbImportRequest.getUrl() +  "Database Successfully connected");
                System.out.println("External DataBase " + dbImportRequest.getUrl() + " Successfully connected");
            }
        } catch (Exception e) {
            LOGGER.error("SESSION_ID", APPLICATION_NAME, APPLICATION_ID, " Error While Connecting Database " + ExceptionUtils.getStackTrace(e));
            System.exit(1);
        }

    }

    public void readDataFromDatabase(DBImportRequest dbImportRequest, Map<FieldCategory, LinkedHashMap<String, Object>> dataHashMap, Map<String, HashSet<String>> fieldsCategoryMap) throws Exception {
        syncronizedQueue.clear();

        Statement statement1 = conn.createStatement();
        try {
            if(conn != null) {
                List<TableRequestDto> tableRequestDtoList = dbImportRequest.getTableDetails();
                Collections.sort(tableRequestDtoList);
                TableRequestDto tableRequestDto  = tableRequestDtoList.get(0);
                ResultSet resultSet = getResult(tableRequestDto, dataHashMap, fieldsCategoryMap, statement1);

                if (resultSet != null) {
                    while(resultSet.next()) {
                        dataHashMap = new HashMap<>();
                        populateDataFromResultSet(tableRequestDto, dbImportRequest.getColumnDetails(), resultSet, dataHashMap, fieldsCategoryMap, false);

                        for (int i = 1; i < tableRequestDtoList.size(); i++) {
                            Statement statement2 = conn.createStatement();
                            try {
                                TableRequestDto tableRequestDto1  = tableRequestDtoList.get(i);
                                ResultSet resultSet1 = getResult(tableRequestDto1, dataHashMap, fieldsCategoryMap, statement2);

                                if (resultSet1 != null && resultSet1.next()) {
                                    populateDataFromResultSet(tableRequestDto1, dbImportRequest.getColumnDetails(), resultSet1, dataHashMap, fieldsCategoryMap, false);
                                }
                            } finally {
                                if(statement2 != null)
                                    statement2.close();
                            }
                        }
                        DataResult result = new DataResult();
                        result.setDemoDetails(dataHashMap.get(FieldCategory.DEMO));
                        result.setBioDetails(dataHashMap.get(FieldCategory.BIO));
                        result.setDocDetails(dataHashMap.get(FieldCategory.DOC));
                        syncronizedQueue.put(result);
                    }
                }
            } else
                throw new SQLException("Unable to Connect With Database. Please check the Configuration");
        } finally {
            if(statement1 != null)
                statement1.close();
        }

    }

    private ResultSet getResult(TableRequestDto tableRequestDto, Map<FieldCategory, LinkedHashMap<String, Object>> dataMap, Map<String, HashSet<String>> fieldsCategoryMap, Statement statement) throws Exception {
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

    public void populateDataFromResultSet(TableRequestDto tableRequestDto, List<FieldFormatRequest> columnDetails, ResultSet resultSet, Map<FieldCategory, LinkedHashMap<String, Object>> dataMap, Map<String, HashSet<String>> fieldsCategoryMap, Boolean localStoreRequired) throws Exception {
        Map<String, Object> resultData = extractResultSet(resultSet);

        if (dataMap != null && dataMap.size() <= 0) {
            dataMap.put(FieldCategory.DEMO, new LinkedHashMap<>());
            dataMap.put(FieldCategory.BIO, new LinkedHashMap<>());
            dataMap.put(FieldCategory.DOC, new LinkedHashMap<>());
        }

        for (FieldFormatRequest fieldFormatRequest : columnDetails) {
            dataMapperUtil.dataMapper(fieldFormatRequest, resultData, dataMap, tableRequestDto.getTableNameWithOutSchema(), fieldsCategoryMap, localStoreRequired);
        }
    }

    public Map<String, Object> extractResultSet(ResultSet resultSet) throws SQLException {
        LinkedHashMap<String, Object> resultData = new LinkedHashMap<>();
        ResultSetMetaData metadata = resultSet.getMetaData();
        int columnCount = metadata.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            resultData.put(metadata.getColumnName(i), null);
        }
        Map<String, Object> resultMap = (LinkedHashMap<String, Object>) resultData.clone();

        for (Map.Entry<String, Object> entry : resultMap.entrySet()) {
            resultMap.put(entry.getKey(), resultSet.getObject(entry.getKey()));
        }
        return resultMap;
    }
}
