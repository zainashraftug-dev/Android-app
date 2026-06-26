package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.RestaurantViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun InventoryScreen(viewModel: RestaurantViewModel) {
    val inventoryList by viewModel.inventoryItems.collectAsState()
    val transactionsList by viewModel.transactions.collectAsState()
    val menuList by viewModel.menuItems.collectAsState()
    val recipeMap by viewModel.menuItemRecipes.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) } // 0: Stock levels, 1: Audit logs, 2: Recipe Studio

    var showRestockDialog by remember { mutableStateOf(false) }
    var selectedItemForRestock by remember { mutableStateOf<InventoryItem?>(null) }

    var showAddIngredientDialog by remember { mutableStateOf(false) }
    var showAddMenuItemDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Screen Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warehouse,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Inventory Control & Recipes",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sub-Tab Row
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("Stock Levels", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("inventory_tab_stock")
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("Audit Ledger", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("inventory_tab_audit")
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    text = { Text("Recipe Studio", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("inventory_tab_recipes")
                )
            }
        }

        HorizontalDivider()

        // Content Area depending on Sub-Tab
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (activeTab) {
                0 -> {
                    // STOCK LEVELS
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Current Raw Materials",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            Button(
                                onClick = { showAddIngredientDialog = true },
                                modifier = Modifier.testTag("add_ingredient_fab")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Material")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (inventoryList.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No materials defined. Click Add to populate stock.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 240.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(inventoryList) { item ->
                                    StockItemCard(
                                        item = item,
                                        onRestockClick = {
                                            selectedItemForRestock = item
                                            showRestockDialog = true
                                        },
                                        onDeleteClick = { viewModel.deleteInventoryItem(item) }
                                    )
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // AUDIT LOGS
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Real-Time Transaction Ledger",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (transactionsList.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No transaction logs yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(transactionsList) { transaction ->
                                    TransactionRow(transaction)
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // RECIPE STUDIO
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Menu Catalog & Recipes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            Button(
                                onClick = { showAddMenuItemDialog = true },
                                modifier = Modifier.testTag("add_menu_item_fab")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Dish / Recipe")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (menuList.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No dishes defined. Add dishes with recipes.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(menuList) { menuItem ->
                                    val ingredients = recipeMap[menuItem.id] ?: emptyList()
                                    RecipeItemCard(
                                        menuItem = menuItem,
                                        ingredients = ingredients,
                                        allInventory = inventoryList,
                                        onDeleteClick = { viewModel.deleteMenuItem(menuItem) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Restock Dialog Overlay
        if (showRestockDialog && selectedItemForRestock != null) {
            RestockDialog(
                item = selectedItemForRestock!!,
                onDismiss = {
                    showRestockDialog = false
                    selectedItemForRestock = null
                },
                onConfirm = { qty ->
                    viewModel.restockItem(selectedItemForRestock!!.id, qty)
                    showRestockDialog = false
                    selectedItemForRestock = null
                }
            )
        }

        // Add Ingredient Dialog Overlay
        if (showAddIngredientDialog) {
            AddIngredientDialog(
                onDismiss = { showAddIngredientDialog = false },
                onConfirm = { name, stock, minLevel, unit ->
                    viewModel.addNewInventoryItem(name, stock, minLevel, unit)
                    showAddIngredientDialog = false
                }
            )
        }

        // Add Menu Item Dialog Overlay
        if (showAddMenuItemDialog) {
            AddMenuItemDialog(
                inventoryItems = inventoryList,
                onDismiss = { showAddMenuItemDialog = false },
                onConfirm = { name, category, price, hasIngredients, mappings ->
                    viewModel.addNewMenuItem(name, category, price, hasIngredients, mappings)
                    showAddMenuItemDialog = false
                }
            )
        }
    }
}

@Composable
fun StockItemCard(
    item: InventoryItem,
    onRestockClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isLowStock = item.currentStock <= item.minStockLevel
    val progress = if (item.currentStock > 0) {
        (item.currentStock / (item.minStockLevel * 3)).coerceIn(0.0, 1.0).toFloat()
    } else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("inventory_item_${item.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLowStock) Color(0xFFFFF2F2) else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isLowStock) Color(0xFFEF9A9A) else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Min stock alert: ${item.minStockLevel} ${item.unit}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Delete material
                IconButton(onClick = onDeleteClick, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Current stock text
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "STOCK LEVEL",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${item.currentStock} ${item.unit}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isLowStock) Color(0xFFC62828) else MaterialTheme.colorScheme.primary
                    )
                }

                // Alert label
                if (isLowStock) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFEF5350), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            "CRITICAL LOW",
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (isLowStock) Color(0xFFEF5350) else MaterialTheme.colorScheme.primary,
                trackColor = if (isLowStock) Color(0xFFFFCDD2) else MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onRestockClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .testTag("restock_btn_${item.id}"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLowStock) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.AddBusiness, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Restock Material", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TransactionRow(transaction: InventoryTransaction) {
    val formattedTime = remember(transaction.timestamp) {
        SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).format(Date(transaction.timestamp))
    }

    val isDeduction = transaction.quantityChanged < 0
    val textSign = if (isDeduction) "" else "+"
    val changeColor = if (isDeduction) Color(0xFFC62828) else Color(0xFF2E7D32)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(
                        if (isDeduction) Color(0xFFFFEBEE) else Color(0xFFE8F5E9),
                        CircleShape
                    )
                    .size(36.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isDeduction) Icons.Default.RemoveCircleOutline else Icons.Default.AddCircleOutline,
                    contentDescription = null,
                    tint = changeColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.inventoryItemName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "Reason: ${transaction.reason}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formattedTime,
                    fontSize = 10.sp,
                    color = Color.LightGray
                )
            }

            Text(
                text = "$textSign${transaction.quantityChanged}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = changeColor
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipeItemCard(
    menuItem: MenuItem,
    ingredients: List<MenuItemIngredient>,
    allInventory: List<InventoryItem>,
    onDeleteClick: () -> Unit
) {
    val inventoryMap = remember(allInventory) { allInventory.associateBy { it.id } }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = menuItem.category,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = String.format(Locale.US, "$%.2f", menuItem.price),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = menuItem.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Dish", tint = Color(0xFFEF5350))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "RECIPE INGREDIENTS:",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (!menuItem.hasIngredients) {
                Text(
                    text = "Direct inventory check disabled. Stock of ingredients is not deducted when ordered.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (ingredients.isEmpty()) {
                Text(
                    text = "Warning: Manage ingredients enabled but no ingredients mapped in recipe!",
                    fontSize = 11.sp,
                    color = Color(0xFFD32F2F),
                    fontWeight = FontWeight.Bold
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ingredients.forEach { ing ->
                        val invItem = inventoryMap[ing.inventoryItemId]
                        val name = invItem?.name ?: "Unknown Ingredient"
                        val unit = invItem?.unit ?: ""
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$name: ${ing.quantityRequired} $unit",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RestockDialog(
    item: InventoryItem,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var restockQty by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restock: ${item.name}") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Current Stock: ${item.currentStock} ${item.unit}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = restockQty,
                    onValueChange = {
                        restockQty = it
                        hasError = it.toDoubleOrNull() == null || it.toDouble() <= 0
                    },
                    label = { Text("Quantity to Add (${item.unit})") },
                    placeholder = { Text("e.g. 50, 10.5") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("restock_quantity_input"),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    isError = hasError,
                    supportingText = if (hasError) {
                        { Text("Please enter a positive numeric value") }
                    } else null
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = restockQty.toDoubleOrNull()
                    if (qty != null && qty > 0) {
                        onConfirm(qty)
                    } else {
                        hasError = true
                    }
                },
                enabled = restockQty.isNotBlank() && !hasError,
                modifier = Modifier.testTag("restock_dialog_confirm")
            ) {
                Text("Confirm Restock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddIngredientDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Double, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var minLevel by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("pcs") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Material Details") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Material Name") },
                    placeholder = { Text("e.g. Mayo, Cheddar cheese") },
                    modifier = Modifier.fillMaxWidth().testTag("add_ing_name"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = stock,
                    onValueChange = { stock = it },
                    label = { Text("Initial Stock") },
                    placeholder = { Text("e.g. 100") },
                    modifier = Modifier.fillMaxWidth().testTag("add_ing_stock"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = minLevel,
                    onValueChange = { minLevel = it },
                    label = { Text("Minimum Stock Alert Level") },
                    placeholder = { Text("e.g. 20") },
                    modifier = Modifier.fillMaxWidth().testTag("add_ing_min"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Measurement Unit") },
                    placeholder = { Text("e.g. pcs, kg, liters, g") },
                    modifier = Modifier.fillMaxWidth().testTag("add_ing_unit"),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sVal = stock.toDoubleOrNull() ?: 0.0
                    val mVal = minLevel.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank()) {
                        onConfirm(name, sVal, mVal, unit)
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("add_ing_confirm")
            ) {
                Text("Save Material")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddMenuItemDialog(
    inventoryItems: List<InventoryItem>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double, Boolean, List<Pair<Long, Double>>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Burgers") }
    var price by remember { mutableStateOf("") }
    var hasIngredients by remember { mutableStateOf(true) }

    // Selected ingredient mappings for recipe creation
    val selectedIngredients = remember { mutableStateListOf<Pair<Long, Double>>() }

    var currentIngSelectionId by remember { mutableStateOf(inventoryItems.firstOrNull()?.id ?: 0L) }
    var currentIngQty by remember { mutableStateOf("") }

    val categoriesList = listOf("Burgers", "Pizzas", "Drinks", "Desserts")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Menu Dish & Recipe") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Dish Name") },
                    placeholder = { Text("e.g. Garlic Bread") },
                    modifier = Modifier.fillMaxWidth().testTag("add_menu_name"),
                    singleLine = true
                )

                // Category selector
                Text("Category:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categoriesList.forEach { cat ->
                        val isSel = category == cat
                        FilterChip(
                            selected = isSel,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 10.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Price ($)") },
                    placeholder = { Text("e.g. 5.99") },
                    modifier = Modifier.fillMaxWidth().testTag("add_menu_price"),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Deduct Raw Materials on Orders?", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = hasIngredients,
                        onCheckedChange = { hasIngredients = it },
                        modifier = Modifier.testTag("add_menu_toggle_ingredients")
                    )
                }

                // Ingredient Mapper
                if (hasIngredients && inventoryItems.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text("RECIPE CREATOR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))

                            // Current Mapped list
                            selectedIngredients.forEach { (id, qty) ->
                                val matchedItem = inventoryItems.find { it.id == id }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${matchedItem?.name}: $qty ${matchedItem?.unit}",
                                        fontSize = 11.sp
                                    )
                                    IconButton(
                                        onClick = { selectedIngredients.remove(Pair(id, qty)) },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.Red, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(6.dp))

                            // Inputs to add new recipe ingredient
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Dropdown simulated (select by index/list for simplicity, or select from grid)
                                val currentItem = inventoryItems.find { it.id == currentIngSelectionId } ?: inventoryItems.first()
                                Button(
                                    onClick = {
                                        // Swap to next item in list for mock selection
                                        val curIdx = inventoryItems.indexOfFirst { it.id == currentIngSelectionId }
                                        val nextIdx = (curIdx + 1) % inventoryItems.size
                                        currentIngSelectionId = inventoryItems[nextIdx].id
                                    },
                                    modifier = Modifier.weight(1.3f),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp)
                                ) {
                                    Text(
                                        text = "Item: ${currentItem.name}",
                                        fontSize = 9.sp,
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 1
                                    )
                                }

                                OutlinedTextField(
                                    value = currentIngQty,
                                    onValueChange = { currentIngQty = it },
                                    placeholder = { Text("Qty") },
                                    modifier = Modifier.weight(0.7f).height(44.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    textStyle = LocalTextStyle.current.copy(fontSize = 10.sp),
                                    singleLine = true
                                )

                                IconButton(
                                    onClick = {
                                        val q = currentIngQty.toDoubleOrNull()
                                        if (q != null && q > 0) {
                                            // Check if already contains, then replace, otherwise add
                                            selectedIngredients.removeAll { it.first == currentIngSelectionId }
                                            selectedIngredients.add(Pair(currentIngSelectionId, q))
                                            currentIngQty = ""
                                        }
                                    },
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                        .size(28.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val pVal = price.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank()) {
                        onConfirm(name, category, pVal, hasIngredients, selectedIngredients.toList())
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("add_menu_confirm")
            ) {
                Text("Create Dish")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
