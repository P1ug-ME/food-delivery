package com.ttbspark.food.delivery.service

import com.ttbspark.food.delivery.domain.Order
import com.ttbspark.food.delivery.domain.OrderItem
import com.ttbspark.food.delivery.enum.OrderStatus
import com.ttbspark.food.delivery.constant.OrderConstants
import com.ttbspark.food.delivery.dto.*
import com.ttbspark.food.delivery.exception.OrderNotFoundException
import com.ttbspark.food.delivery.exception.InvalidOrderStatusTransitionException
import com.ttbspark.food.delivery.repository.OrderRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional
class OrderService(
    private val orderRepository: OrderRepository,
    private val externalServiceClient: ExternalServiceClient
) {
    private val logger = LoggerFactory.getLogger(OrderService::class.java)
    
    fun createOrder(request: CreateOrderRequest): OrderResponse {
        logger.info("Creating order for customer: {} at merchant: {}", request.customerId, request.merchantId)
        
        // Generate unique order number
        val orderNumber = generateOrderNumber()
        
        // Calculate total amount
        val totalAmount = request.items.sumOf { it.unitPrice * it.quantity.toBigDecimal() }
        
        // Create order entity
        val order = Order(
            orderNumber = orderNumber,
            customerId = request.customerId,
            merchantId = request.merchantId,
            totalAmount = totalAmount,
            paymentMethod = request.paymentMethod,
            deliveryAddress = request.deliveryAddress,
            specialInstructions = request.specialInstructions
        )
        
        // Create order items
        val orderItems = request.items.map { item ->
            OrderItem(
                order = order,
                menuItemId = item.menuItemId,
                menuItemName = item.menuItemName,
                quantity = item.quantity,
                unitPrice = item.unitPrice,
                totalPrice = item.unitPrice * item.quantity.toBigDecimal(),
                notes = item.notes
            )
        }
        
        order.items.addAll(orderItems)
        
        // Save order
        val savedOrder = orderRepository.save(order)
        
        // Trigger external services asynchronously
        try {
            triggerExternalServices(savedOrder)
        } catch (e: Exception) {
            logger.warn("Failed to trigger external services for order: {}", orderNumber, e)
            // Continue execution - external services are not critical for order creation
        }
        
        logger.info("Order created successfully with number: {}", orderNumber)
        return mapToOrderResponse(savedOrder)
    }
    
    fun getOrder(orderNumber: String): OrderResponse {
        val order = orderRepository.findByOrderNumber(orderNumber)
            ?: throw OrderNotFoundException("Order not found with number: $orderNumber")
        return mapToOrderResponse(order)
    }
    
    fun getOrdersByCustomer(customerId: Long, pageable: Pageable): Page<OrderResponse> {
        return orderRepository.findByCustomerId(customerId, pageable)
            .map { mapToOrderResponse(it) }
    }
    
    fun getOrdersByMerchant(merchantId: Long, pageable: Pageable): Page<OrderResponse> {
        return orderRepository.findByMerchantId(merchantId, pageable)
            .map { mapToOrderResponse(it) }
    }
    
    fun updateOrderStatus(orderNumber: String, request: UpdateOrderStatusRequest): OrderStatusUpdateResponse {
        logger.info("Updating order status for: {} to {}", orderNumber, request.newStatus)
        
        val order = orderRepository.findByOrderNumber(orderNumber)
            ?: throw OrderNotFoundException("Order not found with number: $orderNumber")
        
        val previousStatus = order.status
        
        try {
            val updatedOrder = order.updateStatus(request.newStatus)
            orderRepository.save(updatedOrder)
            
            // Notify customer about status change
            notifyCustomer(updatedOrder, request.newStatus)
            
            logger.info("Order status updated successfully from {} to {} for order: {}", 
                previousStatus, request.newStatus, orderNumber)
            
            return OrderStatusUpdateResponse(
                orderNumber = orderNumber,
                previousStatus = previousStatus,
                newStatus = request.newStatus,
                updatedAt = updatedOrder.updatedAt,
                success = true,
                message = "Order status updated successfully"
            )
        } catch (e: IllegalArgumentException) {
            throw InvalidOrderStatusTransitionException(
                "Cannot transition order from $previousStatus to ${request.newStatus}: ${e.message}"
            )
        }
    }
    
    fun cancelOrder(orderNumber: String, reason: String?): OrderStatusUpdateResponse {
        return updateOrderStatus(orderNumber, UpdateOrderStatusRequest(OrderStatus.CANCELLED, reason))
    }
    
    @Transactional(readOnly = true)
    fun getDailyOrderCount(): Long {
        val startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay()
        return orderRepository.countOrdersSince(startOfDay)
    }
    
    private fun generateOrderNumber(): String {
        val timestamp = System.currentTimeMillis()
        val random = UUID.randomUUID().toString().take(8).uppercase()
        return "${OrderConstants.ORDER_NUMBER_PREFIX}$timestamp-$random"
    }
    
    private fun triggerExternalServices(order: Order) {
        // Check inventory reservation
        val inventoryRequest = InventoryReservationRequest(
            merchantId = order.merchantId,
            items = order.items.map { InventoryItem(it.menuItemId, it.quantity) }
        )
        externalServiceClient.reserveInventory(inventoryRequest)
        
        // Process payment
        val paymentRequest = PaymentProcessRequest(
            orderNumber = order.orderNumber,
            amount = order.totalAmount,
            paymentMethod = order.paymentMethod,
            customerId = order.customerId
        )
        externalServiceClient.processPayment(paymentRequest)
    }
    
    private fun notifyCustomer(order: Order, newStatus: OrderStatus) {
        val notification = NotificationRequest(
            customerId = order.customerId,
            orderNumber = order.orderNumber,
            message = "Your order status has been updated to: ${newStatus.displayName}",
            notificationType = "ORDER_STATUS_UPDATE"
        )
        externalServiceClient.sendNotification(notification)
    }
    
    private fun mapToOrderResponse(order: Order): OrderResponse {
        return OrderResponse(
            id = order.id!!,
            orderNumber = order.orderNumber,
            customerId = order.customerId,
            merchantId = order.merchantId,
            totalAmount = order.totalAmount,
            paymentMethod = order.paymentMethod,
            paymentStatus = order.paymentStatus,
            status = order.status,
            deliveryAddress = order.deliveryAddress,
            specialInstructions = order.specialInstructions,
            createdAt = order.createdAt,
            updatedAt = order.updatedAt,
            items = order.items.map { item ->
                OrderItemResponse(
                    id = item.id!!,
                    menuItemId = item.menuItemId,
                    menuItemName = item.menuItemName,
                    quantity = item.quantity,
                    unitPrice = item.unitPrice,
                    totalPrice = item.totalPrice,
                    notes = item.notes
                )
            }
        )
    }
}
