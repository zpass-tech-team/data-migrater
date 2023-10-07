package io.mosip.packet.core.util;

import io.mosip.kernel.clientcrypto.service.impl.ClientCryptoFacade;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.packet.core.constant.DBTypes;
import io.mosip.packet.core.constant.tracker.StringType;
import io.mosip.packet.core.constant.tracker.TimeStampType;
import io.mosip.packet.core.constant.tracker.TrackerStatus;
import io.mosip.packet.core.dto.tracker.TrackerRequestDto;
import io.mosip.packet.core.entity.PacketTracker;
import io.mosip.packet.core.logger.DataProcessLogger;
import io.mosip.packet.core.repository.PacketTrackerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.rowset.serial.SerialBlob;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.sql.*;
import java.util.Optional;
import java.util.Properties;
import java.util.Scanner;

import static io.mosip.packet.core.constant.GlobalConfig.IS_TRACKER_REQUIRED;
import static io.mosip.packet.core.constant.RegistrationConstants.*;

@Component
public class TrackerUtil {
    private static final Logger LOGGER = DataProcessLogger.getLogger(TrackerUtil.class);
    private static Connection conn = null;
    private PreparedStatement preparedStatement = null;
    private static Properties keys;
    private int batchLimit = 2;
    private int batchSize = 0;
    private static String connectionHost = null;

    @Autowired
    private PacketTrackerRepository packetTrackerRepository;

    @Autowired
    private ClientCryptoFacade clientCryptoFacade;

    static {
        try (InputStream configKeys = TrackerUtil.class.getClassLoader().getResourceAsStream("database.properties")) {
            keys = new Properties();
            keys.load(configKeys);
        } catch (Exception e) {
            LOGGER.error("SESSION_ID", APPLICATION_NAME, APPLICATION_ID,
                    "Exception encountered during context initialization - TrackerUtil "
                            + ExceptionUtils.getStackTrace(e));
            System.exit(0);
        }

        try {
        //    isTrackerRequired = keys.getProperty("mosip.packet.creator.tracking.required") == null ? false : Booleankeys.getProperty("mosip.packet.creator.tracking.required");

            if(conn == null && IS_TRACKER_REQUIRED) {
                DBTypes dbType = Enum.valueOf(DBTypes.class, keys.getProperty("spring.datasource.tracker.dbtype"));

                Class driverClass = Class.forName(dbType.getDriver());
                DriverManager.registerDriver((Driver) driverClass.newInstance());
                connectionHost = String.format(dbType.getDriverUrl(), keys.getProperty("spring.datasource.tracker.host"), keys.getProperty("spring.datasource.tracker.port"), keys.getProperty("spring.datasource.tracker.database"));
                conn = DriverManager.getConnection(connectionHost, keys.getProperty("spring.datasource.tracker.username"), keys.getProperty("spring.datasource.tracker.password"));
                conn.setAutoCommit(true);

                Statement statement = null;
                try {
                    statement = conn.createStatement();
                    statement.execute("SELECT COUNT(*) FROM " + TRACKER_TABLE_NAME);
                } catch (Exception e) {
                    System.out.println("Table " + TRACKER_TABLE_NAME +  " not Present in DB " + keys.getProperty("spring.datasource.tracker.jdbcurl") +  ". Do you want to create ? Y-Yes, N-No");
                    Scanner scanner = new Scanner(System.in);
                    String option = scanner.next();

                    if(option.equalsIgnoreCase("y")) {
                        try {
                            DBCreator dbCreator = new DBCreator(dbType);
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

    public synchronized void addTrackerEntry(TrackerRequestDto trackerRequestDto) {
        if(IS_TRACKER_REQUIRED) {
            try {
                batchSize++;

                if(batchSize > batchLimit) {
                    preparedStatement.executeBatch();
                    preparedStatement.clearBatch();
                    preparedStatement.closeOnCompletion();
                    preparedStatement = null;
                    batchSize = 1;
                }

                if(preparedStatement == null)
                    preparedStatement = conn.prepareStatement(String.format("INSERT INTO %s (REF_ID, REG_NO, STATUS, CR_BY, CR_DTIMES, SESSION_KEY, ACTIVITY, PROCESS, COMMENTS) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", TRACKER_TABLE_NAME));

                long timeNow = System.currentTimeMillis();
                Timestamp timestamp = new Timestamp(timeNow);
                preparedStatement.setString(1, trackerRequestDto.getRefId());
                preparedStatement.setString(2, trackerRequestDto.getRegNo());
                preparedStatement.setString(3, trackerRequestDto.getStatus());
                preparedStatement.setString(4, "MIGRATOR");
                preparedStatement.setTimestamp(5, timestamp);
                preparedStatement.setString(6, trackerRequestDto.getSessionKey());
                preparedStatement.setString(7, trackerRequestDto.getActivity());
                preparedStatement.setString(8, trackerRequestDto.getProcess());
                preparedStatement.setString(9, trackerRequestDto.getComments());
                preparedStatement.addBatch();
            } catch (SQLException throwables) {
                LOGGER.error("SESSION_ID", APPLICATION_NAME, APPLICATION_ID,
                        "Exception encountered during Tracker record insertion - TrackerUtil "
                                + ExceptionUtils.getStackTrace(throwables));
            }
        }
    }

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

    public boolean isRecordPresent(Object value, String activity) throws SQLException {
        if(IS_TRACKER_REQUIRED) {
            PreparedStatement statement = null;
            try {
                statement = conn.prepareStatement(String.format("SELECT 1 FROM %s WHERE REF_ID = ? AND ACTIVITY = ?", TRACKER_TABLE_NAME));
                statement.setString(1, value.toString());
                statement.setString(2, activity);
                ResultSet resultSet = statement.executeQuery();

                if(resultSet.next())
                    return true;
                else
                    return false;
            } catch (SQLException throwables) {
                LOGGER.error("SESSION_ID", APPLICATION_NAME, APPLICATION_ID,
                        "Exception encountered while checking Tracker Record present - TrackerUtil "
                                + ExceptionUtils.getStackTrace(throwables));
                throw throwables;
            } finally {
                if(statement != null)
                    statement.close();
            }
        } else {
            return false;
        }
    }

    public static boolean isTrackerHostSame(String sourceHost, String databaseName) {
        return connectionHost != null && connectionHost.equalsIgnoreCase(sourceHost) && databaseName.equalsIgnoreCase(keys.getProperty("spring.datasource.tracker.database"));
    }


    private static class DBCreator {

        private StringBuilder sb;

        public DBCreator(DBTypes dbTypes) throws Exception {
            sb = new StringBuilder();
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
            sb.append(");");

            sb.append(String.format("ALTER TABLE %s ADD PRIMARY KEY (SESSION_KEY, REF_ID);", TRACKER_TABLE_NAME));
        }

        private String addColumn(String columnName, Class columnType, Integer length, boolean isNotNull, DBTypes dbTypes) throws Exception {
            switch (columnType.getSimpleName()) {
                case "String" :
                    return columnName + " " + StringType.valueOf(dbTypes.toString()).getValue(length.toString()) + (isNotNull ? " NOT NULL" : "");
                case "Timestamp" :
                    return columnName + " " + TimeStampType.valueOf(dbTypes.toString()).getType() + (isNotNull ? " NOT NULL" : "");
                default:
                    throw new Exception("Column Type " + columnType.getSimpleName() + " Not Found");
            }
        }

        public String getValue() {
            return sb.toString();
        }

    }

    public void addTrackerLocalEntry(String refId, String regNo, TrackerStatus status, String process, Object request, String sessionKey, String activity) throws SQLException, IOException {
        Optional<PacketTracker> optional= packetTrackerRepository.findById(refId);
        byte[] requestValue = null;

        if(request != null) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(request);
            requestValue = clientCryptoFacade.encrypt(clientCryptoFacade.getClientSecurity().getEncryptionPublicPart(), bos.toByteArray());
            oos.close();
            bos.close();
        }

        PacketTracker packetTracker;

        if(optional.isPresent()) {
            packetTracker = optional.get();
            if(regNo != null ) packetTracker.setRegNo(regNo);
            if(status != null ) packetTracker.setStatus(status.toString());
            if(process != null ) packetTracker.setProcess(process);
            if(request != null ) packetTracker.setRequest(new SerialBlob(requestValue));
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
            if(request != null ) packetTracker.setRequest(requestValue != null ? null : new SerialBlob(requestValue));
            if(activity != null ) packetTracker.setActivity(activity);
            packetTracker.setSessionKey(sessionKey);
            packetTracker.setCrBy("BATCH");
            packetTracker.setCrDtime(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()));
        }
        packetTrackerRepository.saveAndFlush(packetTracker);
    }
}
