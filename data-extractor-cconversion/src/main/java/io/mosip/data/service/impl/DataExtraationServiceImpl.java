package io.mosip.data.service.impl;

import io.mosip.data.constant.DBTypes;
import io.mosip.data.dto.dbimport.DBImportRequest;
import io.mosip.data.dto.dbimport.FieldFormatRequest;
import io.mosip.data.service.DataExtractionService;
import io.mosip.data.util.BioConversion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.*;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;

@Service
public class DataExtraationServiceImpl  implements DataExtractionService {

    @Autowired
    private BioConversion bioConversion;

    @Override
    public String extractBioDataFromDB(DBImportRequest dbImportRequest) throws SQLException, IOException {
        Connection conn=null;

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
                    LinkedHashMap<String, String> biodata = new LinkedHashMap<>();
                    for (FieldFormatRequest fieldFormatRequest : dbImportRequest.getColumnDetails()) {
                        byte[] byteVal = resultSet.getBinaryStream(fieldFormatRequest.getFieldName()).readAllBytes();
                        convertBiometric(resultSet.getString(fieldFormatRequest.getDisplayName()), fieldFormatRequest, byteVal);
                    }
                }
            }
        } finally {
            if (conn != null)
                conn.close();
        }

        return "Successfully Completed";
    }

    @Override
    public void convertBiometric(String fileNamePrefix, FieldFormatRequest fieldFormatRequest, byte[] bioValue) throws IOException {
        bioConversion.writeFile(fileNamePrefix + "-" + fieldFormatRequest.getFieldName() , bioValue, fieldFormatRequest.getFromFormat());
        bioConversion.writeFile(fileNamePrefix + "-" + fieldFormatRequest.getFieldName(), bioConversion.convertImage(fieldFormatRequest.getFromFormat(), fieldFormatRequest.getToFormat(), bioValue), fieldFormatRequest.getToFormat());
    }
}
