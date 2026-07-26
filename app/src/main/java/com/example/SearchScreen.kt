package com.example

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    repository: ProductRepository = remember { ProductRepository() },
    initialQuery: String = "",
    onProductClick: (Product) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    var searchQuery by remember { mutableStateOf(initialQuery) }
    var selectedCategory by remember { mutableStateOf("الكل") }
    var sortBy by remember { mutableStateOf("LATEST") } // "LATEST", "PRICE_LOW", "PRICE_HIGH", "RATING"
    var onlyInStock by remember { mutableStateOf(false) }

    var productsList by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddProductDialog by remember { mutableStateOf(false) }

    // Categories List
    val categories = remember {
        listOf("الكل", "إلكترونيات", "ساعات واكسسوارات", "عطور ومستحضرات", "ملابس وأزياء", "خدمات رقمية")
    }

    // Seed products & Listen to Firestore flow
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            repository.seedInitialProductsIfEmpty()
        }
    }

    LaunchedEffect(searchQuery, selectedCategory) {
        isLoading = true
        repository.observeProducts(searchQuery, selectedCategory).collect { list ->
            productsList = list
            isLoading = false
        }
    }

    // Filter and Sort local list
    val processedProducts = remember(productsList, sortBy, onlyInStock) {
        var result = productsList
        if (onlyInStock) {
            result = result.filter { it.inStock }
        }
        when (sortBy) {
            "PRICE_LOW" -> result.sortedBy { it.price }
            "PRICE_HIGH" -> result.sortedByDescending { it.price }
            "RATING" -> result.sortedByDescending { it.rating }
            else -> result.sortedByDescending { it.createdAt } // "LATEST"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("search_screen")
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Search Header Card
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_search_input"),
                    placeholder = { Text("ابحث عن منتج، متجر، أو تصنيف...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "بحث",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "مسح")
                                }
                            }
                            IconButton(onClick = { showAddProductDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.AddCircleOutline,
                                    contentDescription = "إضافة منتج جديد",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        keyboardController?.hide()
                    }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                // Category Chips Scrollable Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(categories) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category, fontWeight = if (selectedCategory == category) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                // Sorting & In-Stock Controls Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Result count text
                    Text(
                        text = "النتائج (${processedProducts.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // In Stock Toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onlyInStock = !onlyInStock }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Checkbox(
                                checked = onlyInStock,
                                onCheckedChange = { onlyInStock = it },
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("المتوفر فقط", style = MaterialTheme.typography.labelSmall)
                        }

                        // Sort Menu Chip
                        var showSortMenu by remember { mutableStateOf(false) }
                        Box {
                            AssistChip(
                                onClick = { showSortMenu = true },
                                label = {
                                    Text(
                                        when (sortBy) {
                                            "PRICE_LOW" -> "الأقل سعراً"
                                            "PRICE_HIGH" -> "الأعلى سعراً"
                                            "RATING" -> "الأعلى تقييماً"
                                            else -> "الأحدث"
                                        },
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Sort,
                                        contentDescription = "ترتيب",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("الأحدث ⏱️") },
                                    onClick = { sortBy = "LATEST"; showSortMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("السعر: من الأقل للأعلى 💵") },
                                    onClick = { sortBy = "PRICE_LOW"; showSortMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("السعر: من الأعلى للأقل 💎") },
                                    onClick = { sortBy = "PRICE_HIGH"; showSortMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("الأعلى تقييماً ⭐") },
                                    onClick = { sortBy = "RATING"; showSortMenu = false }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Product Grid Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text("جاري البحث في قاعدة البيانات...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else if (processedProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.SearchOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(56.dp)
                            )
                            Text(
                                text = "لم نجد أي منتجات تطابق البحث!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "جرب البحث بكلمات أخرى أو اختر تصنيفاً مختلفاً من الأعلى.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = {
                                    searchQuery = ""
                                    selectedCategory = "الكل"
                                    onlyInStock = false
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("إعادة ضبط الفلاتر")
                            }
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(processedProducts, key = { it.id }) { product ->
                        ProductGridItemCard(
                            product = product,
                            onClick = { onProductClick(product) },
                            onAddToCart = {
                                Toast.makeText(context, "تمت إضافة \"${product.name}\" إلى السلة 🛒", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }

    // Add Product Dialog to Firestore
    if (showAddProductDialog) {
        AddProductDialog(
            onDismiss = { showAddProductDialog = false },
            onSubmit = { newProduct ->
                coroutineScope.launch {
                    val result = repository.addProduct(newProduct)
                    if (result.isSuccess) {
                        Toast.makeText(context, "تم حفظ المنتج بنجاح في Firestore 🎉", Toast.LENGTH_SHORT).show()
                        showAddProductDialog = false
                    } else {
                        Toast.makeText(context, "حدث خطأ أثناء الإضافة: ${result.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }
}

@Composable
private fun ProductGridItemCard(
    product: Product,
    onClick: () -> Unit,
    onAddToCart: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("product_item_${product.id}")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Product Image Box with Badges
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (product.imageUrl.isNotBlank()) {
                    CoilAsyncImage(
                        imageUrl = product.imageUrl,
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ShoppingBag,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                // Discount Badge
                if (product.discountPercent > 0) {
                    Surface(
                        shape = RoundedCornerShape(bottomEnd = 12.dp, topStart = 16.dp),
                        color = Color(0xFFEF4444),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = "-${product.discountPercent}%",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Stock status badge
                if (!product.inStock) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.75f),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "نفذت الكمية",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Product Details Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = product.storeName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "التقييم",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f", product.rating),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = product.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    fontSize = 11.sp,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 14.sp
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Price and Add button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        if (product.discountPercent > 0) {
                            val originalPrice = product.price / (1 - product.discountPercent / 100.0)
                            Text(
                                text = "${String.format(Locale.US, "%.0f", originalPrice)} ${product.currency}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                textDecoration = TextDecoration.LineThrough,
                                fontSize = 10.sp
                            )
                        }
                        Text(
                            text = "${String.format(Locale.US, "%.0f", product.price)} ${product.currency}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = onAddToCart,
                        enabled = product.inStock,
                        modifier = Modifier
                            .size(34.dp)
                            .background(
                                color = if (product.inStock) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddShoppingCart,
                            contentDescription = "إضافة للسلة",
                            tint = if (product.inStock) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddProductDialog(
    onDismiss: () -> Unit,
    onSubmit: (Product) -> Unit
) {
    var nameText by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var categoryText by remember { mutableStateOf("إلكترونيات") }
    var storeText by remember { mutableStateOf("متجري الشخصي") }
    var descText by remember { mutableStateOf("") }
    var imageUrlText by remember { mutableStateOf("") }

    val categories = listOf("إلكترونيات", "ساعات واكسسوارات", "عطور ومستحضرات", "ملابس وأزياء", "خدمات رقمية")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.AddBox, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("إضافة منتج جديد للـ Firestore 🛍️", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("اسم المنتج") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("السعر (SAR)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = storeText,
                        onValueChange = { storeText = it },
                        label = { Text("اسم المتجر") },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Text("التصنيف:", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = categoryText == cat,
                            onClick = { categoryText = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = imageUrlText,
                    onValueChange = { imageUrlText = it },
                    label = { Text("رابط الصورة (URL - اختياري)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = descText,
                    onValueChange = { descText = it },
                    label = { Text("وصف المنتج") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = priceText.toDoubleOrNull() ?: 0.0
                    if (nameText.isNotBlank()) {
                        val product = Product(
                            name = nameText,
                            price = price,
                            category = categoryText,
                            storeName = storeText.ifBlank { "متجر رفيق" },
                            description = descText,
                            imageUrl = imageUrlText
                        )
                        onSubmit(product)
                    }
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("حفظ ونشر المنتج")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
