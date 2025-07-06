package com.ttbspark.food.delivery.dto

import com.ttbspark.food.delivery.enum.OrderStatus
import com.ttbspark.food.delivery.enum.PaymentMethod
import com.ttbspark.food.delivery.enum.PaymentStatus
import jakarta.validation.Valid
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.LocalDateTime

data class CreateOrderRequest(
    @field:NotNull(message = "Customer ID is required")
    @field:Positive(message = "Customer ID must be positive")
    val customerId: Long,
    
    @field:NotNull(message = "Merchant ID is required")
    @field:Positive(message = "Merchant ID must be positive")
    val merchantId: Long,
    
    @field:NotNull(message = "Payment method is required")
    val paymentMethod: PaymentMethod,
    
    @field:NotBlank(message = "Delivery address is required")
    @field:Size(max = 500, message = "Delivery address must not exceed 500 characters")
    val deliveryAddress: String,
    
    @field:Size(max = 1000, message = "Special instructions must not exceed 1000 characters")
    val specialInstructions: String? = null,
    
    @field:NotEmpty(message = "Order items are required")
    @field:Valid
    val items: List<CreateOrderItemRequest>
)

data class CreateOrderItemRequest(
    @field:NotNull(message = "Menu item ID is required")
    @field:Positive(message = "Menu item ID must be positive")
    val menuItemId: Long,
    
    @field:NotBlank(message = "Menu item name is required")
    @field:Size(max = 200, message = "Menu item name must not exceed 200 characters")
    val menuItemName: String,
    
    @field:NotNull(message = "Quantity is required")
    @field:Positive(message = "Quantity must be positive")
    val quantity: Int,
    
    @field:NotNull(message = "Unit price is required")
    @field:DecimalMin(value = "0.01", message = "Unit price must be positive")
    val unitPrice: BigDecimal,
    
    @field:Size(max = 500, message = "Notes must not exceed 500 characters")
    val notes: String? = null
)

data class UpdateOrderStatusRequest(
    @field:NotNull(message = "New status is required")
    val newStatus: OrderStatus,
    
    @field:Size(max = 500, message = "Reason must not exceed 500 characters")
    val reason: String? = null
)

data class OrderResponse(
    val id: Long,
    val orderNumber: String,
    val customerId: Long,
    val merchantId: Long,
    val totalAmount: BigDecimal,
    val paymentMethod: PaymentMethod,
    val paymentStatus: PaymentStatus,
    val status: OrderStatus,
    val deliveryAddress: String,
    val specialInstructions: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val items: List<OrderItemResponse>
)

data class OrderItemResponse(
    val id: Long,
    val menuItemId: Long,
    val menuItemName: String,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val totalPrice: BigDecimal,
    val notes: String?
)

data class OrderStatusUpdateResponse(
    val orderNumber: String,
    val previousStatus: OrderStatus,
    val newStatus: OrderStatus,
    val updatedAt: LocalDateTime,
    val success: Boolean,
    val message: String
)

// External service DTOs for inter-service communication
data class PaymentProcessRequest(
    val orderNumber: String,
    val amount: BigDecimal,
    val paymentMethod: PaymentMethod,
    val customerId: Long
)

data class PaymentProcessResponse(
    val transactionId: String,
    val status: PaymentStatus,
    val message: String
)

data class NotificationRequest(
    val customerId: Long,
    val orderNumber: String,
    val message: String,
    val notificationType: String
)

data class InventoryReservationRequest(
    val merchantId: Long,
    val items: List<InventoryItem>
)

data class InventoryItem(
    val menuItemId: Long,
    val quantity: Int
)

data class InventoryReservationResponse(
    val success: Boolean,
    val reservationId: String?,
    val unavailableItems: List<Long>
)
