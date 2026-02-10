package com.deskpet.core.service;

import com.deskpet.core.dto.AckRequest;
import com.deskpet.core.dto.GatewaySendCommandResponse;
import com.deskpet.core.error.BusinessException;
import com.deskpet.core.model.Command;
import com.deskpet.core.model.CommandStatus;
import com.deskpet.core.model.Device;
import com.deskpet.core.repository.CommandRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommandServiceTest {
    private CommandRepository commandRepository;
    private GatewayClient gatewayClient;
    private DeviceService deviceService;
    private CommandService commandService;

    @BeforeEach
    void setUp() {
        commandRepository = Mockito.mock(CommandRepository.class);
        gatewayClient = Mockito.mock(GatewayClient.class);
        deviceService = Mockito.mock(DeviceService.class);
        commandService = new CommandService(commandRepository, gatewayClient, deviceService, new ObjectMapper());
        ReflectionTestUtils.setField(commandService, "timeoutSeconds", 1);
        when(commandRepository.save(any(Command.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createCommand_deviceMissing_throws() {
        when(deviceService.find(anyString())).thenReturn(Optional.empty());
        assertThrows(BusinessException.class,
                () -> commandService.createCommand("pet-1", "move", Map.of("direction", "left")));
    }

    @Test
    void createCommand_gatewayOk_marksSent() {
        when(deviceService.find("pet-1")).thenReturn(Optional.of(mockDevice("pet-1")));
        when(gatewayClient.sendCommand(any())).thenReturn(new GatewaySendCommandResponse(true, "OK"));

        Command result = commandService.createCommand("pet-1", "move", Map.of("direction", "left"));

        assertEquals(CommandStatus.SENT, result.status());
    }

    @Test
    void createCommand_gatewayOffline_marksFailed() {
        when(deviceService.find("pet-1")).thenReturn(Optional.of(mockDevice("pet-1")));
        when(gatewayClient.sendCommand(any())).thenReturn(new GatewaySendCommandResponse(false, "OFFLINE"));

        Command result = commandService.createCommand("pet-1", "move", Map.of("direction", "left"));

        assertEquals(CommandStatus.FAILED, result.status());
        assertEquals("OFFLINE", result.ackCode());
    }

    @Test
    void handleAck_updatesStatus() {
        Command command = new Command("req-1", "pet-1", "move", Map.of(), CommandStatus.SENT,
                null, null, Instant.now(), Instant.now());
        when(commandRepository.findById("req-1")).thenReturn(Optional.of(command));

        commandService.handleAck("req-1", new AckRequest(1, "req-1", true, "DONE", "ok", 1));

        ArgumentCaptor<Command> captor = ArgumentCaptor.forClass(Command.class);
        verify(commandRepository).save(captor.capture());
        assertEquals(CommandStatus.ACKED, captor.getValue().status());
    }

    @Test
    void timeoutScan_marksTimeout() {
        Command command = new Command("req-1", "pet-1", "move", Map.of(), CommandStatus.SENT,
                null, null, Instant.now().minusSeconds(10), Instant.now().minusSeconds(10));
        when(commandRepository.findByStatusAndUpdatedAtBefore(any(), any()))
                .thenReturn(List.of(command));

        commandService.timeoutScan();

        ArgumentCaptor<Command> captor = ArgumentCaptor.forClass(Command.class);
        verify(commandRepository).save(captor.capture());
        assertEquals(CommandStatus.TIMEOUT, captor.getValue().status());
    }

    private Device mockDevice(String deviceId) {
        return new Device(deviceId, "hash", "salt", "model", null, 1L,"remark", Instant.now());
    }
}
