package io.mosip.packet.data.convertion;

import io.mosip.commons.packet.exception.FileNotFoundInDestinationException;
import io.mosip.commons.packet.exception.GetBiometricException;
import io.mosip.packet.core.spi.BioDocApiFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(value = "mosip.packet.bio.doc.data.converter.classname", havingValue = "BioDocDataFetchFromFile")
public class BioDocDataFetchFromLocal implements BioDocApiFactory {

    private static final Logger logger = LoggerFactory.getLogger(BioDocDataFetchFromLocal.class);
    private static final String INDIVIDUAL_BIOMETRICS_FACE = "individualBiometrics_face";

    @Override
    public Map<String, byte[]> getBioData(byte[] byteval, String fieldName) throws Exception {

        Map<String, byte[]> map = new HashMap<>();
        map.put(fieldName, getBioFileByteArray(byteval, fieldName));
        return map;
    }

    @Override
    public Map<String, byte[]> getDocData(byte[] byteval, String fieldName) throws Exception {

        Map<String, byte[]> map = new HashMap<>();
        List<BufferedImage> bufferedImages = convertDocFiles(byteval, fieldName);
        byte[] docPdfBytes = DocPdfConversionUtil.asPDF(bufferedImages, null);
        map.put(fieldName, docPdfBytes);
        return map;
    }

    private byte[] getBioFileByteArray(byte[] byteval, String fieldName) throws Exception {

        if (byteval != null && byteval.length > 0) {
            String filepath = new String(byteval, StandardCharsets.UTF_8);
            filepath = filepath.replaceAll("'","");
            if (filepath.contains(".")) {
                File inputFile = new File(filepath);
                if (inputFile != null && inputFile.isFile()) {
                    return new FileInputStream(inputFile).readAllBytes();
                } else {
                    logger.error("Biometric file not found or not a file: {}", filepath);
                    throw new FileNotFoundInDestinationException("Biometric file not found, path." + filepath);
                }
            } else if (INDIVIDUAL_BIOMETRICS_FACE.equalsIgnoreCase(fieldName)) {
                logger.error("Modality face is mandatory: {}", fieldName);
                throw new GetBiometricException("Missing biometric for - " + fieldName);
            }
        }
        return null;
    }

    private static List<BufferedImage> convertDocFiles(byte[] byteval, String fieldName) throws Exception {

        List<BufferedImage> bufferedImageList = new ArrayList<>();
        String filePath = "";
        if (byteval != null && byteval.length > 0) {
            filePath = new String(byteval, StandardCharsets.UTF_8);
            String[] filePaths = filePath.replaceAll("'","").split(",");
            if (filePaths.length == 2) {
                BufferedImage image1 = getBufferedImage(filePaths[0]);
                if (image1 != null) bufferedImageList.add(image1);
                BufferedImage image2 = getBufferedImage(filePaths[1]);
                if (image2 != null) bufferedImageList.add(image2);
            } else if(filePaths.length == 1) {
                BufferedImage image1 = getBufferedImage(filePaths[0]);
                if (image1 != null) bufferedImageList.add(image1);
            }
        }
        if (bufferedImageList.isEmpty()) {
            logger.error("Document is mandatory, file path: {}", filePath);
            throw new Exception("Document is missing in the given path :" + filePath);
        }
        return bufferedImageList;
    }

    private static BufferedImage getBufferedImage(String filepath) throws IOException {
        if (filepath.contains(".")) {
            File inputFile = new File(filepath);
            if (inputFile != null && inputFile.isFile()) {
                return ImageIO.read(inputFile);
            }
        }
        logger.info("Document file not found or not a file: {}", filepath);
        return null;
    }
}