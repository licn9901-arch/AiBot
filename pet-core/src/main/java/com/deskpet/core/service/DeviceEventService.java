package com.deskpet.core.service;

import com.deskpet.core.dto.DeviceEventRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * 设备事件服务
 * 事件数据写入 TimescaleDB，不再写入 PostgreSQL
 */
@Slf4j
@Service
public class DeviceEventService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private TimeSeriesService timeSeriesService;

    @Autowired(required = false)
    public void setTimeSeriesService(TimeSeriesService timeSeriesService) {
        this.timeSeriesService = timeSeriesService;
    }

    /**
     * 记录设备事件（写入 TimescaleDB）
     */
    public void recordEvent(String deviceId, DeviceEventRequest request) {
        Instant eventTime = request.timestamp() != null ?
            Instant.ofEpochMilli(request.timestamp()) : Instant.now();

        if (timeSeriesService == null) {
            log.warn("TimeSeriesService not available, device event dropped: deviceId={}, eventId={}",
                deviceId, request.eventId());
            return;
        }

        try {
            String paramsJson = request.params() != null ?
                objectMapper.writeValueAsString(request.params()) : null;
            timeSeriesService.writeDeviceEvent(deviceId, request.eventId(),
                request.eventType(), paramsJson, eventTime);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event params: deviceId={}", deviceId, e);
        }

        log.info("Device event recorded: deviceId={}, eventId={}, eventType={}",
            deviceId, request.eventId(), request.eventType());
    }

    /**
     * 异步记录设备事件
     */
    @Async
    public void recordEventAsync(String deviceId, DeviceEventRequest request) {
        recordEvent(deviceId, request);
    }
}
