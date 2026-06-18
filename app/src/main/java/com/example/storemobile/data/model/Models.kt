package com.example.storemobile.data.model

/* ─────────────────────────  API: Products  ───────────────────────── */

data class Product(
    val id: Int = 0,
    val name: String = "",
    val unit: String = "dona",
    val quantityInBlock: Int = 1,
    val buyPriceBlock: Double = 0.0,
    val sellPriceBlock: Double = 0.0,
    val sellPricePiece: Double = 0.0,
    val totalPieces: Int = 0
) {
    val inStock: Boolean get() = totalPieces > 0
    val blocks: Int get() = if (quantityInBlock > 0) totalPieces / quantityInBlock else 0
    val loosePieces: Int get() = if (quantityInBlock > 0) totalPieces % quantityInBlock else totalPieces

    /** Whether selling by block makes sense for this product. */
    val sellableByBlock: Boolean
        get() = quantityInBlock > 1 && sellPriceBlock > 0.0

    val isKg: Boolean get() = unit.equals("kg", ignoreCase = true)
    val isBlockType: Boolean get() = quantityInBlock > 1 && !isKg

    /** True when stock is negative (buxgalter prixod qilmagan / kamomad). */
    val hasShortage: Boolean get() = totalPieces < 0

    /**
     * Short, unit-aware stock label.
     * kg     -> "12 kg"
     * dona   -> "40 dona"
     * blok   -> "3 blok 5 dona"
     * manfiy -> "-7 dona" (kamomad)
     */
    val stockShort: String
        get() = when {
            isKg -> "$totalPieces kg"
            !isBlockType -> "$totalPieces dona"
            totalPieces < 0 -> "$totalPieces dona"
            else -> {
                val b = totalPieces / quantityInBlock
                val p = totalPieces % quantityInBlock
                when {
                    b > 0 && p > 0 -> "$b blok $p dona"
                    b > 0 -> "$b blok"
                    else -> "$p dona"
                }
            }
        }

    /** Status pill text used on product cards. */
    val stockBadge: String
        get() = when {
            totalPieces == 0 -> "Tugagan"
            hasShortage -> "Kamomad: $stockShort"
            else -> stockShort
        }

    /** Detailed stock label for the add-to-cart dialog header. */
    val stockLong: String
        get() = when {
            isKg -> "Omborda: $totalPieces kg"
            !isBlockType -> "Omborda: $totalPieces dona"
            hasShortage -> "Omborda: $totalPieces dona (kamomad — prixod qilinmagan)"
            else -> "Omborda: $stockShort  (jami $totalPieces dona)"
        }
}

/* ─────────────────────────  API: Clients  ───────────────────────── */

data class Client(
    val id: Int = 0,
    val name: String = "",
    val phone: String? = null,
    val debtBalance: Double = 0.0
) {
    /** Treat sub-1 so'm residual as no debt. */
    val hasDebt: Boolean get() = debtBalance > 0.5
}

data class NewClientRequest(
    val name: String,
    val phone: String?
)

/* ─────────────────────────  API: Users / Sellers  ───────────────────────── */

data class Seller(
    val id: Int = 0,
    val fullName: String = "",
    val username: String = ""
)

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val id: Int = 0,
    val fullName: String = "",
    val username: String = "",
    val role: String = ""
)

/* ─────────────────────────  API: Orders  ───────────────────────── */

data class OrderRequest(
    val clientId: Int?,
    val userId: Int?,
    val totalSum: Double,
    val items: List<OrderItemRequest>
)

data class OrderItemRequest(
    val productId: Int,
    val quantity: Int,
    val price: Double
)

data class Order(
    val id: Int = 0,
    val client: Client? = null,
    val user: Seller? = null,
    val totalSum: Double = 0.0,
    val paidSum: Double = 0.0,
    val status: String = "",
    val paymentType: String = "",
    val createdAt: String = "",
    val items: List<OrderItem> = emptyList()
) {
    val clientName: String get() = client?.name ?: "Naqd xaridor"
    val sellerName: String get() = user?.fullName ?: "—"
    val itemCount: Int get() = items.sumOf { it.quantity }
}

data class OrderItem(
    val id: Int = 0,
    val productId: Int = 0,
    val product: Product? = null,
    val quantity: Int = 0,
    val price: Double = 0.0
) {
    val total: Double get() = quantity * price
    val displayName: String get() = product?.name ?: "Mahsulot #$productId"
}

/* ─────────────────────────  Local: Cart  ───────────────────────── */

enum class SaleMode { BLOCK, PIECE }

/**
 * One line in the seller's cart. A product can appear twice — once as
 * blocks and once as loose pieces — so the identity is product + mode.
 */
data class CartLine(
    val product: Product,
    val mode: SaleMode,
    val count: Int
) {
    val key: String get() = "${product.id}-${mode.name}"

    /** Number of physical pieces this line consumes from stock. */
    val pieces: Int
        get() = if (mode == SaleMode.BLOCK) count * product.quantityInBlock else count

    /** Price per single piece (used by the API). */
    val pricePerPiece: Double
        get() = if (mode == SaleMode.BLOCK) {
            if (product.quantityInBlock > 0) product.sellPriceBlock / product.quantityInBlock
            else product.sellPriceBlock
        } else product.sellPricePiece

    /** Total money for this line. */
    val lineTotal: Double
        get() = if (mode == SaleMode.BLOCK) count * product.sellPriceBlock
        else count * product.sellPricePiece

    val unitLabel: String get() = if (mode == SaleMode.BLOCK) "blok" else "dona"
}