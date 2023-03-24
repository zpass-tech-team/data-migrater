package io.mosip.packet.extractor.util;

import org.slf4j.Logger;
import io.mosip.packet.core.logger.DataProcessLogger;
import org.mvel2.MVEL;
import org.springframework.stereotype.Component;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Component
public class MvelUtil {
    private static final Map<String, String> SCRIPT_CACHE = new HashMap<>();
    Logger LOGGER = DataProcessLogger.getLogger(MvelUtil.class);

    public String processViaMVEL(String mvelFileName, Map identity) {
        Map context = new HashMap();
        MVEL.eval(getScript(mvelFileName), context);
        context.put("identity", identity);
        String functionName = mvelFileName.replace(".mvel", "");
        return MVEL.eval("return get" + functionName + "();", context, String.class);
    }

    private String getScript(String scriptName) {
        if(SCRIPT_CACHE.containsKey(scriptName) && SCRIPT_CACHE.get(scriptName) != null)
            return SCRIPT_CACHE.get(scriptName);

        try {
            Path path = Paths.get(System.getProperty("user.dir"), scriptName);
            FileInputStream is = new FileInputStream(path.toFile());
            SCRIPT_CACHE.put(scriptName, new String(is.readAllBytes()));

        } catch (Throwable t) {
            LOGGER.error("Failed to get mvel script", t);
        }
        return SCRIPT_CACHE.get(scriptName);
    }
}
