package com.deskpet.core.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Logback Appender：将日志异步批量写入 TimescaleDB
 * <p>
 * 在 logback-spring.xml 中配置：
 * <pre>
 * &lt;appender name="TSDB" class="com.deskpet.core.logging.TimescaleDbAppender"&gt;
 *     &lt;url&gt;jdbc:postgresql://localhost:5433/deskpet_ts&lt;/url&gt;
 *     &lt;username&gt;deskpet&lt;/username&gt;
 *     &lt;password&gt;deskpet&lt;/password&gt;
 *     &lt;batchSize&gt;50&lt;/batchSize&gt;
 *     &lt;flushIntervalMs&gt;5000&lt;/flushIntervalMs&gt;
 *     &lt;queueSize&gt;10000&lt;/queueSize&gt;
 * &lt;/appender&gt;
 * </pre>
 */
public class TimescaleDbAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    private String url;
    private String username;
    private String password;
    private int batchSize = 50;
    private int flushIntervalMs = 5000;
    private int queueSize = 10000;

    private BlockingQueue<ILoggingEvent> queue;
    private Thread flusherThread;
    private volatile boolean running = true;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String INSERT_SQL =
        "INSERT INTO deskpet_ts.ts_app_log (log_time, level, logger, thread, message, exception, mdc) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?::jsonb)";

    @Override
    public void start() {
        if (url == null || url.isBlank()) {
            addError("TimescaleDB URL is not configured, appender will not start");
            return;
        }
        queue = new LinkedBlockingQueue<>(queueSize);
        flusherThread = new Thread(this::flushLoop, "logback-tsdb-flusher");
        flusherThread.setDaemon(true);
        flusherThread.start();
        super.start();
    }

    @Override
    protected void append(ILoggingEvent event) {
        // 防止递归：跳过本 appender 自身产生的日志
        if (event.getLoggerName().startsWith("com.deskpet.core.logging")) {
            return;
        }
        event.prepareForDeferredProcessing();
        if (!queue.offer(event)) {
            // 队列满则丢弃，避免阻塞业务线程
        }
    }

    private void flushLoop() {
        List<ILoggingEvent> batch = new ArrayList<>(batchSize);
        while (running || !queue.isEmpty()) {
            try {
                ILoggingEvent event = queue.poll(flushIntervalMs, TimeUnit.MILLISECONDS);
                if (event != null) {
                    batch.add(event);
                    queue.drainTo(batch, batchSize - 1);
                }
                if (!batch.isEmpty()) {
                    writeBatch(batch);
                    batch.clear();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        // 关闭前刷出剩余日志
        if (!queue.isEmpty()) {
            queue.drainTo(batch);
            if (!batch.isEmpty()) {
                writeBatch(batch);
            }
        }
    }

    private void writeBatch(List<ILoggingEvent> events) {
        try (Connection conn = DriverManager.getConnection(url, username, password);
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
            for (ILoggingEvent event : events) {
                ps.setTimestamp(1, Timestamp.from(Instant.ofEpochMilli(event.getTimeStamp())));
                ps.setString(2, event.getLevel().toString());
                ps.setString(3, truncate(event.getLoggerName(), 255));
                ps.setString(4, truncate(event.getThreadName(), 100));
                ps.setString(5, event.getFormattedMessage());

                IThrowableProxy tp = event.getThrowableProxy();
                ps.setString(6, tp != null ? ThrowableProxyUtil.asString(tp) : null);

                String mdcJson = null;
                if (event.getMDCPropertyMap() != null && !event.getMDCPropertyMap().isEmpty()) {
                    try {
                        mdcJson = MAPPER.writeValueAsString(event.getMDCPropertyMap());
                    } catch (Exception ignored) {
                        // MDC 序列化失败不影响日志写入
                    }
                }
                ps.setString(7, mdcJson);

                ps.addBatch();
            }
            ps.executeBatch();
        } catch (Exception e) {
            addError("Failed to write log batch to TimescaleDB: " + e.getMessage());
        }
    }

    private String truncate(String value, int maxLen) {
        if (value == null) return null;
        return value.length() > maxLen ? value.substring(0, maxLen) : value;
    }

    @Override
    public void stop() {
        running = false;
        if (flusherThread != null) {
            flusherThread.interrupt();
            try {
                flusherThread.join(10000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        super.stop();
    }

    // ===== Logback XML 配置注入的 setter =====

    public void setUrl(String url) {
        this.url = url;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public void setFlushIntervalMs(int flushIntervalMs) {
        this.flushIntervalMs = flushIntervalMs;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }
}
