package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.RestaurantRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class RestaurantApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }
    val repository by lazy {
        RestaurantRepository(
            database.menuItemDao(),
            database.inventoryDao(),
            database.orderDao()
        )
    }
}
