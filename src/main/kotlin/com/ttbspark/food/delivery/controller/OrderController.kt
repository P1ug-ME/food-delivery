package com.ttbspark.food.delivery.controller

import com.ttbspark.food.delivery.dto.*
import com.ttbspark.food.delivery.service.OrderService
import com.ttbspark.food.delivery.constant.OrderConstants
import com.ttbspark.food.delivery.constant.ApiConstants
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping(ApiConstants.API_BASE_PATH)
class OrderController(
    private val orderService: OrderService
) {
    private val logger = LoggerFactory.getLogger(OrderController::class.java)
    
    @PostMapping
    fun createOrder(@Valid @RequestBody request: CreateOrderRequest): ResponseEntity<OrderResponse> {
        logger.info("Received create order request for customer: {}", request.customerId)
        val response = orderService.createOrder(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }
    
    @GetMapping("/{orderNumber}")
    fun getOrder(@PathVariable orderNumber: String): ResponseEntity<OrderResponse> {
        logger.info("Retrieving order: {}", orderNumber)
        val response = orderService.getOrder(orderNumber)
        return ResponseEntity.ok(response)
    }
    
    @GetMapping("/customer/{customerId}")
    fun getOrdersByCustomer(
        @PathVariable customerId: Long,
        @PageableDefault(size = OrderConstants.DEFAULT_PAGE_SIZE) pageable: Pageable
    ): ResponseEntity<Page<OrderResponse>> {
        logger.info("Retrieving orders for customer: {}", customerId)
        val response = orderService.getOrdersByCustomer(customerId, pageable)
        return ResponseEntity.ok(response)
    }
    
    @GetMapping("/merchant/{merchantId}")
    fun getOrdersByMerchant(
        @PathVariable merchantId: Long,
        @PageableDefault(size = OrderConstants.DEFAULT_PAGE_SIZE) pageable: Pageable
    ): ResponseEntity<Page<OrderResponse>> {
        logger.info("Retrieving orders for merchant: {}", merchantId)
        val response = orderService.getOrdersByMerchant(merchantId, pageable)
        return ResponseEntity.ok(response)
    }
    
    @PutMapping("/{orderNumber}/status")
    fun updateOrderStatus(
        @PathVariable orderNumber: String,
        @Valid @RequestBody request: UpdateOrderStatusRequest
    ): ResponseEntity<OrderStatusUpdateResponse> {
        logger.info("Updating status for order: {} to {}", orderNumber, request.newStatus)
        val response = orderService.updateOrderStatus(orderNumber, request)
        return ResponseEntity.ok(response)
    }
    
    @PutMapping("/{orderNumber}/cancel")
    fun cancelOrder(
        @PathVariable orderNumber: String,
        @RequestParam(required = false) reason: String?
    ): ResponseEntity<OrderStatusUpdateResponse> {
        logger.info("Cancelling order: {}", orderNumber)
        val response = orderService.cancelOrder(orderNumber, reason)
        return ResponseEntity.ok(response)
    }
    
    @GetMapping("/stats/daily-count")
    fun getDailyOrderCount(): ResponseEntity<Map<String, Any>> {
        val count = orderService.getDailyOrderCount()
        val response = mapOf(
            "date" to java.time.LocalDate.now().toString(),
            "orderCount" to count,
            "message" to "Daily order count retrieved successfully"
        )
        return ResponseEntity.ok(response)
    }
}
