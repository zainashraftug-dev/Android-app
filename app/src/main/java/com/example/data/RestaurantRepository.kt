package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class RestaurantRepository(
    private val menuItemDao: MenuItemDao,
    private val inventoryDao: InventoryDao,
    private val orderDao: OrderDao
) {
    // Menu Items
    val allMenuItems: Flow<List<MenuItem>> = menuItemDao.getAllMenuItems()

    suspend fun getMenuItemById(id: Long): MenuItem? = menuItemDao.getMenuItemById(id)

    suspend fun insertMenuItem(item: MenuItem): Long = menuItemDao.insertMenuItem(item)

    suspend fun updateMenuItem(item: MenuItem) = menuItemDao.updateMenuItem(item)

    suspend fun deleteMenuItem(item: MenuItem) = menuItemDao.deleteMenuItem(item)

    // Inventory
    val allInventoryItems: Flow<List<InventoryItem>> = inventoryDao.getAllInventoryItems()

    suspend fun getInventoryItemById(id: Long): InventoryItem? = inventoryDao.getInventoryItemById(id)

    suspend fun insertInventoryItem(item: InventoryItem): Long = inventoryDao.insertInventoryItem(item)

    suspend fun updateInventoryItem(item: InventoryItem) = inventoryDao.updateInventoryItem(item)

    suspend fun deleteInventoryItem(item: InventoryItem) = inventoryDao.deleteInventoryItem(item)

    // Ingredients
    suspend fun getIngredientsForMenuItem(menuItemId: Long): List<MenuItemIngredient> =
        inventoryDao.getIngredientsForMenuItem(menuItemId)

    fun getIngredientsForMenuItemFlow(menuItemId: Long): Flow<List<MenuItemIngredient>> =
        inventoryDao.getIngredientsForMenuItemFlow(menuItemId)

    suspend fun insertMenuItemIngredient(ingredient: MenuItemIngredient) =
        inventoryDao.insertMenuItemIngredient(ingredient)

    suspend fun deleteIngredientsForMenuItem(menuItemId: Long) =
        inventoryDao.deleteIngredientsForMenuItem(menuItemId)

    // Transactions
    val allTransactions: Flow<List<InventoryTransaction>> = inventoryDao.getAllTransactions()

    suspend fun insertTransaction(transaction: InventoryTransaction) =
        inventoryDao.insertTransaction(transaction)

    // Orders
    val allOrders: Flow<List<Order>> = orderDao.getAllOrders()

    suspend fun getOrderById(id: Long): Order? = orderDao.getOrderById(id)

    fun getOrderItems(orderId: Long): Flow<List<OrderItem>> = orderDao.getOrderItems(orderId)

    suspend fun getOrderItemsDirect(orderId: Long): List<OrderItem> = orderDao.getOrderItemsDirect(orderId)

    suspend fun updateOrder(order: Order) = orderDao.updateOrder(order)

    /**
     * Transactional placing of an order that:
     * 1. Creates the Order and retrieves the generated ID.
     * 2. Inserts all Order Items.
     * 3. Subtracts recipe ingredient quantities from inventory in real-time,
     *    and records transactional logs.
     */
    suspend fun placeOrder(
        orderType: String,
        tableNumber: String?,
        carPlateNumber: String?,
        items: List<Pair<MenuItem, Int>>, // List of (MenuItem, Quantity)
        subtotal: Double,
        tax: Double,
        totalAmount: Double
    ): Long {
        // 1. Insert Order
        val order = Order(
            orderType = orderType,
            tableNumber = if (orderType == "DINE_IN") tableNumber else null,
            carPlateNumber = if (orderType == "CAR_SERVICE") carPlateNumber else null,
            status = "PENDING",
            subtotal = subtotal,
            tax = tax,
            totalAmount = totalAmount
        )
        val orderId = orderDao.insertOrder(order)

        // 2. Insert Order Items and subtract inventory
        for ((menuItem, quantity) in items) {
            val orderItem = OrderItem(
                orderId = orderId,
                menuItemId = menuItem.id,
                name = menuItem.name,
                quantity = quantity,
                unitPrice = menuItem.price
            )
            orderDao.insertOrderItem(orderItem)

            // Subtract inventory in real-time
            if (menuItem.hasIngredients) {
                val ingredients = inventoryDao.getIngredientsForMenuItem(menuItem.id)
                for (ingredient in ingredients) {
                    val inventoryItem = inventoryDao.getInventoryItemById(ingredient.inventoryItemId)
                    if (inventoryItem != null) {
                        val deduction = ingredient.quantityRequired * quantity
                        val newStock = inventoryItem.currentStock - deduction
                        
                        // Update inventory
                        inventoryDao.updateInventoryItem(inventoryItem.copy(currentStock = newStock))

                        // Record transaction
                        val transaction = InventoryTransaction(
                            inventoryItemId = inventoryItem.id,
                            inventoryItemName = inventoryItem.name,
                            quantityChanged = -deduction,
                            transactionType = "DEDUCTION",
                            reason = "Order #$orderId - ${menuItem.name} (x$quantity)"
                        )
                        inventoryDao.insertTransaction(transaction)
                    }
                }
            }
        }
        return orderId
    }

    /**
     * Restocks an inventory item manually.
     */
    suspend fun restockItem(itemId: Long, quantity: Double, cost: Double = 0.0) {
        val item = inventoryDao.getInventoryItemById(itemId)
        if (item != null) {
            val newStock = item.currentStock + quantity
            inventoryDao.updateInventoryItem(item.copy(currentStock = newStock))
            inventoryDao.insertTransaction(
                InventoryTransaction(
                    inventoryItemId = item.id,
                    inventoryItemName = item.name,
                    quantityChanged = quantity,
                    transactionType = "RESTOCK",
                    reason = "Manual Restock"
                )
            )
        }
    }
}
