package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RestaurantViewModel(private val repository: RestaurantRepository) : ViewModel() {

    // Flows from Repository
    val menuItems = repository.allMenuItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val inventoryItems = repository.allInventoryItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val orders = repository.allOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI States
    private val _orderType = MutableStateFlow("DINE_IN") // DINE_IN, TAKE_AWAY, CAR_SERVICE
    val orderType = _orderType.asStateFlow()

    private val _tableNumber = MutableStateFlow("")
    val tableNumber = _tableNumber.asStateFlow()

    private val _carPlateNumber = MutableStateFlow("")
    val carPlateNumber = _carPlateNumber.asStateFlow()

    // Cart holds mapping: MenuItem -> Quantity
    private val _cart = MutableStateFlow<Map<MenuItem, Int>>(emptyMap())
    val cart = _cart.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory = _selectedCategory.asStateFlow()

    // Detailed order for viewing and thermal printing simulation
    private val _selectedOrder = MutableStateFlow<Order?>(null)
    val selectedOrder = _selectedOrder.asStateFlow()

    private val _selectedOrderItems = MutableStateFlow<List<OrderItem>>(emptyList())
    val selectedOrderItems = _selectedOrderItems.asStateFlow()

    // Real-time calculated checklist of low-stock ingredients
    val lowStockIngredients: StateFlow<List<InventoryItem>> = inventoryItems
        .map { list -> list.filter { it.currentStock <= it.minStockLevel } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All recipes mappings: MenuItemId -> List of Ingredients
    private val _menuItemRecipes = MutableStateFlow<Map<Long, List<MenuItemIngredient>>>(emptyMap())
    val menuItemRecipes = _menuItemRecipes.asStateFlow()

    init {
        // Fetch recipes for all menu items on load
        viewModelScope.launch {
            menuItems.collect { items ->
                val recipeMap = mutableMapOf<Long, List<MenuItemIngredient>>()
                for (item in items) {
                    recipeMap[item.id] = repository.getIngredientsForMenuItem(item.id)
                }
                _menuItemRecipes.value = recipeMap
            }
        }
    }

    // Setters
    fun setOrderType(type: String) {
        _orderType.value = type
    }

    fun setTableNumber(number: String) {
        _tableNumber.value = number
    }

    fun setCarPlateNumber(plate: String) {
        _carPlateNumber.value = plate
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun selectOrder(order: Order?) {
        _selectedOrder.value = order
        if (order != null) {
            viewModelScope.launch {
                val items = repository.getOrderItemsDirect(order.id)
                _selectedOrderItems.value = items
            }
        } else {
            _selectedOrderItems.value = emptyList()
        }
    }

    // Cart Actions
    fun addToCart(menuItem: MenuItem) {
        val currentCart = _cart.value.toMutableMap()
        val currentQty = currentCart[menuItem] ?: 0
        currentCart[menuItem] = currentQty + 1
        _cart.value = currentCart
    }

    fun removeFromCart(menuItem: MenuItem) {
        val currentCart = _cart.value.toMutableMap()
        val currentQty = currentCart[menuItem] ?: 0
        if (currentQty <= 1) {
            currentCart.remove(menuItem)
        } else {
            currentCart[menuItem] = currentQty - 1
        }
        _cart.value = currentCart
    }

    fun removeEntireItemFromCart(menuItem: MenuItem) {
        val currentCart = _cart.value.toMutableMap()
        currentCart.remove(menuItem)
        _cart.value = currentCart
    }

    fun clearCart() {
        _cart.value = emptyMap()
        _tableNumber.value = ""
        _carPlateNumber.value = ""
    }

    // Checking if cart additions violate available inventory
    fun getCartIngredientRequirements(proposedCart: Map<MenuItem, Int>): Map<Long, Double> {
        val requirements = mutableMapOf<Long, Double>()
        for ((menuItem, qty) in proposedCart) {
            if (menuItem.hasIngredients) {
                val ingredients = _menuItemRecipes.value[menuItem.id] ?: emptyList()
                for (ing in ingredients) {
                    val needed = ing.quantityRequired * qty
                    requirements[ing.inventoryItemId] = (requirements[ing.inventoryItemId] ?: 0.0) + needed
                }
            }
        }
        return requirements
    }

    fun isItemAddable(menuItem: MenuItem): Boolean {
        if (!menuItem.hasIngredients) return true
        val proposedCart = _cart.value.toMutableMap()
        val qty = proposedCart[menuItem] ?: 0
        proposedCart[menuItem] = qty + 1

        val requirements = getCartIngredientRequirements(proposedCart)
        val currentInventory = inventoryItems.value.associateBy { it.id }

        for ((ingId, qtyRequired) in requirements) {
            val invItem = currentInventory[ingId] ?: return false
            if (invItem.currentStock < qtyRequired) {
                return false // Insufficient stock
            }
        }
        return true
    }

    // Checkout Order
    fun checkout(onSuccess: (Long) -> Unit) {
        val cartItems = _cart.value
        if (cartItems.isEmpty()) return

        val orderTypeStr = _orderType.value
        val tableNum = if (orderTypeStr == "DINE_IN") _tableNumber.value else null
        val carPlate = if (orderTypeStr == "CAR_SERVICE") _carPlateNumber.value else null

        val itemsList = cartItems.map { Pair(it.key, it.value) }
        val subtotal = cartItems.entries.sumOf { it.key.price * it.value }
        val tax = subtotal * 0.10 // 10% VAT / Sales Tax
        val totalAmount = subtotal + tax

        viewModelScope.launch {
            val orderId = repository.placeOrder(
                orderType = orderTypeStr,
                tableNumber = tableNum,
                carPlateNumber = carPlate,
                items = itemsList,
                subtotal = subtotal,
                tax = tax,
                totalAmount = totalAmount
            )
            
            // Reload recipes maps to get correct inventory stock update states
            val recipeMap = mutableMapOf<Long, List<MenuItemIngredient>>()
            for (item in menuItems.value) {
                recipeMap[item.id] = repository.getIngredientsForMenuItem(item.id)
            }
            _menuItemRecipes.value = recipeMap

            clearCart()
            onSuccess(orderId)
        }
    }

    // Order Actions
    fun updateOrderStatus(order: Order, newStatus: String) {
        viewModelScope.launch {
            repository.updateOrder(order.copy(status = newStatus))
            // If we selected this order, update the view state too
            if (_selectedOrder.value?.id == order.id) {
                _selectedOrder.value = order.copy(status = newStatus)
            }
        }
    }

    // Inventory Actions
    fun restockItem(itemId: Long, quantity: Double) {
        viewModelScope.launch {
            repository.restockItem(itemId, quantity)
        }
    }

    fun addNewInventoryItem(name: String, initialStock: Double, minStockLevel: Double, unit: String) {
        viewModelScope.launch {
            val newItem = InventoryItem(
                name = name,
                currentStock = initialStock,
                minStockLevel = minStockLevel,
                unit = unit
            )
            val insertedId = repository.insertInventoryItem(newItem)
            repository.insertTransaction(
                InventoryTransaction(
                    inventoryItemId = insertedId,
                    inventoryItemName = name,
                    quantityChanged = initialStock,
                    transactionType = "RESTOCK",
                    reason = "Initial Inventory Setup"
                )
            )
        }
    }

    fun deleteInventoryItem(item: InventoryItem) {
        viewModelScope.launch {
            repository.deleteInventoryItem(item)
        }
    }

    // Custom Menu Item Actions
    fun addNewMenuItem(
        name: String,
        category: String,
        price: Double,
        hasIngredients: Boolean,
        ingredientsList: List<Pair<Long, Double>> // Pair(InventoryItemId, QuantityRequired)
    ) {
        viewModelScope.launch {
            val newItem = MenuItem(
                name = name,
                category = category,
                price = price,
                hasIngredients = hasIngredients
            )
            val menuItemId = repository.insertMenuItem(newItem)

            if (hasIngredients) {
                for ((invId, qty) in ingredientsList) {
                    repository.insertMenuItemIngredient(
                        MenuItemIngredient(
                            menuItemId = menuItemId,
                            inventoryItemId = invId,
                            quantityRequired = qty
                        )
                    )
                }
            }

            // Refresh recipe mappings
            val recipeMap = _menuItemRecipes.value.toMutableMap()
            recipeMap[menuItemId] = repository.getIngredientsForMenuItem(menuItemId)
            _menuItemRecipes.value = recipeMap
        }
    }

    fun deleteMenuItem(menuItem: MenuItem) {
        viewModelScope.launch {
            repository.deleteMenuItem(menuItem)
            repository.deleteIngredientsForMenuItem(menuItem.id)
            
            // Refresh recipe mappings
            val recipeMap = _menuItemRecipes.value.toMutableMap()
            recipeMap.remove(menuItem.id)
            _menuItemRecipes.value = recipeMap
        }
    }
}

// ViewModel Factory
class RestaurantViewModelFactory(private val repository: RestaurantRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RestaurantViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RestaurantViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
