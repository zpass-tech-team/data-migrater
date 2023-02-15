package io.mosip.packet.manager.constants;

import java.util.Arrays;
import java.util.List;

public class RegistrationConstants {
    public static final String MOSIP_HOSTNAME = "mosip.hostname";

    // api related constant values
    public static final String HTTPMETHOD = "service.httpmethod";
    public static final String SERVICE_URL = "service.url";
    public static final String HEALTH_CHECK_URL = "mosip.reg.healthcheck.url";
    public static final String HEADERS = "service.headers";
    public static final String RESPONSE_TYPE = "service.responseType";
    public static final String REQUEST_TYPE = "service.requestType";
    public static final String AUTH_HEADER = "service.authheader";
    public static final String AUTH_REQUIRED = "service.authrequired";
    public static final String SIGN_REQUIRED = "service.signrequired";
    public static final String REQUEST_SIGN_REQUIRED = "service.requestsignrequired";
    public static final String AUTH_TYPE = "BASIC";
    public static final String AUTH_USERNAME = "auth_username";
    public static final String AUTH_PASSWORD = "auth_password";
    public static final String AUTH_TYPE_NEW = "NEW";
    public static final String CENTER_ID = "center.id";
    public static final String STATION_ID = "station.id";

    public static final String DOT = ".";
    public static final String SLASH = "/";
    public static final String UNDER_SCORE = "_";
    public static final String VERSION = "version";
    public static final String MANIFEST_VERSION = "";
    public static final String ERRORS = "errors";
    public static final String RESPONSE = "response";

    //rest
    public static final String REST_RESPONSE_BODY = "responseBody";
    public static final String REST_RESPONSE_HEADERS = "responseHeader";

    // Timeout Configuartion
    public static final String HTTP_API_READ_TIMEOUT = "mosip.registration.HTTP_API_READ_TIMEOUT";
    public static final String HTTP_API_WRITE_TIMEOUT = "mosip.registration.HTTP_API_WRITE_TIMEOUT";

    // Packet Creation
    public static final String ZIP_FILE_EXTENSION = ".zip";
    public static final String CLIENT_STATUS_APPROVED = "APPROVED";
    public static final String PACKET_SYNC_STATUS_ID = "mosip.registration.sync";
    public static final String PACKET_SYNC_VERSION = "1.0";

    // Packet Upload
    public static final String PACKET_TYPE = "file";
    public static final String UPLOAD_STATUS = "status";
    public static final List<String> PACKET_UPLOAD_STATUS = Arrays.asList("SYNCED", "EXPORTED", "RESEND", "E");
    public static final String PACKET_UPLOAD = "packet_upload";
    public static final String PACKET_DUPLICATE = "duplicate";
    public static final String JOB_TRIGGER_POINT_USER = "User";
    public static final String KEY_INDEX = "keyIndex";
    public static final String REST_OAUTH = "oauth";
    public static final String COOKIE = "Cookie";

    // Exception Code for Components
    public static final String PACKET_CREATION_EXP_CODE = "PCC-";
    public static final String PACKET_UPLOAD_EXP_CODE = "PAU-";
    public static final String REG_ACK_EXP_CODE = "ACK-";
    public static final String USER_REG_IRIS_CAPTURE_EXP_CODE = "IRC-";
    public static final String USER_REG_FINGERPRINT_CAPTURE_EXP_CODE = "FPC-";

    // Packet Sync
    public static final String PACKET_SYNC = "packet_sync";
    public static final String PACKET_SYNC_V2 = "packet_sync_v2";
    public static final String PACKET_LOCATION = "object.store.base.location";

}
