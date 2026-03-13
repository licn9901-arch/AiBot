package com.deskpet.core.service;

import com.deskpet.core.dto.ProductCreateRequest;
import com.deskpet.core.dto.ProductResponse;
import com.deskpet.core.dto.ProductUpdateRequest;
import com.deskpet.core.model.Product;
import com.deskpet.core.repository.ProductRepository;
import com.deskpet.core.repository.ThingModelEventRepository;
import com.deskpet.core.repository.ThingModelPropertyRepository;
import com.deskpet.core.repository.ThingModelServiceRepository;
import com.deskpet.core.util.CosUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private ThingModelPropertyRepository propertyRepository;
    @Mock
    private ThingModelServiceRepository serviceRepository;
    @Mock
    private ThingModelEventRepository eventRepository;
    @Mock
    private CosUtil cosUtil;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(
            productRepository,
            propertyRepository,
            serviceRepository,
            eventRepository,
            cosUtil,
            new ObjectMapper()
        );
    }

    @Test
    void createProduct_persistsIcon() {
        when(productRepository.existsByProductKey("deskpet-v2")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cosUtil.resolveObjectKey("https://cdn.test/product-icon.png")).thenReturn("product-icons/abc.png");
        when(cosUtil.resolveObjectKey("product-icons/abc.png")).thenReturn("product-icons/abc.png");
        when(cosUtil.resolveObjectUrl("product-icons/abc.png")).thenReturn("https://cdn.test/product-icon.png");

        ProductResponse response = productService.createProduct(new ProductCreateRequest(
            "deskpet-v2",
            "桌宠 V2",
            "第二代桌宠设备",
            "https://cdn.test/product-icon.png"
        ));

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertEquals("product-icons/abc.png", captor.getValue().getIcon());
        assertEquals("https://cdn.test/product-icon.png", response.icon());
        assertEquals("product-icons/abc.png", response.iconKey());
    }

    @Test
    void updateProduct_updatesIcon() {
        Product product = Product.builder()
            .id(1L)
            .productKey("deskpet-v2")
            .name("桌宠 V2")
            .description("旧描述")
            .icon("product-icons/old.png")
            .status(Product.Status.ACTIVE)
            .build();
        when(productRepository.findByProductKey("deskpet-v2")).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cosUtil.resolveObjectKey("https://cdn.test/new.png")).thenReturn("product-icons/new.png");
        when(cosUtil.resolveObjectKey("product-icons/new.png")).thenReturn("product-icons/new.png");
        when(cosUtil.resolveObjectUrl("product-icons/new.png")).thenReturn("https://cdn.test/new.png");

        ProductResponse response = productService.updateProduct("deskpet-v2", new ProductUpdateRequest(
            "桌宠 V2 Pro",
            "新描述",
            "https://cdn.test/new.png",
            "DEPRECATED"
        ));

        assertEquals("product-icons/new.png", product.getIcon());
        assertEquals("https://cdn.test/new.png", response.icon());
        assertEquals("product-icons/new.png", response.iconKey());
        assertEquals("DEPRECATED", response.status());
    }
}
