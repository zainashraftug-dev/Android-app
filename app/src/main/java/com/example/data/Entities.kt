package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "menu_items")
data class MenuItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String, // e.g. "Burgers", "Pizzas", "Drinks", "Desserts"
    val price: Double,
    val isAvailable: Boolean = true,
    val hasIngredients: Boolean = false // If true, ordering will deduct ingredients from inventory in real-time
)

@Entity(tableName = "inventory_items")
data class InventoryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val currentStock: Double,
    val minStockLevel: Double,
    val unit: String // e.g., "pcs", "kg", "grams", "liters"
)

@Entity(tableName = "menu_item_ingredients")
data class MenuItemIngredient(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val menuItemId: Long,
    val inventoryItemId: Long,
    val quantityRequired: Double // e.g., 1.0 bun, 0.150 kg patty, etc.
)

@Entity(tableName = "orders")
data class Order(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderType: String, // "DINE_IN", "TAKE_AWAY", "CAR_SERVICE"
    val tableNumber: String? = null, // for Dine-In
    val carPlateNumber: String? = null, // for Car Service
    val status: String = "PENDING", // "PENDING", "PREPARING", "COMPLETED", "CANCELLED"
    val timestamp: Long = System.currentTimeMillis(),
    val subtotal: Double,
    val tax: Double,
    val totalAmount: Double
)

@Entity(tableName = "order_items")
data class OrderItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val menuItemId: Long,
    val name: String,
    val quantity: Int,
    val unitPrice: Double
)

@Entity(tableName = "inventory_transactions")
data class InventoryTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val inventoryItemId: Long,
    val inventoryItemName: String,
    val quantityChanged: Double, // negative for deduction, positive for addition
    val transactionType: String, // "DEDUCTION", "RESTOCK", "SPOILAGE", "ADJUSTMENT"
    val reason: String, // e.g., "Order #12" or "Manual Restock"
    val timestamp: Long = System.currentTimeMillis()
)
