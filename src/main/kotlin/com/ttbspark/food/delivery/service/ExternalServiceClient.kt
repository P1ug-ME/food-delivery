package com.ttbspark.food.delivery.service

import com.ttbspark.food.delivery.dto.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.time.Duration

@Service
class ExternalServiceClient(
    private val webClient: WebClient.Builder,
    @Value("\${services.payment.url:http://payment-service:8081}")
    private val paymentServiceUrl: String,
    @Value("\${services.notification.url:http://notification-service:8082}")
    private val notificationServiceUrl: String,
    @Value("\${services.inventory.url:http://inventory-service:8083}")
    private val inventoryServiceUrl: String
) {
    private val logger = LoggerFactory.getLogger(ExternalServiceClient::class.java)
    
    fun processPayment(request: PaymentProcessRequest): PaymentProcessResponse {
        logger.info("Processing payment for order: {}", request.orderNumber)
        
        return try {
            webClient.build()
                .post()
                .uri("$paymentServiceUrl/api/payments/process")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PaymentProcessResponse::class.java)
                .timeout(Duration.ofSeconds(30))
                .onErrorResume { error ->
                    logger.error("Payment processing failed for order: {}", request.orderNumber, error)
                    Mono.just(PaymentProcessResponse(
                        transactionId = "",
                        status = com.ttbspark.food.delivery.enum.PaymentStatus.FAILED,
                        message = "Payment service unavailable"
                    ))
                }
                .block() ?: PaymentProcessResponse(
                    transactionId = "",
                    status = com.ttbspark.food.delivery.enum.PaymentStatus.FAILED,
                    message = "No response from payment service"
                )
        } catch (e: Exception) {
            logger.error("Failed to call payment service for order: {}", request.orderNumber, e)
            PaymentProcessResponse(
                transactionId = "",
                status = com.ttbspark.food.delivery.enum.PaymentStatus.FAILED,
                message = "Payment service error: ${e.message}"
            )
        }
    }
    
    fun sendNotification(request: NotificationRequest) {
        logger.info("Sending notification to customer: {} for order: {}", request.customerId, request.orderNumber)
        
        try {
            webClient.build()
                .post()
                .uri("$notificationServiceUrl/api/notifications/send")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String::class.java)
                .timeout(Duration.ofSeconds(10))
                .onErrorResume { error ->
                    logger.warn("Notification failed for customer: {}", request.customerId, error)
                    Mono.just("Notification failed")
                }
                .subscribe()
        } catch (e: Exception) {
            logger.warn("Failed to send notification for customer: {}", request.customerId, e)
        }
    }
    
    fun reserveInventory(request: InventoryReservationRequest): InventoryReservationResponse {
        logger.info("Reserving inventory for merchant: {}", request.merchantId)
        
        return try {
            webClient.build()
                .post()
                .uri("$inventoryServiceUrl/api/inventory/reserve")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(InventoryReservationResponse::class.java)
                .timeout(Duration.ofSeconds(15))
                .onErrorResume { error ->
                    logger.error("Inventory reservation failed for merchant: {}", request.merchantId, error)
                    Mono.just(InventoryReservationResponse(
                        success = false,
                        reservationId = null,
                        unavailableItems = request.items.map { it.menuItemId }
                    ))
                }
                .block() ?: InventoryReservationResponse(
                    success = false,
                    reservationId = null,
                    unavailableItems = request.items.map { it.menuItemId }
                )
        } catch (e: Exception) {
            logger.error("Failed to call inventory service for merchant: {}", request.merchantId, e)
            InventoryReservationResponse(
                success = false,
                reservationId = null,
                unavailableItems = request.items.map { it.menuItemId }
            )
        }
    }
    
    // Health check endpoint for other services to call
    fun notifyOrderStatusChange(orderNumber: String, newStatus: String, targetServiceUrl: String) {
        logger.info("Notifying external service about order status change: {} -> {}", orderNumber, newStatus)
        
        try {
            val notificationPayload = mapOf(
                "orderNumber" to orderNumber,
                "newStatus" to newStatus,
                "timestamp" to System.currentTimeMillis()
            )
            
            webClient.build()
                .post()
                .uri("$targetServiceUrl/api/webhooks/order-status")
                .bodyValue(notificationPayload)
                .retrieve()
                .bodyToMono(String::class.java)
                .timeout(Duration.ofSeconds(10))
                .subscribe(
                    { response -> logger.debug("Status notification sent successfully: {}", response) },
                    { error -> logger.warn("Failed to send status notification", error) }
                )
        } catch (e: Exception) {
            logger.warn("Failed to notify external service about order status change", e)
        }
    }
}