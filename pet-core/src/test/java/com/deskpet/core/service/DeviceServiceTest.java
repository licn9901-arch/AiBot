package com.deskpet.core.service;

import com.deskpet.core.dto.DeviceResponse;
import com.deskpet.core.model.Device;
import com.deskpet.core.model.Product;
import com.deskpet.core.repository.DeviceRepository;
import com.deskpet.core.repository.DeviceSessionRepository;
import com.deskpet.core.repository.ProductRepository;
import com.deskpet.core.repository.TelemetryLatestRepository;
import com.deskpet.core.security.SecretHasher;
import com.deskpet.core.util.CosUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock
    private DeviceRepository deviceRepository;
    @Mock
    private DeviceSessionRepository sessionRepository;
    @Mock
    private TelemetryLatestRepository telemetryLatestRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CosUtil cosUtil;
    @Mock
    private SecretHasher secretHasher;

    private DeviceService deviceService;

    @BeforeEach
    void setUp() {
        deviceService = new DeviceService(
                deviceRepository,
                sessionRepository,
                telemetryLatestRepository,
                productRepository,
                cosUtil,
                secretHasher
        );
    }

    @Test
    void findByIds_returnsAccessibleProductIconUrl() {
        Device device = new Device("pet-001", "hash", "salt", "deskpet", "deskpet-v1", 10L, "demo", Instant.now());
        Product product = Product.builder()
                .id(10L)
                .productKey("deskpet-v1")
                .name("DeskPet")
                .icon("product-icons/pet.png")
                .build();
        when(deviceRepository.findAllById(List.of("pet-001"))).thenReturn(List.of(device));
        when(productRepository.findAllById(anyIterable())).thenReturn(List.of(product));
        when(cosUtil.resolveObjectUrl("product-icons/pet.png")).thenReturn("https://cdn.example.com/product-icons/pet.png");

        List<DeviceResponse> responses = deviceService.findByIds(List.of("pet-001"));

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).productIcon()).isEqualTo("https://cdn.example.com/product-icons/pet.png");
        assertThat(responses.get(0).productKey()).isEqualTo("deskpet-v1");
    }
}
