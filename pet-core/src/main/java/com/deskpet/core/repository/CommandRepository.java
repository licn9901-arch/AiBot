package com.deskpet.core.repository;

import com.deskpet.core.model.Command;
import com.deskpet.core.model.CommandStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface CommandRepository extends JpaRepository<Command, String> {
    List<Command> findByDeviceId(String deviceId);

    List<Command> findByStatusAndUpdatedAtBefore(CommandStatus status, Instant cutoff);
}
