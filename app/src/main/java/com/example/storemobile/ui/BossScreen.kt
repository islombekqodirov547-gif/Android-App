package com.example.storemobile.ui.boss

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.storemobile.data.model.Client
import com.example.storemobile.data.model.PendingOperation
import com.example.storemobile.data.model.Supplier
import com.example.storemobile.ui.components.EmptyState
import com.example.storemobile.ui.components.JeskoButton
import com.example.storemobile.ui.components.JeskoButtonStyle
import com.example.storemobile.ui.components.JeskoTextField
import com.example.storemobile.ui.components.LoadingBox
import com.example.storemobile.ui.components.StatusPill
import com.example.storemobile.ui.components.clickableNoRipple
import com.example.storemobile.ui.theme.Jesko
import com.example.storemobile.util.Format

private enum class BossTab(val title: String, val icon: ImageVector) {
    Debtors("Qarzdorlar", Icons.Filled.Groups),
    Suppliers("Firmalar", Icons.Filled.LocalShipping),
    Sync("Sinxron", Icons.Filled.CloudSync)
}

@Composable
fun BossScreen(
    vm: BossViewModel,
    onLoggedOut: () -> Unit
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(BossTab.Debtors) }
    val snackbar = remember { SnackbarHostState() }

    // To'lov dialogi uchun tanlangan mijoz/firma
    var payClient by remember { mutableStateOf<Client?>(null) }
    var paySupplier by remember { mutableStateOf<Supplier?>(null) }

    LaunchedEffect(ui.toast) {
        ui.toast?.let {
            snackbar.showSnackbar(it)
            vm.consumeToast()
        }
    }

    Box(Modifier.fillMaxSize().background(Jesko.BgDark)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "bosstab"
                ) { current ->
                    when (current) {
                        BossTab.Debtors -> DebtorsTab(vm, ui, onPay = { payClient = it })
                        BossTab.Suppliers -> SuppliersTab(vm, ui, onPay = { paySupplier = it })
                        BossTab.Sync -> SyncTab(vm, ui, onLoggedOut = onLoggedOut)
                    }
                }
            }
            BossBottomBar(selected = tab, pendingCount = ui.pendingCount, onSelect = { tab = it })
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 96.dp, start = 12.dp, end = 12.dp)
        )
    }

    // ── Mijoz qarzini to'lash dialogi ──
    payClient?.let { c ->
        PaymentDialog(
            title = "Qarz to'lovi",
            name = c.name,
            subtitle = c.phone ?: "—",
            currentDebt = c.debtBalance,
            accent = Jesko.Green,
            confirmLabel = "Qabul qilish",
            onDismiss = { payClient = null },
            onConfirm = { amount, note ->
                vm.payClient(c, amount, note) { ok -> if (ok) payClient = null }
            }
        )
    }

    // ── Firmaga to'lash dialogi ──
    paySupplier?.let { s ->
        PaymentDialog(
            title = "Firmaga to'lov",
            name = s.name,
            subtitle = s.phone ?: "—",
            currentDebt = s.debtBalance,
            accent = Jesko.Gold,
            confirmLabel = "To'lash",
            onDismiss = { paySupplier = null },
            onConfirm = { amount, note ->
                vm.paySupplier(s, amount, note) { ok -> if (ok) paySupplier = null }
            }
        )
    }
}

/* ─────────────────────────  QARZDORLAR  ───────────────────────── */

@Composable
private fun DebtorsTab(vm: BossViewModel, ui: BossUiState, onPay: (Client) -> Unit) {
    val debtors = ui.filteredClients.filter { it.hasDebt }.sortedByDescending { it.debtBalance }

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
        TabHeader(
            title = "Qarzdorlar",
            subtitle = "Mijozlardan qarz yig'ish (internetsiz ishlaydi)"
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard("Umumiy qarz", Format.money(ui.totalClientDebt), "so'm", Jesko.Red, Modifier.weight(1.5f))
            StatCard("Yig'ildi (navbatda)", Format.money(ui.pendingClientTotal), "so'm", Jesko.Green, Modifier.weight(1.5f))
        }
        Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            JeskoTextField(
                value = ui.clientSearch,
                onValueChange = vm::setClientSearch,
                placeholder = "Mijozni qidirish...",
                leadingIcon = Icons.Filled.Search,
                modifier = Modifier.fillMaxWidth()
            )
        }

        when {
            ui.loading -> LoadingBox()
            !ui.hasSnapshot -> EmptyState(
                title = "Ma'lumot yo'q",
                subtitle = "Avval do'kon WiFi'sida \"Sinxron\" bo'limidan ma'lumotlarni yuklab oling.",
                icon = Icons.Filled.CloudSync
            )
            debtors.isEmpty() -> EmptyState(
                title = "Qarzdor mijoz yo'q",
                subtitle = "Hamma qarzlar to'langan yoki qidiruvga mos mijoz topilmadi.",
                icon = Icons.Filled.Groups
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(debtors, key = { it.id }) { c ->
                    DebtRow(
                        name = c.name,
                        subtitle = c.phone ?: "—",
                        debt = c.debtBalance,
                        pending = ui.pendingByClient[c.id] ?: 0.0,
                        accent = Jesko.Red,
                        onClick = { onPay(c) }
                    )
                }
            }
        }
    }
}

/* ─────────────────────────  FIRMALAR  ───────────────────────── */

@Composable
private fun SuppliersTab(vm: BossViewModel, ui: BossUiState, onPay: (Supplier) -> Unit) {
    val owed = ui.filteredSuppliers.filter { it.hasDebt }.sortedByDescending { it.debtBalance }

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
        TabHeader(
            title = "Firmalar",
            subtitle = "Firmalarga qarzni to'lash (internetsiz ishlaydi)"
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard("Jami qarzimiz", Format.money(ui.totalSupplierDebt), "so'm", Jesko.Gold, Modifier.weight(1.5f))
            StatCard("To'landi (navbatda)", Format.money(ui.pendingSupplierTotal), "so'm", Jesko.Green, Modifier.weight(1.5f))
        }
        Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            JeskoTextField(
                value = ui.supplierSearch,
                onValueChange = vm::setSupplierSearch,
                placeholder = "Firmani qidirish...",
                leadingIcon = Icons.Filled.Search,
                modifier = Modifier.fillMaxWidth()
            )
        }

        when {
            ui.loading -> LoadingBox()
            !ui.hasSnapshot -> EmptyState(
                title = "Ma'lumot yo'q",
                subtitle = "Avval do'kon WiFi'sida \"Sinxron\" bo'limidan ma'lumotlarni yuklab oling.",
                icon = Icons.Filled.CloudSync
            )
            owed.isEmpty() -> EmptyState(
                title = "Qarzimiz yo'q firma",
                subtitle = "Barcha firmalarga to'lov qilingan yoki qidiruvga mos firma topilmadi.",
                icon = Icons.Filled.LocalShipping
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(owed, key = { it.id }) { s ->
                    DebtRow(
                        name = s.name,
                        subtitle = s.phone ?: "—",
                        debt = s.debtBalance,
                        pending = ui.pendingBySupplier[s.id] ?: 0.0,
                        accent = Jesko.Gold,
                        onClick = { onPay(s) }
                    )
                }
            }
        }
    }
}

/* ─────────────────────────  SINXRON  ───────────────────────── */

@Composable
private fun SyncTab(vm: BossViewModel, ui: BossUiState, onLoggedOut: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        TabHeader(title = "Sinxron", subtitle = "Do'kon serveri bilan ma'lumotlarni tenglashtirish")

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                // Asosiy sinxron kartasi
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Jesko.BrandGradient, RoundedCornerShape(20.dp))
                        .border(1.dp, Jesko.Gold.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier.size(64.dp).background(Jesko.CardElevated, CircleShape)
                            .border(2.dp, Jesko.Gold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Sync, null, tint = Jesko.GoldLight, modifier = Modifier.size(32.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (ui.pendingCount > 0) "${ui.pendingCount} ta amal kutilmoqda" else "Hammasi sinxron",
                        color = Jesko.TextPrimary, fontWeight = FontWeight.Black, fontSize = 18.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Oxirgi sinxron: " + (if (ui.lastSyncIso.isBlank()) "hali bo'lmagan" else Format.dateTime(ui.lastSyncIso)),
                        color = Jesko.TextSecondary, fontSize = 12.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    JeskoButton(
                        text = if (ui.pendingCount > 0) "Sinxronlash (${ui.pendingCount})" else "Ma'lumotni yangilash",
                        onClick = { vm.syncNow() },
                        style = JeskoButtonStyle.Gold,
                        loading = ui.syncing,
                        leadingIcon = Icons.Filled.CloudSync,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Do'kon WiFi'siga ulangan holda bosing. Ko'chada (internetsiz) amallar telefonda saqlanadi.",
                        color = Jesko.TextMuted, fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            if (ui.syncSummary != null) {
                item {
                    Box(
                        Modifier.fillMaxWidth()
                            .background(Jesko.Green.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            .border(1.dp, Jesko.Green.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Text(ui.syncSummary!!, color = Jesko.GreenLight, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Navbatdagi amallar (yuborilmagan)
            if (ui.pending.isNotEmpty()) {
                item {
                    Text("KUTILAYOTGAN AMALLAR", color = Jesko.TextSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                items(ui.pending, key = { it.operationId }) { op ->
                    PendingRow(op = op, onCancel = { vm.cancelPending(op) })
                }
            }

            // Profil / chiqish
            item {
                Spacer(Modifier.height(4.dp))
                Row(
                    Modifier.fillMaxWidth()
                        .background(Jesko.Card, RoundedCornerShape(12.dp))
                        .border(1.dp, Jesko.Border, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(40.dp).background(Jesko.CardElevated, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            vm.userName.take(1).uppercase(),
                            color = Jesko.GoldLight, fontWeight = FontWeight.Black, fontSize = 18.sp
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(vm.userName, color = Jesko.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Boshliq", color = Jesko.TextSecondary, fontSize = 12.sp)
                    }
                }
            }
            item {
                JeskoButton(
                    text = "Hisobdan chiqish",
                    onClick = { vm.logout(onLoggedOut) },
                    style = JeskoButtonStyle.Danger,
                    leadingIcon = Icons.AutoMirrored.Filled.Logout,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

/* ─────────────────────────  TO'LOV DIALOGI  ───────────────────────── */

@Composable
private fun PaymentDialog(
    title: String,
    name: String,
    subtitle: String,
    currentDebt: Double,
    accent: Color,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (Double, String?) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val amount = amountText.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
    val valid = amount > 0
    val remaining = (currentDebt - amount).coerceAtLeast(0.0)

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Jesko.Card, RoundedCornerShape(22.dp))
                .border(1.dp, Jesko.Border, RoundedCornerShape(22.dp))
                .padding(20.dp)
        ) {
            Text(title, color = Jesko.TextPrimary, fontWeight = FontWeight.Black, fontSize = 19.sp)
            Spacer(Modifier.height(2.dp))
            Text(name, color = accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(subtitle, color = Jesko.TextSecondary, fontSize = 12.sp)

            Spacer(Modifier.height(14.dp))

            // Joriy qarz
            Row(
                Modifier.fillMaxWidth()
                    .background(Jesko.BgPanel, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Joriy qarz", color = Jesko.TextSecondary, fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                Text("${Format.money(currentDebt)} so'm", color = Jesko.Red, fontWeight = FontWeight.Black, fontSize = 17.sp)
            }

            Spacer(Modifier.height(12.dp))

            JeskoTextField(
                value = amountText,
                onValueChange = { amountText = it.filter { ch -> ch.isDigit() } },
                placeholder = "To'lov summasi (so'm)",
                leadingIcon = Icons.Filled.AccountBalanceWallet,
                keyboardType = KeyboardType.Number,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                JeskoButton(
                    text = "To'liq qarz",
                    onClick = { amountText = Math.round(currentDebt).toString() },
                    style = JeskoButtonStyle.Outline,
                    height = 44,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(10.dp))
            JeskoTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = "Izoh (ixtiyoriy)",
                modifier = Modifier.fillMaxWidth()
            )

            if (valid) {
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth()
                        .background(accent.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("To'lovdan keyin qoladi", color = Jesko.TextSecondary, fontSize = 13.sp)
                    Spacer(Modifier.weight(1f))
                    Text("${Format.money(remaining)} so'm", color = accent, fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
            }

            Spacer(Modifier.height(16.dp))
            Row {
                JeskoButton("Bekor", onDismiss, style = JeskoButtonStyle.Outline, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(10.dp))
                JeskoButton(
                    text = confirmLabel,
                    onClick = { if (valid) onConfirm(amount, note.ifBlank { null }) },
                    style = JeskoButtonStyle.Green,
                    enabled = valid,
                    modifier = Modifier.weight(1.4f)
                )
            }
        }
    }
}

/* ─────────────────────────  UMUMIY KOMPONENTLAR  ───────────────────────── */

@Composable
private fun TabHeader(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(start = 18.dp, end = 16.dp, top = 16.dp, bottom = 6.dp)) {
        Text(title, color = Jesko.TextPrimary, fontWeight = FontWeight.Black, fontSize = 22.sp)
        Text(subtitle, color = Jesko.TextSecondary, fontSize = 13.sp)
    }
}

@Composable
private fun StatCard(label: String, value: String, unit: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(Jesko.Card, RoundedCornerShape(14.dp))
            .border(1.dp, Jesko.Border, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Text(label, color = Jesko.TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, color = color, fontWeight = FontWeight.Black, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.width(3.dp))
            Text(unit, color = Jesko.TextMuted, fontSize = 10.sp, modifier = Modifier.padding(bottom = 3.dp))
        }
    }
}

@Composable
private fun DebtRow(
    name: String,
    subtitle: String,
    debt: Double,
    pending: Double,
    accent: Color,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Jesko.Card, RoundedCornerShape(14.dp))
            .border(1.dp, Jesko.Border, RoundedCornerShape(14.dp))
            .clickableNoRipple(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(42.dp).background(Jesko.CardElevated, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(name.take(1).uppercase(), color = Jesko.GoldLight, fontWeight = FontWeight.Black, fontSize = 17.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(name, color = Jesko.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = Jesko.TextSecondary, fontSize = 11.sp)
            if (pending > 0.5) {
                Spacer(Modifier.height(4.dp))
                StatusPill("Navbatda: ${Format.money(pending)} so'm", Jesko.Green)
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text("${Format.money(debt)}", color = accent, fontWeight = FontWeight.Black, fontSize = 16.sp)
            Text("so'm", color = Jesko.TextMuted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun PendingRow(op: PendingOperation, onCancel: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Jesko.Card, RoundedCornerShape(12.dp))
            .border(1.dp, Jesko.Border, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(op.entityName, color = Jesko.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(
                (if (op.isSupplier) "Firmaga to'lov" else "Qarz to'lovi") +
                        " · " + Format.dateTime(op.createdAt),
                color = Jesko.TextSecondary, fontSize = 11.sp
            )
        }
        Text("${Format.money(op.amount)} so'm", color = Jesko.GreenLight, fontWeight = FontWeight.Black, fontSize = 15.sp)
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier.size(30.dp).background(Jesko.Input, CircleShape).clickableNoRipple(onClick = onCancel),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Close, "bekor qilish", tint = Jesko.Red, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun BossBottomBar(selected: BossTab, pendingCount: Int, onSelect: (BossTab) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Jesko.BgPanel)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BossTab.entries.forEach { item ->
            BossBottomItem(
                item = item,
                active = item == selected,
                badge = if (item == BossTab.Sync && pendingCount > 0) pendingCount else null,
                modifier = Modifier.weight(1f),
                onClick = { onSelect(item) }
            )
        }
    }
}

@Composable
private fun BossBottomItem(
    item: BossTab,
    active: Boolean,
    badge: Int?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier.clickableNoRipple(onClick = onClick).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.height(34.dp).width(64.dp).background(
                if (active) Jesko.Gold.copy(alpha = 0.16f) else Color.Transparent,
                RoundedCornerShape(18.dp)
            ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                item.icon, item.title,
                tint = if (active) Jesko.GoldLight else Jesko.TextMuted,
                modifier = Modifier.size(23.dp)
            )
            if (badge != null) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(end = 8.dp).size(18.dp)
                        .background(Jesko.Red, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (badge > 9) "9+" else badge.toString(),
                        color = Jesko.TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(
            item.title,
            color = if (active) Jesko.GoldLight else Jesko.TextMuted,
            fontSize = 10.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
        )
    }
}