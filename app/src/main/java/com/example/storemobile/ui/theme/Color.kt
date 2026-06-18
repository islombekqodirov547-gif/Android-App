package com.example.storemobile.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * JESKO brand palette — mirrors the WPF Desktop application so the
 * mobile app feels like the same product.
 */
object Jesko {
    // Backgrounds
    val BgDark = Color(0xFF0B1120)
    val BgPanel = Color(0xFF0F172A)
    val Card = Color(0xFF131D33)
    val CardElevated = Color(0xFF18243F)
    val Input = Color(0xFF1E293B)
    val InputBorder = Color(0xFF334155)
    val Border = Color(0xFF1F2A44)
    val Overlay = Color(0xCC05080F)

    // Text
    val TextPrimary = Color(0xFFF8FAFC)
    val TextSecondary = Color(0xFF94A3B8)
    val TextMuted = Color(0xFF64748B)

    // Accents
    val Gold = Color(0xFFF59E0B)
    val GoldLight = Color(0xFFFCD34D)
    val GoldDeep = Color(0xFFD97706)

    val Green = Color(0xFF22C55E)
    val GreenLight = Color(0xFF34D399)
    val GreenDeep = Color(0xFF16A34A)

    val Blue = Color(0xFF38BDF8)
    val BlueDeep = Color(0xFF1D4ED8)

    val Red = Color(0xFFF87171)
    val RedDeep = Color(0xFFDC2626)

    // Gradients
    val GoldGradient = Brush.horizontalGradient(
        listOf(Color(0xFFFBBF24), Color(0xFFF59E0B), Color(0xFFD97706))
    )
    val GoldGradientSoft = Brush.horizontalGradient(
        listOf(Color(0xFFFCD34D), Color(0xFFFBBF24))
    )
    val GreenGradient = Brush.horizontalGradient(
        listOf(Color(0xFF22C55E), Color(0xFF16A34A))
    )
    val BrandGradient = Brush.linearGradient(
        listOf(Color(0xFF111827), Color(0xFF1E293B), Color(0xFF0B1120))
    )
    val SidebarGradient = Brush.verticalGradient(
        listOf(Color(0xFF111827), Color(0xFF0B1322))
    )
}
