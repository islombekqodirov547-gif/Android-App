package com.example.storemobile.ui.seller

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.storemobile.data.model.Product
import com.example.storemobile.data.model.SaleMode
import com.example.storemobile.ui.components.JeskoButton
import com.example.storemobile.ui.components.QuantityStepper
import com.example.storemobile.ui.components.clickableNoRipple
import com.example.storemobile.ui.theme.Jesko
import com.example.storemobile.util.Format

@Composable
fun AddToCartDialog(
    product: Product,
    onDismiss: () -> Unit,
    onConfirm: (SaleMode, Int) -> Unit
) {
    var mode by remember { mutableStateOf(if (product.sellableByBlock) SaleMode.BLOCK else SaleMode.PIECE) }
    var count by remember { mutableIntStateOf(1) }

    val maxCount = remember(mode) {
        if (mode == SaleMode.BLOCK && product.quantityInBlock > 0)
            (product.totalPieces / product.quantityInBlock).coerceAtLeast(1)
        else product.totalPieces.coerceAtLeast(1)
    }
    if (count > maxCount) count = maxCount

    val unitPrice = if (mode == SaleMode.BLOCK) product.sellPriceBlock else product.sellPricePiece
    val total = unitPrice * count

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Jesko.Card, RoundedCornerShape(22.dp))
                .border(1.dp, Jesko.Border, RoundedCornerShape(22.dp))
                .padding(20.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).background(Jesko.CardElevated, RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(product.name.take(1).uppercase(), color = Jesko.GoldLight, fontWeight = FontWeight.Black, fontSize = 20.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(product.name, color = Jesko.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 2)
                    Text(
                        product.stockLong,
                        color = if (product.hasShortage) Jesko.Red else Jesko.TextSecondary,
                        fontWeight = if (product.hasShortage) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp
                    )
                }
                Box(
                    Modifier.size(34.dp).background(Jesko.Input, CircleShape).clickableNoRipple(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Close, "yopish", tint = Jesko.TextSecondary, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(18.dp))

            // Mode selector
            if (product.sellableByBlock) {
                Text("O'LCHOV BIRLIGI", color = Jesko.TextSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Jesko.BgPanel, RoundedCornerShape(12.dp))
                        .padding(4.dp)
                ) {
                    ModeChip(
                        label = "Blok",
                        sub = "${Format.money(product.sellPriceBlock)} so'm",
                        active = mode == SaleMode.BLOCK,
                        modifier = Modifier.weight(1f)
                    ) { mode = SaleMode.BLOCK; count = 1 }
                    Spacer(Modifier.width(4.dp))
                    ModeChip(
                        label = "Dona",
                        sub = "${Format.money(product.sellPricePiece)} so'm",
                        active = mode == SaleMode.PIECE,
                        modifier = Modifier.weight(1f)
                    ) { mode = SaleMode.PIECE; count = 1 }
                }
                Spacer(Modifier.height(18.dp))
            }

            // Quantity
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Soni", color = Jesko.TextSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text(
                        if (mode == SaleMode.BLOCK) "blok hisobida" else "dona hisobida",
                        color = Jesko.TextMuted, fontSize = 11.sp
                    )
                }
                QuantityStepper(
                    value = count,
                    onMinus = { if (count > 1) count-- },
                    onPlus = { if (count < maxCount) count++ },
                    minusEnabled = count > 1,
                    plusEnabled = count < maxCount,
                    buttonSize = 44
                )
            }

            Spacer(Modifier.height(18.dp))

            // Total
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Jesko.BgPanel, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Jami", color = Jesko.TextSecondary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.weight(1f))
                Text("${Format.money(total)} so'm", color = Jesko.GoldLight, fontWeight = FontWeight.Black, fontSize = 20.sp)
            }

            Spacer(Modifier.height(18.dp))

            JeskoButton(
                text = "Savatga qo'shish",
                onClick = { onConfirm(mode, count) },
                modifier = Modifier.fillMaxWidth(),
                height = 52
            )
        }
    }
}

@Composable
private fun ModeChip(
    label: String,
    sub: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier
            .background(if (active) Jesko.Gold else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(9.dp))
            .clickableNoRipple(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = if (active) Jesko.BgDark else Jesko.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(sub, color = if (active) Jesko.BgDark.copy(alpha = 0.7f) else Jesko.TextMuted, fontSize = 11.sp)
    }
}