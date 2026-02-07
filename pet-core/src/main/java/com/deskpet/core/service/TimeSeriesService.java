package com.deskpet.core.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;

/**
 * 时序数据写入服务
 * 负责将设备交互数据异步写入 TimescaleDB
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "timescaledb.enabled", havingValue = "true")
public class TimeSeriesService {

    private final JdbcTemplate tsJdbc;

    public TimeSeriesService(@Qualifier("timescaleJdbcTemplate") JdbcTemplate tsJdbc) {
        this.tsJdbc = tsJdbc;
    }

    /**
     * 写入遥测数据
     */
    @Async
    public void writeTelemetry(String deviceId, String telemetryJson, Instant time) {
        try {
            tsJdbc.update(
                "INSERT INTO deskpet_ts.ts_telemetry (device_id, telemetry, created_at) VALUES (?, ?::jsonb, ?)",
                deviceId, telemetryJson, Timestamp.from(time));
        } catch (Exception e) {
            log.error("Failed to write telemetry to TimescaleDB: deviceId={}", deviceId, e);
        }
    }

    /**
     * 写入设备事件
     */
    @Async
    public void writeDeviceEvent(String deviceId, String eventId, String eventType,
                                 String paramsJson, Instant time) {
        try {
            tsJdbc.update(
                "INSERT INTO deskpet_ts.ts_device_event (device_id, event_id, event_type, params, created_at) " +
                "VALUES (?, ?, ?, ?::jsonb, ?)",
                deviceId, eventId, eventType, paramsJson, Timestamp.from(time));
        } catch (Exception e) {
            log.error("Failed to write device event to TimescaleDB: deviceId={}, eventId={}", deviceId, eventId, e);
        }
    }

    /**
     * 写入设备上下线记录
     */
    @Async
    public void writeDeviceSession(String deviceId, boolean online, String gatewayInstanceId,
                                   String ip, Instant time) {
        try {
            tsJdbc.update(
                "INSERT INTO deskpet_ts.ts_device_session (device_id, online, gateway_instance_id, ip, created_at) " +
                "VALUES (?, ?, ?, ?, ?)",
                deviceId, online, gatewayInstanceId, ip, Timestamp.from(time));
        } catch (Exception e) {
            log.error("Failed to write device session to TimescaleDB: deviceId={}, online={}", deviceId, online, e);
        }
    }

    /**
     * 写入指令记录
     */
    @Async
    public void writeDeviceCommand(String reqId, String deviceId, String type,
                                   String payloadJson, String status, Instant time) {
        try {
            tsJdbc.update(
                "INSERT INTO deskpet_ts.ts_device_command (req_id, device_id, type, payload, status, created_at) " +
                "VALUES (?, ?, ?, ?::jsonb, ?, ?)",
                reqId, deviceId, type, payloadJson, status, Timestamp.from(time));
        } catch (Exception e) {
            log.error("Failed to write device command to TimescaleDB: reqId={}", reqId, e);
        }
    }

    /**
     * 写入操作日志
     */
    @Async
    public void writeOperationLog(Long userId, String deviceId, String action,
                                  String payloadJson, String ip, String userAgent, Instant time) {
        try {
            tsJdbc.update(
                "INSERT INTO deskpet_ts.ts_operation_log (user_id, device_id, action, payload, ip, user_agent, created_at) " +
                "VALUES (?, ?, ?, ?::jsonb, ?, ?, ?)",
                userId, deviceId, action, payloadJson, ip, userAgent, Timestamp.from(time));
        } catch (Exception e) {
            log.error("Failed to write operation log to TimescaleDB: action={}", action, e);
        }
    }

    /**
     * 查询设备上下线历史
     */
    public java.util.List<Map<String, Object>> queryDeviceSessionHistory(String deviceId, Instant since) {
        return tsJdbc.queryForList(
            "SELECT device_id, online, gateway_instance_id, ip, created_at " +
            "FROM deskpet_ts.ts_device_session " +
            "WHERE device_id = ? AND created_at >= ? " +
            "ORDER BY created_at DESC",
            deviceId, Timestamp.from(since));
    }

    /**
     * 查询设备上下线历史（分页）
     */
    public java.util.List<Map<String, Object>> queryDeviceSessionHistory(String deviceId, int limit, int offset) {
        return tsJdbc.queryForList(
            "SELECT device_id, online, gateway_instance_id, ip, created_at " +
            "FROM deskpet_ts.ts_device_session " +
            "WHERE device_id = ? " +
            "ORDER BY created_at DESC LIMIT ? OFFSET ?",
            deviceId, limit, offset);
    }

    /**
     * 统计设备上下线次数
     */
    public Map<String, Object> queryDeviceSessionStats(String deviceId, Instant since) {
        var onlineCount = tsJdbc.queryForObject(
            "SELECT COUNT(*) FROM deskpet_ts.ts_device_session WHERE device_id = ? AND online = true AND created_at >= ?",
            Long.class, deviceId, Timestamp.from(since));
        var offlineCount = tsJdbc.queryForObject(
            "SELECT COUNT(*) FROM deskpet_ts.ts_device_session WHERE device_id = ? AND online = false AND created_at >= ?",
            Long.class, deviceId, Timestamp.from(since));
        return Map.of(
            "deviceId", deviceId,
            "onlineCount", onlineCount != null ? onlineCount : 0L,
            "offlineCount", offlineCount != null ? offlineCount : 0L,
            "since", since.toString()
        );
    }

    /**
     * 查询遥测时序数据（用于图表展示）
     */
    public java.util.List<Map<String, Object>> queryTelemetryTimeSeries(String deviceId, Instant since) {
        return tsJdbc.queryForList(
            "SELECT device_id, telemetry, created_at " +
            "FROM deskpet_ts.ts_telemetry " +
            "WHERE device_id = ? AND created_at >= ? " +
            "ORDER BY created_at ASC",
            deviceId, Timestamp.from(since));
    }

    /**
     * 查询应用日志（分页）
     */
    public java.util.List<Map<String, Object>> queryAppLogs(String level, String logger,
                                                             String search, Instant since,
                                                             int limit, int offset) {
        StringBuilder sql = new StringBuilder(
            "SELECT log_time, level, logger, thread, message, exception " +
            "FROM deskpet_ts.ts_app_log WHERE log_time >= ?");
        java.util.List<Object> params = new java.util.ArrayList<>();
        params.add(Timestamp.from(since));

        if (level != null && !level.isEmpty()) {
            sql.append(" AND level = ?");
            params.add(level);
        }
        if (logger != null && !logger.isEmpty()) {
            sql.append(" AND logger = ?");
            params.add(logger);
        }
        if (search != null && !search.isEmpty()) {
            sql.append(" AND message ILIKE ?");
            params.add("%" + search + "%");
        }
        sql.append(" ORDER BY log_time DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        return tsJdbc.queryForList(sql.toString(), params.toArray());
    }

    /**
     * 统计应用日志总数
     */
    public long countAppLogs(String level, String logger, String search, Instant since) {
        StringBuilder sql = new StringBuilder(
            "SELECT COUNT(*) FROM deskpet_ts.ts_app_log WHERE log_time >= ?");
        java.util.List<Object> params = new java.util.ArrayList<>();
        params.add(Timestamp.from(since));

        if (level != null && !level.isEmpty()) {
            sql.append(" AND level = ?");
            params.add(level);
        }
        if (logger != null && !logger.isEmpty()) {
            sql.append(" AND logger = ?");
            params.add(logger);
        }
        if (search != null && !search.isEmpty()) {
            sql.append(" AND message ILIKE ?");
            params.add("%" + search + "%");
        }

        Long count = tsJdbc.queryForObject(sql.toString(), Long.class, params.toArray());
        return count != null ? count : 0L;
    }

    /**
     * 按级别统计应用日志
     */
    public java.util.List<Map<String, Object>> queryAppLogStats(Instant since) {
        return tsJdbc.queryForList(
            "SELECT level, COUNT(*) AS count FROM deskpet_ts.ts_app_log " +
            "WHERE log_time >= ? GROUP BY level ORDER BY count DESC",
            Timestamp.from(since));
    }

    /**
     * 查询操作日志（分页）
     */
    public java.util.List<Map<String, Object>> queryOperationLogs(Long userId, String deviceId,
                                                                    String action, Instant startTime,
                                                                    Instant endTime, int limit, int offset) {
        StringBuilder sql = new StringBuilder(
            "SELECT user_id, device_id, action, payload, ip, user_agent, created_at " +
            "FROM deskpet_ts.ts_operation_log WHERE 1=1");
        java.util.List<Object> params = new java.util.ArrayList<>();

        if (userId != null) {
            sql.append(" AND user_id = ?");
            params.add(userId);
        }
        if (deviceId != null && !deviceId.isEmpty()) {
            sql.append(" AND device_id = ?");
            params.add(deviceId);
        }
        if (action != null && !action.isEmpty()) {
            sql.append(" AND action = ?");
            params.add(action);
        }
        if (startTime != null) {
            sql.append(" AND created_at >= ?");
            params.add(Timestamp.from(startTime));
        }
        if (endTime != null) {
            sql.append(" AND created_at <= ?");
            params.add(Timestamp.from(endTime));
        }
        sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        return tsJdbc.queryForList(sql.toString(), params.toArray());
    }

    /**
     * 统计操作日志总数
     */
    public long countOperationLogs(Long userId, String deviceId, String action,
                                    Instant startTime, Instant endTime) {
        StringBuilder sql = new StringBuilder(
            "SELECT COUNT(*) FROM deskpet_ts.ts_operation_log WHERE 1=1");
        java.util.List<Object> params = new java.util.ArrayList<>();

        if (userId != null) {
            sql.append(" AND user_id = ?");
            params.add(userId);
        }
        if (deviceId != null && !deviceId.isEmpty()) {
            sql.append(" AND device_id = ?");
            params.add(deviceId);
        }
        if (action != null && !action.isEmpty()) {
            sql.append(" AND action = ?");
            params.add(action);
        }
        if (startTime != null) {
            sql.append(" AND created_at >= ?");
            params.add(Timestamp.from(startTime));
        }
        if (endTime != null) {
            sql.append(" AND created_at <= ?");
            params.add(Timestamp.from(endTime));
        }

        Long count = tsJdbc.queryForObject(sql.toString(), Long.class, params.toArray());
        return count != null ? count : 0L;
    }

    /**
     * 查询设备事件（分页）
     */
    public java.util.List<Map<String, Object>> queryDeviceEvents(String deviceId, String eventType,
                                                                   Instant startTime, Instant endTime,
                                                                   int limit, int offset) {
        StringBuilder sql = new StringBuilder(
            "SELECT device_id, event_id, event_type, params, created_at " +
            "FROM deskpet_ts.ts_device_event WHERE 1=1");
        java.util.List<Object> params = new java.util.ArrayList<>();

        if (deviceId != null && !deviceId.isEmpty()) {
            sql.append(" AND device_id = ?");
            params.add(deviceId);
        }
        if (eventType != null && !eventType.isEmpty()) {
            sql.append(" AND event_type = ?");
            params.add(eventType);
        }
        if (startTime != null) {
            sql.append(" AND created_at >= ?");
            params.add(Timestamp.from(startTime));
        }
        if (endTime != null) {
            sql.append(" AND created_at <= ?");
            params.add(Timestamp.from(endTime));
        }
        sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        return tsJdbc.queryForList(sql.toString(), params.toArray());
    }

    /**
     * 查询设备事件统计（按 event_type 分组）
     */
    public java.util.List<Map<String, Object>> queryDeviceEventStats(String deviceId, Instant since) {
        return tsJdbc.queryForList(
            "SELECT event_type, COUNT(*) AS count FROM deskpet_ts.ts_device_event " +
            "WHERE device_id = ? AND created_at >= ? GROUP BY event_type ORDER BY count DESC",
            deviceId, Timestamp.from(since));
    }

    /**
     * 统计设备事件总数
     */
    public long countDeviceEvents(String deviceId, String eventType,
                                   Instant startTime, Instant endTime) {
        StringBuilder sql = new StringBuilder(
            "SELECT COUNT(*) FROM deskpet_ts.ts_device_event WHERE 1=1");
        java.util.List<Object> params = new java.util.ArrayList<>();

        if (deviceId != null && !deviceId.isEmpty()) {
            sql.append(" AND device_id = ?");
            params.add(deviceId);
        }
        if (eventType != null && !eventType.isEmpty()) {
            sql.append(" AND event_type = ?");
            params.add(eventType);
        }
        if (startTime != null) {
            sql.append(" AND created_at >= ?");
            params.add(Timestamp.from(startTime));
        }
        if (endTime != null) {
            sql.append(" AND created_at <= ?");
            params.add(Timestamp.from(endTime));
        }

        Long count = tsJdbc.queryForObject(sql.toString(), Long.class, params.toArray());
        return count != null ? count : 0L;
    }
}
