package com.deskpet.core.store;

import com.deskpet.core.model.Command;
import com.deskpet.core.model.CommandStatus;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class CommandStore {
    private final ConcurrentHashMap<String, Command> commands = new ConcurrentHashMap<>();

    public Command save(Command command) {
        commands.put(command.reqId(), command);
        return command;
    }

    public Optional<Command> findByReqId(String reqId) {
        return Optional.ofNullable(commands.get(reqId));
    }

    public Collection<Command> findByDeviceId(String deviceId) {
        return commands.values().stream()
                .filter(command -> command.deviceId().equals(deviceId))
                .collect(Collectors.toList());
    }

    public Collection<Command> findSentBefore(Instant cutoff) {
        return commands.values().stream()
                .filter(command -> command.status() == CommandStatus.SENT)
                .filter(command -> command.updatedAt().isBefore(cutoff))
                .collect(Collectors.toList());
    }
}
