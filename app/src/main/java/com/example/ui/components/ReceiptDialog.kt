package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.Order
import com.example.data.OrderItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReceiptDialog(
    order: Order,
    items: List<OrderItem>,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isPrinting by remember { mutableStateOf(false) }
    var printProgress by remember { mutableFloatStateOf(0f) }
    var printCompleted by remember { mutableStateOf(false) }
    
    // Receipt feeding animation offsets
    val animatedPaperHeight = remember { Animatable(0f) }
    val formattedDate = remember(order.timestamp) {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(order.timestamp))
    }

    Dialog(
        onDismissRequest = { if (!isPrinting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .heightIn(max = 680.dp)
                    .testTag("receipt_dialog_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Dialog Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "80mm Thermal Receipt",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = onDismiss,
                            enabled = !isPrinting,
                            modifier = Modifier.testTag("close_receipt_button")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isPrinting) {
                        // Print Progress Screen
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                progress = { printProgress },
                                modifier = Modifier.size(72.dp),
                                strokeWidth = 6.dp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Feeding roll paper to printer...",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Generating ESC/POS character streams",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else if (printCompleted) {
                        // Print Success Screen
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(80.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Printed Successfully!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Sent 1,248 bytes to 80mm Thermal Printer.",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            Button(
                                onClick = onDismiss,
                                modifier = Modifier.fillMaxWidth(0.6f)
                            ) {
                                Text("Done")
                            }
                        }
                    } else {
                        // Receipt Preview Window
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(Color(0xFFE2E2E2), shape = RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            // 80mm Thermal Paper Slip
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                                    .background(Color(0xFFFFFDF2)) // Thermal paper color
                                    .border(1.dp, Color(0xFFD4D4D4))
                                    .padding(horizontal = 16.dp, vertical = 20.dp)
                            ) {
                                // STORE HEADER
                                Text(
                                    text = "=== GOURMET BISTRO ===",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = "128 Rue de la Gastronomie\nTel: +33 1 45 67 89\nReg: FR-83921092\n* DUPLICATE RECEIPT *",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "--------------------------------",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // ORDER DETAILS
                                Text(
                                    text = "ORDER ID : #${order.id}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "DATE     : $formattedDate",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "TYPE     : ${order.orderType}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                )
                                if (order.orderType == "DINE_IN" && !order.tableNumber.isNullOrEmpty()) {
                                    Text(
                                        text = "TABLE    : Table ${order.tableNumber}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else if (order.orderType == "CAR_SERVICE" && !order.carPlateNumber.isNullOrEmpty()) {
                                    Text(
                                        text = "VEHICLE  : Plate ${order.carPlateNumber}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "STATUS   : ${order.status}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                )

                                Text(
                                    text = "--------------------------------",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // ITEMS TABLE
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Text("ITEM NAME", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.5f))
                                    Text("QTY", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.15f), textAlign = TextAlign.Center)
                                    Text("PRICE", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.15f), textAlign = TextAlign.End)
                                    Text("TOTAL", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.2f), textAlign = TextAlign.End)
                                }

                                Text(
                                    text = "- - - - - - - - - - - - - - - -",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                for (item in items) {
                                    val itemTotal = item.quantity * item.unitPrice
                                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = item.name,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            modifier = Modifier.weight(0.5f)
                                        )
                                        Text(
                                            text = "x${item.quantity}",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            modifier = Modifier.weight(0.15f),
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = String.format(Locale.US, "$%.2f", item.unitPrice),
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            modifier = Modifier.weight(0.15f),
                                            textAlign = TextAlign.End
                                        )
                                        Text(
                                            text = String.format(Locale.US, "$%.2f", itemTotal),
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            modifier = Modifier.weight(0.2f),
                                            textAlign = TextAlign.End
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                }

                                Text(
                                    text = "--------------------------------",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // TOTALS
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Text("SUBTOTAL:", fontFamily = FontFamily.Monospace, fontSize = 11.sp, modifier = Modifier.weight(0.6f))
                                    Text(String.format(Locale.US, "$%.2f", order.subtotal), fontFamily = FontFamily.Monospace, fontSize = 11.sp, modifier = Modifier.weight(0.4f), textAlign = TextAlign.End)
                                }
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Text("TAX (10.0%):", fontFamily = FontFamily.Monospace, fontSize = 11.sp, modifier = Modifier.weight(0.6f))
                                    Text(String.format(Locale.US, "$%.2f", order.tax), fontFamily = FontFamily.Monospace, fontSize = 11.sp, modifier = Modifier.weight(0.4f), textAlign = TextAlign.End)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Text("GRAND TOTAL:", fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.6f))
                                    Text(String.format(Locale.US, "$%.2f", order.totalAmount), fontFamily = FontFamily.Monospace, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.4f), textAlign = TextAlign.End)
                                }

                                Text(
                                    text = "--------------------------------",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // BARCODE DRAWING
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val canvasWidth = size.width
                                        val canvasHeight = size.height
                                        val barcodeLines = 45
                                        val seed = order.id + 15234
                                        val random = Random(seed)

                                        val step = canvasWidth / barcodeLines
                                        for (i in 0 until barcodeLines) {
                                            val isBlack = random.nextBoolean()
                                            val thickness = if (random.nextBoolean()) 3f else 1.5f
                                            if (isBlack && i > 3 && i < barcodeLines - 3) {
                                                drawLine(
                                                    color = Color.Black,
                                                    start = Offset(i * step, 0f),
                                                    end = Offset(i * step, canvasHeight),
                                                    strokeWidth = thickness
                                                )
                                            }
                                        }
                                    }
                                }
                                Text(
                                    text = "*ORD-${order.id}-POS*",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "THANK YOU FOR YOUR VISIT!\nPOWERED BY BISTRO-POS",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                // Decorative torn paper serrations
                                Canvas(modifier = Modifier.fillMaxWidth().height(8.dp)) {
                                    val serrations = 32
                                    val step = size.width / serrations
                                    for (i in 0..serrations) {
                                        drawLine(
                                            color = Color(0xFFE2E2E2),
                                            start = Offset(i * step, 0f),
                                            end = Offset((i + 0.5f) * step, size.height),
                                            strokeWidth = 2f
                                        )
                                        drawLine(
                                            color = Color(0xFFE2E2E2),
                                            start = Offset((i + 0.5f) * step, size.height),
                                            end = Offset((i + 1) * step, 0f),
                                            strokeWidth = 2f
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Dialog Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Close")
                            }
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isPrinting = true
                                        printProgress = 0f
                                        while (printProgress < 1f) {
                                            delay(150)
                                            printProgress += 0.1f
                                        }
                                        isPrinting = false
                                        printCompleted = true
                                    }
                                },
                                modifier = Modifier
                                    .weight(1.5f)
                                    .testTag("simulate_print_button")
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Simulate 80mm Print")
                            }
                        }
                    }
                }
            }
        }
    }
}
