package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        MenuItem::class,
        InventoryItem::class,
        MenuItemIngredient::class,
        Order::class,
        OrderItem::class,
        InventoryTransaction::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun menuItemDao(): MenuItemDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun orderDao(): OrderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "restaurant_database"
                )
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateDatabase(database)
                    }
                }
            }
        }

        suspend fun populateDatabase(database: AppDatabase) {
            val menuItemDao = database.menuItemDao()
            val inventoryDao = database.inventoryDao()

            // 1. Populate initial stock/ingredients
            val bunId = inventoryDao.insertInventoryItem(InventoryItem(name = "Burger Buns", currentStock = 100.0, minStockLevel = 20.0, unit = "pcs"))
            val beefId = inventoryDao.insertInventoryItem(InventoryItem(name = "Beef Patties", currentStock = 80.0, minStockLevel = 15.0, unit = "pcs"))
            val chickenId = inventoryDao.insertInventoryItem(InventoryItem(name = "Chicken Patties", currentStock = 60.0, minStockLevel = 10.0, unit = "pcs"))
            val cheeseId = inventoryDao.insertInventoryItem(InventoryItem(name = "Cheese Slices", currentStock = 150.0, minStockLevel = 30.0, unit = "pcs"))
            val lettuceId = inventoryDao.insertInventoryItem(InventoryItem(name = "Lettuce Leaves", currentStock = 200.0, minStockLevel = 40.0, unit = "pcs"))
            val tomatoId = inventoryDao.insertInventoryItem(InventoryItem(name = "Tomato Slices", currentStock = 250.0, minStockLevel = 50.0, unit = "pcs"))
            val doughId = inventoryDao.insertInventoryItem(InventoryItem(name = "Pizza Dough", currentStock = 50.0, minStockLevel = 10.0, unit = "pcs"))
            val mozId = inventoryDao.insertInventoryItem(InventoryItem(name = "Mozzarella Cheese", currentStock = 10.0, minStockLevel = 2.0, unit = "kg"))
            val sauceId = inventoryDao.insertInventoryItem(InventoryItem(name = "Tomato Sauce", currentStock = 15.0, minStockLevel = 3.0, unit = "liters"))
            val milkId = inventoryDao.insertInventoryItem(InventoryItem(name = "Whole Milk", currentStock = 24.0, minStockLevel = 6.0, unit = "liters"))
            val coffeeId = inventoryDao.insertInventoryItem(InventoryItem(name = "Coffee Beans", currentStock = 5.0, minStockLevel = 1.0, unit = "kg"))
            val cokeId = inventoryDao.insertInventoryItem(InventoryItem(name = "Coke Cans", currentStock = 120.0, minStockLevel = 24.0, unit = "pcs"))

            // 2. Populate menu items and map recipes
            val burger1Id = menuItemDao.insertMenuItem(MenuItem(name = "Classic Cheeseburger", category = "Burgers", price = 8.99, hasIngredients = true))
            inventoryDao.insertMenuItemIngredient(MenuItemIngredient(menuItemId = burger1Id, inventoryItemId = bunId, quantityRequired = 1.0))
            inventoryDao.insertMenuItemIngredient(MenuItemIngredient(menuItemId = burger1Id, inventoryItemId = beefId, quantityRequired = 1.0))
            inventoryDao.insertMenuItemIngredient(MenuItemIngredient(menuItemId = burger1Id, inventoryItemId = cheeseId, quantityRequired = 1.0))
            inventoryDao.insertMenuItemIngredient(MenuItemIngredient(menuItemId = burger1Id, inventoryItemId = lettuceId, quantityRequired = 2.0))
            inventoryDao.insertMenuItemIngredient(MenuItemIngredient(menuItemId = burger1Id, inventoryItemId = tomatoId, quantityRequired = 1.0))

            val burger2Id = menuItemDao.insertMenuItem(MenuItem(name = "Crispy Chicken Burger", category = "Burgers", price = 7.99, hasIngredients = true))
            inventoryDao.insertMenuItemIngredient(MenuItemIngredient(menuItemId = burger2Id, inventoryItemId = bunId, quantityRequired = 1.0))
            inventoryDao.insertMenuItemIngredient(MenuItemIngredient(menuItemId = burger2Id, inventoryItemId = chickenId, quantityRequired = 1.0))
            inventoryDao.insertMenuItemIngredient(MenuItemIngredient(menuItemId = burger2Id, inventoryItemId = lettuceId, quantityRequired = 2.0))
            inventoryDao.insertMenuItemIngredient(MenuItemIngredient(menuItemId = burger2Id, inventoryItemId = tomatoId, quantityRequired = 1.0))

            val pizza1Id = menuItemDao.insertMenuItem(MenuItem(name = "Margherita Pizza", category = "Pizzas", price = 11.99, hasIngredients = true))
            inventoryDao.insertMenuItemIngredient(MenuItemIngredient(menuItemId = pizza1Id, inventoryItemId = doughId, quantityRequired = 1.0))
            inventoryDao.insertMenuItemIngredient(MenuItemIngredient(menuItemId = pizza1Id, inventoryItemId = mozId, quantityRequired = 0.150))
            inventoryDao.insertMenuItemIngredient(MenuItemIngredient(menuItemId = pizza1Id, inventoryItemId = sauceId, quantityRequired = 0.100))

            val drink1Id = menuItemDao.insertMenuItem(MenuItem(name = "Hot Latte", category = "Drinks", price = 3.99, hasIngredients = true))
            inventoryDao.insertMenuItemIngredient(MenuItemIngredient(menuItemId = drink1Id, inventoryItemId = milkId, quantityRequired = 0.250))
            inventoryDao.insertMenuItemIngredient(MenuItemIngredient(menuItemId = drink1Id, inventoryItemId = coffeeId, quantityRequired = 0.018))

            val drink2Id = menuItemDao.insertMenuItem(MenuItem(name = "Coca-Cola Can", category = "Drinks", price = 1.99, hasIngredients = true))
            inventoryDao.insertMenuItemIngredient(MenuItemIngredient(menuItemId = drink2Id, inventoryItemId = cokeId, quantityRequired = 1.0))

            val dessert1Id = menuItemDao.insertMenuItem(MenuItem(name = "Chocolate Muffin", category = "Desserts", price = 3.49, hasIngredients = false))
        }
    }
}
