package com.ttbspark.food.delivery.enum

enum class OrderStatus(val displayName: String) {
    WAITING_FOR_CONFIRMATION("Waiting for order confirmation"),
    CONFIRMED("Order confirmed"),
    COOKING("Cooking"),
    READY_FOR_DELIVERY("Ready for delivery"),
    DELIVERING("Delivering"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");
    
    fun canTransitionTo(newStatus: OrderStatus): Boolean {
        return when (this) {
            WAITING_FOR_CONFIRMATION -> newStatus in listOf(CONFIRMED, CANCELLED)
            CONFIRMED -> newStatus in listOf(COOKING, CANCELLED)
            COOKING -> newStatus in listOf(READY_FOR_DELIVERY, CANCELLED)
            READY_FOR_DELIVERY -> newStatus in listOf(DELIVERING, CANCELLED)
            DELIVERING -> newStatus in listOf(COMPLETED, CANCELLED)
            COMPLETED -> false // No transitions from completed
            CANCELLED -> false // No transitions from cancelled
        }
    }
}
