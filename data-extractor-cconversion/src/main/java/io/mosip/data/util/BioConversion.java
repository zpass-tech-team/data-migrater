package io.mosip.data.util;

import io.mosip.biometrics.util.ConvertRequestDto;
import io.mosip.biometrics.util.face.FaceEncoder;
import io.mosip.biometrics.util.finger.FingerEncoder;
import io.mosip.biometrics.util.iris.IrisEncoder;
import io.mosip.commons.packet.constants.Biometric;
import io.mosip.commons.packet.dto.packet.BiometricsType;
import io.mosip.data.constant.ImageFormat;
import io.mosip.data.dto.dbimport.FieldFormatRequest;
import io.mosip.kernel.biometrics.constant.BiometricType;
import org.jnbis.api.Jnbis;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

@Component
public class BioConversion {

    public byte[] convertImage(FieldFormatRequest fieldFormatRequest, byte[] imageData) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] byteData = new byte[0];

        if(fieldFormatRequest.getFromFormat().equals(ImageFormat.JPEG) && !fieldFormatRequest.getToFormat().equals(ImageFormat.JPEG)) {
            ImageIO.write(ImageIO.read(new ByteArrayInputStream(imageData)), fieldFormatRequest.getToFormat().getFormat(), baos);
            byteData = baos.toByteArray();
        } else if(fieldFormatRequest.getToFormat().equals(ImageFormat.JP2)) {
            ImageIO.write(ImageIO.read(new ByteArrayInputStream(Jnbis.wsq().decode(imageData).toJpg().asByteArray())), fieldFormatRequest.getToFormat().getFormat(), baos);
            byteData = baos.toByteArray();
        } else if(fieldFormatRequest.getToFormat().equals(ImageFormat.ISO) && (fieldFormatRequest.getFromFormat().equals(ImageFormat.JP2) || fieldFormatRequest.getFromFormat().equals(ImageFormat.WSQ))) {
            Integer inputImageType = null;

            switch(fieldFormatRequest.getFromFormat().toString()) {
                case "JP2":
                    inputImageType = 0;
                    break;
                case "WSQ":
                    inputImageType = 1;
                    break;
            }

            String bioAttribute = fieldFormatRequest.getFieldToMap().split("_")[1];
            BiometricType biometricType = Biometric.getSingleTypeByAttribute(bioAttribute);

            if (biometricType.equals(BiometricType.FINGER)) {
                ConvertRequestDto requestDto = new ConvertRequestDto();
                requestDto.setModality("Finger");
                requestDto.setPurpose("REGISTRATION");
                requestDto.setVersion("ISO19794_4_2011");
                requestDto.setImageType(inputImageType);
                requestDto.setBiometricSubType("UNKNOWN");
                requestDto.setInputBytes(imageData);
                byteData = FingerEncoder.convertFingerImageToISO(requestDto);
            } else if (biometricType.equals(BiometricType.IRIS)) {
                ConvertRequestDto requestDto = new ConvertRequestDto();
                requestDto.setModality("Iris");
                requestDto.setPurpose("REGISTRATION");
                requestDto.setVersion("ISO19794_6_2011");
                requestDto.setImageType(inputImageType);
                requestDto.setBiometricSubType(bioAttribute);
                requestDto.setInputBytes(imageData);
                byteData = IrisEncoder.convertIrisImageToISO(requestDto);
            } else if (biometricType.equals(BiometricType.FACE)) {
                ConvertRequestDto requestDto = new ConvertRequestDto();
                requestDto.setModality("Face");
                requestDto.setPurpose("REGISTRATION");
                requestDto.setVersion("ISO19794_5_2011");
                requestDto.setImageType(inputImageType);
                requestDto.setInputBytes(imageData);
                byteData = FaceEncoder.convertFaceImageToISO (requestDto);
            }
        } else {
            byteData = imageData;
        }

        return byteData;
    }

    public byte[] writeFile(String fileName, byte[] imageData, ImageFormat toFormat) throws IOException {
        File bioFile = new File("C:/Users/Thamarai.Kannan/Downloads/" + fileName + "."+ toFormat.getFileFormat());
        OutputStream os = new FileOutputStream(bioFile);
        os.write(imageData);
        os.close();
        return imageData;
    }
}
