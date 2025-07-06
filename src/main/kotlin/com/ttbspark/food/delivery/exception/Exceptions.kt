package com.ttbspark.food.delivery.exception

class OrderNotFoundException(message: String) : RuntimeException(message)

class InvalidOrderStatusTransitionException(message: String) : RuntimeException(message)

class PaymentProcessingException(message: String) : RuntimeException(message)

class InventoryReservationException(message: String) : RuntimeException(message)