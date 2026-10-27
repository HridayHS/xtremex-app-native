package com.xtremex.data

import java.math.BigDecimal

// Models ported from docs/DATA.md. Prices are integer paise (cents)
// internally; formatting happens only at the UI layer.
// Variant ids are kept as strings: they are 64-bit-ish (e.g. 46955372052736).
data class Variant(
    val id: String,
    val title: String,
    val pricePaise: Int,
    val compareAtPricePaise: Int?,
    val available: Boolean,
    val option1: String?,
    val option2: String?,
    val option3: String?,
    val position: Int,
    val imageId: String? = null
)

data class OptionDefinition(
    val key: String,
    val name: String,
    val position: Int,
    val values: List<String>
)

data class Product(
    val id: String,
    val title: String,
    val handle: String,
    val bodyHtml: String,
    val vendor: String,
    val tags: List<String>,
    val variants: List<Variant>,
    val options: List<OptionDefinition>,
    val images: List<String>,
    val createdAt: String,
    val publishedAt: String,
    val imageIds: List<String> = emptyList()
) {
    val image: String get() = images.firstOrNull().orEmpty()
    val firstVariant: Variant? get() = variants.firstOrNull()
    val inStock: Boolean get() = variants.any { it.available }
}

fun formatPricePaise(paise: Int?): String {
    if (paise == null) return ""
    // Indian numbering: ₹1,499 — use plain grouping, no decimals for whole rupees.
    val rupees = BigDecimal(paise).divide(BigDecimal(100))
    val plain = rupees.stripTrailingZeros().toPlainString()
    val parts = plain.split(".")
    val intPart = parts[0]
    val grouped = buildString {
        // Indian grouping: last 3 digits, then groups of 2.
        if (intPart.length <= 3) {
            append(intPart)
        } else {
            val last3 = intPart.takeLast(3)
            var rest = intPart.dropLast(3)
            val groups = mutableListOf<String>()
            while (rest.length > 2) {
                groups.add(0, rest.takeLast(2))
                rest = rest.dropLast(2)
            }
            if (rest.isNotEmpty()) groups.add(0, rest)
            append(groups.joinToString(","))
            append(",")
            append(last3)
        }
    }
    return if (parts.size > 1) "₹$grouped.${parts[1]}" else "₹$grouped"
}
