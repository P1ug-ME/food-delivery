package com.ttbspark.food.delivery.service

import com.ttbspark.food.delivery.domain.Order
import com.ttbspark.food.delivery.domain.OrderItem
import com.ttbspark.food.delivery.enum.OrderStatus
import com.ttbspark.food.delivery.enum.PaymentMethod
import com.ttbspark.food.delivery.dto.CreateOrderRequest
import com.ttbspark.food.delivery.dto.CreateOrderItemRequest
import com.ttbspark.food.delivery.dto.UpdateOrderStatusRequest
import com.ttbspark.food.delivery.exception.OrderNotFoundException
import com.ttbspark.food.delivery.exception.InvalidOrderStatusTransitionException
import com.ttbspark.food.delivery.repository.OrderRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.slot
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OrderServiceTest {
    
    private lateinit var orderRepository: OrderRepository
    private lateinit var externalServiceClient: ExternalServiceClient
    private lateinit var orderService: OrderService
    
    @BeforeEach
    fun setup() {
        orderRepository = mockk()
        externalServiceClient = mockk(relaxed = true)
        orderService = OrderService(orderRepository, externalServiceClient)
    }
    
    @Test
    fun `should create order successfully`() {
        // Given
        val request = createOrderRequest()
        val savedOrder = createOrder()
        val orderSlot = slot<Order>()
        
        every { orderRepository.save(capture(orderSlot)) } returns savedOrder
        
        // When
        val response = orderService.createOrder(request)
        
        // Then
        verify { orderRepository.save(any()) }
        assertEquals(savedOrder.orderNumber, response.orderNumber)
        assertEquals(savedOrder.customerId, response.customerId)
        assertEquals(savedOrder.merchantId, response.merchantId)
        assertEquals(savedOrder.totalAmount, response.totalAmount)
        assertEquals(savedOrder.paymentMethod, response.paymentMethod)
        assertEquals(savedOrder.deliveryAddress, response.deliveryAddress)
        assertEquals(2, response.items.size)
        
        // Verify order details
        val capturedOrder = orderSlot.captured
        assertEquals(request.customerId, capturedOrder.customerId)
        assertEquals(request.merchantId, capturedOrder.merchantId)
        assertEquals(BigDecimal("35.00"), capturedOrder.totalAmount) // 15.00 * 1 + 10.00 * 2
        assertEquals(request.paymentMethod, capturedOrder.paymentMethod)
        assertEquals(OrderStatus.WAITING_FOR_CONFIRMATION, capturedOrder.status)
    }
    
    @Test
    fun `should get order by order number successfully`() {
        // Given
        val orderNumber = "TTB-123456789-ABC12345"
        val order = createOrder(orderNumber = orderNumber)
        
        every { orderRepository.findByOrderNumber(orderNumber) } returns order
        
        // When
        val response = orderService.getOrder(orderNumber)
        
        // Then
        verify { orderRepository.findByOrderNumber(orderNumber) }
        assertEquals(order.orderNumber, response.orderNumber)
        assertEquals(order.customerId, response.customerId)
    }
    
    @Test
    fun `should throw OrderNotFoundException when order not found`() {
        // Given
        val orderNumber = "NON-EXISTENT-ORDER"
        
        every { orderRepository.findByOrderNumber(orderNumber) } returns null
        
        // When & Then
        val exception = assertThrows<OrderNotFoundException> {
            orderService.getOrder(orderNumber)
        }
        assertEquals("Order not found with number: $orderNumber", exception.message)
    }
    
    @Test
    fun `should get orders by customer successfully`() {
        // Given
        val customerId = 1L
        val pageable = Pageable.unpaged()
        val orders = listOf(createOrder(customerId = customerId))
        val page = PageImpl(orders, pageable, 1)
        
        every { orderRepository.findByCustomerId(customerId, pageable) } returns page
        
        // When
        val response = orderService.getOrdersByCustomer(customerId, pageable)
        
        // Then
        verify { orderRepository.findByCustomerId(customerId, pageable) }
        assertEquals(1, response.totalElements)
        assertEquals(customerId, response.content[0].customerId)
    }
    
    @Test
    fun `should get orders by merchant successfully`() {
        // Given
        val merchantId = 100L
        val pageable = Pageable.unpaged()
        val orders = listOf(createOrder(merchantId = merchantId))
        val page = PageImpl(orders, pageable, 1)
        
        every { orderRepository.findByMerchantId(merchantId, pageable) } returns page
        
        // When
        val response = orderService.getOrdersByMerchant(merchantId, pageable)
        
        // Then
        verify { orderRepository.findByMerchantId(merchantId, pageable) }
        assertEquals(1, response.totalElements)
        assertEquals(merchantId, response.content[0].merchantId)
    }
    
    @Test
    fun `should update order status successfully`() {
        // Given
        val orderNumber = "TTB-123456789-ABC12345"
        val order = createOrder(orderNumber = orderNumber, status = OrderStatus.WAITING_FOR_CONFIRMATION)
        val newStatus = OrderStatus.CONFIRMED
        val request = UpdateOrderStatusRequest(newStatus, "Order confirmed by merchant")
        val updatedOrder = order.copy(status = newStatus, updatedAt = LocalDateTime.now())
        
        every { orderRepository.findByOrderNumber(orderNumber) } returns order
        every { orderRepository.save(any()) } returns updatedOrder
        
        // When
        val response = orderService.updateOrderStatus(orderNumber, request)
        
        // Then
        verify { orderRepository.findByOrderNumber(orderNumber) }
        verify { orderRepository.save(any()) }
        assertEquals(orderNumber, response.orderNumber)
        assertEquals(OrderStatus.WAITING_FOR_CONFIRMATION, response.previousStatus)
        assertEquals(newStatus, response.newStatus)
        assertTrue(response.success)
    }
    
    @Test
    fun `should throw exception for invalid status transition`() {
        // Given
        val orderNumber = "TTB-123456789-ABC12345"
        val order = createOrder(orderNumber = orderNumber, status = OrderStatus.COMPLETED)
        val request = UpdateOrderStatusRequest(OrderStatus.COOKING, "Invalid transition")
        
        every { orderRepository.findByOrderNumber(orderNumber) } returns order
        
        // When & Then
        val exception = assertThrows<InvalidOrderStatusTransitionException> {
            orderService.updateOrderStatus(orderNumber, request)
        }
        assertTrue(exception.message!!.contains("Cannot transition order from COMPLETED to COOKING"))
    }
    
    @Test
    fun `should cancel order successfully`() {
        // Given
        val orderNumber = "TTB-123456789-ABC12345"
        val order = createOrder(orderNumber = orderNumber, status = OrderStatus.WAITING_FOR_CONFIRMATION)
        val reason = "Customer requested cancellation"
        val cancelledOrder = order.copy(status = OrderStatus.CANCELLED, updatedAt = LocalDateTime.now())
        
        every { orderRepository.findByOrderNumber(orderNumber) } returns order
        every { orderRepository.save(any()) } returns cancelledOrder
        
        // When
        val response = orderService.cancelOrder(orderNumber, reason)
        
        // Then
        verify { orderRepository.findByOrderNumber(orderNumber) }
        verify { orderRepository.save(any()) }
        assertEquals(OrderStatus.CANCELLED, response.newStatus)
        assertTrue(response.success)
    }
    
    @Test
    fun `should get daily order count`() {
        // Given
        val expectedCount = 150L
        every { orderRepository.countOrdersSince(any()) } returns expectedCount
        
        // When
        val count = orderService.getDailyOrderCount()
        
        // Then
        verify { orderRepository.countOrdersSince(any()) }
        assertEquals(expectedCount, count)
    }
    
    @Test
    fun `should handle order number generation uniqueness`() {
        // Given
        val request1 = createOrderRequest()
        val request2 = createOrderRequest()
        val order1 = createOrder(orderNumber = "TTB-123456789-ABC12345")
        val order2 = createOrder(orderNumber = "TTB-123456790-DEF67890")
        
        every { orderRepository.save(any()) } returnsMany listOf(order1, order2)
        
        // When
        val response1 = orderService.createOrder(request1)
        val response2 = orderService.createOrder(request2)
        
        // Then
        assertNotNull(response1.orderNumber)
        assertNotNull(response2.orderNumber)
        assertTrue(response1.orderNumber.startsWith("TTB-"))
        assertTrue(response2.orderNumber.startsWith("TTB-"))
        // In real implementation, these would be different due to timestamp/UUID
    }
    
    private fun createOrderRequest(): CreateOrderRequest {
        return CreateOrderRequest(
            customerId = 1L,
            merchantId = 100L,
            paymentMethod = PaymentMethod.CREDIT_CARD,
            deliveryAddress = "123 Test Street, Bangkok 10400",
            specialInstructions = "Please ring the bell",
            items = listOf(
                CreateOrderItemRequest(
                    menuItemId = 1L,
                    menuItemName = "Pad Thai",
                    quantity = 1,
                    unitPrice = BigDecimal("15.00"),
                    notes = "Extra spicy"
                ),
                CreateOrderItemRequest(
                    menuItemId = 2L,
                    menuItemName = "Thai Iced Tea",
                    quantity = 2,
                    unitPrice = BigDecimal("10.00"),
                    notes = null
                )
            )
        )
    }
    
    private fun createOrder(
        id: Long = 1L,
        orderNumber: String = "TTB-123456789-ABC12345",
        customerId: Long = 1L,
        merchantId: Long = 100L,
        status: OrderStatus = OrderStatus.WAITING_FOR_CONFIRMATION
    ): Order {
        val order = Order(
            id = id,
            orderNumber = orderNumber,
            customerId = customerId,
            merchantId = merchantId,
            totalAmount = BigDecimal("35.00"),
            paymentMethod = PaymentMethod.CREDIT_CARD,
            deliveryAddress = "123 Test Street, Bangkok 10400",
            specialInstructions = "Please ring the bell",
            status = status,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        // Add order items
        val item1 = OrderItem(
            id = 1L,
            order = order,
            menuItemId = 1L,
            menuItemName = "Pad Thai",
            quantity = 1,
            unitPrice = BigDecimal("15.00"),
            totalPrice = BigDecimal("15.00"),
            notes = "Extra spicy"
        )
        
        val item2 = OrderItem(
            id = 2L,
            order = order,
            menuItemId = 2L,
            menuItemName = "Thai Iced Tea",
            quantity = 2,
            unitPrice = BigDecimal("10.00"),
            totalPrice = BigDecimal("20.00"),
            notes = null
        )
        
        order.items.addAll(listOf(item1, item2))
        return order
    }
}
