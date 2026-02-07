package com.deskpet.core.service;

import com.deskpet.core.model.TelemetryLatest;
import com.deskpet.core.repository.TelemetryLatestRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
public class TelemetryService {
    private final TelemetryLatestRepository telemetryLatestRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private TimeSeriesService timeSeriesService;

    public TelemetryService(TelemetryLatestRepository telemetryLatestRepository) {
        this.telemetryLatestRepository = telemetryLatestRepository;
    }

    @Autowired(required = false)
    public void setTimeSeriesService(TimeSeriesService timeSeriesService) {
        this.timeSeriesService = timeSeriesService;
    }

    public TelemetryLatest updateLatest(String deviceId, Map<String, Object> telemetry) {
        Instant now = Instant.now();
        TelemetryLatest latest = telemetryLatestRepository.save(new TelemetryLatest(deviceId, telemetry, now));

        // 写入 TimescaleDB 时序库
        if (timeSeriesService != null) {
            try {
                String json = objectMapper.writeValueAsString(telemetry);
                timeSeriesService.writeTelemetry(deviceId, json, now);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize telemetry for TimescaleDB: deviceId={}", deviceId, e);
            }
        }

        return latest;
    }
}
