package com.ttbspark.food.delivery.controller

import com.ttbspark.food.delivery.service.OrderService
import com.ttbspark.food.delivery.constant.ApiConstants
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping(ApiConstants.WEBHOOK_BASE_PATH)
class WebhookController(
    private val orderService: OrderService
) {
    private val logger = LoggerFactory.getLogger(WebhookController::class.java)
    
    @PostMapping("/payment-status")
    fun handlePaymentStatusUpdate(@RequestBody payload: Map<String, Any>): ResponseEntity<String> {
        val orderNumber = payload["orderNumber"] as? String
        val paymentStatus = payload["paymentStatus"] as? String
        
        logger.info("Received payment status update for order: {} -> {}", orderNumber, paymentStatus)
        
        // Process payment status update logic here
        // This would update the order's payment status
        
        return ResponseEntity.ok("Payment status updated successfully")
    }
    
    @PostMapping("/delivery-status")
    fun handleDeliveryStatusUpdate(@RequestBody payload: Map<String, Any>): ResponseEntity<String> {
        val orderNumber = payload["orderNumber"] as? String
        val deliveryStatus = payload["deliveryStatus"] as? String
        
        logger.info("Received delivery status update for order: {} -> {}", orderNumber, deliveryStatus)
        
        // Process delivery status update logic here
        // This would update the order status based on delivery progress
        
        return ResponseEntity.ok("Delivery status updated successfully")
    }
}