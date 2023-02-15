package io.mosip.packet.manager.controller;

import io.mosip.commons.packet.dto.PacketInfo;
import io.mosip.commons.packet.dto.packet.PacketDto;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.http.ResponseFilter;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.packet.manager.dto.PacketUploadDTO;
import io.mosip.packet.manager.exception.RegBaseCheckedException;
import io.mosip.packet.manager.service.PacketCreatorService;
import io.mosip.packet.manager.service.PacketUploaderService;
import io.swagger.annotations.Api;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/packetcreator")
@Tag(name = "packet-creator-controller", description = "Packet Controller")
public class PacketCreatorController {

    @Autowired
    private PacketCreatorService packetCreatorService;

    @Autowired
    private PacketUploaderService packetUploaderService;

    @ResponseFilter
    @PutMapping(path = "/create", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "createPacket", description = "createPacket", tags = { "packet-creator-controller" })
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "201", description = "Created", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Not Found", content = @Content(schema = @Schema(hidden = true))) })
    public ResponseWrapper<List<PacketInfo>> createPacket(@RequestBody(required = true) RequestWrapper<PacketDto> request) {

        List<PacketInfo> resultField = packetCreatorService.persistPacket(request.getRequest());
        ResponseWrapper<List<PacketInfo>> response = getResponseWrapper();
        response.setResponse(resultField);
        return response;
    }

    @ResponseFilter
    @PostMapping(path = "/upload", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "createPacket", description = "createPacket", tags = { "packet-creator-controller" })
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "201", description = "Created", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Not Found", content = @Content(schema = @Schema(hidden = true))) })
    public ResponseWrapper<List<PacketInfo>> uploadPackets(@RequestBody(required = true) RequestWrapper<List<PacketUploadDTO>> request) {

        try {
            packetUploaderService.syncPacket(request.getRequest());
            packetUploaderService.uploadSyncedPacket(request.getRequest());
        } catch (Exception e) {
            e.printStackTrace();
        }
        ResponseWrapper<List<PacketInfo>> response = getResponseWrapper();
        return response;
    }


    private ResponseWrapper getResponseWrapper() {
        ResponseWrapper<Object> response = new ResponseWrapper<>();
        response.setId("mosip.registration.packet.writer");
        response.setVersion("v1");
        response.setResponsetime(DateUtils.getUTCCurrentDateTime());
        return response;
    }
}
