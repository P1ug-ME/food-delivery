package com.ttbspark.food.delivery.domain

import com.ttbspark.food.delivery.enum.OrderStatus
import com.ttbspark.food.delivery.enum.PaymentMethod
import com.ttbspark.food.delivery.enum.PaymentStatus
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "orders")
data class Order(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(nullable = false, unique = true)
    val orderNumber: String,
    
    @Column(nullable = false)
    val customerId: Long,
    
    @Column(nullable = false)
    val merchantId: Long,
    
    @Column(nullable = false)
    val totalAmount: BigDecimal,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(50)")
    val paymentMethod: PaymentMethod,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(50)")
    val paymentStatus: PaymentStatus = PaymentStatus.PENDING,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(50)")
    val status: OrderStatus = OrderStatus.WAITING_FOR_CONFIRMATION,
    
    @Column(nullable = false)
    val deliveryAddress: String,
    
    @Column
    val specialInstructions: String? = null,
    
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    
    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val items: MutableList<OrderItem> = mutableListOf()
) {
    fun updateStatus(newStatus: OrderStatus): Order {
        require(status.canTransitionTo(newStatus)) {
            "Cannot transition from $status to $newStatus"
        }
        return copy(status = newStatus, updatedAt = LocalDateTime.now())
    }
    
    fun updatePaymentStatus(newPaymentStatus: PaymentStatus): Order {
        return copy(paymentStatus = newPaymentStatus, updatedAt = LocalDateTime.now())
    }
}

@Entity
@Table(name = "order_items")
data class OrderItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    val order: Order,
    
    @Column(nullable = false)
    val menuItemId: Long,
    
    @Column(nullable = false)
    val menuItemName: String,
    
    @Column(nullable = false)
    val quantity: Int,
    
    @Column(nullable = false)
    val unitPrice: BigDecimal,
    
    @Column(nullable = false)
    val totalPrice: BigDecimal,
    
    @Column
    val notes: String? = null
)
