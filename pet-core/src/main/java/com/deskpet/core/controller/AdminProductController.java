package com.deskpet.core.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.deskpet.core.dto.*;
import com.deskpet.core.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 产品与物模型管理控制器（管理员）
 */
@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
@Tag(name = "产品管理", description = "产品与物模型管理")
public class AdminProductController {

    private final ProductService productService;

    // ==================== 产品管理 ====================

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @SaCheckPermission("product:create")
    @Operation(summary = "创建产品")
    public ProductResponse createProduct(@Valid @RequestBody ProductCreateRequest request) {
        return productService.createProduct(request);
    }

    @GetMapping
    @SaCheckPermission("product:list")
    @Operation(summary = "产品列表")
    public List<ProductResponse> listProducts() {
        return productService.listProducts();
    }

    @GetMapping("/{productKey}")
    @SaCheckPermission("product:list")
    @Operation(summary = "产品详情", description = "获取产品及完整物模型")
    public ThingModelDTO getProduct(@PathVariable String productKey) {
        return productService.getProductWithThingModel(productKey);
    }

    @PutMapping("/{productKey}")
    @SaCheckPermission("product:update")
    @Operation(summary = "更新产品")
    public ProductResponse updateProduct(@PathVariable String productKey,
                                         @Valid @RequestBody ProductUpdateRequest request) {
        return productService.updateProduct(productKey, request);
    }

    @DeleteMapping("/{productKey}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SaCheckPermission("product:delete")
    @Operation(summary = "删除产品")
    public void deleteProduct(@PathVariable String productKey) {
        productService.deleteProduct(productKey);
    }

    // ==================== 属性管理 ====================

    @PostMapping("/{productKey}/properties")
    @ResponseStatus(HttpStatus.CREATED)
    @SaCheckPermission("product:update")
    @Operation(summary = "添加属性")
    public PropertyDTO addProperty(@PathVariable String productKey,
                                   @Valid @RequestBody PropertyCreateRequest request) {
        return productService.addProperty(productKey, request);
    }

    @PutMapping("/{productKey}/properties/{propertyId}")
    @SaCheckPermission("product:update")
    @Operation(summary = "更新属性")
    public PropertyDTO updateProperty(@PathVariable String productKey,
                                      @PathVariable Long propertyId,
                                      @Valid @RequestBody PropertyCreateRequest request) {
        return productService.updateProperty(productKey, propertyId, request);
    }

    @DeleteMapping("/{productKey}/properties/{propertyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SaCheckPermission("product:update")
    @Operation(summary = "删除属性")
    public void deleteProperty(@PathVariable String productKey,
                               @PathVariable Long propertyId) {
        productService.deleteProperty(productKey, propertyId);
    }

    // ==================== 服务管理 ====================

    @PostMapping("/{productKey}/services")
    @ResponseStatus(HttpStatus.CREATED)
    @SaCheckPermission("product:update")
    @Operation(summary = "添加服务")
    public ServiceDTO addService(@PathVariable String productKey,
                                 @Valid @RequestBody ServiceCreateRequest request) {
        return productService.addService(productKey, request);
    }

    @PutMapping("/{productKey}/services/{serviceId}")
    @SaCheckPermission("product:update")
    @Operation(summary = "更新服务")
    public ServiceDTO updateService(@PathVariable String productKey,
                                    @PathVariable Long serviceId,
                                    @Valid @RequestBody ServiceCreateRequest request) {
        return productService.updateService(productKey, serviceId, request);
    }

    @DeleteMapping("/{productKey}/services/{serviceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SaCheckPermission("product:update")
    @Operation(summary = "删除服务")
    public void deleteService(@PathVariable String productKey,
                              @PathVariable Long serviceId) {
        productService.deleteService(productKey, serviceId);
    }

    // ==================== 事件管理 ====================

    @PostMapping("/{productKey}/events")
    @ResponseStatus(HttpStatus.CREATED)
    @SaCheckPermission("product:update")
    @Operation(summary = "添加事件")
    public EventDTO addEvent(@PathVariable String productKey,
                             @Valid @RequestBody EventCreateRequest request) {
        return productService.addEvent(productKey, request);
    }

    @PutMapping("/{productKey}/events/{eventId}")
    @SaCheckPermission("product:update")
    @Operation(summary = "更新事件")
    public EventDTO updateEvent(@PathVariable String productKey,
                                @PathVariable Long eventId,
                                @Valid @RequestBody EventCreateRequest request) {
        return productService.updateEvent(productKey, eventId, request);
    }

    @DeleteMapping("/{productKey}/events/{eventId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SaCheckPermission("product:update")
    @Operation(summary = "删除事件")
    public void deleteEvent(@PathVariable String productKey,
                            @PathVariable Long eventId) {
        productService.deleteEvent(productKey, eventId);
    }

    // ==================== 导入导出 ====================

    @GetMapping(value = "/{productKey}/export", produces = MediaType.APPLICATION_JSON_VALUE)
    @SaCheckPermission("product:list")
    @Operation(summary = "导出物模型", description = "导出为 JSON 格式")
    public void exportThingModel(@PathVariable String productKey,
                                 HttpServletResponse response) throws IOException {
        String json = productService.exportThingModel(productKey);

        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Content-Disposition",
            "attachment; filename=" + productKey + "-thing-model.json");

        PrintWriter writer = response.getWriter();
        writer.write(json);
        writer.flush();
    }

    @PostMapping(value = "/{productKey}/import", consumes = MediaType.APPLICATION_JSON_VALUE)
    @SaCheckPermission("product:update")
    @Operation(summary = "导入物模型", description = "导入 JSON 格式物模型（会覆盖现有定义）")
    public ThingModelDTO importThingModel(@PathVariable String productKey,
                                          @RequestBody String json) {
        return productService.importThingModel(productKey, json);
    }
}
