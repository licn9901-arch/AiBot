package com.deskpet.gateway;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

public final class GatewayMetrics {
    private static final GatewayMetrics INSTANCE = new GatewayMetrics();

    private final LongAdder connectCount = new LongAdder();
    private final LongAdder disconnectCount = new LongAdder();
    private final LongAdder telemetryCount = new LongAdder();
    private final LongAdder ackCount = new LongAdder();
    private final LongAdder authFailCount = new LongAdder();
    private final LongAdder authRetryCount = new LongAdder();
    private final LongAdder callbackFailCount = new LongAdder();
    private final LongAdder commandSendCount = new LongAdder();
    private final LongAdder commandSendOkCount = new LongAdder();
    private final LongAdder commandSendFailCount = new LongAdder();
    private final AtomicInteger onlineCount = new AtomicInteger();
    private final long startTimeMs = System.currentTimeMillis();

    private GatewayMetrics() {
    }

    public static GatewayMetrics getInstance() {
        return INSTANCE;
    }

    public long onConnect() {
        connectCount.increment();
        return connectCount.sum();
    }

    public long onDisconnect() {
        disconnectCount.increment();
        return disconnectCount.sum();
    }

    public long onTelemetry() {
        telemetryCount.increment();
        return telemetryCount.sum();
    }

    public long onAck() {
        ackCount.increment();
        return ackCount.sum();
    }

    public long onAuthFail() {
        authFailCount.increment();
        return authFailCount.sum();
    }

    public long onAuthRetry() {
        authRetryCount.increment();
        return authRetryCount.sum();
    }

    public long onCallbackFail() {
        callbackFailCount.increment();
        return callbackFailCount.sum();
    }

    public long onCommandSend() {
        commandSendCount.increment();
        return commandSendCount.sum();
    }

    public long onCommandSendOk() {
        commandSendOkCount.increment();
        return commandSendOkCount.sum();
    }

    public long onCommandSendFail() {
        commandSendFailCount.increment();
        return commandSendFailCount.sum();
    }

    public void setOnlineCount(int count) {
        onlineCount.set(Math.max(0, count));
    }

    public int onlineCount() {
        return onlineCount.get();
    }

    public long connectCount() {
        return connectCount.sum();
    }

    public long disconnectCount() {
        return disconnectCount.sum();
    }

    public long telemetryCount() {
        return telemetryCount.sum();
    }

    public long ackCount() {
        return ackCount.sum();
    }

    public long authFailCount() {
        return authFailCount.sum();
    }

    public long authRetryCount() {
        return authRetryCount.sum();
    }

    public long callbackFailCount() {
        return callbackFailCount.sum();
    }

    public long commandSendCount() {
        return commandSendCount.sum();
    }

    public long commandSendOkCount() {
        return commandSendOkCount.sum();
    }

    public long commandSendFailCount() {
        return commandSendFailCount.sum();
    }

    public double uptimeSeconds() {
        return (System.currentTimeMillis() - startTimeMs) / 1000.0;
    }

    public String toPrometheus() {
        StringBuilder sb = new StringBuilder();
        appendGauge(sb, "deskpet_gateway_online", onlineCount(), "当前在线设备数");
        appendCounter(sb, "deskpet_gateway_connect_total", connectCount(), "连接次数");
        appendCounter(sb, "deskpet_gateway_disconnect_total", disconnectCount(), "断开次数");
        appendCounter(sb, "deskpet_gateway_telemetry_total", telemetryCount(), "遥测上报次数");
        appendCounter(sb, "deskpet_gateway_ack_total", ackCount(), "回执次数");
        appendCounter(sb, "deskpet_gateway_auth_fail_total", authFailCount(), "鉴权失败次数");
        appendCounter(sb, "deskpet_gateway_auth_retry_total", authRetryCount(), "鉴权重试次数");
        appendCounter(sb, "deskpet_gateway_callback_fail_total", callbackFailCount(), "回调失败次数");
        appendCounter(sb, "deskpet_gateway_command_send_total", commandSendCount(), "下发请求次数");
        appendCounter(sb, "deskpet_gateway_command_send_ok_total", commandSendOkCount(), "下发成功次数");
        appendCounter(sb, "deskpet_gateway_command_send_fail_total", commandSendFailCount(), "下发失败次数");
        appendGauge(sb, "deskpet_gateway_uptime_seconds", uptimeSeconds(), "运行时长(秒)");
        return sb.toString();
    }

    private void appendCounter(StringBuilder sb, String name, long value, String help) {
        sb.append("# HELP ").append(name).append(' ').append(help).append('\n');
        sb.append("# TYPE ").append(name).append(" counter\n");
        sb.append(name).append(' ').append(value).append('\n');
    }

    private void appendGauge(StringBuilder sb, String name, double value, String help) {
        sb.append("# HELP ").append(name).append(' ').append(help).append('\n');
        sb.append("# TYPE ").append(name).append(" gauge\n");
        sb.append(name).append(' ').append(value).append('\n');
    }
}
