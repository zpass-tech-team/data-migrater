package io.mosip.data.controller;

import io.mosip.data.constant.DBTypes;
import io.mosip.data.dto.RequestWrapper;
import io.mosip.data.dto.ResponseWrapper;
import io.mosip.data.dto.dbimport.DBImportRequest;
import io.mosip.data.dto.dbimport.DBImportResponse;
import io.mosip.data.dto.dbimport.FieldFormatRequest;
import io.mosip.data.service.DataExtractionService;
import io.mosip.data.util.BioConversion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.sql.*;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/dataExtractor")
public class DataExtractionController {

    @Autowired
    DataExtractionService dataExtractionService;

    @PostMapping(value = "/importDBBioDataToImg", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseWrapper> importBioImgFromDB(@RequestBody RequestWrapper<DBImportRequest> request) {
        ResponseWrapper<DBImportResponse> responseWrapper = new ResponseWrapper();
        DBImportResponse response = new DBImportResponse();
        try {
            DBImportRequest importRequest = request.getRequest();
            dataExtractionService.extractBioDataFromDB(importRequest);
            response.setMessage("Successfully Completed");
        } catch (SQLException e) {
            e.printStackTrace();
            response.setMessage("Error : " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            response.setMessage("Error : " + e.getMessage());
        }
        responseWrapper.setResponse(response);
        return new ResponseEntity<ResponseWrapper>(responseWrapper, HttpStatus.OK);
    }

    @PostMapping(value = "/importDBBioDataToData", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseWrapper> importBioDataFromDB(@RequestBody RequestWrapper<DBImportRequest> request) {
        ResponseWrapper<DBImportResponse> responseWrapper = new ResponseWrapper();
        DBImportResponse response = new DBImportResponse();
        try {
            DBImportRequest importRequest = request.getRequest();
            response.setConvertedBioValues(dataExtractionService.extractBioDataFromDB(importRequest));
            response.setMessage("Successfully Completed");
        } catch (SQLException e) {
            e.printStackTrace();
            response.setMessage("Error : " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            response.setMessage("Error : " + e.getMessage());
        }
        responseWrapper.setResponse(response);
        return new ResponseEntity<ResponseWrapper>(responseWrapper, HttpStatus.OK);
    }

    @PostMapping(value = "/importDBBioDataToByteData", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseWrapper> importBioByteDataFromDB(@RequestBody RequestWrapper<DBImportRequest> request) {
        ResponseWrapper<DBImportResponse> responseWrapper = new ResponseWrapper();
        DBImportResponse response = new DBImportResponse();
        try {
            DBImportRequest importRequest = request.getRequest();
            response.setConvertedBioValues(dataExtractionService.extractBioDataFromDBAsBytes(importRequest));
            response.setMessage("Successfully Completed");
        } catch (SQLException e) {
            e.printStackTrace();
            response.setMessage("Error : " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            response.setMessage("Error : " + e.getMessage());
        }
        responseWrapper.setResponse(response);
        return new ResponseEntity<ResponseWrapper>(responseWrapper, HttpStatus.OK);
    }
}
