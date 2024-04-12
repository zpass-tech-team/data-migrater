package io.mosip.packet.extractor.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import io.mosip.commons.packet.dto.PacketInfo;
import io.mosip.commons.packet.dto.packet.PacketDto;
import io.mosip.kernel.clientcrypto.service.impl.ClientCryptoFacade;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.packet.core.constant.FieldCategory;
import io.mosip.packet.core.constant.GlobalConfig;
import io.mosip.packet.core.constant.ValidatorEnum;
import io.mosip.packet.core.constant.tracker.TrackerStatus;
import io.mosip.packet.core.dto.dbimport.*;
import io.mosip.packet.core.dto.masterdata.DocumentCategoryDto;
import io.mosip.packet.core.dto.masterdata.DocumentTypeExtnDto;
import io.mosip.packet.core.dto.tracker.TrackerRequestDto;
import io.mosip.packet.core.dto.upload.PacketUploadDTO;
import io.mosip.packet.core.dto.upload.PacketUploadResponseDTO;
import io.mosip.packet.core.entity.PacketTracker;
import io.mosip.packet.core.exception.ExceptionUtils;
import io.mosip.packet.core.logger.DataProcessLogger;
import io.mosip.packet.core.repository.PacketTrackerRepository;
import io.mosip.packet.core.service.CustomNativeRepository;
import io.mosip.packet.core.service.thread.*;
import io.mosip.packet.core.spi.QualityWriterFactory;
import io.mosip.packet.core.util.CommonUtil;
import io.mosip.packet.core.util.DataBaseUtil;
import io.mosip.packet.core.util.FixedListQueue;
import io.mosip.packet.core.util.TrackerUtil;
import io.mosip.packet.extractor.service.DataExtractionService;
import io.mosip.packet.extractor.util.ConfigUtil;
import io.mosip.packet.extractor.util.PacketCreator;
import io.mosip.packet.extractor.util.TableDataMapperUtil;
import io.mosip.packet.extractor.util.ValidationUtil;
import io.mosip.packet.manager.service.PacketCreatorService;
import io.mosip.packet.manager.util.mock.sbi.devicehelper.MockDeviceUtil;
import io.mosip.packet.uploader.service.PacketUploaderService;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static io.mosip.packet.core.constant.GlobalConfig.SESSION_KEY;
import static io.mosip.packet.core.constant.GlobalConfig.*;
import static io.mosip.packet.core.constant.RegistrationConstants.*;

@Service
public class DataExtractionServiceImpl implements DataExtractionService {

    private static final Logger LOGGER = DataProcessLogger.getLogger(DataExtractionServiceImpl.class);

    @Value("${packet.manager.account.name}")
    private String packetUploadPath;

    @Value("${mosip.selected.languages}")
    private String primaryLanguage;

    @Value("${mosip.packet.creater.source}")
    private String source;

    @Value("${mosip.packet.creater.max-threadpool-count:1}")
    private Integer maxThreadPoolCount;

    @Value("${mosip.packet.creater.max-records-process-per-threadpool:100}")
    private Integer maxRecordsCountPerThreadPool;

    @Value("${mosip.packet.creater.max-thread-execution-count:100}")
    private Integer maxThreadExecCount;

    @Value("${mosip.packet.uploader.enable:true}")
    private boolean enablePaccketUploader;

    @Value("${mosip.extractor.application.id.column:}")
    private String applicationIdColumn;

    @Value("${mosip.packet.uploader.enable.only.packet.upload:false}")
    private boolean enableOnlyPacketUploader;

    @Autowired
    ValidationUtil validationUtil;

    @Autowired
    private MockDeviceUtil mockDeviceUtil;

    @Autowired
    private DataBaseUtil dataBaseUtil;

    @Autowired
    private CommonUtil commonUtil;

    @Autowired
    private TableDataMapperUtil tableDataMapperUtil;

    @Autowired
    PacketCreatorService packetCreatorService;

    @Autowired
    PacketUploaderService packetUploaderService;

    @Autowired
    private PacketCreator packetCreator;

    @Autowired
    private TrackerUtil trackerUtil;

    @Autowired
    private QualityWriterFactory qualityWriterFactory;

    @Autowired
    private PacketTrackerRepository packetTrackerRepository;

    @Autowired
    private ClientCryptoFacade clientCryptoFacade;

    private boolean uploadProcessStarted = false;

    private Map<String, HashMap<String, String>> fieldsCategoryMap = new HashMap<>();
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public HashMap<String, Object> extractBioDataFromDBAsBytes(DBImportRequest dbImportRequest, Boolean localStoreRequired) throws Exception {
        HashMap<String, Object> biodata = new HashMap<>();
        dataBaseUtil.connectDatabase(dbImportRequest);
        populateTableFields(dbImportRequest);
        commonUtil.updateFieldCategory(dbImportRequest);

        List<TableRequestDto> tableRequestDtoList = dbImportRequest.getTableDetails();
        Collections.sort(tableRequestDtoList);
        TableRequestDto tableRequestDto  = tableRequestDtoList.get(0);
 /*       ResultSet resultSet = null;
        resultSet = dataBaseUtil.readDataFromDatabase(dbImportRequest, null, fieldsCategoryMap);

        if (resultSet != null) {
            dataBaseUtil.populateDataFromResultSet(tableRequestDto, dbImportRequest.getColumnDetails(), resultSet, null, dataMap, fieldsCategoryMap, localStoreRequired, false);

            for (Map<FieldCategory, HashMap<String, Object>> dataHashMap : dataMap) {
                for (int i = 1; i < tableRequestDtoList.size(); i++) {
                    TableRequestDto tableRequestDto1 = tableRequestDtoList.get(i);
                    resultSet = dataBaseUtil.readDataFromDatabase(tableRequestDto1, dataHashMap, fieldsCategoryMap);

                    if (resultSet != null) {
                        dataBaseUtil.populateDataFromResultSet(tableRequestDto1, dbImportRequest.getColumnDetails(), resultSet, dataHashMap, dataMap, fieldsCategoryMap, localStoreRequired, false);
                    }

                    for (FieldFormatRequest fieldFormatRequest : dbImportRequest.getColumnDetails()) {
                        if(fieldFormatRequest.getFieldCategory().equals(FieldCategory.BIO)) {
                            byte[] convertedImageData = (byte[]) dataHashMap.get(FieldCategory.BIO).get(fieldFormatRequest.getFieldToMap());
                            biodata.put(dataHashMap.get(FieldCategory.DEMO).get(fieldFormatRequest.getPrimaryField())+ "-" + fieldFormatRequest.getFieldNameWithoutSchema(fieldFormatRequest.getFieldName()),  convertedImageData);
                        }
                    }
                }
            }
        }*/

        return biodata;
    }

    @Override
    public HashMap<String, Object> extractBioDataFromDB(DBImportRequest dbImportRequest, Boolean localStoreRequired) throws Exception {
        HashMap<String, Object> bioData = extractBioDataFromDBAsBytes(dbImportRequest, localStoreRequired);
        HashMap<String, Object> convertedData = new HashMap<>();

        for(Map.Entry<String, Object> entry : bioData.entrySet()) {
            String data = Base64.getEncoder().encodeToString((byte[])entry.getValue());
             convertedData.put(entry.getKey(), data);
        }
        return convertedData;
    }

    @Override
    public PacketCreatorResponse createPacketFromDataBase(DBImportRequest dbImportRequest) throws Exception {
        LOGGER.info("SESSION_ID", APPLICATION_NAME, APPLICATION_ID, "DataExtractionServiceImpl :: createPacketFromDataBase():: entry");
        TIMECONSUPTIONQUEUE = new FixedListQueue<Long>(100);
        mockDeviceUtil.resetDevices();
        mockDeviceUtil.initDeviceHelpers();
        PacketCreatorResponse packetCreatorResponse = new PacketCreatorResponse();
        packetCreatorResponse.setRID(new ArrayList<>());
        PacketDto packetDto = null;
        TOTAL_RECORDS_FOR_PROCESS=0L;
        //Timer processor = null;
        //Long DELAY_SECONDS = 10000L;


        try {
            commonUtil.initialize(dbImportRequest);
            List<ValidatorEnum> enumList = new ArrayList<>();
            enumList.add(ValidatorEnum.ID_SCHEMA_VALIDATOR);
            enumList.add(ValidatorEnum.FILTER_VALIDATOR);
            validationUtil.validateRequest(dbImportRequest, enumList);
            populateTableFields(dbImportRequest);
            dataBaseUtil.connectDatabase(dbImportRequest);
            CustomizedThreadPoolExecutor threadPool = new CustomizedThreadPoolExecutor(maxThreadPoolCount, maxRecordsCountPerThreadPool, maxThreadExecCount, GlobalConfig.getActivityName());
/*            Thread.UncaughtExceptionHandler handler = new Thread.UncaughtExceptionHandler() {

                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    FAILED_RECORDS++;
                    LOGGER.error("SESSION_ID", APPLICATION_NAME, APPLICATION_ID, "Thread - " + t.getName(), e);
                }
            };*/

            ResultSetter setter = new ResultSetter() {
                @SneakyThrows
                @Override
                public void setResult(Object obj) {
                    ResultDto resultDto = (ResultDto) obj;
                    packetCreatorResponse.getRID().add(resultDto.getRegNo());
                    TrackerRequestDto trackerRequestDto = new TrackerRequestDto();
                    trackerRequestDto.setRegNo(resultDto.getRegNo());
                    trackerRequestDto.setRefId(resultDto.getRefId());
                    trackerRequestDto.setProcess(dbImportRequest.getProcess());
                    trackerRequestDto.setActivity(GlobalConfig.getActivityName());
                    trackerRequestDto.setSessionKey(SESSION_KEY);
                    trackerRequestDto.setStatus(resultDto.getStatus().toString());
                    trackerRequestDto.setComments(resultDto.getComments());
                    trackerUtil.addTrackerEntry(trackerRequestDto);
                    trackerUtil.addTrackerLocalEntry(resultDto.getRefId(), null, resultDto.getStatus(), dbImportRequest.getProcess(), resultDto.getComments(), SESSION_KEY, GlobalConfig.getActivityName());
                }
            };

            ResultSetter DataProcessor = new ResultSetter() {
                @SneakyThrows
                @Override
                public void setResult(Object obj) {
                    ThreadDataProcessController threadDataProcessController = new ThreadDataProcessController();
                    threadDataProcessController.setSetter(setter);
                    Map<FieldCategory, HashMap<String, Object>> dataHashMap = (Map<FieldCategory, HashMap<String, Object>>) obj;
                    if(processPacket(dbImportRequest, packetCreatorResponse, threadDataProcessController, dataHashMap))
                        threadPool.ExecuteTask(threadDataProcessController);
                    trackerUtil.addTrackerLocalEntry(dataHashMap.get(FieldCategory.DEMO).get(dbImportRequest.getTrackerInfo().getTrackerColumn()).toString(), null, TrackerStatus.STARTED, dbImportRequest.getProcess(), null, SESSION_KEY, getActivityName());
                }
            };

 /*           processor = new Timer("Packet_Processor");
            processor.schedule(new TimerTask() {
                @SneakyThrows
                @Override
                public void run() {
                    List<String> list = new ArrayList<>();
                    list.add(TrackerStatus.QUEUED.toString());
                    List<PacketTracker> packetList =  packetTrackerRepository.findByStatusIn(list);
                    System.out.println("No of Records Present" + packetList.size());

                    if(!backendProcess && threadPool.isBatchAcceptRequest()) {
                        System.out.println("Batch Accepting for Process Records");
                        backendProcess = true;

                        if(packetList.size() > 0)
                            for(PacketTracker tracker : packetList) {
                                if(threadPool.isBatchAcceptRequest()) {
                                    ByteArrayInputStream bis = new ByteArrayInputStream(clientCryptoFacade.getClientSecurity().isTPMInstance() ? clientCryptoFacade.decrypt(Base64.getDecoder().decode(tracker.getRequest())) : Base64.getDecoder().decode(tracker.getRequest()));
                                    ObjectInputStream is = new ObjectInputStream(bis);
                                    BaseThreadController baseThreadController = new BaseThreadController();
                                    baseThreadController.setSetter(setter);

                                    Map<FieldCategory, LinkedHashMap<String, Object>> dataHashMap = (Map<FieldCategory, LinkedHashMap<String, Object>>) is.readObject();
                                    if(processPacket(dbImportRequest, packetCreatorResponse, baseThreadController, dataHashMap))
                                        threadPool.ExecuteTask(baseThreadController);
                                    trackerUtil.addTrackerLocalEntry(dataHashMap.get(FieldCategory.DEMO).get(dbImportRequest.getTrackerInfo().getTrackerColumn()).toString(), null, TrackerStatus.STARTED, dbImportRequest.getProcess(), null, SESSION_KEY, getActivityName());
                                } else {
                                    System.out.println("System Queue full. Skipping");
                                    break;
                                }
                            }
                        else
                            isRecordPresentForProcess = false;

                        backendProcess = false;
                    }
                }
            }, 0, DELAY_SECONDS); */

            Date startTime = new Date();
            if(enablePaccketUploader) {
                IS_PACKET_UPLOAD_OPERATION = true;
                NO_OF_PACKETS_UPLOADED = 0L;
                Timer uploaderTimer = new Timer("Uploading Packet");
                uploaderTimer.schedule(new TimerTask() {
                    @SneakyThrows
                    @Override
                    public void run() {
                        try {
                            if(!uploadProcessStarted) {
                                uploadProcessStarted = true;
                                List<String> statusList = new ArrayList<>();
                                statusList.add("READY_TO_SYNC");
                                List<PacketTracker> trackerList =  packetTrackerRepository.findByStatusIn(statusList);

                                for(PacketTracker packetTracker : trackerList) {
                                    ByteArrayInputStream bis = new ByteArrayInputStream(clientCryptoFacade.getClientSecurity().isTPMInstance() ? clientCryptoFacade.decrypt(Base64.getDecoder().decode(packetTracker.getRequest())) : Base64.getDecoder().decode(packetTracker.getRequest()));
                                    ObjectInputStream is = new ObjectInputStream(bis);
                                    PacketUploadDTO uploadDTO = (PacketUploadDTO) is.readObject();

                                    //       String requestJson = new String(packetTracker.getRequest().getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
                                    //      PacketUploadDTO uploadDTO = objectMapper.readValue(requestJson, new TypeReference<PacketUploadDTO>() {});
                                    List<PacketUploadDTO> uploadList = new ArrayList<>();
                                    uploadList.add(uploadDTO);
                                    HashMap<String, PacketUploadResponseDTO> response = new HashMap<>();
                                    packetUploaderService.syncPacket(uploadList, ConfigUtil.getConfigUtil().getCenterId(), ConfigUtil.getConfigUtil().getMachineId(), response);
                                    trackerUtil.addTrackerLocalEntry(packetTracker.getRefId(), uploadDTO.getPacketId(), TrackerStatus.SYNCED, null, uploadList, SESSION_KEY, GlobalConfig.getActivityName());
                                    packetUploaderService.uploadSyncedPacket(uploadList, response);
                                    NO_OF_PACKETS_UPLOADED++;
                                    ResultDto resultDto = new ResultDto();
                                    resultDto.setRegNo(uploadDTO.getPacketId());
                                    resultDto.setRefId(packetTracker.getRefId());
                                    resultDto.setComments((new Gson()).toJson(response));
                                    resultDto.setStatus(enablePaccketUploader ? TrackerStatus.PROCESSED : TrackerStatus.PROCESSED_WITHOUT_UPLOAD);
                                    setter.setResult(resultDto);
                                    LOGGER.info("SESSION_ID", APPLICATION_NAME, APPLICATION_ID, "Packet Upload Response : " + (new Gson()).toJson(response));
                                }

                                if(trackerList.size() <= 0)
                                    IS_PACKET_UPLOAD_OPERATION = false;

                                uploadProcessStarted = false;
                            }
                        } catch (Exception e) {
                            uploadProcessStarted = false;
                            e.printStackTrace();
                            LOGGER.error("SESSION_ID", APPLICATION_NAME, APPLICATION_ID, "Packet Upload Response : " + e.getMessage() + ExceptionUtils.getStackTrace(e));
                        }
                    }
                }, 0, 5000L);
            }

            if(!enableOnlyPacketUploader)
                dataBaseUtil.readDataFromDatabase(dbImportRequest, null, fieldsCategoryMap, DataProcessor);
            threadPool.setInputProcessCompleted(true);


            do {
                Thread.sleep(15000);
            } while(!GlobalConfig.isThreadPoolCompleted());

            System.out.println("Start Time " + startTime);
            System.out.println("End Time Time " + new Date());

 //           if(threadPool.isTaskCompleted())
 //               processor.cancel();
        } catch (Exception e) {
          e.printStackTrace();
        } finally {
            dataBaseUtil.closeConnection();
            if(!IS_ONLY_FOR_QUALITY_CHECK)
                trackerUtil.closeStatement();

            qualityWriterFactory.preDestroyProcess();
        }
        System.out.println("Packet Uploaded List : " + (new Gson()).toJson(packetCreatorResponse));
        LOGGER.info("SESSION_ID", APPLICATION_NAME, APPLICATION_ID, "Packet Uploaded List : " + (new Gson()).toJson(packetCreatorResponse));
        LOGGER.info("SESSION_ID", APPLICATION_NAME, APPLICATION_ID, "DataExtractionServiceImpl :: createPacketFromDataBase():: exit");

        return packetCreatorResponse;
    }

    @Override
    public String refreshQualityAnalysisData() throws Exception {
        qualityWriterFactory.preDestroyProcess();
        return "Quality Analysis Data Refresh Successfully";
    }

    private boolean processPacket(DBImportRequest dbImportRequest, PacketCreatorResponse packetCreatorResponse, ThreadDataProcessController threadDataProcessController, Map<FieldCategory, HashMap<String, Object>> dataHashMap) throws Exception {
        if ( dataHashMap != null) {
            String registrationId = null;

            if(!IS_ONLY_FOR_QUALITY_CHECK) {
                if(applicationIdColumn != null && !applicationIdColumn.isEmpty()) {
                    if(dataHashMap.get(FieldCategory.DEMO).containsKey(applicationIdColumn)) {
                        registrationId = dataHashMap.get(FieldCategory.DEMO).get(applicationIdColumn).toString();
                    } else {
                        LOGGER.error("SESSION_ID", APPLICATION_NAME, APPLICATION_ID, "Application ID : " + applicationIdColumn + " not found in DataMap");
                        throw new Exception("Application ID : " + applicationIdColumn + " not found in DataMap");
                    }
                } else {
                    registrationId = commonUtil.generateRegistrationId(ConfigUtil.getConfigUtil().getCenterId(), ConfigUtil.getConfigUtil().getMachineId());
                }
            }

            threadDataProcessController.setDataHashMap(dataHashMap);
            threadDataProcessController.setRegistrationId(registrationId);
            threadDataProcessController.setTrackerColumn(dbImportRequest.getTrackerInfo().getTrackerColumn());
            threadDataProcessController.setProcessor(new ThreadProcessor() {
                @Override
                public void processData(ResultSetter setter, Map<FieldCategory, HashMap<String, Object>> dataHashMap, String registrationId, String trackerColumn) throws Exception {
                    Long startTime = System.nanoTime();
                    HashMap<String, Object> demoDetails = dataHashMap.get(FieldCategory.DEMO);
                    HashMap<String, Object> bioDetails = dataHashMap.get(FieldCategory.BIO);
                    HashMap<String, Object> docDetails = dataHashMap.get(FieldCategory.DOC);
                    String refId = registrationId == null ? demoDetails.get(trackerColumn).toString() : registrationId;
                    LOGGER.info("SESSION_ID", APPLICATION_NAME, APPLICATION_ID, "Thread - " + refId + " Process Started");

                    try {
                        trackerUtil.addTrackerLocalEntry(demoDetails.get(trackerColumn).toString(), registrationId, TrackerStatus.STARTED, dbImportRequest.getProcess(), null, SESSION_KEY, getActivityName());

                        HashMap<String, String> csvMap = qualityWriterFactory.getDataMap();
                        HashMap<String, String> metaInfo = new HashMap<>();

                        PacketDto packetDto = new PacketDto();
                        packetDto.setProcess(dbImportRequest.getProcess());
                        packetDto.setSource(source);
                        packetDto.setSchemaVersion(String.valueOf(commonUtil.getLatestIdSchema().get("idVersion")));
                        packetDto.setAdditionalInfoReqId(null);
                        packetDto.setMetaInfo(null);
                        packetDto.setOfflineMode(false);

                        if (!IS_ONLY_FOR_QUALITY_CHECK && docDetails.size() > 0) {
                            packetDto.setDocuments(packetCreator.setDocuments(docDetails, dbImportRequest.getIgnoreIdSchemaFields(), metaInfo, demoDetails));
                        }
                        Long timeDifference = System.nanoTime()-startTime;
                        LOGGER.debug("SESSION_ID", APPLICATION_NAME, APPLICATION_ID, "After Completion of Document Process " + refId + " " + TimeUnit.SECONDS.convert(timeDifference, TimeUnit.NANOSECONDS));

                        if (!IS_ONLY_FOR_QUALITY_CHECK && demoDetails.size() > 0) {
                            packetDto.setFields(packetCreator.setDemographic(demoDetails, (bioDetails.size() > 0), dbImportRequest.getIgnoreIdSchemaFields()));
                        }

                        timeDifference = System.nanoTime()-startTime;
                        LOGGER.debug("SESSION_ID", APPLICATION_NAME, APPLICATION_ID, "After Completion of Demographic Process " + refId + " " + TimeUnit.SECONDS.convert(timeDifference, TimeUnit.NANOSECONDS));

                        if (bioDetails.size() > 0) {
                            packetDto.setBiometrics(packetCreator.setBiometrics(bioDetails, metaInfo, csvMap, demoDetails.get(trackerColumn).toString()));
                        }

                        timeDifference = System.nanoTime()-startTime;
                        LOGGER.debug("SESSION_ID", APPLICATION_NAME, APPLICATION_ID, "After Completion of Biometric Process " + refId + " " + TimeUnit.SECONDS.convert(timeDifference, TimeUnit.NANOSECONDS));

                        csvMap.put("reg_no", registrationId);
                        csvMap.put("ref_id", demoDetails.get(trackerColumn).toString());
                        qualityWriterFactory.writeQualityData(csvMap);

                        if (!IS_ONLY_FOR_QUALITY_CHECK) {
                            packetDto.setId(registrationId);
                            packetDto.setRefId(ConfigUtil.getConfigUtil().getCenterId() + "_" + ConfigUtil.getConfigUtil().getMachineId());
                            packetCreator.setMetaData(metaInfo, packetDto, dbImportRequest);
                            packetDto.setMetaInfo(metaInfo);
                            packetDto.setAudits(packetCreator.setAudits(packetDto.getId()));

                            HashMap<String, Object> idSchema = commonUtil.getLatestIdSchema();
                            packetDto.setSchemaJson(idSchema.get("schemaJson").toString());
                            packetDto.setOfflineMode(true);

                            List<PacketInfo> infoList = packetCreatorService.persistPacket(packetDto);
                            PacketInfo info = infoList.get(0);

                            trackerUtil.addTrackerLocalEntry(demoDetails.get(trackerColumn).toString(), info.getId(), TrackerStatus.CREATED, dbImportRequest.getProcess(), demoDetails, SESSION_KEY, GlobalConfig.getActivityName());

                            Path identityFile = Paths.get(System.getProperty("user.dir"), "identity.json");

                            if (identityFile.toFile().exists()) {
                                PacketUploadDTO uploadDTO = new PacketUploadDTO();

                                JSONParser parser = new JSONParser();
                                JSONObject jsonObject = (JSONObject) parser.parse(IOUtils.toString(new FileInputStream(identityFile.toFile()), StandardCharsets.UTF_8));
                                JSONObject identityJsonObject = (JSONObject) jsonObject.get("identity");
                                for (Object entry : identityJsonObject.keySet()) {
                                    String val = (String) ((JSONObject) identityJsonObject.get(entry)).get("value");
                                    if (val.contains(",")) {
                                        String[] valList = val.split(",");
                                        String fullVal = null;

                                        for (String val2 : valList) {
                                            if (fullVal == null) {
                                                fullVal = (String) demoDetails.get(val2);
                                            } else {
                                                fullVal += " " + demoDetails.get(val2);
                                            }
                                        }
                                        uploadDTO.setValue(entry.toString(), fullVal);
                                    } else {
                                        uploadDTO.setValue(entry.toString(), demoDetails.get(entry));
                                    }
                                }

                                Path path = Paths.get(System.getProperty("user.dir"), "home/" + packetUploadPath);
                                uploadDTO.setPacketPath(path.toAbsolutePath().toString());
                                uploadDTO.setRegistrationType(dbImportRequest.getProcess());
                                uploadDTO.setPacketId(info.getId());
                                uploadDTO.setRegistrationId(info.getId().split("-")[0]);
                                uploadDTO.setLangCode(primaryLanguage);

                                if (enablePaccketUploader) {
  //                                  packetUploaderService.syncPacket(uploadList, ConfigUtil.getConfigUtil().getCenterId(), ConfigUtil.getConfigUtil().getMachineId(), response);
                                    trackerUtil.addTrackerLocalEntry(demoDetails.get(trackerColumn).toString(), info.getId(), TrackerStatus.READY_TO_SYNC, null, uploadDTO, SESSION_KEY, GlobalConfig.getActivityName());
  //                                  packetUploaderService.uploadSyncedPacket(uploadList, response);
                                } else {
                                    LOGGER.warn("SESSION_ID", APPLICATION_NAME, APPLICATION_ID, "Packet Uploader Disabled : " + demoDetails.get(trackerColumn).toString());
                                    ResultDto resultDto = new ResultDto();
                                    resultDto.setRegNo(info.getId());
                                    resultDto.setRefId(demoDetails.get(trackerColumn).toString());
                                    resultDto.setComments("Packet Created");
                                    resultDto.setStatus(TrackerStatus.PROCESSED_WITHOUT_UPLOAD);
                                    setter.setResult(resultDto);
                                }
                            } else {
                                throw new Exception("Identity Mapping JSON File missing");
                            }
                        } else {
                            ResultDto resultDto = new ResultDto();
                            resultDto.setRegNo(null);
                            resultDto.setRefId(demoDetails.get(trackerColumn).toString());
                            resultDto.setComments("Quality Calculation Completed Successfully");
                            resultDto.setStatus(TrackerStatus.PROCESSED_WITHOUT_UPLOAD);
                            setter.setResult(resultDto);
                        }
                    } catch (Exception e) {
                        LOGGER.error("SESSION_ID", APPLICATION_NAME, APPLICATION_ID, "Exception : " + e.getMessage(), e);
                            ResultDto resultDto = new ResultDto();
                        resultDto.setRegNo(null);
                        resultDto.setRefId(demoDetails.get(trackerColumn).toString());
                        resultDto.setComments(e.getMessage());
                        resultDto.setStatus(TrackerStatus.FAILED);
                        setter.setResult(resultDto);
                        throw e;
                    }
                    LOGGER.info("SESSION_ID", APPLICATION_NAME, APPLICATION_ID, "Thread - " + (registrationId == null ? demoDetails.get(trackerColumn).toString() : registrationId) + " Process Ended");
                    Long endTime = System.nanoTime();
                    Long timeDifference = endTime-startTime;
                    LOGGER.info("SESSION_ID", APPLICATION_NAME, APPLICATION_ID, "Thread - " + refId + " Time taken to complete " + TimeUnit.SECONDS.convert(timeDifference, TimeUnit.NANOSECONDS));
                    TIMECONSUPTIONQUEUE.add(timeDifference);
                }
            });
            return true;
        }
        return false;
    }

    private void populateTableFields(DBImportRequest dbImportRequest) throws Exception {
        fieldsCategoryMap.clear();

        for (FieldFormatRequest fieldFormatRequest : dbImportRequest.getColumnDetails()) {
            String tableName = DEFAULT_TABLE;
            if (fieldFormatRequest.getFieldName().contains(",")) {
                switch(dbImportRequest.getDbType().toString()) {
                    case "MSSQL":
                    case "ORACLE":
                        for (FieldName fieldName : fieldFormatRequest.getFieldList()) {
                            if(fieldName.getTableName() != null)
                                tableName = fieldName.getTableName();

                            if (!fieldsCategoryMap.containsKey(tableName))
                                fieldsCategoryMap.put(tableName, new HashMap<>());

                            fieldsCategoryMap.get(tableName).put(fieldName.getFieldName(), fieldFormatRequest.getStaticValue());
                        }
                        break;

                    default:
                        throw new Exception("Implementation missing for Database to Read Data DBType :" +  dbImportRequest.getDbType().toString());
                }
            } else {
                FieldName fieldName = fieldFormatRequest.getFieldList().get(0);
                if(fieldName.getTableName() != null)
                    tableName = fieldName.getTableName();

                if (!fieldsCategoryMap.containsKey(tableName))
                    fieldsCategoryMap.put(tableName, new HashMap<>());

                fieldsCategoryMap.get(tableName).put(fieldName.getFieldName(), fieldFormatRequest.getStaticValue());
            }

            if(fieldFormatRequest.getPrimaryField() != null)
                fieldsCategoryMap.get(tableName).put(fieldFormatRequest.getFieldNameWithoutSchema(fieldFormatRequest.getPrimaryField()),null);
            if(fieldFormatRequest.getSrcFieldForQualityScore() != null)
                fieldsCategoryMap.get(tableName).put(fieldFormatRequest.getFieldNameWithoutSchema(fieldFormatRequest.getSrcFieldForQualityScore()), null);

            if(fieldFormatRequest.getDocumentAttributes() != null) {
                DocumentAttributes documentAttributes = fieldFormatRequest.getDocumentAttributes();
                fieldsCategoryMap.get(tableName).put(documentAttributes.getDocumentRefNoField().contains("STATIC") ? "'" + commonUtil.getDocumentAttributeStaticValue(documentAttributes.getDocumentRefNoField()) + "' AS STATIC_" +  commonUtil.getDocumentAttributeStaticValue(documentAttributes.getDocumentRefNoField())
                        :  fieldFormatRequest.getFieldNameWithoutSchema(documentAttributes.getDocumentRefNoField()), null);
                fieldsCategoryMap.get(tableName).put(documentAttributes.getDocumentFormatField().contains("STATIC") ? "'" + commonUtil.getDocumentAttributeStaticValue(documentAttributes.getDocumentFormatField()) + "' AS STATIC_" + commonUtil.getDocumentAttributeStaticValue(documentAttributes.getDocumentFormatField())
                        :  fieldFormatRequest.getFieldNameWithoutSchema(documentAttributes.getDocumentFormatField()),null);
                fieldsCategoryMap.get(tableName).put(documentAttributes.getDocumentCodeField().contains("STATIC") ? "'" + commonUtil.getDocumentAttributeStaticValue(documentAttributes.getDocumentCodeField()) + "' AS STATIC_" + commonUtil.getDocumentAttributeStaticValue(documentAttributes.getDocumentCodeField())
                        :  fieldFormatRequest.getFieldNameWithoutSchema(documentAttributes.getDocumentCodeField()), null);

                if(fieldFormatRequest.getDocumentAttributes().getDocumentValueMap() == null) {
                    throw new Exception("Implementation missing for Document Value Map to ID Schema for Column :" +  fieldFormatRequest.getFieldName());
                }
            }
        }
    }
}
