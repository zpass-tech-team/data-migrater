package io.mosip.packet.core.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.kernel.clientcrypto.service.impl.ClientCryptoFacade;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.packet.core.constant.DBTypes;
import io.mosip.packet.core.constant.TableQueries;
import io.mosip.packet.core.constant.tracker.*;
import io.mosip.packet.core.dto.TrackerAdditionalColumns;
import io.mosip.packet.core.dto.tracker.TrackerRequestDto;
import io.mosip.packet.core.entity.PacketTracker;
import io.mosip.packet.core.logger.DataProcessLogger;
import io.mosip.packet.core.repository.PacketTrackerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.sql.*;
import java.util.*;

import static io.mosip.packet.core.constant.GlobalConfig.SESSION_KEY;
import static io.mosip.packet.core.constant.GlobalConfig.*;
import static io.mosip.packet.core.constant.RegistrationConstants.*;

@Component
public class TrackerUtil {
    private static final Logger LOGGER = DataProcessLogger.getLogger(TrackerUtil.class);
    private static Connection conn = null;
    private PreparedStatement preparedStatement = null;

    @Value("${mosip.packet.creator.tracking.batch.connection.reset.count:1000}")
    private int batchConResetCount;

    @Value("${mosip.packet.tracker.table.creation.additional.fields:}")
    private String additionalFields;

    private int connSize = 0;
    private static String connectionHost = null;
    private boolean isConnCreation = false;

    @Autowired
    private PacketTrackerRepository packetTrackerRepository;

    @Autowired
    private ClientCryptoFacade clientCryptoFacade;

    @Autowired
    private Environment env;

    @Autowired
    private QueryFormatter queryFormatter;

    private ObjectMapper mapper = new ObjectMapper();

    @PostConstruct
    public void initialize(){
        try {
            IS_TRACKER_REQUIRED = env.getProperty("mosip.packet.creator.tracking.required") == null ? false : Boolean.valueOf(env.getProperty("mosip.packet.creator.tracking.required"));
            IS_RUNNING_AS_BATCH = env.getProperty("mosip.packet.creator.run.as.batch.execution") == null ? false : Boolean.valueOf(env.getProperty("mosip.packet.creator.run.as.batch.execution"));

            if(conn == null && IS_TRACKER_REQUIRED) {
                DBTypes dbType = Enum.valueOf(DBTypes.class, env.getProperty("spring.datasource.tracker.dbtype"));

                Class driverClass = Class.forName(dbType.getDriver());
                DriverManager.registerDriver((Driver) driverClass.newInstance());
                connectionHost = String.format(dbType.getDriverUrl(), env.getProperty("spring.datasource.tracker.host"), env.getProperty("spring.datasource.tracker.port"), env.getProperty("spring.datasource.tracker.database"));
                conn = DriverManager.getConnection(connectionHost, env.getProperty("spring.datasource.tracker.username"), env.getProperty("spring.datasource.tracker.password"));
                conn.setAutoCommit(true);

                Statement statement = null;
                try {
                    statement = conn.createStatement();
                    statement.execute("SELECT COUNT(*) FROM " + TRACKER_TABLE_NAME);
                    if(additionalFields != null) {
                        List<TrackerAdditionalColumns> val = mapper.readValue(additionalFields, new TypeReference<List<TrackerAdditionalColumns>>() {});
                        for(TrackerAdditionalColumns detail : val) {
                            PACKET_TRACKER_ADDITIONAL_FIELDS.put(detail.getColumnName(), detail.getIdSchemaField());
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Table " + TRACKER_TABLE_NAME +  " not Present in DB " + env.getProperty("spring.datasource.tracker.jdbcurl") +  ". Do you want to create ? Y-Yes, N-No");
                    String option ="";
                    if(!IS_RUNNING_AS_BATCH) {
                        Scanner scanner = new Scanner(System.in);
                        option = scanner.next();
                    } else {
                        option = "Y";
                    }

                    if(option.equalsIgnoreCase("y")) {
                        try {
                            DBCreator dbCreator = new DBCreator(dbType, true, additionalFields);
                            String[] scripts = dbCreator.getValue().split(";");
                            for(String script : scripts)
                                statement.execute(script);
                        } catch (Exception e1) {
                            LOGGER.error("SESSION_ID", APPLICATION_NAME, APPLICATION_ID,
                                    "Exception encountered during Table Creation  "
                                            + ExceptionUtils.getStackTrace(e1));
                            System.exit(1);
                        }
                    } else {
                        System.exit(1);
                    }
                } finally {
                    if(statement != null)
                        statement.close();
                }

                try {
                    statement = conn.createStatement();
                    statement.execute("SELECT COUNT(*) FROM " + OFFSET_TRACKER_TABLE_NAME);
                } catch (Exception e) {
                    System.out.println("Table " + OFFSET_TRACKER_TABLE_NAME +  " not Present in DB " + env.getProperty("spring.datasource.tracker.jdbcurl") +  ". Do you want to create ? Y-Yes, N-No");
                    String option ="";
                    if(!IS_RUNNING_AS_BATCH) {
                        Scanner scanner = new Scanner(System.in);
                        option = scanner.next();
                    } else {
                        option = "Y";
                    }

                    if(option.equalsIgnoreCase("y")) {
                        try {
                            DBCreator dbCreator = new DBCreator(dbType, false, additionalFields);
                            String[] scripts = dbCreator.getValue().split(";");
                            for(String script : scripts)
                                statement.execute(script);
                        } catch (Exception e1) {
                            LOGGER.error("SESSION_ID", APPLICATION_NAME, APPLICATION_ID,
                                    "Exception encountered during Table Creation  "
                                            + ExceptionUtils.getStackTrace(e1));
                            System.exit(1);
                        }
                    } else {
                        System.exit(1);
                    }
                } finally {
                    if(statement != null)
                        statement.close();
                }
            }
        } catch (Exception e) {
            LOGGER.error("SESSION_ID", APPLICATION_NAME, APPLICATION_ID,
                    "Exception encountered during context initialization - TrackerUtil "
                            + ExceptionUtils.getStackTrace(e));
        }
    }

    public synchronized void addTrackerEntry(TrackerRequestDto trackerRequestDto) throws SQLException {
        if(IS_TRACKER_REQUIRED) {
            PreparedStatement preparedStatement = null;
            DBTypes dbType = Enum.valueOf(DBTypes.class, env.getProperty("spring.datasource.tracker.dbtype"));

            try {
                connSize++;

                if(connSize > batchConResetCount) {
                    isConnCreation=true;
                    LOGGER.info("TrackerUtil Closing Connection");
                    conn.close();
                    conn=null;
                    this.initialize();
                    LOGGER.info("TrackerUtil Starting Connection");
                    isConnCreation=false;
                    connSize=1;
                }


                String query = TableQueries.getInsertQueries(TRACKER_TABLE_NAME, dbType);
                long timeNow = System.currentTimeMillis();
                Timestamp timestamp = new Timestamp(timeNow);

                Map<String, String> valueMap = new HashMap<>();
                valueMap.put("TABLE_NAME", TRACKER_TABLE_NAME);
                valueMap.put("REF_ID", trackerRequestDto.getRefId());
                valueMap.put("REG_NO", trackerRequestDto.getRegNo());
                valueMap.put("STATUS", trackerRequestDto.getStatus());
                valueMap.put("CR_BY", "MIGRATOR");
                valueMap.put("CR_DTIMES", DateUtils.formatDate(timestamp, "yyyy-MM-dd HH:mm:ss"));
                valueMap.put("SESSION_KEY", trackerRequestDto.getSessionKey());
                valueMap.put("ACTIVITY", trackerRequestDto.getActivity());
                valueMap.put("PROCESS", trackerRequestDto.getProcess());
                valueMap.put("COMMENTS", trackerRequestDto.getComments());
                String insetAddCol = "";
                String insetAddVal = "";
                String insetAddColVal = "";

                if(trackerRequestDto.getAdditionalMaps() != null) {
                    for(Map.Entry<String, Object> entry : trackerRequestDto.getAdditionalMaps().entrySet()) {
                        if(PACKET_TRACKER_ADDITIONAL_FIELDS.containsKey(entry.getKey())) {
                            insetAddCol += ", " + entry.getKey();
                            insetAddVal += ",'" + entry.getValue().toString() + "'";
                            insetAddColVal += ", " + entry.getKey() + "= '" + entry.getValue().toString() + "'";
                        }
                    }
                }
                valueMap.put("IN_ADD_COL", insetAddCol);
                valueMap.put("IN_ADD_VAL", insetAddVal);
                valueMap.put("IN_ADD_COL_VAL", insetAddColVal);
                preparedStatement = conn.prepareStatement(queryFormatter.queryFormatter(query, valueMap));
                preparedStatement.execute();
            } catch (SQLException throwables) {
                if(throwables.getMessage().contains("ORA-01000")) {
                    try {
                        connSize = 0;
                        conn.close();
                        conn=null;
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    this.initialize();
                    addTrackerEntry(trackerRequestDto);
                } else {
                    LOGGER.error("SESSION_ID", APPLICATION_NAME, APPLICATION_ID,
                            "Exception encountered during Tracker record insertion - TrackerUtil "
                                    + ExceptionUtils.getStackTrace(throwables));
                }
            } finally {
                if(preparedStatement != null)
                    preparedStatement.close();
            }
        }
    }

    public synchronized void updateDatabaseOffset(Long offset) throws SQLException, InterruptedException {
        if(IS_TRACKER_REQUIRED) {
            PreparedStatement preparedStatement = null;
            DBTypes dbType = Enum.valueOf(DBTypes.class, env.getProperty("spring.datasource.tracker.dbtype"));

            try {
                String query = TableQueries.getInsertQueries(OFFSET_TRACKER_TABLE_NAME, dbType);
                Map<String, String> valueMap = new HashMap<>();
                valueMap.put("TABLE_NAME", OFFSET_TRACKER_TABLE_NAME);
                valueMap.put("SESSION_ID", SESSION_KEY);
                valueMap.put("VALUE", offset.toString());
                valueMap.put("IN_USE", "N");

                while(isConnCreation)
                    Thread.sleep(2000);

                preparedStatement = conn.prepareStatement(queryFormatter.queryFormatter(query, valueMap));
                preparedStatement.execute();
            } finally {
                if(preparedStatement != null)
                    preparedStatement.close();
            }
        }
    }

    public synchronized Long getDatabaseOffset() throws SQLException, InterruptedException {
        if(IS_TRACKER_REQUIRED) {
            Statement statement = null;
            ResultSet resultSet = null;
            DBTypes dbType = Enum.valueOf(DBTypes.class, env.getProperty("spring.datasource.tracker.dbtype"));

            try {
                while(isConnCreation)
                    Thread.sleep(2000);

                statement = conn.createStatement();
                resultSet = statement.executeQuery("SELECT OFFSET_VALUE, IN_USE FROM " + OFFSET_TRACKER_TABLE_NAME + " WHERE SESSION_KEY = '" + SESSION_KEY + "'");
                if(resultSet.next()) {
                    Long value = resultSet.getLong(1);
                    String inUse = resultSet.getString(2);

                    if(inUse == null || inUse.equals("N") || inUse.isEmpty()) {
                        statement.executeUpdate("UPDATE " + OFFSET_TRACKER_TABLE_NAME + " SET IN_USE = 'Y' WHERE SESSION_KEY = '" + SESSION_KEY + "'");
                        return value;
                    } else {
                        LOGGER.info("SESSION_ID", APPLICATION_NAME, APPLICATION_ID, "OffSet Tracker Table in Use retry after 5 seconds");
                        Thread.sleep(5000);
                        return getDatabaseOffset();
                    }
                } else {
                    String query = TableQueries.getInsertQueries(OFFSET_TRACKER_TABLE_NAME, dbType);
                    Map<String, String> valueMap = new HashMap<>();
                    valueMap.put("TABLE_NAME", OFFSET_TRACKER_TABLE_NAME);
                    valueMap.put("SESSION_ID", SESSION_KEY);
                    valueMap.put("VALUE", "0");
                    valueMap.put("IN_USE", "Y");
                    statement.execute(queryFormatter.queryFormatter(query, valueMap));
                    return 0L;
                }
            } finally {
                if(resultSet != null)
                    resultSet.close();

                if(statement != null)
                    statement.close();
            }
        } else {
            return 0L;
        }
    }

    @PreDestroy
    public void closeStatement() {
        if(IS_TRACKER_REQUIRED) {
            if (conn != null) {
                try {
                    if(preparedStatement != null) {
                        preparedStatement.executeBatch();
                        preparedStatement.clearBatch();
                        preparedStatement.closeOnCompletion();
                    }
                } catch (SQLException e) {
                    LOGGER.error("SESSION_ID", APPLICATION_NAME, APPLICATION_ID, " Error While Closing Database Connection " + e.getMessage());
                }
            }
        }
    }

    public synchronized boolean isRecordPresent(Object value, String activity) throws SQLException, InterruptedException {
        if(IS_TRACKER_REQUIRED) {
            PreparedStatement statement = null;
            ResultSet resultSet = null;

            try {
                while(isConnCreation)
                    Thread.sleep(2000);

                statement = conn.prepareStatement(String.format("SELECT 1 FROM %s WHERE REF_ID = ? AND ACTIVITY = ? AND SESSION_KEY = ? AND STATUS != 'FAILED'", TRACKER_TABLE_NAME));
                statement.setString(1, value.toString());
                statement.setString(2, activity);
                statement.setString(3, SESSION_KEY);
                resultSet = statement.executeQuery();

                if(resultSet.next())
                    return true;
                else
                    return false;
            } catch (SQLException throwables) {
                if(throwables.getMessage().contains("ORA-01000")) {
                    connSize=0;
                    conn.close();
                    conn=null;
                    this.initialize();
                    return isRecordPresent(value, activity);
                } else {
                    LOGGER.error("SESSION_ID", APPLICATION_NAME, APPLICATION_ID,
                            "Exception encountered while checking Tracker Record present - TrackerUtil "
                                    + ExceptionUtils.getStackTrace(throwables));
                    throw throwables;
                }
            } finally {
                try {
                    resultSet.close();
                } catch (Exception e){};

                try {
                    statement.close();
                }catch (Exception e){};
            }
        } else {
            return false;
        }
    }

    public boolean isTrackerHostSame(String sourceHost, String databaseName) {
        return connectionHost != null && connectionHost.equalsIgnoreCase(sourceHost) && databaseName.equalsIgnoreCase(env.getProperty("spring.datasource.tracker.database"));
    }


    private static class DBCreator {

        private StringBuilder sb;

        private ObjectMapper mapper = new ObjectMapper();

        public DBCreator(DBTypes dbTypes, Boolean isTrackerTable, String additionalFields) throws Exception {
            sb = new StringBuilder();

            if(isTrackerTable) {
                sb.append(String.format("CREATE TABLE %s (", TRACKER_TABLE_NAME));
                sb.append(addColumn("SESSION_KEY", String.class, 100, true, dbTypes) + ",");
                sb.append(addColumn("REF_ID", String.class, 100, true, dbTypes) + ",");
                sb.append(addColumn("REG_NO", String.class, 100, false, dbTypes) + ",");
                sb.append(addColumn("ACTIVITY", String.class, 50, false, dbTypes) + ",");
                sb.append(addColumn("STATUS", String.class, 50, false, dbTypes) + ",");
                sb.append(addColumn("PROCESS", String.class, 50, true, dbTypes) + ",");
                sb.append(addColumn("COMMENTS", String.class, 3000, false, dbTypes) + ",");
                sb.append(addColumn("CR_BY", String.class, 50, true, dbTypes) + ",");
                sb.append(addColumn("CR_DTIMES", Timestamp.class, 100, true, dbTypes) + ",");
                sb.append(addColumn("UPD_BY", String.class, 100, false, dbTypes) + ",");
                sb.append(addColumn("UPD_DTIMES", Timestamp.class, 100, false, dbTypes));

                if(StringUtils.hasText(additionalFields)) {
                    List<TrackerAdditionalColumns> val = mapper.readValue(additionalFields, new TypeReference<List<TrackerAdditionalColumns>>() {});
                    for(TrackerAdditionalColumns detail : val) {
                        sb.append("," + addColumn(detail.getColumnName(), getClass(detail.getColumnType()), detail.getLength(), detail.getIsNotNull(), dbTypes));
                        PACKET_TRACKER_ADDITIONAL_FIELDS.put(detail.getColumnName(), detail.getIdSchemaField());
                    }
                }
                sb.append(");");

                sb.append(String.format("ALTER TABLE %s ADD PRIMARY KEY (SESSION_KEY, REF_ID);", TRACKER_TABLE_NAME));
                sb.append(String.format("CREATE INDEX IX_SEARCH_1 ON  %s (REF_ID, ACTIVITY, SESSION_KEY);", TRACKER_TABLE_NAME));
                sb.append(String.format("CREATE INDEX IX_SEARCH_2 ON  %s (REF_ID, ACTIVITY, SESSION_KEY, STATUS);", TRACKER_TABLE_NAME));
                sb.append(String.format("CREATE INDEX IX_SEARCH_3 ON  %s (REF_ID);", TRACKER_TABLE_NAME));
                sb.append(String.format("CREATE INDEX IX_SEARCH_4 ON  %s (REF_ID, SESSION_KEY);", TRACKER_TABLE_NAME));
            } else {
                sb.append(String.format("CREATE TABLE %s (", OFFSET_TRACKER_TABLE_NAME));
                sb.append(addColumn("SESSION_KEY", String.class, 100, true, dbTypes) + ",");
                sb.append(addColumn("OFFSET_VALUE", Number.class, 12, false, dbTypes) + ",");
                sb.append(addColumn("IN_USE", Character.class, 1, false, dbTypes));
                sb.append(");");

                sb.append(String.format("ALTER TABLE %s ADD PRIMARY KEY (SESSION_KEY);", OFFSET_TRACKER_TABLE_NAME));
            }
        }

        private Class getClass(String columnType) {
            if(columnType.equals("String"))
                return String.class;
            else if(columnType.equals("Timestamp"))
                return Timestamp.class;
            else
                return String.class;
        }

        private String addColumn(String columnName, Class columnType, Integer length, boolean isNotNull, DBTypes dbTypes) throws Exception {
            switch (columnType.getSimpleName()) {
                case "String" :
                    return columnName + " " + StringType.valueOf(dbTypes.toString()).getValue(length.toString()) + (isNotNull ? " NOT NULL" : "");
                case "Number" :
                    return columnName + " " + NumberType.valueOf(dbTypes.toString()).getValue(length.toString(), "0") + (isNotNull ? " NOT NULL" : "");
                case "Timestamp" :
                    return columnName + " " + TimeStampType.valueOf(dbTypes.toString()).getType() + (isNotNull ? " NOT NULL" : "");
                case "Character" :
                    return columnName + " " + CharacterType.valueOf(dbTypes.toString()).getType() + (isNotNull ? " NOT NULL" : "");
                default:
                    throw new Exception("Column Type " + columnType.getSimpleName() + " Not Found");
            }
        }

        public String getValue() {
            return sb.toString();
        }

    }

    public synchronized void addTrackerLocalEntry(String refId, String regNo, TrackerStatus status, String process, Object request, String sessionKey, String activity) throws SQLException, IOException, InterruptedException {
        while(isConnCreation)
            Thread.sleep(10000);

        Optional<PacketTracker> optional= packetTrackerRepository.findById(refId);
        PacketTracker packetTracker;

        if(status.equals(TrackerStatus.PROCESSED)) {
            if(optional.isPresent()) {
                packetTracker = optional.get();
                packetTrackerRepository.delete(packetTracker);
            }
        } else {
            byte[] requestValue = null;

            if(request != null) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos);
                oos.writeObject(request);
                if(IS_TPM_AVAILABLE)
                    requestValue = clientCryptoFacade.encrypt(clientCryptoFacade.getClientSecurity().getEncryptionPublicPart(), bos.toByteArray());
                else
                    requestValue = bos.toByteArray();

                oos.close();
                bos.close();
            }

            if(optional.isPresent()) {
                packetTracker = optional.get();
                if(regNo != null ) packetTracker.setRegNo(regNo);
                if(status != null ) packetTracker.setStatus(status.toString());
                if(process != null ) packetTracker.setProcess(process);
                if(request != null ) packetTracker.setRequest(Base64.getEncoder().encodeToString(requestValue));
                if(activity != null ) packetTracker.setActivity(activity);
                packetTracker.setSessionKey(sessionKey);
                packetTracker.setUpdBy("BATCH");
                packetTracker.setUpdDtimes(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()));
            } else {
                packetTracker = new PacketTracker();
                if(refId != null ) packetTracker.setRefId(refId);
                if(regNo != null ) packetTracker.setRegNo(regNo);
                if(status != null ) packetTracker.setStatus(status.toString());
                if(process != null ) packetTracker.setProcess(process);
                if(request != null ) packetTracker.setRequest(requestValue == null ? null : Base64.getEncoder().encodeToString(requestValue));
                if(activity != null ) packetTracker.setActivity(activity);
                packetTracker.setSessionKey(sessionKey);
                packetTracker.setCrBy("BATCH");
                packetTracker.setCrDtime(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()));
            }
            packetTrackerRepository.saveAndFlush(packetTracker);
        }
    }
}
