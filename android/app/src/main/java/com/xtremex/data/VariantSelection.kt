package com.xtremex.data

// Port of utils/variantUtils.ts: option definitions, default variant,
// selection matching, and per-value availability. Option names in Shopify
// carry a trailing colon ("Size:") — normalised before matching.
fun norm(v: String?): String = (v ?: "").trim()
fun normKey(v: String?): String = norm(v).lowercase()

fun optionDefinitionsOf(p: Product): List<OptionDefinition> {
    val defs = mutableListOf<OptionDefinition>()
    p.options.take(3).forEachIndexed { idx, opt ->
        val pos = opt.position.takeIf { it > 0 } ?: (idx + 1)
        val name = norm(opt.name.ifEmpty { "Option ${idx + 1}" })
        val seen = linkedSetOf<String>()
        p.variants.forEach { v ->
            val value = when (pos) { 1 -> v.option1; 2 -> v.option2; else -> v.option3 }
            if (!value.isNullOrEmpty()) seen.add(value)
        }
        if (normKey(name) !in listOf("title", "default title") && seen.isNotEmpty()
            && !(seen.size == 1 && normKey(seen.first()) == "default title")) {
            defs.add(OptionDefinition("option$pos", name, pos, seen.toList()))
        }
    }
    val priority = { n: String -> when { normKey(n).contains("color") || normKey(n).contains("colour") -> 0
        normKey(n).contains("size") -> 1; else -> 2 } }
    return defs.sortedWith(compareBy({ priority(it.name) }, { it.position }))
}

fun variantOption(v: Variant, pos: Int): String = norm(when (pos) { 1 -> v.option1; 2 -> v.option2; else -> v.option3 })
fun isDefaultTitle(v: String?): Boolean = normKey(v) == "default title"

fun defaultVariant(p: Product): Variant? {
    val available = p.variants.filter { it.available }
    if (available.isEmpty()) return p.variants.firstOrNull()
    val defs = optionDefinitionsOf(p)
    val color = defs.firstOrNull { normKey(it.name).contains("color") || normKey(it.name).contains("colour") }
    if (color == null || color.values.isEmpty()) return available.firstOrNull()
    for (cv in color.values) {
        available.firstOrNull { variantOption(it, color.position) == cv }?.let { return it }
    }
    return available.firstOrNull()
}

fun findVariant(p: Product, defs: List<OptionDefinition>, sel: Map<Int, String>): Variant? =
    p.variants.firstOrNull { v -> defs.all { variantOption(v, it.position) == norm(sel[it.position]) } }

fun variantDisplayTitle(v: Variant?): String {
    val t = norm(v?.title)
    return if (isDefaultTitle(t)) "" else t
}

/** Availability of each option value given the current selection (colours always all shown). */
fun availabilityMap(p: Product, defs: List<OptionDefinition>, sel: Map<Int, String>): Map<Int, Set<String>> {
    val available = p.variants.filter { it.available }
    val out = mutableMapOf<Int, Set<String>>()
    for (def in defs) {
        if (normKey(def.name).contains("color") || normKey(def.name).contains("colour")) {
            out[def.position] = def.values.map { norm(it) }.toSet(); continue
        }
        val ok = mutableSetOf<String>()
        for (value in def.values) {
            val nv = norm(value)
            val next = sel + (def.position to nv)
            if (available.any { v -> defs.all { d -> variantOption(v, d.position) == norm(next[d.position]) } }) ok.add(nv)
        }
        out[def.position] = ok
    }
    return out
}

fun selectOption(p: Product, defs: List<OptionDefinition>, sel: Map<Int, String>, pos: Int, value: String): Map<Int, String> {
    val next = sel + (pos to norm(value))
    val exact = findVariant(p, defs, next)
    if (exact != null && exact.available) return next
    val changed = defs.firstOrNull { it.position == pos }
    if (changed != null && (normKey(changed.name).contains("color") || normKey(changed.name).contains("colour"))) {
        val fb = p.variants.filter { it.available }.firstOrNull { variantOption(it, pos) == norm(value) }
        if (fb != null) {
            val m = mutableMapOf<Int, String>()
            defs.forEach { d -> m[d.position] = variantOption(fb, d.position) }
            return m
        }
    }
    return next
}

fun initialSelection(p: Product, defs: List<OptionDefinition>, initialVariantId: String? = null): Map<Int, String> {
    val v = p.variants.firstOrNull { it.id == initialVariantId } ?: defaultVariant(p) ?: return emptyMap()
    return defs.associate { it.position to variantOption(v, it.position) }
}

fun variantImageIndex(p: Product, v: Variant?): Int {
    if (v == null) return -1
    // .js featured_image src match, else image_id match against .json image ids.
    val imgId = v.imageId
    if (!imgId.isNullOrEmpty()) {
        val i = p.imageIds.indexOf(imgId)
        if (i >= 0) return i
    }
    return -1
}
