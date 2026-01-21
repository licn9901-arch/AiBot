package com.deskpet.core.controller;

import com.deskpet.core.dto.CommandCreateRequest;
import com.deskpet.core.dto.CommandResponse;
import com.deskpet.core.error.BusinessException;
import com.deskpet.core.error.ErrorCode;
import com.deskpet.core.model.Command;
import com.deskpet.core.service.CommandService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/devices/{deviceId}/commands")
public class CommandController {
    private final CommandService commandService;

    public CommandController(CommandService commandService) {
        this.commandService = commandService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CommandResponse createCommand(@PathVariable String deviceId,
                                         @Valid @RequestBody CommandCreateRequest request) {
        Command command = commandService.createCommand(deviceId, request.type(), request.payload());
        return CommandResponse.of(command);
    }

    @GetMapping("/{reqId}")
    public CommandResponse getCommand(@PathVariable String reqId) {
        Command command = commandService.findCommand(reqId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMAND_NOT_FOUND));
        return CommandResponse.of(command);
    }
}
