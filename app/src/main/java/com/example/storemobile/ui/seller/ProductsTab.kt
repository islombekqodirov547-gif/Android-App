package com.example.storemobile.ui.seller

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.storemobile.data.model.Product
import com.example.storemobile.ui.components.EmptyState
import com.example.storemobile.ui.components.JeskoButton
import com.example.storemobile.ui.components.JeskoButtonStyle
import com.example.storemobile.ui.components.JeskoTextField
import com.example.storemobile.ui.components.LoadingBox
import com.example.storemobile.ui.components.StatusPill
import com.example.storemobile.ui.theme.Jesko
import com.example.storemobile.util.Format

@Composable
fun ProductsTab(
    vm: SellerViewModel,
    sellerName: String,
    onOpenCart: () -> Unit
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var dialogProduct by remember { mutableStateOf<Product?>(null) }

    // ── Yengil avtomatik yangilash ──
    // 1) Mahsulotlar tabi ochiq turganda har 20 soniyada jimgina yangilanadi
    //    (faqat kichik /Products endpointi — serverni qiynamaydi).
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(20_000)
            vm.autoRefreshProducts()
        }
    }
    // 2) Ilova fonga o'tib qaytganda (yoki ekranga qaytilganda) darhol yangilanadi.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.autoRefreshProducts()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Header
        Row(
            Modifier.fillMaxWidth().padding(start = 18.dp, end = 14.dp, top = 14.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Assalomu alaykum,", color = Jesko.TextSecondary, fontSize = 13.sp)
                Text(sellerName, color = Jesko.TextPrimary, fontWeight = FontWeight.Black, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            RefreshButton(refreshing = ui.refreshing, onClick = { vm.refreshProducts() })
            Spacer(Modifier.width(10.dp))
            CartShortcut(count = ui.cartCount, onClick = onOpenCart)
        }

        // Search
        Box(Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
            JeskoTextField(
                value = ui.search,
                onValueChange = vm::setSearch,
                placeholder = "Mahsulot qidirish... (masalan: pep)",
                leadingIcon = Icons.Filled.Search,
                modifier = Modifier.fillMaxWidth(),
                trailing = if (ui.search.isNotEmpty()) {
                    {
                        Icon(
                            Icons.Filled.Close, "tozalash",
                            tint = Jesko.TextMuted,
                            modifier = Modifier.size(20.dp).clickable { vm.setSearch("") }
                        )
                    }
                } else null
            )
        }

        when {
            ui.productsLoading && ui.products.isEmpty() -> LoadingBox()
            ui.productsError != null && ui.products.isEmpty() -> Column(
                Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally
            ) {
                EmptyState(title = "Yuklab bo'lmadi", subtitle = ui.productsError!!)
                JeskoButton("Qayta urinish", { vm.loadProducts() }, style = JeskoButtonStyle.Outline, modifier = Modifier.padding(horizontal = 40.dp).fillMaxWidth())
            }
            ui.filteredProducts.isEmpty() -> EmptyState(
                title = if (ui.search.isBlank()) "Mahsulotlar yo'q" else "Topilmadi",
                subtitle = if (ui.search.isBlank()) "Admin mahsulot qo'shganda shu yerda ko'rinadi" else "Boshqa nom bilan qidiring",
                icon = Icons.Filled.Storefront
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(ui.filteredProducts, key = { it.id }) { product ->
                    ProductCard(product = product, onClick = { dialogProduct = product })
                }
            }
        }
    }

    dialogProduct?.let { product ->
        AddToCartDialog(
            product = product,
            onDismiss = { dialogProduct = null },
            onConfirm = { mode, count ->
                vm.addToCart(product, mode, count)
                dialogProduct = null
            }
        )
    }
}

@Composable
private fun RefreshButton(refreshing: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .background(Jesko.CardElevated, RoundedCornerShape(14.dp))
            .border(1.dp, Jesko.Border, RoundedCornerShape(14.dp))
            .clickable(enabled = !refreshing, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (refreshing) {
            CircularProgressIndicator(
                color = Jesko.GoldLight,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Icon(Icons.Filled.Refresh, "Yangilash", tint = Jesko.GoldLight, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun CartShortcut(count: Int, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .background(Jesko.CardElevated, RoundedCornerShape(14.dp))
            .border(1.dp, Jesko.Border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Filled.ShoppingCart, "Savat", tint = Jesko.GoldLight, modifier = Modifier.size(22.dp))
        if (count > 0) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(18.dp)
                    .background(Jesko.Red, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(if (count > 9) "9+" else count.toString(), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ProductCard(product: Product, onClick: () -> Unit) {
    val stockColor = when {
        product.totalPieces <= 0 -> Jesko.Red
        product.totalPieces < 10 -> Jesko.Gold
        else -> Jesko.Green
    }
    val enabled = product.inStock
    Column(
        Modifier
            .background(Jesko.Card, RoundedCornerShape(16.dp))
            .border(1.dp, Jesko.Border, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(38.dp).background(Jesko.CardElevated, RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(product.name.take(1).uppercase(), color = Jesko.GoldLight, fontWeight = FontWeight.Black, fontSize = 18.sp)
            }
            Spacer(Modifier.weight(1f))
            StatusPill(
                text = product.stockBadge,
                color = stockColor
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            product.name,
            color = Jesko.TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.height(40.dp)
        )
        Spacer(Modifier.height(6.dp))
        Text("${Format.money(product.sellPricePiece)} so'm / dona", color = Jesko.GoldLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        if (product.sellableByBlock) {
            Text(
                "${Format.money(product.sellPriceBlock)} so'm / blok (${product.quantityInBlock} dona)",
                color = Jesko.TextSecondary, fontSize = 11.sp
            )
        } else {
            Spacer(Modifier.height(15.dp))
        }
        Spacer(Modifier.height(8.dp))
        // Omborda qancha qolganini aniq ko'rsatamiz (blok/dona/kg yoki kamomad)
        Text(
            if (product.hasShortage) "⚠️ Kamomad: ${product.stockShort}" else "Omborda: ${product.stockShort}",
            color = if (product.hasShortage) Jesko.Red else Jesko.TextSecondary,
            fontWeight = if (product.hasShortage) FontWeight.Bold else FontWeight.Medium,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(if (enabled) Jesko.Gold.copy(alpha = 0.15f) else Jesko.Input, RoundedCornerShape(11.dp))
                .border(1.dp, if (enabled) Jesko.Gold.copy(alpha = 0.4f) else Jesko.Border, RoundedCornerShape(11.dp))
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Add, null, tint = if (enabled) Jesko.GoldLight else Jesko.TextMuted, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Qo'shish", color = if (enabled) Jesko.GoldLight else Jesko.TextMuted, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}