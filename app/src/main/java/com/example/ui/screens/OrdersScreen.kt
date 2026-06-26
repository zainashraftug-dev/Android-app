package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.data.Order
import com.example.data.OrderItem
import com.example.ui.RestaurantViewModel
import com.example.ui.components.ReceiptDialog
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(viewModel: RestaurantViewModel) {
    val orders by viewModel.orders.collectAsState()
    val selectedOrder by viewModel.selectedOrder.collectAsState()
    val selectedOrderItems by viewModel.selectedOrderItems.collectAsState()

    var statusFilter by remember { mutableStateOf("ACTIVE") } // ACTIVE, COMPLETED, CANCELLED, ALL
    var typeFilter by remember { mutableStateOf("ALL") } // ALL, DINE_IN, TAKE_AWAY, CAR_SERVICE
    var showReceiptDialog by remember { mutableStateOf(false) }

    // Filter orders
    val filteredOrders = remember(orders, statusFilter, typeFilter) {
        orders.filter { order ->
            val matchesStatus = when (statusFilter) {
                "ACTIVE" -> order.status == "PENDING" || order.status == "PREPARING"
                "COMPLETED" -> order.status == "COMPLETED"
                "CANCELLED" -> order.status == "CANCELLED"
                else -> true
            }
            val matchesType = when (typeFilter) {
                "ALL" -> true
                else -> order.orderType == typeFilter
            }
            matchesStatus && matchesType
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isWideScreen = maxWidth >= 760.dp

        Column(modifier = Modifier.fillMaxSize()) {
            // Screen Header & Filter row
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
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Orders & Kitchen Queue",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Status Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "ACTIVE" to "Kitchen Queue",
                        "COMPLETED" to "Completed",
                        "CANCELLED" to "Cancelled",
                        "ALL" to "All Orders"
                    ).forEach { (filterVal, label) ->
                        FilterChip(
                            selected = statusFilter == filterVal,
                            onClick = { statusFilter = filterVal },
                            label = { Text(label) },
                            modifier = Modifier.testTag("status_filter_$filterVal")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Order Type Selector Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Filter Type:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    listOf(
                        "ALL" to "All",
                        "DINE_IN" to "Dine In",
                        "TAKE_AWAY" to "Takeaway",
                        "CAR_SERVICE" to "Car Service"
                    ).forEach { (typeVal, label) ->
                        FilterChip(
                            selected = typeFilter == typeVal,
                            onClick = { typeFilter = typeVal },
                            label = { Text(label, fontSize = 11.sp) },
                            modifier = Modifier.testTag("type_filter_$typeVal")
                        )
                    }
                }
            }

            HorizontalDivider()

            if (filteredOrders.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Inbox,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No orders found in this section.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    // LEFT SIDE: ORDERS LIST
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredOrders) { order ->
                            val isSelected = selectedOrder?.id == order.id
                            OrderListItemCard(
                                order = order,
                                isSelected = isSelected,
                                onClick = { viewModel.selectOrder(order) }
                            )
                        }
                    }

                    // RIGHT SIDE: ORDER DETAIL PANE (Only shown on Wide Screen / Tablet)
                    if (isWideScreen) {
                        Box(
                            modifier = Modifier
                                .weight(1.2f)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            if (selectedOrder != null) {
                                OrderDetailView(
                                    order = selectedOrder!!,
                                    items = selectedOrderItems,
                                    onUpdateStatus = { status ->
                                        viewModel.updateOrderStatus(selectedOrder!!, status)
                                    },
                                    onPrintBillClick = { showReceiptDialog = true }
                                )
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.Receipt,
                                        contentDescription = null,
                                        modifier = Modifier.size(56.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        "Select an order to view details\nand simulate 80mm thermal bill print",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Mobile Full Screen / Bottom Sheet detail view overlay
        if (!isWideScreen && selectedOrder != null) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.selectOrder(null) },
                dragHandle = { BottomSheetDefaults.DragHandle() },
                modifier = Modifier.fillMaxHeight(0.95f)
            ) {
                OrderDetailView(
                    order = selectedOrder!!,
                    items = selectedOrderItems,
                    onUpdateStatus = { status ->
                        viewModel.updateOrderStatus(selectedOrder!!, status)
                    },
                    onPrintBillClick = { showReceiptDialog = true }
                )
            }
        }

        // Receipt Print Dialog Simulation overlay
        if (showReceiptDialog && selectedOrder != null) {
            ReceiptDialog(
                order = selectedOrder!!,
                items = selectedOrderItems,
                onDismiss = { showReceiptDialog = false }
            )
        }
    }
}

@Composable
fun OrderListItemCard(
    order: Order,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val formattedTime = remember(order.timestamp) {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(order.timestamp))
    }

    val typeIcon = when (order.orderType) {
        "DINE_IN" -> Icons.Default.Coffee
        "TAKE_AWAY" -> Icons.Default.ShoppingBag
        else -> Icons.Default.DirectionsCar
    }

    val typeLabel = when (order.orderType) {
        "DINE_IN" -> "Table ${order.tableNumber ?: "N/A"}"
        "TAKE_AWAY" -> "Takeaway"
        else -> "Car (${order.carPlateNumber ?: "N/A"})"
    }

    val statusColor = when (order.status) {
        "PENDING" -> Color(0xFFFFB300)   // Amber
        "PREPARING" -> Color(0xFF1E88E5) // Blue
        "COMPLETED" -> Color(0xFF43A047) // Green
        else -> Color(0xFF757575)        // Grey for cancelled
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("order_list_item_${order.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                             else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) borderStroke(2.dp, MaterialTheme.colorScheme.primary) else borderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Order ID & Icon
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        typeIcon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Order #${order.id}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Order Status Pill
                Box(
                    modifier = Modifier
                        .background(statusColor, RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = order.status,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = typeLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formattedTime,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Total Bill",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = String.format(Locale.US, "$%.2f", order.totalAmount),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun OrderDetailView(
    order: Order,
    items: List<OrderItem>,
    onUpdateStatus: (String) -> Unit,
    onPrintBillClick: () -> Unit
) {
    val formattedDate = remember(order.timestamp) {
        SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).format(Date(order.timestamp))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Detail Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = "Order Summary #${order.id}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Print Bill Floating Accent Button
            Button(
                onClick = onPrintBillClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("detail_print_receipt_button")
            ) {
                Icon(Icons.Default.Print, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Print Receipt")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Info cards (Type and Table/Vehicle details)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("ORDER CATEGORY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(order.orderType.replace("_", " "), fontWeight = FontWeight.SemiBold)
                    }

                    if (order.orderType == "DINE_IN") {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("TABLE NUMBER", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(order.tableNumber ?: "Counter / Walk-In", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        }
                    } else if (order.orderType == "CAR_SERVICE") {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("VEHICLE PLATE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(order.carPlateNumber ?: "N/A", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "DISHE DETAILS",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))

        // List of items
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                .size(28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${item.quantity}x",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = String.format(Locale.US, "$%.2f", item.unitPrice * item.quantity),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        // Calculations Card
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Subtotal", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(String.format(Locale.US, "$%.2f", order.subtotal), fontSize = 13.sp)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Vat (10.0%)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(String.format(Locale.US, "$%.2f", order.tax), fontSize = 13.sp)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total bill", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(String.format(Locale.US, "$%.2f", order.totalAmount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // KITCHEN QUEUE CONTROLS
        Text(
            "UPDATE KITCHEN STATUS",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (order.status) {
                "PENDING" -> {
                    Button(
                        onClick = { onUpdateStatus("PREPARING") },
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp)
                            .testTag("action_prepare_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.SoupKitchen, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Start Cooking")
                    }

                    OutlinedButton(
                        onClick = { onUpdateStatus("CANCELLED") },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("action_cancel_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE57373))
                    ) {
                        Text("Cancel Order")
                    }
                }
                "PREPARING" -> {
                    Button(
                        onClick = { onUpdateStatus("COMPLETED") },
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp)
                            .testTag("action_complete_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047))
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mark Served")
                    }

                    OutlinedButton(
                        onClick = { onUpdateStatus("CANCELLED") },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("action_cancel_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE57373))
                    ) {
                        Text("Cancel")
                    }
                }
                "COMPLETED" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Order has been successfully served and billed.",
                                color = Color(0xFF2E7D32),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                "CANCELLED" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFFEBEE), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Cancel, contentDescription = null, tint = Color(0xFFEF5350))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "This order was cancelled.",
                                color = Color(0xFFC62828),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

// Border Stroke helper for cleaner design compatibility
private fun borderStroke(width: androidx.compose.ui.unit.Dp, color: Color) = 
    androidx.compose.foundation.BorderStroke(width, color)
