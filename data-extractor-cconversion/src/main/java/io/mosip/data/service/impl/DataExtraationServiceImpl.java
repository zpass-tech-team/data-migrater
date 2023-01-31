package io.mosip.data.service.impl;

import io.mosip.data.constant.DBTypes;
import io.mosip.data.dto.dbimport.DBImportRequest;
import io.mosip.data.dto.dbimport.FieldFormatRequest;
import io.mosip.data.service.DataExtractionService;
import io.mosip.data.util.BioConversion;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.core.cbeffutil.entity.BIRInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.*;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DataExtraationServiceImpl  implements DataExtractionService {

    @Autowired
    private BioConversion bioConversion;

    @Autowired
    private BIRInfo.BIRInfoBuilder birBuilder;

    @Override
    public LinkedHashMap<String, String> extractBioDataFromDB(DBImportRequest dbImportRequest) throws SQLException, IOException {
        Connection conn=null;
        LinkedHashMap<String, String> biodata = new LinkedHashMap<>();

        try {
            if (dbImportRequest.getDbType().equals(DBTypes.MSSQL)) {
                DriverManager.registerDriver(new com.microsoft.sqlserver.jdbc.SQLServerDriver());
                conn = DriverManager.getConnection("jdbc:sqlserver://" + dbImportRequest.getUrl() + ";sslProtocol=TLSv1.2;databaseName=" + dbImportRequest.getDatabaseName()+ ";Trusted_Connection=True;", dbImportRequest.getUserId(), dbImportRequest.getPassword());
            }

            if(conn!=null) {
                ResultSet resultSet = null;
                String columnNames = null;
                HashSet<String> uniqueCloumns = new HashSet<>();
                for (FieldFormatRequest fieldFormatRequest : dbImportRequest.getColumnDetails()) {
                    uniqueCloumns.add(fieldFormatRequest.getFieldName());
                    uniqueCloumns.add(fieldFormatRequest.getDisplayName());
                }

                for (String column : uniqueCloumns) {
                    if (columnNames == null)
                        columnNames = column;
                    else
                        columnNames += "," + column;
                }

                System.out.println("Database Successfully connected");
                Statement statement = conn.createStatement();

                String selectSql = "SELECT " + columnNames + "  from " + dbImportRequest.getTableName();
                resultSet = statement.executeQuery(selectSql);

                while (resultSet.next()) {
                    for (FieldFormatRequest fieldFormatRequest : dbImportRequest.getColumnDetails()) {
                        byte[] byteVal = resultSet.getBinaryStream(fieldFormatRequest.getFieldName()).readAllBytes();
                        String convertedImageData = convertBiometric(resultSet.getString(fieldFormatRequest.getDisplayName()), fieldFormatRequest, byteVal);
                        biodata.put(resultSet.getString(fieldFormatRequest.getDisplayName())+ "-" + fieldFormatRequest.getFieldName(),  convertedImageData);
                    }
                }
            }
        } finally {
            if (conn != null)
                conn.close();
        }

        return biodata;
    }

    @Override
    public LinkedHashMap<String, String> extractBioDataFromDBAsBytes(DBImportRequest dbImportRequest) throws SQLException, IOException {
        LinkedHashMap<String, String> bioData = extractBioDataFromDB(dbImportRequest);
        LinkedHashMap<String, String> convertedData = new LinkedHashMap<>();

        for(Map.Entry<String, String> entry : bioData.entrySet()) {
            byte[] data = Base64.getDecoder().decode(entry.getValue());
            StringBuffer byteStringData = new StringBuffer();

            for (byte b : data)
                if (byteStringData.length() > 0)
                    byteStringData.append("," + b);
                else
                    byteStringData.append(b);

            convertedData.put(entry.getKey(), byteStringData.toString());
        }
        return convertedData;
    }

    private String convertBiometric(String fileNamePrefix, FieldFormatRequest fieldFormatRequest, byte[] bioValue) throws IOException {
        bioConversion.writeFile(fileNamePrefix + "-" + fieldFormatRequest.getFieldName() , bioValue, fieldFormatRequest.getFromFormat());
        return bioConversion.writeFile(fileNamePrefix + "-" + fieldFormatRequest.getFieldName(), bioConversion.convertImage(fieldFormatRequest.getFromFormat(), fieldFormatRequest.getToFormat(), bioValue), fieldFormatRequest.getToFormat());
    }
}
