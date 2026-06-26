package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MenuItemDao {
    @Query("SELECT * FROM menu_items ORDER BY name ASC")
    fun getAllMenuItems(): Flow<List<MenuItem>>

    @Query("SELECT * FROM menu_items WHERE id = :id")
    suspend fun getMenuItemById(id: Long): MenuItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenuItem(item: MenuItem): Long

    @Update
    suspend fun updateMenuItem(item: MenuItem)

    @Delete
    suspend fun deleteMenuItem(item: MenuItem)
}

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory_items ORDER BY name ASC")
    fun getAllInventoryItems(): Flow<List<InventoryItem>>

    @Query("SELECT * FROM inventory_items WHERE id = :id")
    suspend fun getInventoryItemById(id: Long): InventoryItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryItem(item: InventoryItem): Long

    @Update
    suspend fun updateInventoryItem(item: InventoryItem)

    @Delete
    suspend fun deleteInventoryItem(item: InventoryItem)

    // Ingredients mapping
    @Query("SELECT * FROM menu_item_ingredients WHERE menuItemId = :menuItemId")
    suspend fun getIngredientsForMenuItem(menuItemId: Long): List<MenuItemIngredient>

    @Query("SELECT * FROM menu_item_ingredients WHERE menuItemId = :menuItemId")
    fun getIngredientsForMenuItemFlow(menuItemId: Long): Flow<List<MenuItemIngredient>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenuItemIngredient(ingredient: MenuItemIngredient)

    @Query("DELETE FROM menu_item_ingredients WHERE menuItemId = :menuItemId")
    suspend fun deleteIngredientsForMenuItem(menuItemId: Long)

    // Transactions
    @Query("SELECT * FROM inventory_transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<InventoryTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: InventoryTransaction)
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY timestamp DESC")
    fun getAllOrders(): Flow<List<Order>>

    @Query("SELECT * FROM orders WHERE id = :id")
    suspend fun getOrderById(id: Long): Order?

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    fun getOrderItems(orderId: Long): Flow<List<OrderItem>>

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    suspend fun getOrderItemsDirect(orderId: Long): List<OrderItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: Order): Long

    @Update
    suspend fun updateOrder(order: Order)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItem(item: OrderItem)
}
