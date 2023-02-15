package io.mosip.packet.extractor.controller;

import io.mosip.commons.packet.dto.packet.PacketDto;
import io.mosip.packet.extractor.dto.RequestWrapper;
import io.mosip.packet.extractor.dto.ResponseWrapper;
import io.mosip.packet.extractor.dto.dbimport.DBImportRequest;
import io.mosip.packet.extractor.dto.dbimport.DBImportResponse;
import io.mosip.packet.extractor.exception.ServiceError;
import io.mosip.packet.extractor.service.DataExtractionService;
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
import java.util.ArrayList;

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
            dataExtractionService.extractBioDataFromDB(importRequest, true);
            response.setMessage("Successfully Completed");
        } catch (SQLException e) {
            e.printStackTrace();
            response.setMessage("Error : " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            response.setMessage("Error : " + e.getMessage());
        } catch (Exception e) {
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
            response.setConvertedBioValues(dataExtractionService.extractBioDataFromDB(importRequest, false));
            response.setMessage("Successfully Completed");
        } catch (SQLException e) {
            e.printStackTrace();
            response.setMessage("Error : " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            response.setMessage("Error : " + e.getMessage());
        } catch (Exception e) {
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
        responseWrapper.setErrors(new ArrayList<>());

        try {
            DBImportRequest importRequest = request.getRequest();
            response.setConvertedBioValues(dataExtractionService.extractBioDataFromDBAsBytes(importRequest, false));
            response.setMessage("Successfully Completed");
        } catch (SQLException e) {
            e.printStackTrace();
            response.setMessage("Error : " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            response.setMessage("Error : " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            response.setMessage("Error : " + e.getMessage());
        }
        responseWrapper.setResponse(response);
        return new ResponseEntity<ResponseWrapper>(responseWrapper, HttpStatus.OK);
    }

    @PostMapping(value = "/importPacketsFromOtherDomain", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseWrapper> createPacketFromOtherDomain(@RequestBody RequestWrapper<DBImportRequest> request) {
        ResponseWrapper<PacketDto> responseWrapper = new ResponseWrapper();
        PacketDto response = new PacketDto();
        try {
            DBImportRequest importRequest = request.getRequest();
            response = dataExtractionService.createPacketFromDataBase(importRequest);
        } catch (SQLException e) {
            e.printStackTrace();
            ServiceError error = new ServiceError();
            error.setErrorCode("IX-0001");
            error.setMessage("Error : " + e.getMessage());
            responseWrapper.getErrors().add(error);
        } catch (IOException e) {
            e.printStackTrace();
            ServiceError error = new ServiceError();
            error.setErrorCode("IX-0001");
            error.setMessage("Error : " + e.getMessage());
            responseWrapper.getErrors().add(error);
        } catch (Exception e) {
            e.printStackTrace();
            ServiceError error = new ServiceError();
            error.setErrorCode("IX-0001");
            error.setMessage("Error : " + e.getMessage());
            responseWrapper.getErrors().add(error);
        }
        responseWrapper.setResponse(response);
        return new ResponseEntity<ResponseWrapper>(responseWrapper, HttpStatus.OK);
    }
}
