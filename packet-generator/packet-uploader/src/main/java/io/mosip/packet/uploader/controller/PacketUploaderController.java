package io.mosip.packet.uploader.controller;

import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.http.ResponseFilter;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.packet.core.dto.upload.PacketUploadDTO;
import io.mosip.packet.core.dto.upload.PacketUploadResponseDTO;
import io.mosip.packet.core.util.DateUtils;
import io.mosip.packet.uploader.service.PacketUploaderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/packetuploader")
@Tag(name = "packet-upload-controller", description = "Packet Upload Controller")
public class PacketUploaderController {

    @Autowired
    PacketUploaderService packetUploaderService;

    @ResponseFilter
    @PostMapping(path = "/upload/{Center_ID}/{Machine_ID}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "createPacket", description = "createPacket", tags = { "packet-creator-controller" })
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "201", description = "Created", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Not Found", content = @Content(schema = @Schema(hidden = true))) })
    public ResponseWrapper<List<PacketUploadResponseDTO>> uploadPackets(@PathVariable("Center_ID") String centerId, @PathVariable("Machine_ID") String machineId, @RequestBody(required = true) RequestWrapper<List<PacketUploadDTO>> request) {
        HashMap<String, PacketUploadResponseDTO> response = new HashMap<>();
        try {
            packetUploaderService.syncPacket(request.getRequest(), centerId, machineId, response);
            packetUploaderService.uploadSyncedPacket(request.getRequest(), response);
        } catch (Exception e) {
            e.printStackTrace();
        }
        ResponseWrapper<List<PacketUploadResponseDTO>> apiResponse = getResponseWrapper(response);
        return apiResponse;
    }

    public ResponseWrapper<List<PacketUploadResponseDTO>> getResponseWrapper(HashMap<String, PacketUploadResponseDTO> response) {
        List<PacketUploadResponseDTO> responseList= List.copyOf(response.values());
        ResponseWrapper<List<PacketUploadResponseDTO>> wrapper = new ResponseWrapper();
        wrapper.setResponse(responseList);
        wrapper.setId("mosip-packet-uploader");
        wrapper.setVersion("v1");
        wrapper.setResponsetime(DateUtils.getUTCCurrentDateTime());
        return wrapper;
    }
}
