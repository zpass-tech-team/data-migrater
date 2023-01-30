package io.mosip.data.controller;

import io.mosip.data.constant.DBTypes;
import io.mosip.data.dto.RequestWrapper;
import io.mosip.data.dto.ResponseWrapper;
import io.mosip.data.dto.dbimport.DBImportRequest;
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
import java.util.HashSet;

@RestController
@RequestMapping("/dataExtractor")
public class DataExtractionController {

    @Autowired
    DataExtractionService dataExtractionService;

    @PostMapping(value = "/importDBBioDataToImg", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseWrapper> importBioDataFromDB(@RequestBody RequestWrapper<DBImportRequest> request) {
        ResponseWrapper responseWrapper = new ResponseWrapper();
        try {
            DBImportRequest importRequest = request.getRequest();
            dataExtractionService.extractBioDataFromDB(importRequest);
            responseWrapper.setResponse("Successfully Completed");
        } catch (SQLException e) {
            e.printStackTrace();
            responseWrapper.setResponse("Error : " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            responseWrapper.setResponse("Error : " + e.getMessage());
        }
        return new ResponseEntity<ResponseWrapper>(responseWrapper, HttpStatus.OK);
    }
}
