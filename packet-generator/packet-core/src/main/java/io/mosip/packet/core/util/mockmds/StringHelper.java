package io.mosip.packet.core.util.mockmds;

import java.nio.charset.StandardCharsets;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.logger.logback.factory.Logfactory;
import io.mosip.packet.core.logger.DataProcessLogger;

public class StringHelper {
	private static final Logger LOGGER = DataProcessLogger.getLogger(StringHelper.class);

	public static String base64UrlEncode (byte [] arg)
    {
        return CryptoUtil.encodeToURLSafeBase64(arg);
    }

	public static String base64UrlEncode (String arg)
    {
        return CryptoUtil.encodeToURLSafeBase64(arg.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] base64UrlDecode (String arg)
    {
    	return CryptoUtil.decodeURLSafeBase64(arg);
	}

    public static byte [] toUtf8ByteArray (String arg)
    {
        return arg.getBytes (StandardCharsets.UTF_8);
    }
}
