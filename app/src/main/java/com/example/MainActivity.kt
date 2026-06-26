package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.RestaurantViewModel
import com.example.ui.RestaurantViewModelFactory
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.OrdersScreen
import com.example.ui.screens.POSScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    private val viewModel: RestaurantViewModel by viewModels {
        val app = application as RestaurantApplication
        RestaurantViewModelFactory(app.repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }
                val coroutineScope = rememberCoroutineScope()
                
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: "pos"

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier.testTag("app_bottom_navigation"),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            tonalElevation = 0.dp
                        ) {
                            val navItemColors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.secondary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            NavigationBarItem(
                                selected = currentRoute == "pos",
                                onClick = {
                                    navController.navigate("pos") {
                                        popUpTo("pos") { inclusive = false }
                                        launchSingleTop = true
                                    }
                                },
                                icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Point Of Sale") },
                                label = { Text("POS", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
                                colors = navItemColors,
                                modifier = Modifier.testTag("nav_item_pos")
                            )

                            NavigationBarItem(
                                selected = currentRoute == "orders",
                                onClick = {
                                    navController.navigate("orders") {
                                        popUpTo("pos") { inclusive = false }
                                        launchSingleTop = true
                                    }
                                },
                                icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Orders Queue") },
                                label = { Text("Orders", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
                                colors = navItemColors,
                                modifier = Modifier.testTag("nav_item_orders")
                            )

                            NavigationBarItem(
                                selected = currentRoute == "inventory",
                                onClick = {
                                    navController.navigate("inventory") {
                                        popUpTo("pos") { inclusive = false }
                                        launchSingleTop = true
                                    }
                                },
                                icon = { Icon(Icons.Default.Inventory, contentDescription = "Inventory Levels") },
                                label = { Text("Inventory", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
                                colors = navItemColors,
                                modifier = Modifier.testTag("nav_item_inventory")
                            )
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "pos",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("pos") {
                            POSScreen(
                                viewModel = viewModel,
                                onOrderPlaced = { orderId ->
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Order #$orderId placed! Go to 'Orders' to view & print bill.",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                            )
                        }
                        composable("orders") {
                            OrdersScreen(viewModel = viewModel)
                        }
                        composable("inventory") {
                            InventoryScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
