package com.deskpet.core.service;

import com.deskpet.core.dto.DeviceResponse;
import com.deskpet.core.error.BusinessException;
import com.deskpet.core.error.ErrorCode;
import com.deskpet.core.model.Device;
import com.deskpet.core.model.DeviceSession;
import com.deskpet.core.model.Product;
import com.deskpet.core.model.TelemetryLatest;
import com.deskpet.core.security.SecretHash;
import com.deskpet.core.security.SecretHasher;
import com.deskpet.core.util.CosUtil;
import com.deskpet.core.repository.DeviceRepository;
import com.deskpet.core.repository.DeviceSessionRepository;
import com.deskpet.core.repository.ProductRepository;
import com.deskpet.core.repository.TelemetryLatestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DeviceService {
    private final DeviceRepository deviceRepository;
    private final DeviceSessionRepository sessionRepository;
    private final TelemetryLatestRepository telemetryLatestRepository;
    private final ProductRepository productRepository;
    private final CosUtil cosUtil;
    private final SecretHasher secretHasher;

    private TimeSeriesService timeSeriesService;
    private WebSocketPushService webSocketPushService;

    public DeviceService(DeviceRepository deviceRepository,
                         DeviceSessionRepository sessionRepository,
                         TelemetryLatestRepository telemetryLatestRepository,
                         ProductRepository productRepository,
                         CosUtil cosUtil,
                         SecretHasher secretHasher) {
        this.deviceRepository = deviceRepository;
        this.sessionRepository = sessionRepository;
        this.telemetryLatestRepository = telemetryLatestRepository;
        this.productRepository = productRepository;
        this.cosUtil = cosUtil;
        this.secretHasher = secretHasher;
    }

    @Autowired(required = false)
    public void setTimeSeriesService(TimeSeriesService timeSeriesService) {
        this.timeSeriesService = timeSeriesService;
    }

    @Autowired(required = false)
    public void setWebSocketPushService(WebSocketPushService webSocketPushService) {
        this.webSocketPushService = webSocketPushService;
    }

    public Device register(String deviceId, String secret, String model, String productKey, String remark,Long productId) {
        if (deviceRepository.existsById(deviceId)) {
            throw new BusinessException(ErrorCode.DEVICE_ALREADY_EXISTS);
        }
        SecretHash hashed = secretHasher.hash(secret);
        Device device = new Device(deviceId, hashed.hash(), hashed.salt(), model, productKey, productId, remark, Instant.now());
        return deviceRepository.save(device);
    }

    public Collection<Device> list() {
        return deviceRepository.findAll();
    }

    public Optional<Device> find(String deviceId) {
        return deviceRepository.findById(deviceId);
    }

    public Optional<DeviceSession> findSession(String deviceId) {
        return sessionRepository.findById(deviceId);
    }

    public Optional<TelemetryLatest> findTelemetry(String deviceId) {
        return telemetryLatestRepository.findById(deviceId);
    }

    public boolean verifySecret(Device device, String secret) {
        return secretHasher.matches(secret, device.secretSalt(), device.secretHash());
    }

    public DeviceSession markOnline(String deviceId, String gatewayInstanceId, String ip) {
        Instant now = Instant.now();
        DeviceSession session = new DeviceSession(deviceId, true, gatewayInstanceId, ip, now);
        // 写入上下线历史到 TimescaleDB
        if (timeSeriesService != null) {
            timeSeriesService.writeDeviceSession(deviceId, true, gatewayInstanceId, ip, now);
        }
        DeviceSession saved = sessionRepository.save(session);
        // 推送设备上线事件
        if (webSocketPushService != null) {
            webSocketPushService.pushPresence(deviceId, true);
        }
        return saved;
    }

    public DeviceSession markOffline(String deviceId, String gatewayInstanceId, String ip) {
        Instant now = Instant.now();
        DeviceSession session = new DeviceSession(deviceId, false, gatewayInstanceId, ip, now);
        // 写入上下线历史到 TimescaleDB
        if (timeSeriesService != null) {
            timeSeriesService.writeDeviceSession(deviceId, false, gatewayInstanceId, ip, now);
        }
        DeviceSession saved = sessionRepository.save(session);
        // 推送设备下线事件
        if (webSocketPushService != null) {
            webSocketPushService.pushPresence(deviceId, false);
        }
        return saved;
    }

    /**
     * 根据设备ID列表查询设备（带会话和遥测信息）
     */
    public List<DeviceResponse> findByIds(List<String> deviceIds) {
        if (deviceIds == null || deviceIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Device> devices = deviceRepository.findAllById(deviceIds);
        Map<Long, String> productIconMap = loadProductIconMap(devices);
        return devices.stream()
            .map(device -> toDeviceResponse(device, productIconMap))
            .toList();
    }

    /**
     * 获取所有设备（带会话和遥测信息）
     */
    public List<DeviceResponse> listAll() {
        List<Device> devices = deviceRepository.findAll();
        Map<Long, String> productIconMap = loadProductIconMap(devices);
        return devices.stream()
            .map(device -> toDeviceResponse(device, productIconMap))
            .toList();
    }

    public DeviceResponse toDeviceResponse(Device device) {
        return toDeviceResponse(device, loadProductIconMap(List.of(device)));
    }

    /**
     * 网关启动时清理：将指定网关实例下所有残留在线设备标记为离线
     */
    public void markAllOfflineByGateway(String gatewayInstanceId) {
        List<DeviceSession> sessions = sessionRepository
                .findByGatewayInstanceIdAndOnlineTrue(gatewayInstanceId);
        for (DeviceSession s : sessions) {
            markOffline(s.deviceId(), gatewayInstanceId, s.ip());
        }
        log.info("Gateway cleanup: instanceId={}, offlined={} devices",
                gatewayInstanceId, sessions.size());
    }

    private Map<Long, String> loadProductIconMap(List<Device> devices) {
        Set<Long> productIds = devices.stream()
                .map(Device::productId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (productIds.isEmpty()) {
            return Map.of();
        }
        return productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, product -> cosUtil.resolveObjectUrl(product.getIcon())));
    }

    private String resolveProductIcon(Map<Long, String> productIconMap, Long productId) {
        if (productId == null) {
            return null;
        }
        return productIconMap.get(productId);
    }

    private DeviceResponse toDeviceResponse(Device device, Map<Long, String> productIconMap) {
        return DeviceResponse.of(
                device,
                findSession(device.deviceId()).orElse(null),
                findTelemetry(device.deviceId()).orElse(null),
                resolveProductIcon(productIconMap, device.productId())
        );
    }
}
