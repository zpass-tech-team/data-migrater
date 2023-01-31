package io.mosip.data.util;

import io.mosip.data.constant.ImageFormat;
import org.jnbis.api.Jnbis;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

@Component
public class BioConversion {

    public byte[] convertImage(ImageFormat imageFormat, ImageFormat toFormat, byte[] imageData) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        if(imageFormat.equals(ImageFormat.JPEG) && !toFormat.equals(ImageFormat.JPEG))
            ImageIO.write(ImageIO.read(new ByteArrayInputStream(imageData)), toFormat.getFormat(), baos);
        else if(imageFormat.equals(ImageFormat.WSQ))
            ImageIO.write(ImageIO.read(new ByteArrayInputStream(Jnbis.wsq().decode(imageData).toJpg().asByteArray())), toFormat.getFormat(), baos);
        else
            baos.write(imageData);

        return baos.toByteArray();
    }

    public String writeFile(String fileName, byte[] imageData, ImageFormat toFormat) throws IOException {
        File bioFile = new File("C:/Users/Thamarai.Kannan/Downloads/" + fileName + "."+ toFormat.getFileFormat());
        OutputStream os = new FileOutputStream(bioFile);
        os.write(imageData);
        os.close();
        return Base64.getEncoder().encodeToString(imageData);
    }
}
