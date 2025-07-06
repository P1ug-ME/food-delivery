package com.ttbspark.food.delivery.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import com.ttbspark.food.delivery.enum.OrderStatus
import com.ttbspark.food.delivery.enum.PaymentMethod
import com.ttbspark.food.delivery.enum.PaymentStatus
import com.ttbspark.food.delivery.dto.*
import com.ttbspark.food.delivery.exception.OrderNotFoundException
import com.ttbspark.food.delivery.service.OrderService
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.math.BigDecimal
import java.time.LocalDateTime

@WebMvcTest(OrderController::class)
class OrderControllerTest {
    
    @Autowired
    private lateinit var mockMvc: MockMvc
    
    @Autowired
    private lateinit var objectMapper: ObjectMapper
    
    @MockkBean
    private lateinit var orderService: OrderService
    
    @Test
    fun `should create order successfully`() {
        // Given
        val request = createOrderRequest()
        val response = createOrderResponse()
        
        every { orderService.createOrder(request) } returns response
        
        // When & Then
        mockMvc.perform(
            post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.orderNumber").value(response.orderNumber))
            .andExpect(jsonPath("$.customerId").value(response.customerId))
            .andExpect(jsonPath("$.status").value(response.status.name))
        
        verify { orderService.createOrder(request) }
    }
    
    @Test
    fun `should get order successfully`() {
        // Given
        val orderNumber = "TTB-123456789-ABC12345"
        val response = createOrderResponse(orderNumber = orderNumber)
        
        every { orderService.getOrder(orderNumber) } returns response
        
        // When & Then
        mockMvc.perform(get("/api/orders/$orderNumber"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.orderNumber").value(orderNumber))
        
        verify { orderService.getOrder(orderNumber) }
    }
    
    @Test
    fun `should return 404 when order not found`() {
        // Given
        val orderNumber = "NON-EXISTENT"
        
        every { orderService.getOrder(orderNumber) } throws OrderNotFoundException("Order not found")
        
        // When & Then
        mockMvc.perform(get("/api/orders/$orderNumber"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error").value("Order Not Found"))
        
        verify { orderService.getOrder(orderNumber) }
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
                )
            )
        )
    }
    
    private fun createOrderResponse(orderNumber: String = "TTB-123456789-ABC12345"): OrderResponse {
        return OrderResponse(
            id = 1L,
            orderNumber = orderNumber,
            customerId = 1L,
            merchantId = 100L,
            totalAmount = BigDecimal("15.00"),
            paymentMethod = PaymentMethod.CREDIT_CARD,
            paymentStatus = PaymentStatus.PENDING,
            status = OrderStatus.WAITING_FOR_CONFIRMATION,
            deliveryAddress = "123 Test Street, Bangkok 10400",
            specialInstructions = "Please ring the bell",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            items = listOf(
                OrderItemResponse(
                    id = 1L,
                    menuItemId = 1L,
                    menuItemName = "Pad Thai",
                    quantity = 1,
                    unitPrice = BigDecimal("15.00"),
                    totalPrice = BigDecimal("15.00"),
                    notes = "Extra spicy"
                )
            )
        )
    }
}
