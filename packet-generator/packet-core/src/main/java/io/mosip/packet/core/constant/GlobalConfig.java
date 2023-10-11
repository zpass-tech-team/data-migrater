package io.mosip.packet.core.constant;

import org.springframework.stereotype.Component;

@Component
public class GlobalConfig {

    public static Boolean IS_ONLY_FOR_QUALITY_CHECK = false;

    public static Boolean WRITE_BIOSDK_RESPONSE = false;

    public static String SESSION_KEY;

    public static Boolean IS_TRACKER_REQUIRED = true;

    public static Boolean IS_NETWORK_AVAILABLE = true;

    public static Boolean IS_RUNNING_AS_BATCH = false;
}
