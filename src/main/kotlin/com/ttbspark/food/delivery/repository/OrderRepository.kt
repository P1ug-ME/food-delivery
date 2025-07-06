package com.ttbspark.food.delivery.repository

import com.ttbspark.food.delivery.domain.Order
import com.ttbspark.food.delivery.enum.OrderStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface OrderRepository : JpaRepository<Order, Long> {
    
    fun findByOrderNumber(orderNumber: String): Order?
    
    fun findByCustomerId(customerId: Long, pageable: Pageable): Page<Order>
    
    fun findByMerchantId(merchantId: Long, pageable: Pageable): Page<Order>
    
    fun findByStatus(status: OrderStatus, pageable: Pageable): Page<Order>
    
    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate")
    fun findByCreatedAtBetween(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime,
        pageable: Pageable
    ): Page<Order>
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :date")
    fun countOrdersSince(@Param("date") date: LocalDateTime): Long
}
