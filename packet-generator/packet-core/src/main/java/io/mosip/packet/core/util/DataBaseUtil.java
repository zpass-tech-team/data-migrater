package io.mosip.packet.core.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.packet.core.constant.DBTypes;
import io.mosip.packet.core.constant.FieldCategory;
import io.mosip.packet.core.constant.GlobalConfig;
import io.mosip.packet.core.constant.QuerySelection;
import io.mosip.packet.core.constant.database.QueryLimitSetter;
import io.mosip.packet.core.constant.database.QueryOffsetSetter;
import io.mosip.packet.core.dto.dbimport.DBImportRequest;
import io.mosip.packet.core.dto.dbimport.FieldFormatRequest;
import io.mosip.packet.core.dto.dbimport.QueryFilter;
import io.mosip.packet.core.dto.dbimport.TableRequestDto;
import io.mosip.packet.core.logger.DataProcessLogger;
import io.mosip.packet.core.service.thread.ThreadDBController;
import io.mosip.packet.core.service.thread.CustomizedThreadPoolExecutor;
import io.mosip.packet.core.service.thread.ResultSetter;
import io.mosip.packet.core.service.thread.ThreadDBProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.*;

import static io.mosip.packet.core.constant.GlobalConfig.SESSION_KEY;
import static io.mosip.packet.core.constant.GlobalConfig.*;
import static io.mosip.packet.core.constant.RegistrationConstants.*;

@Component
public class DataBaseUtil {
    private static final Logger LOGGER = DataProcessLogger.getLogger(DataBaseUtil.class);
    private Connection conn = null;
    private boolean isTrackerSameHost = false;
    private String trackColumn = null;
    private DBTypes dbType = null;
    private Long OFFSET_VALUE = 0L;

    @Autowired
    private QueryFormatter formatter;

    @Autowired
    private DataMapperUtil dataMapperUtil;

    @Autowired
    private TrackerUtil trackerUtil;

    @Autowired
    private CommonUtil commonUtil;

    @Value("${mosip.packet.creater.max-threadpool-count:1}")
    private Integer dbReaderMaxThreadPoolCount;

    @Value("${mosip.packet.creater.max-records-process-per-threadpool:10000}")
    private Integer dbReaderMaxRecordsCountPerThreadPool;

    @Value("${mosip.packet.creater.max-thread-execution-count:3}")
    private Integer dbReaderMaxThreadExecCount;

    @Value("${mosip.extractor.application.id.column}")
    private String applicationIdColumn;


    public void connectDatabase(DBImportRequest dbImportRequest) throws SQLException {
        try {
            if(conn == null) {
                dbType = dbImportRequest.getDbType();
                Class driverClass = Class.forName(dbType.getDriver());
                DriverManager.registerDriver((Driver) driverClass.newInstance());
                String connectionHost = String.format(dbType.getDriverUrl(), dbImportRequest.getUrl(), dbImportRequest.getPort(), dbImportRequest.getDatabaseName());
                conn = DriverManager.getConnection(connectionHost, dbImportRequest.getUserId(), dbImportRequest.getPassword());

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

    public void readDataFromDatabase(DBImportRequest dbImportRequest, Map<FieldCategory, HashMap<String, Object>> dataHashMap, Map<String, HashMap<String, String>> fieldsCategoryMap, ResultSetter setter) throws Exception {
        Statement statement1 = conn.createStatement();
        ResultSet resultSetCount = null;
        ResultSet resultSet = null;
        CustomizedThreadPoolExecutor threadPool = null;

        try {
            if(conn != null) {
                OFFSET_VALUE = 0L;
                List<TableRequestDto> tableRequestDtoList = dbImportRequest.getTableDetails();
                Collections.sort(tableRequestDtoList);
                TableRequestDto tableRequestDto  = tableRequestDtoList.get(0);
                resultSetCount = getResult(tableRequestDto, dataHashMap, fieldsCategoryMap, statement1, true);
                if(resultSetCount.next())
                    TOTAL_RECORDS_FOR_PROCESS = resultSetCount.getLong(1);

                int noOfLoopRequired = (int) ((TOTAL_RECORDS_FOR_PROCESS/(dbReaderMaxThreadPoolCount*dbReaderMaxRecordsCountPerThreadPool)) +
                                        (TOTAL_RECORDS_FOR_PROCESS % (dbReaderMaxThreadPoolCount*dbReaderMaxRecordsCountPerThreadPool) > 0 ? 1 : 0));

                threadPool = new CustomizedThreadPoolExecutor(dbReaderMaxThreadPoolCount, dbReaderMaxRecordsCountPerThreadPool, dbReaderMaxThreadExecCount, "DATABASE READER", true);

                boolean oneTimeCheckForZeroOffset = true;

                while(noOfLoopRequired > 0) {
                    Float processPercentage = Float.valueOf((getPendingCountForProcess().floatValue() / Float.valueOf(dbReaderMaxThreadPoolCount*dbReaderMaxRecordsCountPerThreadPool)));

                    if((processPercentage > 0.05 && processPercentage != 0) || (processPercentage == 0 && OFFSET_VALUE > 0 && oneTimeCheckForZeroOffset) || threadPool.getCurrentPendingCount() > 0) {
                        if(processPercentage != 0 && oneTimeCheckForZeroOffset)
                            oneTimeCheckForZeroOffset = false;

                        Thread.sleep(70000);
                        continue;
                    }

                    resultSet = getResult(tableRequestDto, dataHashMap, fieldsCategoryMap, statement1, false);
                    OFFSET_VALUE += (dbReaderMaxThreadPoolCount*dbReaderMaxRecordsCountPerThreadPool);

                    if (resultSet != null) {
                        while(resultSet.next()) {
                            try {
                                Map<String, Object> resultData = extractResultSet(resultSet);
                                ThreadDBController baseDbThreadController = new ThreadDBController();
                                baseDbThreadController.setSetter(setter);
                                baseDbThreadController.setResultMap(resultData);
                                baseDbThreadController.setProcessor(new ThreadDBProcessor() {
                                    @Override
                                    public void processData(ResultSetter setter, Map<String, Object> resultMap) throws Exception {
                                        Map<FieldCategory, HashMap<String, Object>> dataHashMap = new HashMap<>();
                                        populateDataFromResultSet(tableRequestDto, dbImportRequest.getColumnDetails(), resultMap, dataHashMap, fieldsCategoryMap, false);

                                        if(!trackerUtil.isRecordPresent(dataHashMap.get(FieldCategory.DEMO).get(dbImportRequest.getTrackerInfo().getTrackerColumn()), GlobalConfig.getActivityName())) {
                                            for (int i = 1; i < tableRequestDtoList.size(); i++) {
                                                Statement statement2 = conn.createStatement();
                                                ResultSet resultSet1 = null;
                                                try {
                                                    TableRequestDto tableRequestDto1  = tableRequestDtoList.get(i);
                                                    resultSet1 = getResult(tableRequestDto1, dataHashMap, fieldsCategoryMap, statement2, false);

                                                    if (resultSet1 != null && resultSet1.next()) {
                                                        Map<String, Object> resultData1 = extractResultSet(resultSet1);
                                                        populateDataFromResultSet(tableRequestDto1, dbImportRequest.getColumnDetails(), resultData1, dataHashMap, fieldsCategoryMap, false);
                                                    }
                                                } finally {
                                                    if(resultSet1 != null)
                                                        resultSet1.close();

                                                    if(statement2 != null)
                                                        statement2.close();
                                                }
                                            }
                                            setter.setResult(dataHashMap);
                                        } else {
                                            LOGGER.debug("SESSION_ID", APPLICATION_NAME, APPLICATION_ID, " Record Already Processed for ref_id" +  dataHashMap.get(FieldCategory.DEMO).get(dbImportRequest.getTrackerInfo().getTrackerColumn()));
                                        }
                                    }
                                });
                                threadPool.ExecuteTask(baseDbThreadController);
                            } catch (Exception e) {
                                threadPool.increaseFailedRecordCount();
                                LOGGER.error("SESSION_ID", APPLICATION_NAME, APPLICATION_ID, " Error While Extracting Data " + (new Gson()).toJson(dataHashMap) + " Stack Trace : " + ExceptionUtils.getStackTrace(e));
                            }
                        }
                    }
                    noOfLoopRequired--;
                }

                threadPool.setInputProcessCompleted(true);
            } else
                throw new SQLException("Unable to Connect With Database. Please check the Configuration");
        } catch(Exception e) {
            if(threadPool != null)
                threadPool.increaseFailedRecordCount();
            throw e;
        } finally {
            if(resultSetCount != null)
                resultSetCount.close();

            if(resultSet != null)
                resultSet.close();

            if(statement1 != null)
                statement1.close();
        }
    }

    private ResultSet getResult(TableRequestDto tableRequestDto, Map<FieldCategory, HashMap<String, Object>> dataMap, Map<String, HashMap<String, String>> fieldsCategoryMap, Statement statement, boolean fetchCount) throws Exception {
        if (tableRequestDto.getQueryType().equals(QuerySelection.TABLE)) {
            String tableName = tableRequestDto.getTableNameWithOutSchema();
            List<String> ignoreFields = commonUtil.getNonIdSchemaNonTableFieldsMap();

            String columnNames = null;

            for(Map.Entry<String, String> entry : fieldsCategoryMap.get(tableName).entrySet()) {
                String column = entry.getKey();
                if(!ignoreFields.contains(column)) {
                    if(entry.getValue() != null)
                        column = "'"+entry.getValue()+"'" + " AS " + column;

                    if (columnNames == null)
                        columnNames = column;
                    else
                        columnNames += "," + column;
                }
            }

            if(fieldsCategoryMap.containsKey(DEFAULT_TABLE))
                for(Map.Entry<String, String> entry : fieldsCategoryMap.get(DEFAULT_TABLE).entrySet()) {
                    String column = entry.getKey();

                    if(!ignoreFields.contains(column)) {
                        if(entry.getValue() != null)
                            column = "'"+entry.getValue()+"'" + " AS " + column;

                        if (columnNames == null)
                            columnNames = column;
                        else
                            columnNames += "," + column;
                    }
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

                    String condition1 = queryFilter.getFilterField() + " " + queryFilter.getFilterCondition().format(queryFilter.getFromValue(), queryFilter.getToValue(), queryFilter.getFieldType());

                    if(queryFilter.getConjunctionFilter() != null) {
                        condition1 = PrepareConjuctionQuery(queryFilter, condition1);
                    }

                    filterCondition +=condition1;
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

                filterCondition += trackColumn + String.format(" NOT IN (SELECT REF_ID FROM %s WHERE STATUS IN ('PROCESSED','PROCESSED_WITHOUT_UPLOAD', 'FAILED') AND SESSION_KEY = '%s') ", TRACKER_TABLE_NAME, SESSION_KEY);
                selectSql += filterCondition;
            }

            String countSql = "SELECT COUNT(*) from (" + selectSql + ") a"; //alias has been added for MSSQL

            selectSql += " ORDER BY  " + applicationIdColumn;

            if(OFFSET_VALUE != null && OFFSET_VALUE > 0)
                selectSql += " " + QueryOffsetSetter.valueOf(dbType.toString()).getValue(OFFSET_VALUE);

            selectSql += " " + QueryLimitSetter.valueOf(dbType.toString()).getValue(dbReaderMaxThreadPoolCount*dbReaderMaxRecordsCountPerThreadPool);

            if(fetchCount)
                return statement.executeQuery(formatter.replaceColumntoDataIfAny(countSql, dataMap));
            else {
                return statement.executeQuery(formatter.replaceColumntoDataIfAny(selectSql, dataMap));
            }
        } else if (tableRequestDto.getQueryType().equals(QuerySelection.SQL_QUERY)) {
            String sqlQuery = tableRequestDto.getSqlQuery().toUpperCase();
            String countSql = "SELECT COUNT(*) FROM (" + sqlQuery + ") a"; //alias has been added for MSSQL

            if(fetchCount)
                return statement.executeQuery(formatter.replaceColumntoDataIfAny(countSql, dataMap));
            else
                return statement.executeQuery(formatter.replaceColumntoDataIfAny(sqlQuery, dataMap));
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

    public void populateDataFromResultSet(TableRequestDto tableRequestDto, List<FieldFormatRequest> columnDetails, Map<String, Object> resultData, Map<FieldCategory, HashMap<String, Object>> dataMap, Map<String, HashMap<String, String>> fieldsCategoryMap, Boolean localStoreRequired) throws Exception {
        if (dataMap != null && dataMap.size() <= 0) {
            dataMap.put(FieldCategory.DEMO, new HashMap<>());
            dataMap.put(FieldCategory.BIO, new HashMap<>());
            dataMap.put(FieldCategory.DOC, new HashMap<>());
        }

        for (FieldFormatRequest fieldFormatRequest : columnDetails) {
            dataMapperUtil.dataMapper(fieldFormatRequest, resultData, dataMap, tableRequestDto.getTableNameWithOutSchema(), fieldsCategoryMap, localStoreRequired);
        }
    }

    public Map<String, Object> extractResultSet(ResultSet resultSet) throws SQLException {
        HashMap<String, Object> resultData = new HashMap<>();
        ResultSetMetaData metadata = resultSet.getMetaData();
        int columnCount = metadata.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            resultData.put(metadata.getColumnName(i).toUpperCase(), null);
        }
        Map<String, Object> resultMap = (HashMap<String, Object>) resultData.clone();

        for (Map.Entry<String, Object> entry : resultMap.entrySet()) {
            resultMap.put(entry.getKey(), resultSet.getObject(entry.getKey()));
        }
        return resultMap;
    }

    private String PrepareConjuctionQuery(QueryFilter queryFilter, String mainCondition) throws Exception {
        String condition = "";
        String conjCondition = queryFilter.getConjunctionFilter().getConjuctionType().toString();

        for(QueryFilter queryFilter1 : queryFilter.getConjunctionFilter().getFilters()) {
            condition += " " + conjCondition + " ";

            condition += queryFilter1.getFilterField() + " " + queryFilter1.getFilterCondition().format(queryFilter1.getFromValue(), queryFilter1.getToValue(), queryFilter1.getFieldType());

            if(queryFilter1.getConjunctionFilter() != null) {
                String subCondition =PrepareConjuctionQuery(queryFilter1, condition);
                condition +=subCondition;

            }
        }

        return  "(" + mainCondition + " " + condition + ")";
    }
}
