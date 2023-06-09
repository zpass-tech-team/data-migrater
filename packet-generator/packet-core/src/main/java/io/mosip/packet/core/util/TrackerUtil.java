package io.mosip.packet.core.util;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.packet.core.constant.DBTypes;
import io.mosip.packet.core.constant.FieldCategory;
import io.mosip.packet.core.constant.QuerySelection;
import io.mosip.packet.core.constant.tracker.StringType;
import io.mosip.packet.core.constant.tracker.TimeStampType;
import io.mosip.packet.core.constant.tracker.TrackerStatus;
import io.mosip.packet.core.dto.dbimport.DBImportRequest;
import io.mosip.packet.core.dto.dbimport.FieldFormatRequest;
import io.mosip.packet.core.dto.dbimport.QueryFilter;
import io.mosip.packet.core.dto.dbimport.TableRequestDto;
import io.mosip.packet.core.dto.tracker.TrackerRequestDto;
import io.mosip.packet.core.entity.PacketTracker;
import io.mosip.packet.core.logger.DataProcessLogger;
import io.mosip.packet.core.repository.PacketTrackerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;

import static io.mosip.packet.core.constant.RegistrationConstants.*;
import static io.mosip.packet.core.constant.RegistrationConstants.APPLICATION_ID;

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
            if(conn == null) {
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
                preparedStatement = conn.prepareStatement(String.format("INSERT INTO %s (REF_ID, REG_NO, STATUS, CR_BY, CR_DTIMES) VALUES (?, ?, ?, ?, ?)", TRACKER_TABLE_NAME));

                long timeNow = System.currentTimeMillis();
                Timestamp timestamp = new Timestamp(timeNow);
                preparedStatement.setString(1, trackerRequestDto.getRefId());
                preparedStatement.setString(2, trackerRequestDto.getRegNo());
                preparedStatement.setString(3, trackerRequestDto.getStatus());
                preparedStatement.setString(4, "MIGRATOR");
                preparedStatement.setTimestamp(5, timestamp);
                preparedStatement.addBatch();
        } catch (SQLException throwables) {
            LOGGER.error("SESSION_ID", APPLICATION_NAME, APPLICATION_ID,
                    "Exception encountered during Tracker record insertion - TrackerUtil "
                            + ExceptionUtils.getStackTrace(throwables));
        }
    }

    public void closeStatement() {
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

    public boolean isRecordPresent(Object value) throws SQLException {
        PreparedStatement statement = null;
        try {
            statement = conn.prepareStatement(String.format("SELECT 1 FROM %s WHERE REF_ID = ?", TRACKER_TABLE_NAME));
            statement.setString(1, value.toString());
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
    }

    public static boolean isTrackerHostSame(String sourceHost, String databaseName) {
        return connectionHost != null && connectionHost.equalsIgnoreCase(sourceHost) && databaseName.equalsIgnoreCase(keys.getProperty("spring.datasource.tracker.database"));
    }


    private static class DBCreator {

        private StringBuilder sb;

        public DBCreator(DBTypes dbTypes) throws Exception {
            sb = new StringBuilder();
            sb.append(String.format("CREATE TABLE %s (", TRACKER_TABLE_NAME));
            sb.append(addColumn("REF_ID", String.class, 100, true, dbTypes) + ",");
            sb.append(addColumn("REG_NO", String.class, 100, true, dbTypes) + ",");
            sb.append(addColumn("STATUS", String.class, 50, false, dbTypes) + ",");
            sb.append(addColumn("CR_BY", String.class, 50, true, dbTypes) + ",");
            sb.append(addColumn("CR_DTIMES", Timestamp.class, 100, true, dbTypes) + ",");
            sb.append(addColumn("UPD_BY", String.class, 100, false, dbTypes) + ",");
            sb.append(addColumn("UPD_DTIMES", Timestamp.class, 100, false, dbTypes));
            sb.append(");");

            sb.append(String.format("ALTER TABLE %s ADD PRIMARY KEY (REF_ID);", TRACKER_TABLE_NAME));
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

    public void addTrackerLocalEntry(String refId, String regNo, TrackerStatus status, String process, String request) {
        Optional<PacketTracker> optional= packetTrackerRepository.findById(refId);

        PacketTracker packetTracker;

        if(optional.isPresent()) {
            packetTracker = optional.get();
            if(regNo != null ) packetTracker.setRegNo(regNo);
            if(status != null ) packetTracker.setStatus(status.toString());
            if(process != null ) packetTracker.setProcess(process);
            if(request != null ) packetTracker.setRequest(request);
            packetTracker.setUpdBy("BATCH");
            packetTracker.setUpdDtimes(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()));
        } else {
            packetTracker = new PacketTracker();
            if(refId != null ) packetTracker.setRefId(refId);
            if(regNo != null ) packetTracker.setRegNo(regNo);
            if(status != null ) packetTracker.setStatus(status.toString());
            if(process != null ) packetTracker.setProcess(process);
            if(request != null ) packetTracker.setRequest(request);
            packetTracker.setCrBy("BATCH");
            packetTracker.setCrDtime(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()));
        }
        packetTrackerRepository.saveAndFlush(packetTracker);
    }
}
