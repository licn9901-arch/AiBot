package com.deskpet.core.service;

import com.deskpet.core.dto.DeviceEventRequest;
import com.deskpet.core.dto.DeviceEventResponse;
import com.deskpet.core.model.DeviceEvent;
import com.deskpet.core.repository.DeviceEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 设备事件服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceEventService {

    private final DeviceEventRepository eventRepository;

    /**
     * 记录设备事件（网关调用）
     */
    @Transactional(rollbackFor = Exception.class)
    public DeviceEventResponse recordEvent(String deviceId, DeviceEventRequest request) {
        DeviceEvent event = DeviceEvent.builder()
            .deviceId(deviceId)
            .eventId(request.eventId())
            .eventType(request.eventType())
            .params(request.params())
            .createdAt(request.timestamp() != null ?
                Instant.ofEpochMilli(request.timestamp()) : Instant.now())
            .build();

        event = eventRepository.save(event);
        log.info("Device event recorded: deviceId={}, eventId={}, eventType={}",
            deviceId, request.eventId(), request.eventType());

        return DeviceEventResponse.from(event);
    }

    /**
     * 异步记录设备事件
     */
    @Async
    @Transactional(rollbackFor = Exception.class)
    public void recordEventAsync(String deviceId, DeviceEventRequest request) {
        recordEvent(deviceId, request);
    }

    /**
     * 查询设备事件历史
     */
    @Transactional(rollbackFor = Exception.class)
    public Page<DeviceEventResponse> findEvents(String deviceId, String eventId,
            String eventType, Instant startTime, Instant endTime, Pageable pageable) {
        return eventRepository.findByFilters(deviceId, eventId, eventType, startTime, endTime, pageable)
            .map(DeviceEventResponse::from);
    }

    /**
     * 查询设备最近事件
     */
    @Transactional(rollbackFor = Exception.class)
    public List<DeviceEventResponse> findRecentEvents(String deviceId, Duration duration) {
        Instant after = Instant.now().minus(duration);
        return eventRepository.findByDeviceIdAndCreatedAtAfterOrderByCreatedAtDesc(deviceId, after)
            .stream()
            .map(DeviceEventResponse::from)
            .toList();
    }

    /**
     * 查询设备事件（分页）
     */
    @Transactional(rollbackFor = Exception.class)
    public Page<DeviceEventResponse> findEventsByDevice(String deviceId, Pageable pageable) {
        return eventRepository.findByDeviceIdOrderByCreatedAtDesc(deviceId, pageable)
            .map(DeviceEventResponse::from);
    }

    /**
     * 统计设备事件
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> getEventStats(String deviceId, Duration duration) {
        Instant since = Instant.now().minus(duration);
        List<Object[]> counts = eventRepository.countByDeviceIdGroupByEventId(deviceId, since);

        Map<String, Long> eventCounts = new HashMap<>();
        long total = 0;
        for (Object[] row : counts) {
            String eventId = (String) row[0];
            Long count = (Long) row[1];
            eventCounts.put(eventId, count);
            total += count;
        }

        long infoCount = eventRepository.countByDeviceIdAndEventType(deviceId, "info");
        long alertCount = eventRepository.countByDeviceIdAndEventType(deviceId, "alert");
        long errorCount = eventRepository.countByDeviceIdAndEventType(deviceId, "error");

        return Map.of(
            "deviceId", deviceId,
            "duration", duration.toString(),
            "total", total,
            "byEventId", eventCounts,
            "byType", Map.of(
                "info", infoCount,
                "alert", alertCount,
                "error", errorCount
            )
        );
    }
}
