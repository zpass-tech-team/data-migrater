package io.mosip.packet.core.constant;

import io.mosip.packet.core.util.FixedListQueue;
import org.springframework.stereotype.Component;

@Component
public class GlobalConfig {

    public static Boolean IS_ONLY_FOR_QUALITY_CHECK = false;

    public static Boolean WRITE_BIOSDK_RESPONSE = false;

    public static String SESSION_KEY;

    public static Boolean IS_TRACKER_REQUIRED = true;

    public static Boolean IS_NETWORK_AVAILABLE = true;

    public static Boolean IS_RUNNING_AS_BATCH = false;

    public static FixedListQueue<Long> TIMECONSUPTIONQUEUE;

    public static Long TOTAL_RECORDS_FOR_PROCESS = 0l;

    public static String getActivityName() {
        return (IS_ONLY_FOR_QUALITY_CHECK ? "QUALITY ANALYSIS" : "PACKET CREATOR");
    }

}
