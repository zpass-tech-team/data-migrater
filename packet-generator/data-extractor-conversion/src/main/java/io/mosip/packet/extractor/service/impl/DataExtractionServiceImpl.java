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
import io.mosip.packet.core.logger.DataProcessLogger;
import io.mosip.packet.core.repository.PacketTrackerRepository;
import io.mosip.packet.core.service.CustomNativeRepository;
import io.mosip.packet.core.service.impl.CustomNativeRepositoryImpl;
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

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Clob;
import java.sql.ResultSet;
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

    private QualityWriterFactory qualityWriterFactory;

    @Autowired
    private PacketTrackerRepository packetTrackerRepository;

    @Autowired
    private ClientCryptoFacade clientCryptoFacade;

    @Autowired
    private CustomNativeRepository customNativeRepository;
    private LinkedHashMap<String, DocumentCategoryDto> documentCategory = new LinkedHashMap<>();
    private LinkedHashMap<String, DocumentTypeExtnDto> documentType = new LinkedHashMap<>();
    private Map<String, HashSet<String>> fieldsCategoryMap = new HashMap<>();
    private ObjectMapper objectMapper = new ObjectMapper();
    private boolean backendProcess = false;
    private boolean isRecordPresentForProcess = true;

    @Value("${mosip.data.quality.writer.classname:io.mosip.packet.data.quality.writer.CSVFileWriter}")
    private String qualityWriterClassName;

    @Autowired
    private List<QualityWriterFactory> qualityWriterFactoryList;

    @PostConstruct
    public void loadConfiguration() {
        for(QualityWriterFactory factory : qualityWriterFactoryList) {
            if(factory.getClass().getName().equals(qualityWriterClassName))
                qualityWriterFactory = factory;
        }
    }

    @Override
    public LinkedHashMap<String, Object> extractBioDataFromDBAsBytes(DBImportRequest dbImportRequest, Boolean localStoreRequired) throws Exception {
        LinkedHashMap<String, Object> biodata = new LinkedHashMap<>();
        dataBaseUtil.connectDatabase(dbImportRequest);
        populateTableFields(dbImportRequest);
        commonUtil.updateFieldCategory(dbImportRequest);

        List<TableRequestDto> tableRequestDtoList = dbImportRequest.getTableDetails();
        Collections.sort(tableRequestDtoList);
        TableRequestDto tableRequestDto  = tableRequestDtoList.get(0);
        ResultSet resultSet = null;
 /*       resultSet = dataBaseUtil.readDataFromDatabase(dbImportRequest, null, fieldsCategoryMap);

        if (resultSet != null) {
            dataBaseUtil.populateDataFromResultSet(tableRequestDto, dbImportRequest.getColumnDetails(), resultSet, null, dataMap, fieldsCategoryMap, localStoreRequired, false);

            for (Map<FieldCategory, LinkedHashMap<String, Object>> dataHashMap : dataMap) {
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
    public LinkedHashMap<String, Object> extractBioDataFromDB(DBImportRequest dbImportRequest, Boolean localStoreRequired) throws Exception {
        LinkedHashMap<String, Object> bioData = extractBioDataFromDBAsBytes(dbImportRequest, localStoreRequired);
        LinkedHashMap<String, Object> convertedData = new LinkedHashMap<>();

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
        Timer processor = null;
        Long DELAY_SECONDS = 10000L;


        try {
            commonUtil.updateFieldCategory(dbImportRequest);
            commonUtil.updateBioDestFormat(dbImportRequest);
            List<ValidatorEnum> enumList = new ArrayList<>();
            enumList.add(ValidatorEnum.ID_SCHEMA_VALIDATOR);
            enumList.add(ValidatorEnum.FILTER_VALIDATOR);
            validationUtil.validateRequest(dbImportRequest, enumList);
            populateTableFields(dbImportRequest);
            dataBaseUtil.connectDatabase(dbImportRequest);
            CustomizedThreadPoolExecutor threadPool = new CustomizedThreadPoolExecutor(maxThreadPoolCount, maxRecordsCountPerThreadPool, maxThreadExecCount, GlobalConfig.getActivityName());
            Thread.UncaughtExceptionHandler handler = new Thread.UncaughtExceptionHandler() {

                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    LOGGER.error("SESSION_ID", APPLICATION_NAME, APPLICATION_ID, "Thread - " + t.getName() + " ERROR : " + e.getMessage());
                }
            };

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
                    trackerUtil.addTrackerLocalEntry(resultDto.getRefId(), null, (enablePaccketUploader ? TrackerStatus.PROCESSED : TrackerStatus.PROCESSED_WITHOUT_UPLOAD), dbImportRequest.getProcess(), resultDto.getComments(), SESSION_KEY, GlobalConfig.getActivityName());
                }
            };

            processor = new Timer("Packet_Processor");
            processor.schedule(new TimerTask() {
                @SneakyThrows
                @Override
                public void run() {
                    if(!backendProcess && threadPool.isBatchAcceptRequest()) {
                        List<String> list = new ArrayList<>();
                        list.add(TrackerStatus.QUEUED.toString());
                        List<PacketTracker> packetList =  packetTrackerRepository.findByStatusIn(list);

                        if(packetList.size() > 0)
                            for(PacketTracker tracker : packetList) {
                                if(threadPool.isBatchAcceptRequest()) {
                                    backendProcess = true;
                                    ByteArrayInputStream bis = new ByteArrayInputStream(clientCryptoFacade.getClientSecurity().isTPMInstance() ? clientCryptoFacade.decrypt(Base64.getDecoder().decode(tracker.getRequest())) : Base64.getDecoder().decode(tracker.getRequest()));
                                    ObjectInputStream is = new ObjectInputStream(bis);
                                    BaseThreadController baseThreadController = new BaseThreadController();
                                    baseThreadController.setSetter(setter);

                                    if(processPacket(dbImportRequest, packetCreatorResponse, baseThreadController, (Map<FieldCategory, LinkedHashMap<String, Object>>) is.readObject()))
                                        threadPool.ExecuteTask(baseThreadController);
                                } else {
                                    break;
                                }
                            }
                        else
                            isRecordPresentForProcess = false;

/*                        customNativeRepository.getPacketTrackerData(list, new CustomNativeRepositoryImpl.PacketTrackerInterface() {
                            @Override
                            public void processData(Map<FieldCategory, LinkedHashMap<String, Object>> dataHashMap) throws Exception {
                                backendProcess = true;

                                BaseThreadController baseThreadController = new BaseThreadController();
                                baseThreadController.setSetter(setter);

                                if(processPacket(dbImportRequest, packetCreatorResponse, baseThreadController, dataHashMap))
                                    threadPool.ExecuteTask(baseThreadController);
                            }
                        });*/
                        backendProcess = false;
                    }
                }
            }, 0, DELAY_SECONDS);

            Date startTime = new Date();
            dataBaseUtil.readDataFromDatabase(dbImportRequest, null, fieldsCategoryMap);

            do {
                System.out.println("Thread Sleeping for some time 60S");
                Thread.sleep(60000);
            } while(isRecordPresentForProcess || !threadPool.isTaskCompleted() || backendProcess || !threadPool.isBatchAcceptRequest());

            System.out.println("Start Time " + startTime);
            System.out.println("End Time Time " + new Date());

            if(threadPool.isTaskCompleted())
                processor.cancel();
        } finally {
            dataBaseUtil.closeConnection();
            if(!IS_ONLY_FOR_QUALITY_CHECK)
                trackerUtil.closeStatement();

            if(IS_RUNNING_AS_BATCH)
                qualityWriterFactory.preDestroyProcess();
        }

        LOGGER.info("SESSION_ID", APPLICATION_NAME, APPLICATION_ID, "Packet Uploaded List : " + (new Gson()).toJson(packetCreatorResponse));
        LOGGER.info("SESSION_ID", APPLICATION_NAME, APPLICATION_ID, "DataExtractionServiceImpl :: createPacketFromDataBase():: exit");

        return packetCreatorResponse;
    }

    private boolean processPacket(DBImportRequest dbImportRequest, PacketCreatorResponse packetCreatorResponse, BaseThreadController baseThreadController, Map<FieldCategory, LinkedHashMap<String, Object>> dataHashMap) throws Exception {
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

            if(!trackerUtil.isRecordPresent(dataHashMap.get(FieldCategory.DEMO).get(dbImportRequest.getTrackerInfo().getTrackerColumn()), GlobalConfig.getActivityName()) || IS_ONLY_FOR_QUALITY_CHECK) {
                baseThreadController.setDataHashMap(dataHashMap);
                baseThreadController.setRegistrationId(registrationId);
                baseThreadController.setTrackerColumn(dbImportRequest.getTrackerInfo().getTrackerColumn());
                baseThreadController.setProcessor(new ThreadProcessor() {
                    @Override
                    public void processData(ResultSetter setter, Map<FieldCategory, LinkedHashMap<String, Object>> dataHashMap, String registrationId, String trackerColumn) {
                        Long startTime = System.nanoTime();
                        LinkedHashMap<String, Object> demoDetails = dataHashMap.get(FieldCategory.DEMO);
                        LinkedHashMap<String, Object> bioDetails = dataHashMap.get(FieldCategory.BIO);
                        LinkedHashMap<String, Object> docDetails = dataHashMap.get(FieldCategory.DOC);
                        String refId = registrationId == null ? demoDetails.get(trackerColumn).toString() : registrationId;
                        LOGGER.info("SESSION_ID", APPLICATION_NAME, APPLICATION_ID, "Thread - " + refId + " Process Started");

                        try {
                            trackerUtil.addTrackerLocalEntry(demoDetails.get(trackerColumn).toString(), registrationId, TrackerStatus.STARTED, dbImportRequest.getProcess(), null, SESSION_KEY, getActivityName());

                            HashMap<String, String> csvMap = qualityWriterFactory.getDataMap();
                            LinkedHashMap<String, String> metaInfo = new LinkedHashMap<>();

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

                                LinkedHashMap<String, Object> idSchema = commonUtil.getLatestIdSchema();
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

                                    List<PacketUploadDTO> uploadList = new ArrayList<>();
                                    uploadList.add(uploadDTO);
                                    LinkedHashMap<String, PacketUploadResponseDTO> response = new LinkedHashMap<>();

                                    if (enablePaccketUploader) {
                                        packetUploaderService.syncPacket(uploadList, ConfigUtil.getConfigUtil().getCenterId(), ConfigUtil.getConfigUtil().getMachineId(), response);
                                        trackerUtil.addTrackerLocalEntry(demoDetails.get(trackerColumn).toString(), info.getId(), TrackerStatus.SYNCED, null, uploadList, SESSION_KEY, GlobalConfig.getActivityName());
                                        packetUploaderService.uploadSyncedPacket(uploadList, response);
                                    } else {
                                        LOGGER.warn("SESSION_ID", APPLICATION_NAME, APPLICATION_ID, "Packet Uploader Disabled : " + (new Gson()).toJson(response));
                                    }

                                    ResultDto resultDto = new ResultDto();
                                    resultDto.setRegNo(info.getId());
                                    resultDto.setRefId(demoDetails.get(trackerColumn).toString());
                                    resultDto.setComments((new Gson()).toJson(response));
                                    resultDto.setStatus(enablePaccketUploader ? TrackerStatus.PROCESSED : TrackerStatus.PROCESSED_WITHOUT_UPLOAD);
                                    setter.setResult(resultDto);
                                    LOGGER.info("SESSION_ID", APPLICATION_NAME, APPLICATION_ID, "Packet Upload Response : " + (new Gson()).toJson(response));
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
                            LOGGER.error("SESSION_ID", APPLICATION_NAME, APPLICATION_ID, "Exception : " + e.getLocalizedMessage());
                            ResultDto resultDto = new ResultDto();
                            resultDto.setRegNo(null);
                            resultDto.setRefId(demoDetails.get(trackerColumn).toString());
                            resultDto.setComments(e.getMessage());
                            resultDto.setStatus(TrackerStatus.FAILED);
                            setter.setResult(resultDto);
                        }
                        LOGGER.info("SESSION_ID", APPLICATION_NAME, APPLICATION_ID, "Thread - " + (registrationId == null ? demoDetails.get(trackerColumn).toString() : registrationId) + " Process Ended");
                        Long endTime = System.nanoTime();
                        Long timeDifference = endTime-startTime;
                        LOGGER.info("SESSION_ID", APPLICATION_NAME, APPLICATION_ID, "Thread - " + refId + " Time taken to complete " + TimeUnit.SECONDS.convert(timeDifference, TimeUnit.NANOSECONDS));
                        TIMECONSUPTIONQUEUE.add(timeDifference);
                    }
                });
                return true;
            } else {
                return false;
            }
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
                                fieldsCategoryMap.put(tableName, new HashSet<>());

                            fieldsCategoryMap.get(tableName).add(fieldName.getFieldName());
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
                    fieldsCategoryMap.put(tableName, new HashSet<>());

                fieldsCategoryMap.get(tableName).add(fieldName.getFieldName());
            }

            if(fieldFormatRequest.getPrimaryField() != null)
                fieldsCategoryMap.get(tableName).add(fieldFormatRequest.getFieldNameWithoutSchema(fieldFormatRequest.getPrimaryField()));
            if(fieldFormatRequest.getSrcFieldForQualityScore() != null)
                fieldsCategoryMap.get(tableName).add(fieldFormatRequest.getFieldNameWithoutSchema(fieldFormatRequest.getSrcFieldForQualityScore()));

            if(fieldFormatRequest.getDocumentAttributes() != null) {
                DocumentAttributes documentAttributes = fieldFormatRequest.getDocumentAttributes();
                fieldsCategoryMap.get(tableName).add(documentAttributes.getDocumentRefNoField().contains("STATIC") ? "'" + commonUtil.getDocumentAttributeStaticValue(documentAttributes.getDocumentRefNoField()) + "' AS STATIC_" +  commonUtil.getDocumentAttributeStaticValue(documentAttributes.getDocumentRefNoField())
                        :  fieldFormatRequest.getFieldNameWithoutSchema(documentAttributes.getDocumentRefNoField()));
                fieldsCategoryMap.get(tableName).add(documentAttributes.getDocumentFormatField().contains("STATIC") ? "'" + commonUtil.getDocumentAttributeStaticValue(documentAttributes.getDocumentFormatField()) + "' AS STATIC_" + commonUtil.getDocumentAttributeStaticValue(documentAttributes.getDocumentFormatField())
                        :  fieldFormatRequest.getFieldNameWithoutSchema(documentAttributes.getDocumentFormatField()));
                fieldsCategoryMap.get(tableName).add(documentAttributes.getDocumentCodeField().contains("STATIC") ? "'" + commonUtil.getDocumentAttributeStaticValue(documentAttributes.getDocumentCodeField()) + "' AS STATIC_" + commonUtil.getDocumentAttributeStaticValue(documentAttributes.getDocumentCodeField())
                        :  fieldFormatRequest.getFieldNameWithoutSchema(documentAttributes.getDocumentCodeField()));
            }
        }
    }
}
