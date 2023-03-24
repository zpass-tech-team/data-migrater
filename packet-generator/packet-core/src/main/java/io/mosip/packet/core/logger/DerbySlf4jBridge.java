package io.mosip.packet.core.logger;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.logger.logback.factory.Logfactory;

import java.io.*;

public final class DerbySlf4jBridge
{
    private static final Logger logger = DataProcessLogger.getLogger(DerbySlf4jBridge.class);

    private DerbySlf4jBridge()
    {
    }

    public static final LoggingijResult out = null;
    /**
     * A basic adapter that funnels Derby's logs through an SLF4J logger.
     */

    public static final class LoggingijResult extends OutputStream {
        @Override
        public void write(int b) throws IOException {
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (len > 1)
            {
                logger.info(new String(b, off, len));
            }
        }
    }

    public static final class LoggingWriter extends Writer
    {
        @Override
        public void write(final char[] cbuf, final int off, final int len)
        {
            // Don't bother with empty lines.
            if (len > 1)
            {
                logger.error(new String(cbuf, off, len));
            }
        }

        @Override
        public void flush()
        {
            // noop.
        }

        @Override
        public void close()
        {
            // noop.
        }
    }

    public static Writer bridge()
    {
        return new LoggingWriter();
    }

    public static OutputStream ijBridge()
    {
        return new LoggingijResult();
    }
}
