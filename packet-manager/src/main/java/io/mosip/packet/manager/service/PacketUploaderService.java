package io.mosip.packet.manager.service;

import io.mosip.commons.packet.spi.IPacketCryptoService;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.FileUtils;
import io.mosip.kernel.core.util.HMACUtils2;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.kernel.keymanagerservice.exception.KeymanagerServiceException;
import io.mosip.packet.manager.constants.RegistrationConstants;
import io.mosip.packet.manager.dto.PacketUploadDTO;
import io.mosip.packet.manager.dto.RegistrationPacketSyncDTO;
import io.mosip.packet.manager.dto.SyncRegistrationDTO;
import io.mosip.packet.manager.exception.ConnectionException;
import io.mosip.packet.manager.exception.RegBaseCheckedException;
import io.mosip.packet.manager.exception.RegistrationExceptionConstants;
import io.mosip.packet.manager.util.ServiceDelegateUtil;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.retry.support.RetryTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import static io.mosip.kernel.core.util.JsonUtils.javaObjectToJsonString;
import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;

@Component
public class PacketUploaderService {

    @Autowired
    private ServiceDelegateUtil serviceDelegateUtil;

    @Value("${mosip.registration.retry.delay.packet.upload}")
    private String MOSIP_RETRY_DELAY;

    @Value("${mosip.registration.retry.maxattempts.packet.upload}")
    private String MOSIP_RETRY_ATTEMPT;

    @Autowired
    private Environment environment;

    @Autowired
    @Qualifier("OfflinePacketCryptoServiceImpl")
    private IPacketCryptoService offlinePacketCryptoServiceImpl;

    private RetryTemplate retryTemplate;

    @PostConstruct
    public void init() {
        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(parseLong(MOSIP_RETRY_DELAY));

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(parseInt(MOSIP_RETRY_ATTEMPT));

        retryTemplate = new RetryTemplateBuilder()
                .retryOn(ConnectionException.class)
                .customPolicy(retryPolicy)
                .customBackoff(backOffPolicy)
                .build();
    }

    public void syncPacket(@NonNull List<PacketUploadDTO> packets) {
        try {
            syncRIDToServerWithRetryWrapper(packets);
        } catch (ConnectionException | RegBaseCheckedException | JsonProcessingException | KeymanagerServiceException e) {
            e.printStackTrace();
        }
    }

    private void syncRIDToServerWithRetryWrapper(List<PacketUploadDTO> packets)
            throws KeymanagerServiceException, RegBaseCheckedException, JsonProcessingException, ConnectionException {
        RetryCallback<Boolean, ConnectionException> retryCallback = new RetryCallback<Boolean, ConnectionException>() {
            @SneakyThrows
            @Override
            public Boolean doWithRetry(RetryContext retryContext) throws ConnectionException {
                syncRIDToServer(packets);
                return true;
            }
        };
        retryTemplate.execute(retryCallback);
    }

    private synchronized void syncRIDToServer(List<PacketUploadDTO> packets)
            throws Exception {
        //Precondition check, proceed only if met, otherwise throws exception
        proceedWithPacketSync();

        List<SyncRegistrationDTO> syncDtoList = getPacketSyncDtoList(packets);
        List<SyncRegistrationDTO> syncDtoWithPacketId = syncDtoList.stream().filter(dto -> !dto.getRegistrationId().equals(dto.getPacketId())).collect(Collectors.toList());

        if (syncDtoList != null && !syncDtoList.isEmpty()) {
            try {
                syncRID(syncDtoWithPacketId, true);
            } catch (Exception e) {
                throw e;
            }
        }
    }

    public void proceedWithPacketSync() throws Exception {
        if(!serviceDelegateUtil.isNetworkAvailable())
            throw new Exception("No Internet Connection");
    }

    private List<SyncRegistrationDTO> getPacketSyncDtoList(@NonNull List<PacketUploadDTO> packets) throws IOException, NoSuchAlgorithmException {
        List<SyncRegistrationDTO> syncDtoList = new ArrayList<>();
        for (PacketUploadDTO packet : packets) {

            SyncRegistrationDTO syncDto = new SyncRegistrationDTO();
            syncDto.setRegistrationId(packet.getRegistrationId());
            syncDto.setRegistrationType(packet.getRegistrationType());
            syncDto.setPacketId(packet.getPacketId());
            syncDto.setSupervisorStatus(RegistrationConstants.CLIENT_STATUS_APPROVED);
            syncDto.setSupervisorComment("");
            syncDtoList.add(syncDto);
            syncDto.setLangCode(packet.getLangCode());
            syncDto.setName(packet.getName());
            syncDto.setPhone(packet.getPhone());
            syncDto.setEmail(packet.getEmail());

            try (FileInputStream fis = new FileInputStream(FileUtils.getFile(getEnvironmentProperty(RegistrationConstants.PACKET_LOCATION) +
                    RegistrationConstants.SLASH + packet.getPacketId() + RegistrationConstants.ZIP_FILE_EXTENSION))) {
                byte[] byteArray = new byte[(int) fis.available()];
                fis.read(byteArray);
                syncDto.setPacketHashValue(HMACUtils2.digestAsPlainText(byteArray));
                syncDto.setPacketSize(BigInteger.valueOf(byteArray.length));
            } catch (IOException | NoSuchAlgorithmException ioException) {
                throw ioException;
            }
        }
        return syncDtoList;
    }

    private void syncRID(List<SyncRegistrationDTO> syncDtoList, boolean packetIdExists) throws Exception {
        if (!syncDtoList.isEmpty()) {
            RegistrationPacketSyncDTO registrationPacketSyncDTO = new RegistrationPacketSyncDTO();
            registrationPacketSyncDTO
                    .setRequesttime(DateUtils.formatToISOString(DateUtils.getUTCCurrentDateTime()));
            registrationPacketSyncDTO.setSyncRegistrationDTOs(syncDtoList);
            registrationPacketSyncDTO.setId(RegistrationConstants.PACKET_SYNC_STATUS_ID);
            registrationPacketSyncDTO.setVersion(RegistrationConstants.PACKET_SYNC_VERSION);

            String refId = String.valueOf(getEnvironmentProperty(RegistrationConstants.CENTER_ID))
                    .concat(RegistrationConstants.UNDER_SCORE)
                    .concat(String.valueOf(getEnvironmentProperty(RegistrationConstants.STATION_ID)));

            syncPacketsToServer(CryptoUtil.encodeToURLSafeBase64(offlinePacketCryptoServiceImpl
                    .encrypt(refId, javaObjectToJsonString(registrationPacketSyncDTO).getBytes())), "User", packetIdExists);

        }
    }

    private void syncPacketsToServer(@NonNull String encodedString, @NonNull String triggerPoint, boolean packetIdExists)
            throws Exception {
        try {
            LinkedHashMap<String, Object> response = (LinkedHashMap<String, Object>) serviceDelegateUtil
                    .post(packetIdExists ? RegistrationConstants.PACKET_SYNC_V2 : RegistrationConstants.PACKET_SYNC, javaObjectToJsonString(encodedString), triggerPoint);
            if (response.get("errors") != null) {
                throw new Exception(response.get("errors").toString());
            }
        } catch (ConnectionException e) {
            throw e;
        } catch (JsonProcessingException | RuntimeException e) {
            throw new RegBaseCheckedException(RegistrationExceptionConstants.REG_PACKET_SYNC_EXCEPTION.getErrorCode(),
                    RegistrationExceptionConstants.REG_PACKET_SYNC_EXCEPTION.getErrorMessage());
        }
    }

    public void uploadSyncedPacket(@NonNull List<PacketUploadDTO> packets) throws Exception {
        for (PacketUploadDTO packet : packets) {
            try {
               uploadPacket(packet);
            } catch (RegBaseCheckedException e) {
               throw e;
            }
        }
    }

    public void uploadPacket(@NonNull PacketUploadDTO packetUpload) throws RegBaseCheckedException {
        File packet = FileUtils.getFile(getEnvironmentProperty(RegistrationConstants.PACKET_LOCATION) +
                RegistrationConstants.SLASH + packetUpload.getPacketId() + RegistrationConstants.ZIP_FILE_EXTENSION);
        try {
            pushPacketWithRetryWrapper(packet);
        } catch (ConnectionException e) {
            e.printStackTrace();
        }
    }

    private String pushPacketWithRetryWrapper(File packet) throws ConnectionException {
        RetryCallback<String, ConnectionException> retryCallback = new RetryCallback<String, ConnectionException>() {
            @SneakyThrows
            @Override
            public String doWithRetry(RetryContext retryContext) {
                return pushPacket(packet);
            }
        };
        return retryTemplate.execute(retryCallback);
    }

    private String pushPacket(File packet) throws ConnectionException, RegBaseCheckedException {
        if (!packet.exists())
            throw new RegBaseCheckedException(RegistrationExceptionConstants.REG_FILE_NOT_FOUND_ERROR_CODE.getErrorCode(),
                    RegistrationExceptionConstants.REG_FILE_NOT_FOUND_ERROR_CODE.getErrorMessage());

        LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
        map.add(RegistrationConstants.PACKET_TYPE, new FileSystemResource(packet));
        LinkedHashMap<String, Object> response = (LinkedHashMap<String, Object>) serviceDelegateUtil
                .post(RegistrationConstants.PACKET_UPLOAD, map, RegistrationConstants.JOB_TRIGGER_POINT_USER);

        if (response.get(RegistrationConstants.ERRORS) != null) {
            LinkedHashMap<String, String> error = ((List<LinkedHashMap<String, String>>) response.get(RegistrationConstants.ERRORS)).get(0);
            throw new RegBaseCheckedException(error.get("errorCode"), error.get("message"));
        }

        if (response.get(RegistrationConstants.RESPONSE) != null) {
            return (String) ((LinkedHashMap<String, Object>) response.get(RegistrationConstants.RESPONSE)).get(RegistrationConstants.UPLOAD_STATUS);
        }

        throw new RegBaseCheckedException(RegistrationExceptionConstants.REG_PACKET_UPLOAD_ERROR.getErrorCode(),
                RegistrationExceptionConstants.REG_PACKET_UPLOAD_ERROR.getErrorMessage());
    }

    private String getEnvironmentProperty(String property) {
        return environment.getProperty(property);
    }
}
