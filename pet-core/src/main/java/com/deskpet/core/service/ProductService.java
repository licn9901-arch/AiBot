package com.deskpet.core.service;

import com.deskpet.core.dto.*;
import com.deskpet.core.error.BusinessException;
import com.deskpet.core.error.ErrorCode;
import com.deskpet.core.model.*;
import com.deskpet.core.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 产品与物模型服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ThingModelPropertyRepository propertyRepository;
    private final ThingModelServiceRepository serviceRepository;
    private final ThingModelEventRepository eventRepository;
    private final ObjectMapper objectMapper;

    // ==================== 产品管理 ====================

    /**
     * 创建产品
     */
    @Transactional(rollbackFor = Exception.class)
    public ProductResponse createProduct(ProductCreateRequest request) {
        if (productRepository.existsByProductKey(request.productKey())) {
            throw new BusinessException(ErrorCode.PRODUCT_ALREADY_EXISTS, "产品标识已存在");
        }

        Product product = Product.builder()
            .productKey(request.productKey())
            .name(request.name())
            .description(request.description())
            .status(Product.Status.ACTIVE)
            .build();

        product = productRepository.save(product);
        log.info("Product created: productKey={}", product.getProductKey());
        return ProductResponse.from(product);
    }

    /**
     * 获取产品列表
     */
    @Transactional(rollbackFor = Exception.class)
    public List<ProductResponse> listProducts() {
        return productRepository.findAll().stream()
            .map(ProductResponse::from)
            .toList();
    }

    /**
     * 获取产品详情（含完整物模型）
     */
    @Transactional(rollbackFor = Exception.class)
    public ThingModelDTO getProductWithThingModel(String productKey) {
        Product product = productRepository.findByProductKeyWithThingModel(productKey)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "产品不存在"));
        return ThingModelDTO.from(product);
    }

    /**
     * 更新产品
     */
    @Transactional(rollbackFor = Exception.class)
    public ProductResponse updateProduct(String productKey, ProductUpdateRequest request) {
        Product product = productRepository.findByProductKey(productKey)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "产品不存在"));

        if (request.name() != null && !request.name().isBlank()) {
            product.setName(request.name());
        }
        if (request.description() != null) {
            product.setDescription(request.description());
        }
        if (request.status() != null) {
            try {
                product.setStatus(Product.Status.valueOf(request.status()));
            } catch (IllegalArgumentException e) {
                throw new BusinessException(ErrorCode.INVALID_PARAM, "无效的状态值");
            }
        }

        product = productRepository.save(product);
        log.info("Product updated: productKey={}", productKey);
        return ProductResponse.from(product);
    }

    /**
     * 删除产品
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(String productKey) {
        Product product = productRepository.findByProductKey(productKey)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "产品不存在"));

        productRepository.delete(product);
        log.info("Product deleted: productKey={}", productKey);
    }

    // ==================== 属性管理 ====================

    /**
     * 添加属性
     */
    @Transactional(rollbackFor = Exception.class)
    public PropertyDTO addProperty(String productKey, PropertyCreateRequest request) {
        Product product = productRepository.findByProductKey(productKey)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "产品不存在"));

        if (propertyRepository.existsByProductIdAndIdentifier(product.getId(), request.identifier())) {
            throw new BusinessException(ErrorCode.PROPERTY_ALREADY_EXISTS, "属性标识已存在");
        }

        ThingModelProperty property = ThingModelProperty.builder()
            .product(product)
            .identifier(request.identifier())
            .name(request.name())
            .dataType(request.dataType())
            .specs(request.specs())
            .accessMode(request.accessMode() != null ? request.accessMode() : "r")
            .required(request.required() != null ? request.required() : false)
            .description(request.description())
            .sortOrder(request.sortOrder() != null ? request.sortOrder() : 0)
            .build();

        property = propertyRepository.save(property);
        log.info("Property added: productKey={}, identifier={}", productKey, request.identifier());
        return PropertyDTO.from(property);
    }

    /**
     * 更新属性
     */
    @Transactional(rollbackFor = Exception.class)
    public PropertyDTO updateProperty(String productKey, Long propertyId, PropertyCreateRequest request) {
        Product product = productRepository.findByProductKey(productKey)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "产品不存在"));

        ThingModelProperty property = propertyRepository.findById(propertyId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PROPERTY_NOT_FOUND, "属性不存在"));

        if (!property.getProduct().getId().equals(product.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "属性不属于该产品");
        }

        // 检查标识是否冲突
        if (!property.getIdentifier().equals(request.identifier()) &&
            propertyRepository.existsByProductIdAndIdentifier(product.getId(), request.identifier())) {
            throw new BusinessException(ErrorCode.PROPERTY_ALREADY_EXISTS, "属性标识已存在");
        }

        property.setIdentifier(request.identifier());
        property.setName(request.name());
        property.setDataType(request.dataType());
        if (request.specs() != null) {
            property.setSpecs(request.specs());
        }
        if (request.accessMode() != null) {
            property.setAccessMode(request.accessMode());
        }
        if (request.required() != null) {
            property.setRequired(request.required());
        }
        if (request.description() != null) {
            property.setDescription(request.description());
        }
        if (request.sortOrder() != null) {
            property.setSortOrder(request.sortOrder());
        }

        property = propertyRepository.save(property);
        log.info("Property updated: productKey={}, propertyId={}", productKey, propertyId);
        return PropertyDTO.from(property);
    }

    /**
     * 删除属性
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteProperty(String productKey, Long propertyId) {
        Product product = productRepository.findByProductKey(productKey)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "产品不存在"));

        ThingModelProperty property = propertyRepository.findById(propertyId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PROPERTY_NOT_FOUND, "属性不存在"));

        if (!property.getProduct().getId().equals(product.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "属性不属于该产品");
        }

        propertyRepository.delete(property);
        log.info("Property deleted: productKey={}, propertyId={}", productKey, propertyId);
    }

    // ==================== 服务管理 ====================

    /**
     * 添加服务
     */
    @Transactional(rollbackFor = Exception.class)
    public ServiceDTO addService(String productKey, ServiceCreateRequest request) {
        Product product = productRepository.findByProductKey(productKey)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "产品不存在"));

        if (serviceRepository.existsByProductIdAndIdentifier(product.getId(), request.identifier())) {
            throw new BusinessException(ErrorCode.SERVICE_ALREADY_EXISTS, "服务标识已存在");
        }

        ThingModelService service = ThingModelService.builder()
            .product(product)
            .identifier(request.identifier())
            .name(request.name())
            .callType(request.callType() != null ? request.callType() : "async")
            .inputParams(request.inputParams())
            .outputParams(request.outputParams())
            .description(request.description())
            .sortOrder(request.sortOrder() != null ? request.sortOrder() : 0)
            .build();

        service = serviceRepository.save(service);
        log.info("Service added: productKey={}, identifier={}", productKey, request.identifier());
        return ServiceDTO.from(service);
    }

    /**
     * 更新服务
     */
    @Transactional(rollbackFor = Exception.class)
    public ServiceDTO updateService(String productKey, Long serviceId, ServiceCreateRequest request) {
        Product product = productRepository.findByProductKey(productKey)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "产品不存在"));

        ThingModelService service = serviceRepository.findById(serviceId)
            .orElseThrow(() -> new BusinessException(ErrorCode.SERVICE_NOT_FOUND, "服务不存在"));

        if (!service.getProduct().getId().equals(product.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "服务不属于该产品");
        }

        // 检查标识是否冲突
        if (!service.getIdentifier().equals(request.identifier()) &&
            serviceRepository.existsByProductIdAndIdentifier(product.getId(), request.identifier())) {
            throw new BusinessException(ErrorCode.SERVICE_ALREADY_EXISTS, "服务标识已存在");
        }

        service.setIdentifier(request.identifier());
        service.setName(request.name());
        if (request.callType() != null) {
            service.setCallType(request.callType());
        }
        if (request.inputParams() != null) {
            service.setInputParams(request.inputParams());
        }
        if (request.outputParams() != null) {
            service.setOutputParams(request.outputParams());
        }
        if (request.description() != null) {
            service.setDescription(request.description());
        }
        if (request.sortOrder() != null) {
            service.setSortOrder(request.sortOrder());
        }

        service = serviceRepository.save(service);
        log.info("Service updated: productKey={}, serviceId={}", productKey, serviceId);
        return ServiceDTO.from(service);
    }

    /**
     * 删除服务
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteService(String productKey, Long serviceId) {
        Product product = productRepository.findByProductKey(productKey)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "产品不存在"));

        ThingModelService service = serviceRepository.findById(serviceId)
            .orElseThrow(() -> new BusinessException(ErrorCode.SERVICE_NOT_FOUND, "服务不存在"));

        if (!service.getProduct().getId().equals(product.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "服务不属于该产品");
        }

        serviceRepository.delete(service);
        log.info("Service deleted: productKey={}, serviceId={}", productKey, serviceId);
    }

    // ==================== 事件管理 ====================

    /**
     * 添加事件
     */
    @Transactional(rollbackFor = Exception.class)
    public EventDTO addEvent(String productKey, EventCreateRequest request) {
        Product product = productRepository.findByProductKey(productKey)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "产品不存在"));

        if (eventRepository.existsByProductIdAndIdentifier(product.getId(), request.identifier())) {
            throw new BusinessException(ErrorCode.EVENT_ALREADY_EXISTS, "事件标识已存在");
        }

        ThingModelEvent event = ThingModelEvent.builder()
            .product(product)
            .identifier(request.identifier())
            .name(request.name())
            .eventType(request.eventType())
            .outputParams(request.outputParams())
            .description(request.description())
            .sortOrder(request.sortOrder() != null ? request.sortOrder() : 0)
            .build();

        event = eventRepository.save(event);
        log.info("Event added: productKey={}, identifier={}", productKey, request.identifier());
        return EventDTO.from(event);
    }

    /**
     * 更新事件
     */
    @Transactional(rollbackFor = Exception.class)
    public EventDTO updateEvent(String productKey, Long eventId, EventCreateRequest request) {
        Product product = productRepository.findByProductKey(productKey)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "产品不存在"));

        ThingModelEvent event = eventRepository.findById(eventId)
            .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND, "事件不存在"));

        if (!event.getProduct().getId().equals(product.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "事件不属于该产品");
        }

        // 检查标识是否冲突
        if (!event.getIdentifier().equals(request.identifier()) &&
            eventRepository.existsByProductIdAndIdentifier(product.getId(), request.identifier())) {
            throw new BusinessException(ErrorCode.EVENT_ALREADY_EXISTS, "事件标识已存在");
        }

        event.setIdentifier(request.identifier());
        event.setName(request.name());
        event.setEventType(request.eventType());
        if (request.outputParams() != null) {
            event.setOutputParams(request.outputParams());
        }
        if (request.description() != null) {
            event.setDescription(request.description());
        }
        if (request.sortOrder() != null) {
            event.setSortOrder(request.sortOrder());
        }

        event = eventRepository.save(event);
        log.info("Event updated: productKey={}, eventId={}", productKey, eventId);
        return EventDTO.from(event);
    }

    /**
     * 删除事件
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteEvent(String productKey, Long eventId) {
        Product product = productRepository.findByProductKey(productKey)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "产品不存在"));

        ThingModelEvent event = eventRepository.findById(eventId)
            .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND, "事件不存在"));

        if (!event.getProduct().getId().equals(product.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "事件不属于该产品");
        }

        eventRepository.delete(event);
        log.info("Event deleted: productKey={}, eventId={}", productKey, eventId);
    }

    // ==================== 导入导出 ====================

    /**
     * 导出物模型为 JSON
     */
    @Transactional(rollbackFor = Exception.class)
    public String exportThingModel(String productKey) {
        ThingModelDTO thingModel = getProductWithThingModel(productKey);
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(thingModel);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "导出失败");
        }
    }

    /**
     * 导入物模型 JSON
     */
    @Transactional(rollbackFor = Exception.class)
    public ThingModelDTO importThingModel(String productKey, String json) {
        Product product = productRepository.findByProductKey(productKey)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "产品不存在"));

        ThingModelDTO dto;
        try {
            dto = objectMapper.readValue(json, ThingModelDTO.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "JSON 格式错误");
        }

        // 清空现有物模型
        propertyRepository.deleteByProductId(product.getId());
        serviceRepository.deleteByProductId(product.getId());
        eventRepository.deleteByProductId(product.getId());

        // 导入属性
        if (dto.properties() != null) {
            for (PropertyDTO p : dto.properties()) {
                ThingModelProperty property = ThingModelProperty.builder()
                    .product(product)
                    .identifier(p.identifier())
                    .name(p.name())
                    .dataType(p.dataType())
                    .specs(p.specs())
                    .accessMode(p.accessMode() != null ? p.accessMode() : "r")
                    .required(p.required() != null ? p.required() : false)
                    .description(p.description())
                    .sortOrder(p.sortOrder() != null ? p.sortOrder() : 0)
                    .build();
                propertyRepository.save(property);
            }
        }

        // 导入服务
        if (dto.services() != null) {
            for (ServiceDTO s : dto.services()) {
                ThingModelService service = ThingModelService.builder()
                    .product(product)
                    .identifier(s.identifier())
                    .name(s.name())
                    .callType(s.callType() != null ? s.callType() : "async")
                    .inputParams(s.inputParams())
                    .outputParams(s.outputParams())
                    .description(s.description())
                    .sortOrder(s.sortOrder() != null ? s.sortOrder() : 0)
                    .build();
                serviceRepository.save(service);
            }
        }

        // 导入事件
        if (dto.events() != null) {
            for (EventDTO e : dto.events()) {
                ThingModelEvent event = ThingModelEvent.builder()
                    .product(product)
                    .identifier(e.identifier())
                    .name(e.name())
                    .eventType(e.eventType())
                    .outputParams(e.outputParams())
                    .description(e.description())
                    .sortOrder(e.sortOrder() != null ? e.sortOrder() : 0)
                    .build();
                eventRepository.save(event);
            }
        }

        log.info("Thing model imported: productKey={}", productKey);
        return getProductWithThingModel(productKey);
    }
}
