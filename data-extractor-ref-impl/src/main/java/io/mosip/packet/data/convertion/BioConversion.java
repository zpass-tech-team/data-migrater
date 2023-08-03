package io.mosip.packet.data.convertion;

import io.mosip.biometrics.util.ConvertRequestDto;
import io.mosip.biometrics.util.face.FaceEncoder;
import io.mosip.biometrics.util.finger.FingerEncoder;
import io.mosip.biometrics.util.iris.IrisEncoder;
import io.mosip.commons.packet.constants.Biometric;
import io.mosip.packet.core.constant.DataFormat;
import io.mosip.packet.core.dto.dbimport.FieldFormatRequest;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.packet.core.spi.BioConvertorApiFactory;
import org.jnbis.api.Jnbis;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.io.*;
import java.util.List;

@Component
public class BioConversion implements BioConvertorApiFactory {

    @Override
    public byte[] convertImage(FieldFormatRequest fieldFormatRequest, byte[] imageData) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] byteData = imageData;
        List<DataFormat> destFormats = fieldFormatRequest.getDestFormat();
        DataFormat currentFormat = fieldFormatRequest.getSrcFormat();

        for(DataFormat toFormat : destFormats) {
            if(currentFormat.equals(toFormat)) {
               currentFormat = toFormat;
            } else if(currentFormat.equals(DataFormat.JPEG) && !toFormat.equals(DataFormat.JPEG)) {
                ImageIO.write(ImageIO.read(new ByteArrayInputStream(byteData)), toFormat.getFormat(), baos);
                byteData = baos.toByteArray();
                currentFormat = toFormat;
            } else if(currentFormat.equals(DataFormat.WSQ)) {
                ImageIO.write(ImageIO.read(new ByteArrayInputStream(Jnbis.wsq().decode(byteData).toJpg().asByteArray())), toFormat.getFormat(), baos);
                byteData = baos.toByteArray();
                currentFormat = toFormat;
            } else if(toFormat.equals(DataFormat.ISO) && (currentFormat.equals(DataFormat.JP2) || currentFormat.equals(DataFormat.WSQ))) {
                Integer inputImageType = null;

                switch(currentFormat.toString()) {
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
                    requestDto.setInputBytes(byteData);
                    byteData = FingerEncoder.convertFingerImageToISO(requestDto);
                } else if (biometricType.equals(BiometricType.IRIS)) {
                    ConvertRequestDto requestDto = new ConvertRequestDto();
                    requestDto.setModality("Iris");
                    requestDto.setPurpose("REGISTRATION");
                    requestDto.setVersion("ISO19794_6_2011");
                    requestDto.setImageType(inputImageType);
                    requestDto.setBiometricSubType(bioAttribute);
                    requestDto.setInputBytes(byteData);
                    byteData = IrisEncoder.convertIrisImageToISO(requestDto);
                } else if (biometricType.equals(BiometricType.FACE)) {
                    ConvertRequestDto requestDto = new ConvertRequestDto();
                    requestDto.setModality("Face");
                    requestDto.setPurpose("REGISTRATION");
                    requestDto.setVersion("ISO19794_5_2011");
                    requestDto.setImageType(inputImageType);
                    requestDto.setInputBytes(byteData);
                    byteData = FaceEncoder.convertFaceImageToISO (requestDto);
                }
                currentFormat = toFormat;
            }
        }

        return byteData;
    }

    @Override
    public byte[] writeFile(String fileName, byte[] imageData, DataFormat toFormat) throws IOException {
        File imagePath = new File(System.getProperty("user.dir") + "/Images");
        if(!imagePath.exists())
            imagePath.mkdirs();

        File bioFile = new File(imagePath.getAbsolutePath() + "/" + fileName + "."+ toFormat.getFileFormat());
        OutputStream os = new FileOutputStream(bioFile);
        os.write(imageData);
        os.close();
        return imageData;
    }
}
