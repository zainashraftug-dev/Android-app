package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MenuItem
import com.example.ui.RestaurantViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun POSScreen(
    viewModel: RestaurantViewModel,
    onOrderPlaced: (Long) -> Unit
) {
    val menuItems by viewModel.menuItems.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val orderType by viewModel.orderType.collectAsState()
    val tableNumber by viewModel.tableNumber.collectAsState()
    val carPlateNumber by viewModel.carPlateNumber.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    var showMobileCart by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val categories = listOf("All", "Burgers", "Pizzas", "Drinks", "Desserts")

    // Filtered Menu Items
    val filteredItems = remember(menuItems, selectedCategory, searchQuery) {
        menuItems.filter { item ->
            val matchesCategory = selectedCategory == "All" || item.category.lowercase() == selectedCategory.lowercase()
            val matchesSearch = item.name.lowercase().contains(searchQuery.lowercase())
            matchesCategory && matchesSearch
        }
    }

    val cartItemCount = cart.values.sum()
    val cartSubtotal = cart.entries.sumOf { it.key.price * it.value }
    val cartTax = cartSubtotal * 0.10
    val cartTotal = cartSubtotal + cartTax

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isWideScreen = maxWidth >= 760.dp

        Row(modifier = Modifier.fillMaxSize()) {
            // LEFT COLUMN: MENU & CONTROLS
            Column(
                modifier = Modifier
                    .weight(if (isWideScreen) 1.6f else 1f)
                    .fillMaxHeight()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Header & Search
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Gourmet Bistro POS",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search dishes...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pos_search_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Category Tabs Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        val isSelected = selectedCategory == category
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .clickable {
                                    viewModel.setSelectedCategory(category)
                                    focusManager.clearFocus()
                                }
                                .padding(vertical = 10.dp)
                                .testTag("category_pill_$category"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = category,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (filteredItems.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No dishes found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // Menu Grid
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredItems) { menuItem ->
                            val isAddable = viewModel.isItemAddable(menuItem)
                            MenuGridItem(
                                item = menuItem,
                                isAddable = isAddable,
                                onAddClick = {
                                    if (isAddable) {
                                        viewModel.addToCart(menuItem)
                                    }
                                }
                            )
                        }
                    }
                }

                // If mobile screen, show a bottom floating checkout bar
                if (!isWideScreen && cartItemCount > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showMobileCart = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("pos_mobile_view_cart_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.onPrimary, CircleShape)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = cartItemCount.toString(),
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "View Cart & Order",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = String.format(Locale.US, "$%.2f", cartTotal),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            // RIGHT COLUMN: CART PANEL (Only shown on Wide Screen / Tablet Layout)
            if (isWideScreen) {
                Box(
                    modifier = Modifier
                        .width(360.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    CartPanel(
                        cart = cart,
                        orderType = orderType,
                        tableNumber = tableNumber,
                        carPlateNumber = carPlateNumber,
                        subtotal = cartSubtotal,
                        tax = cartTax,
                        total = cartTotal,
                        onOrderTypeChange = { viewModel.setOrderType(it) },
                        onTableNumberChange = { viewModel.setTableNumber(it) },
                        onCarPlateChange = { viewModel.setCarPlateNumber(it) },
                        onPlusClick = { viewModel.addToCart(it) },
                        onMinusClick = { viewModel.removeFromCart(it) },
                        onDeleteClick = { viewModel.removeEntireItemFromCart(it) },
                        onClearCart = { viewModel.clearCart() },
                        onCheckout = {
                            viewModel.checkout { orderId ->
                                onOrderPlaced(orderId)
                            }
                        }
                    )
                }
            }
        }

        // Mobile Bottom Sheet Dialog for Cart
        if (showMobileCart && !isWideScreen) {
            ModalBottomSheet(
                onDismissRequest = { showMobileCart = false },
                dragHandle = { BottomSheetDefaults.DragHandle() },
                modifier = Modifier.fillMaxHeight(0.9f)
            ) {
                CartPanel(
                    cart = cart,
                    orderType = orderType,
                    tableNumber = tableNumber,
                    carPlateNumber = carPlateNumber,
                    subtotal = cartSubtotal,
                    tax = cartTax,
                    total = cartTotal,
                    onOrderTypeChange = { viewModel.setOrderType(it) },
                    onTableNumberChange = { viewModel.setTableNumber(it) },
                    onCarPlateChange = { viewModel.setCarPlateNumber(it) },
                    onPlusClick = { viewModel.addToCart(it) },
                    onMinusClick = { viewModel.removeFromCart(it) },
                    onDeleteClick = { viewModel.removeEntireItemFromCart(it) },
                    onClearCart = { viewModel.clearCart() },
                    onCheckout = {
                        viewModel.checkout { orderId ->
                            showMobileCart = false
                            onOrderPlaced(orderId)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun MenuGridItem(
    item: MenuItem,
    isAddable: Boolean,
    onAddClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isAddable) { onAddClick() }
            .testTag("menu_item_${item.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAddable) MaterialTheme.colorScheme.surface 
                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isAddable) 2.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Category tag
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = item.category.uppercase(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Item Name
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = if (isAddable) MaterialTheme.colorScheme.onSurface 
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format(Locale.US, "$%.2f", item.price),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isAddable) MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )

                if (isAddable) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .size(36.dp)
                            .clickable { onAddClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFE57373), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "OUT OF STOCK",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CartPanel(
    cart: Map<MenuItem, Int>,
    orderType: String,
    tableNumber: String,
    carPlateNumber: String,
    subtotal: Double,
    tax: Double,
    total: Double,
    onOrderTypeChange: (String) -> Unit,
    onTableNumberChange: (String) -> Unit,
    onCarPlateChange: (String) -> Unit,
    onPlusClick: (MenuItem) -> Unit,
    onMinusClick: (MenuItem) -> Unit,
    onDeleteClick: (MenuItem) -> Unit,
    onClearCart: () -> Unit,
    onCheckout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Cart Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Current Order",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            if (cart.isNotEmpty()) {
                TextButton(
                    onClick = onClearCart,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFE57373)),
                    modifier = Modifier.testTag("clear_cart_button")
                ) {
                    Text("Clear All")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Order Type Selector
        Text(
            text = "Select Order Type",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                Triple("DINE_IN", Icons.Default.TableBar, "DINE IN"),
                Triple("TAKE_AWAY", Icons.Default.ShoppingBag, "TAKEAWAY"),
                Triple("CAR_SERVICE", Icons.Default.DirectionsCar, "CAR SVC")
            ).forEach { (type, icon, label) ->
                val isSelected = orderType == type
                
                val containerColor = if (isSelected) {
                    when (type) {
                        "DINE_IN" -> Color(0xFFEADDFF)
                        "TAKE_AWAY" -> Color(0xFFE8DEF8)
                        else -> Color(0xFFF3EDF7)
                    }
                } else {
                    MaterialTheme.colorScheme.surface
                }
                
                val borderColor = if (isSelected) {
                    when (type) {
                        "DINE_IN", "TAKE_AWAY" -> Color(0xFFD0BCFF)
                        else -> Color(0xFFCAC4D0)
                    }
                } else {
                    Color(0xFFCAC4D0).copy(alpha = 0.5f)
                }
                
                val contentColor = if (isSelected) {
                    when (type) {
                        "DINE_IN" -> Color(0xFF21005D)
                        else -> Color(0xFF1D192B)
                    }
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp)
                        .clickable { onOrderTypeChange(type) }
                        .testTag("order_type_btn_$type"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = containerColor,
                        contentColor = contentColor
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = contentColor,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Order Type Context Input
        when (orderType) {
            "DINE_IN" -> {
                OutlinedTextField(
                    value = tableNumber,
                    onValueChange = onTableNumberChange,
                    label = { Text("Table Number") },
                    placeholder = { Text("e.g. 5, 12, Terrace-2") },
                    leadingIcon = { Icon(Icons.Default.TableBar, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("table_number_input"),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
            }
            "CAR_SERVICE" -> {
                OutlinedTextField(
                    value = carPlateNumber,
                    onValueChange = onCarPlateChange,
                    label = { Text("Car Plate / Vehicle Number") },
                    placeholder = { Text("e.g. ABC-1234, White Audi") },
                    leadingIcon = { Icon(Icons.Default.DirectionsCar, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("car_plate_input"),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
            }
            else -> {
                // Takeaway has no extra fields
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "No table or vehicle registration required for counter pickup.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Cart Items List
        Text(
            text = "Ordered Items",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))

        if (cart.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Receipt,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Cart is empty",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cart.entries.toList()) { (menuItem, quantity) ->
                    CartItemRow(
                        menuItem = menuItem,
                        quantity = quantity,
                        onPlusClick = { onPlusClick(menuItem) },
                        onMinusClick = { onMinusClick(menuItem) },
                        onDeleteClick = { onDeleteClick(menuItem) }
                    )
                }
            }
        }

        // Bill Summary and Checkout Button
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Subtotal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(String.format(Locale.US, "$%.2f", subtotal), fontWeight = FontWeight.Medium)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Tax (10.0%)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(String.format(Locale.US, "$%.2f", tax), fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Total Amount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = String.format(Locale.US, "$%.2f", total),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Check is input complete
        val isInputComplete = when (orderType) {
            "DINE_IN" -> tableNumber.isNotBlank()
            "CAR_SERVICE" -> carPlateNumber.isNotBlank()
            else -> true
        }

        Button(
            onClick = onCheckout,
            enabled = cart.isNotEmpty() && isInputComplete,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("checkout_order_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (!isInputComplete) "Enter Table / Plate" else "Place Order & Print Bill",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun CartItemRow(
    menuItem: MenuItem,
    quantity: Int,
    onPlusClick: () -> Unit,
    onMinusClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = menuItem.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = String.format(Locale.US, "$%.2f each", menuItem.price),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Stepper Quantity Control
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onMinusClick,
                modifier = Modifier
                    .size(24.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(14.dp))
            }

            Text(
                text = quantity.toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.widthIn(min = 20.dp),
                textAlign = TextAlign.Center
            )

            IconButton(
                onClick = onPlusClick,
                modifier = Modifier
                    .size(24.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(14.dp))
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Delete item entirely button
        IconButton(
            onClick = onDeleteClick,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Remove item",
                tint = Color(0xFFEF5350),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
