package com.ttbspark.food.delivery.handler

import com.ttbspark.food.delivery.exception.OrderNotFoundException
import com.ttbspark.food.delivery.exception.InvalidOrderStatusTransitionException
import com.ttbspark.food.delivery.exception.PaymentProcessingException
import com.ttbspark.food.delivery.exception.InventoryReservationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import java.time.LocalDateTime

@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    
    @ExceptionHandler(OrderNotFoundException::class)
    fun handleOrderNotFoundException(
        ex: OrderNotFoundException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Order not found: {}", ex.message)
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.NOT_FOUND.value(),
            error = "Order Not Found",
            message = ex.message ?: "Order not found",
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse)
    }
    
    @ExceptionHandler(InvalidOrderStatusTransitionException::class)
    fun handleInvalidOrderStatusTransition(
        ex: InvalidOrderStatusTransitionException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Invalid order status transition: {}", ex.message)
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Invalid Status Transition",
            message = ex.message ?: "Invalid order status transition",
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }
    
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(
        ex: MethodArgumentNotValidException,
        request: WebRequest
    ): ResponseEntity<ValidationErrorResponse> {
        logger.warn("Validation failed: {}", ex.message)
        
        val fieldErrors = mutableMapOf<String, String>()
        ex.bindingResult.allErrors.forEach { error ->
            val fieldName = (error as FieldError).field
            val errorMessage = error.defaultMessage ?: "Invalid value"
            fieldErrors[fieldName] = errorMessage
        }
        
        val errorResponse = ValidationErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Validation Failed",
            message = "Request validation failed",
            path = request.getDescription(false).removePrefix("uri="),
            fieldErrors = fieldErrors
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }
    
    @ExceptionHandler(PaymentProcessingException::class)
    fun handlePaymentProcessingException(
        ex: PaymentProcessingException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Payment processing failed: {}", ex.message)
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.PAYMENT_REQUIRED.value(),
            error = "Payment Processing Failed",
            message = ex.message ?: "Payment processing failed",
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(errorResponse)
    }
    
    @ExceptionHandler(InventoryReservationException::class)
    fun handleInventoryReservationException(
        ex: InventoryReservationException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Inventory reservation failed: {}", ex.message)
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.CONFLICT.value(),
            error = "Inventory Reservation Failed",
            message = ex.message ?: "Inventory reservation failed",
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse)
    }
    
    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected error occurred", ex)
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Internal Server Error",
            message = "An unexpected error occurred",
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }
}