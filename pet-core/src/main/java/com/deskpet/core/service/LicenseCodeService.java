package com.deskpet.core.service;

import cn.dev33.satoken.stp.StpUtil;
import com.deskpet.core.dto.ActivateLicenseRequest;
import com.deskpet.core.dto.GenerateBatchResult;
import com.deskpet.core.dto.GenerateLicenseRequest;
import com.deskpet.core.dto.GenerateLicenseResponse;
import com.deskpet.core.dto.LicenseCodeResponse;
import com.deskpet.core.dto.LicenseQueryRequest;
import com.deskpet.core.error.BusinessException;
import com.deskpet.core.error.ErrorCode;
import com.deskpet.core.model.LicenseCode;
import com.deskpet.core.model.PendingDeviceSecret;
import com.deskpet.core.model.Product;
import com.deskpet.core.repository.LicenseCodeRepository;
import com.deskpet.core.repository.PendingDeviceSecretRepository;
import com.deskpet.core.repository.ProductRepository;
import com.deskpet.core.util.DeviceSnGenerator;
import com.deskpet.core.util.LicenseCodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 授权码服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LicenseCodeService {

    private final LicenseCodeRepository licenseCodeRepository;
    private final PendingDeviceSecretRepository pendingDeviceSecretRepository;
    private final ProductRepository productRepository;
    private final DeviceService deviceService;
    private final DeviceSnGenerator deviceSnGenerator;
    private final OperationLogService operationLogService;

    /**
     * 批量生成授权码（自动创建预绑定设备），暂存密钥供下载
     */
    @Transactional(rollbackFor = Exception.class)
    public GenerateBatchResult generateBatch(GenerateLicenseRequest request) {
        long operatorId = StpUtil.getLoginIdAsLong();
        String productKey = request.productKey();

        // 验证产品存在
        Product product = productRepository.findByProductKey(productKey)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "产品不存在: " + productKey));

        // 自动生成 batchNo（如果未提供）
        String batchNo = request.batchNo();
        if (batchNo == null || batchNo.isBlank()) {
            batchNo = "BATCH-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + String.format("%04d", SECURE_RANDOM.nextInt(10000));
        }

        List<String> codes = LicenseCodeGenerator.generateBatch(request.count());

        // 确保生成的授权码不重复
        codes = codes.stream()
            .filter(code -> !licenseCodeRepository.existsByCode(code))
            .toList();

        if (codes.size() < request.count()) {
            int remaining = request.count() - codes.size();
            List<String> finalCodes = codes;
            List<String> additionalCodes = LicenseCodeGenerator.generateBatch(remaining * 2).stream()
                .filter(code -> !licenseCodeRepository.existsByCode(code) && !finalCodes.contains(code))
                .limit(remaining)
                .toList();
            codes = new ArrayList<>(codes);
            codes.addAll(additionalCodes);
        }

        List<LicenseCode> licenses = new ArrayList<>();
        List<GenerateLicenseResponse> responses = new ArrayList<>();
        List<PendingDeviceSecret> pendingSecrets = new ArrayList<>();

        for (String code : codes) {
            // 生成设备 SN 和密钥
            String sn = deviceSnGenerator.nextSn(productKey);
            String rawSecret = generateRandomSecret();

            // 创建预绑定设备
            deviceService.register(sn, rawSecret, product.getName(), productKey, null,product.getId());

            // 构建授权码
            LicenseCode license = LicenseCode.builder()
                .code(code)
                .batchNo(batchNo)
                .status(LicenseCode.Status.UNUSED)
                .deviceId(sn)
                .productKey(productKey)
                .expiresAt(request.expiresAt())
                .remark(request.remark())
                .createdAt(Instant.now())
                .build();
            licenses.add(license);

            // 暂存密钥记录
            pendingSecrets.add(PendingDeviceSecret.builder()
                .batchNo(batchNo)
                .deviceId(sn)
                .code(code)
                .rawSecret(rawSecret)
                .productKey(productKey)
                .build());

            // 构建响应（含明文密钥，兼容前端即时展示）
            responses.add(new GenerateLicenseResponse(
                null, code, sn, rawSecret, productKey,
                batchNo, LicenseCode.Status.UNUSED.name(),
                request.expiresAt(), request.remark(), license.getCreatedAt()
            ));
        }

        licenses = licenseCodeRepository.saveAll(licenses);
        pendingDeviceSecretRepository.saveAll(pendingSecrets);

        // 回填 ID 到响应
        for (int i = 0; i < licenses.size(); i++) {
            GenerateLicenseResponse old = responses.get(i);
            responses.set(i, new GenerateLicenseResponse(
                licenses.get(i).getId(), old.code(), old.deviceId(), old.deviceSecret(),
                old.productKey(), old.batchNo(), old.status(),
                old.expiresAt(), old.remark(), old.createdAt()
            ));
        }

        log.info("Generated {} license codes with devices, batchNo={}, productKey={}", licenses.size(), batchNo, productKey);
        operationLogService.log(operatorId, null, "GENERATE_LICENSE",
            Map.of("count", licenses.size(), "batchNo", batchNo, "productKey", productKey));

        return new GenerateBatchResult(batchNo, responses);
    }

    /**
     * 激活授权码（设备已预绑定，只需授权码即可激活）
     */
    @Transactional(rollbackFor = Exception.class)
    public LicenseCodeResponse activate(ActivateLicenseRequest request) {
        long userId = StpUtil.getLoginIdAsLong();

        // 1. 查找授权码
        LicenseCode license = licenseCodeRepository.findByCode(request.code())
            .orElseThrow(() -> new BusinessException(ErrorCode.LICENSE_NOT_FOUND, "授权码不存在"));

        // 2. 检查授权码状态
        if (license.getStatus() != LicenseCode.Status.UNUSED) {
            throw new BusinessException(ErrorCode.LICENSE_ALREADY_USED, "授权码已被使用");
        }

        // 3. 检查授权码是否过期
        if (license.isExpired()) {
            throw new BusinessException(ErrorCode.LICENSE_EXPIRED, "授权码已过期");
        }

        // 4. 激活授权码（设备已在生成时预绑定）
        license.setStatus(LicenseCode.Status.ACTIVATED);
        license.setUserId(userId);
        license.setActivatedAt(Instant.now());

        license = licenseCodeRepository.save(license);

        log.info("License activated: code={}, deviceId={}, userId={}", request.code(), license.getDeviceId(), userId);
        operationLogService.log(userId, license.getDeviceId(), "ACTIVATE_LICENSE",
            Map.of("code", request.code()));

        return LicenseCodeResponse.from(license);
    }

    /**
     * 撤销授权码
     */
    @Transactional(rollbackFor = Exception.class)
    public void revoke(Long licenseId) {
        long operatorId = StpUtil.getLoginIdAsLong();

        LicenseCode license = licenseCodeRepository.findById(licenseId)
            .orElseThrow(() -> new BusinessException(ErrorCode.LICENSE_NOT_FOUND, "授权码不存在"));

        license.setStatus(LicenseCode.Status.REVOKED);
        // 解除设备绑定
        String deviceId = license.getDeviceId();
        license.setDeviceId(null);
        license.setUserId(null);
        license.setActivatedAt(null);

        licenseCodeRepository.save(license);

        log.info("License revoked: id={}, code={}", licenseId, license.getCode());
        operationLogService.log(operatorId, deviceId, "REVOKE_LICENSE",
            Map.of("licenseId", licenseId, "code", license.getCode()));
    }

    /**
     * 查询授权码列表
     */
    @Transactional(rollbackFor = Exception.class)
    public Page<LicenseCodeResponse> list(LicenseQueryRequest query, Pageable pageable) {
        Page<LicenseCode> page;
        if (query.status() != null && query.batchNo() != null) {
            page = licenseCodeRepository.findByStatusAndBatchNo(query.status(), query.batchNo(), pageable);
        } else if (query.status() != null) {
            page = licenseCodeRepository.findByStatus(query.status(), pageable);
        } else if (query.batchNo() != null) {
            page = licenseCodeRepository.findByBatchNo(query.batchNo(), pageable);
        } else {
            page = licenseCodeRepository.findAll(pageable);
        }
        return page.map(LicenseCodeResponse::from);
    }

    /**
     * 获取用户的授权码列表
     */
    @Transactional(rollbackFor = Exception.class)
    public List<LicenseCodeResponse> findByCurrentUser() {
        long userId = StpUtil.getLoginIdAsLong();
        return licenseCodeRepository.findByUserId(userId).stream()
            .map(LicenseCodeResponse::from)
            .toList();
    }

    /**
     * 获取用户绑定的设备ID列表
     */
    @Transactional(rollbackFor = Exception.class)
    public List<String> findDeviceIdsByUserId(Long userId) {
        return licenseCodeRepository.findDeviceIdsByUserId(userId);
    }

    /**
     * 检查用户是否拥有某设备
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean hasDevice(Long userId, String deviceId) {
        return licenseCodeRepository.existsByDeviceIdAndStatus(deviceId, LicenseCode.Status.ACTIVATED)
            && licenseCodeRepository.findDeviceIdsByUserId(userId).contains(deviceId);
    }

    /**
     * 导出授权码为 CSV
     */
    @Transactional(rollbackFor = Exception.class)
    public void exportCsv(PrintWriter writer, LicenseQueryRequest query) {
        // CSV 头
        writer.println("授权码,批次号,状态,设备ID,用户ID,激活时间,过期时间,创建时间");

        List<LicenseCode> licenses;
        if (query.status() != null && query.batchNo() != null) {
            licenses = licenseCodeRepository.findByStatusAndBatchNo(query.status(), query.batchNo(), Pageable.unpaged())
                .getContent();
        } else if (query.status() != null) {
            licenses = licenseCodeRepository.findByStatus(query.status(), Pageable.unpaged())
                .getContent();
        } else if (query.batchNo() != null) {
            licenses = licenseCodeRepository.findByBatchNo(query.batchNo(), Pageable.unpaged())
                .getContent();
        } else {
            licenses = licenseCodeRepository.findAll();
        }

        for (LicenseCode license : licenses) {
            writer.printf("%s,%s,%s,%s,%s,%s,%s,%s%n",
                license.getCode(),
                license.getBatchNo() != null ? license.getBatchNo() : "",
                license.getStatus(),
                license.getDeviceId() != null ? license.getDeviceId() : "",
                license.getUserId() != null ? license.getUserId() : "",
                license.getActivatedAt() != null ? license.getActivatedAt() : "",
                license.getExpiresAt() != null ? license.getExpiresAt() : "",
                license.getCreatedAt()
            );
        }

        long operatorId = StpUtil.getLoginIdAsLong();
        operationLogService.log(operatorId, null, "EXPORT_LICENSE",
            Map.of("count", licenses.size()));
    }

    /**
     * 统计批次信息
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> getBatchStats(String batchNo) {
        long total = licenseCodeRepository.countByBatchNo(batchNo);
        long unused = licenseCodeRepository.countByBatchNoAndStatus(batchNo, LicenseCode.Status.UNUSED);
        long activated = licenseCodeRepository.countByBatchNoAndStatus(batchNo, LicenseCode.Status.ACTIVATED);
        long revoked = licenseCodeRepository.countByBatchNoAndStatus(batchNo, LicenseCode.Status.REVOKED);

        return Map.of(
            "batchNo", batchNo,
            "total", total,
            "unused", unused,
            "activated", activated,
            "revoked", revoked
        );
    }

    /**
     * 查询批次暂存密钥
     */
    @Transactional(rollbackFor = Exception.class)
    public List<PendingDeviceSecret> findPendingSecrets(String batchNo) {
        return pendingDeviceSecretRepository.findByBatchNo(batchNo);
    }

    /**
     * 确认下载并删除暂存密钥
     */
    @Transactional(rollbackFor = Exception.class)
    public int confirmAndDeletePendingSecrets(String batchNo) {
        long operatorId = StpUtil.getLoginIdAsLong();
        List<PendingDeviceSecret> secrets = pendingDeviceSecretRepository.findByBatchNo(batchNo);
        int count = secrets.size();
        pendingDeviceSecretRepository.deleteByBatchNo(batchNo);
        log.info("Confirmed and deleted {} pending secrets for batchNo={}", count, batchNo);
        operationLogService.log(operatorId, null, "CONFIRM_LICENSE_DOWNLOAD",
            Map.of("batchNo", batchNo, "count", count));
        return count;
    }

    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private String generateRandomSecret() {
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            sb.append(ALPHANUMERIC.charAt(SECURE_RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }
}
