import service from '@/utils/request'
import type {
    ProductResponse,
    ProductCreateRequest,
    ProductUpdateRequest,
    ThingModelDTO,
    PropertyDTO,
    PropertyCreateRequest,
    ServiceDTO,
    ServiceCreateRequest,
    EventDTO,
    EventCreateRequest
} from '@/types/product'

// ==================== 产品 CRUD ====================

export function getProducts(): Promise<ProductResponse[]> {
    return service.get('/admin/products')
}

export function getProductDetail(productKey: string): Promise<ThingModelDTO> {
    return service.get(`/admin/products/${productKey}`)
}

export function createProduct(data: ProductCreateRequest): Promise<ProductResponse> {
    return service.post('/admin/products', data)
}

export function updateProduct(productKey: string, data: ProductUpdateRequest): Promise<ProductResponse> {
    return service.put(`/admin/products/${productKey}`, data)
}

export function deleteProduct(productKey: string): Promise<void> {
    return service.delete(`/admin/products/${productKey}`)
}

// ==================== 属性 CRUD ====================

export function addProperty(productKey: string, data: PropertyCreateRequest): Promise<PropertyDTO> {
    return service.post(`/admin/products/${productKey}/properties`, data)
}

export function updateProperty(productKey: string, propertyId: string, data: PropertyCreateRequest): Promise<PropertyDTO> {
    return service.put(`/admin/products/${productKey}/properties/${propertyId}`, data)
}

export function deleteProperty(productKey: string, propertyId: string): Promise<void> {
    return service.delete(`/admin/products/${productKey}/properties/${propertyId}`)
}

// ==================== 服务 CRUD ====================

export function addService(productKey: string, data: ServiceCreateRequest): Promise<ServiceDTO> {
    return service.post(`/admin/products/${productKey}/services`, data)
}

export function updateService(productKey: string, serviceId: string, data: ServiceCreateRequest): Promise<ServiceDTO> {
    return service.put(`/admin/products/${productKey}/services/${serviceId}`, data)
}

export function deleteService(productKey: string, serviceId: string): Promise<void> {
    return service.delete(`/admin/products/${productKey}/services/${serviceId}`)
}

// ==================== 事件 CRUD ====================

export function addEvent(productKey: string, data: EventCreateRequest): Promise<EventDTO> {
    return service.post(`/admin/products/${productKey}/events`, data)
}

export function updateEvent(productKey: string, eventId: string, data: EventCreateRequest): Promise<EventDTO> {
    return service.put(`/admin/products/${productKey}/events/${eventId}`, data)
}

export function deleteEvent(productKey: string, eventId: string): Promise<void> {
    return service.delete(`/admin/products/${productKey}/events/${eventId}`)
}

// ==================== 导入导出 ====================

export function exportThingModel(productKey: string): Promise<Blob> {
    return service.get(`/admin/products/${productKey}/export`, {
        responseType: 'blob',
        transformResponse: undefined
    })
}

export function importThingModel(productKey: string, json: string): Promise<ThingModelDTO> {
    return service.post(`/admin/products/${productKey}/import`, json, {
        headers: { 'Content-Type': 'application/json' }
    })
}
