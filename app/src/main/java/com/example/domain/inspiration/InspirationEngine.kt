package com.example.domain.inspiration

import com.example.data.dao.SupplyWithBatches

data class InspirationPrompt(
    val title: String,
    val craftType: String,
    val description: String,
    val suggestedSearchQuery: String,
    val applicableCategory: String,
    val isBeginnerFriendly: Boolean,
    val isSmallProject: Boolean,
    val isScrapFriendly: Boolean,
    val requiresMachine: Boolean,
    val intendedUse: String // "Gift", "Accessory", "Decorative", "Home Organization"
)

object InspirationEngine {

    val BUNDLED_PROMPTS = listOf(
        InspirationPrompt(
            title = "Patchwork Coasters & Mug Rugs",
            craftType = "Quilting / Sewing",
            description = "Piece small cotton fabric scraps or remnants into quick quilted coasters or mini mug mats.",
            suggestedSearchQuery = "beginner patchwork mug rug fabric scraps tutorial",
            applicableCategory = "Fabric",
            isBeginnerFriendly = true,
            isSmallProject = true,
            isScrapFriendly = true,
            requiresMachine = false,
            intendedUse = "Gift"
        ),
        InspirationPrompt(
            title = "Zippered Box Pouch",
            craftType = "Sewing",
            description = "Construct a compact boxy pencil or notions pouch from woven fabric remnants and a zipper.",
            suggestedSearchQuery = "diy zippered box pouch fabric remnants tutorial",
            applicableCategory = "Fabric",
            isBeginnerFriendly = false,
            isSmallProject = true,
            isScrapFriendly = true,
            requiresMachine = true,
            intendedUse = "Home Organization"
        ),
        InspirationPrompt(
            title = "Ribbon Bookmark with Charm",
            craftType = "Paper / General Craft",
            description = "Fold a length of patterned ribbon and secure with ribbon clamps and a small bead or charm.",
            suggestedSearchQuery = "ribbon bookmark with charm tutorial easy",
            applicableCategory = "Ribbon / Thread / Trim",
            isBeginnerFriendly = true,
            isSmallProject = true,
            isScrapFriendly = true,
            requiresMachine = false,
            intendedUse = "Gift"
        ),
        InspirationPrompt(
            title = "Beaded Stitch Markers & Progress Keepers",
            craftType = "Jewelry / Knitting",
            description = "Thread accent beads onto headpins or wire jump rings for snag-free knitting or crochet stitch markers.",
            suggestedSearchQuery = "diy beaded snag free stitch markers tutorial",
            applicableCategory = "Beads & Jewelry",
            isBeginnerFriendly = true,
            isSmallProject = true,
            isScrapFriendly = true,
            requiresMachine = false,
            intendedUse = "Accessory"
        ),
        InspirationPrompt(
            title = "Scrap-Yarn Granny Square Motif",
            craftType = "Crochet",
            description = "Crochet classic or sunburst granny squares to use up odd yardage and colorway remnants.",
            suggestedSearchQuery = "crochet scrap yarn granny square pattern beginner",
            applicableCategory = "Yarn",
            isBeginnerFriendly = true,
            isSmallProject = true,
            isScrapFriendly = true,
            requiresMachine = false,
            intendedUse = "Decorative"
        ),
        InspirationPrompt(
            title = "Amigurumi Mini Motif or Keychain",
            craftType = "Crochet / Knitting",
            description = "Create tiny acorns, stars, or geometric accents using small leftover yarn balls.",
            suggestedSearchQuery = "small crochet amigurumi motif scrap yarn tutorial",
            applicableCategory = "Yarn",
            isBeginnerFriendly = false,
            isSmallProject = true,
            isScrapFriendly = true,
            requiresMachine = false,
            intendedUse = "Accessory"
        ),
        InspirationPrompt(
            title = "Friendship & Kumihimo Braided Cords",
            craftType = "Embroidery / Thread",
            description = "Braid leftover embroidery floss or micro-cords into colorful key lanyards or wristbands.",
            suggestedSearchQuery = "diy braided friendship bracelet embroidery floss easy",
            applicableCategory = "Ribbon / Thread / Trim",
            isBeginnerFriendly = true,
            isSmallProject = true,
            isScrapFriendly = true,
            requiresMachine = false,
            intendedUse = "Gift"
        ),
        InspirationPrompt(
            title = "Small Cross Stitch Bookmark Edge",
            craftType = "Cross Stitch / Embroidery",
            description = "Stitch a geometric border or floral mini motif onto perforated cardstock or aida tape.",
            suggestedSearchQuery = "small cross stitch bookmark motifs free chart",
            applicableCategory = "Ribbon / Thread / Trim",
            isBeginnerFriendly = true,
            isSmallProject = true,
            isScrapFriendly = true,
            requiresMachine = false,
            intendedUse = "Gift"
        ),
        InspirationPrompt(
            title = "Painted Wooden Trinket Tray",
            craftType = "Painting / Mixed Media",
            description = "Use acrylic media or chalk paint remnants to give an organic geometric design to a small tray or coaster.",
            suggestedSearchQuery = "diy painted wooden trinket dish acrylic easy",
            applicableCategory = "Paint & Liquid Media",
            isBeginnerFriendly = true,
            isSmallProject = true,
            isScrapFriendly = true,
            requiresMachine = false,
            intendedUse = "Home Organization"
        ),
        InspirationPrompt(
            title = "Paper Collage Gift Tags",
            craftType = "Paper / Cardmaking",
            description = "Layer cardstock scraps, patterned vinyl, and metallic stickers to make decorative botanical gift tags.",
            suggestedSearchQuery = "handmade paper scrap gift tags ideas",
            applicableCategory = "Paper & Vinyl",
            isBeginnerFriendly = true,
            isSmallProject = true,
            isScrapFriendly = true,
            requiresMachine = false,
            intendedUse = "Gift"
        )
    )

    /**
     * Finds inspiration prompts matching the user's selected supplies and filter preferences.
     */
    fun findPrompts(
        selectedSupplies: List<SupplyWithBatches>,
        craftFilter: String? = null,
        beginnerOnly: Boolean = false,
        smallProjectOnly: Boolean = false,
        noSewingMachine: Boolean = false,
        intendedUseFilter: String? = null
    ): List<InspirationPrompt> {
        val categories = selectedSupplies.map { it.supply.category.lowercase() }.toSet()

        return BUNDLED_PROMPTS.filter { prompt ->
            if (beginnerOnly && !prompt.isBeginnerFriendly) return@filter false
            if (smallProjectOnly && !prompt.isSmallProject) return@filter false
            if (noSewingMachine && prompt.requiresMachine) return@filter false
            if (!craftFilter.isNullOrBlank() && !prompt.craftType.contains(craftFilter, ignoreCase = true)) return@filter false
            if (!intendedUseFilter.isNullOrBlank() && !prompt.intendedUse.equals(intendedUseFilter, ignoreCase = true)) return@filter false

            if (categories.isNotEmpty()) {
                val matchesCategory = categories.any { cat ->
                    prompt.applicableCategory.lowercase().contains(cat) || cat.contains(prompt.applicableCategory.lowercase())
                }
                if (!matchesCategory) return@filter false
            }

            true
        }
    }

    /**
     * Generates a clean, privacy-conscious search phrase based strictly on public product attributes.
     * Never leaks private notes, storage locations, or user details!
     */
    fun generateSearchQuery(
        prompt: InspirationPrompt?,
        selectedSupplies: List<SupplyWithBatches>,
        craftOverride: String? = null
    ): String {
        val craftPart = craftOverride ?: prompt?.craftType?.split("/")?.firstOrNull()?.trim() ?: "craft"
        val supplyTerms = mutableListOf<String>()

        for (s in selectedSupplies.take(3)) {
            val mat = s.supply.material.trim()
            val cat = s.supply.category.trim()
            val hasRemnants = s.batches.any { it.condition.contains("remnant", ignoreCase = true) || it.condition.contains("scrap", ignoreCase = true) }

            if (mat.isNotBlank() && !mat.equals(cat, ignoreCase = true)) {
                supplyTerms.add(mat.lowercase())
            }
            if (cat.isNotBlank()) {
                supplyTerms.add(cat.lowercase())
            }
            if (hasRemnants) {
                supplyTerms.add("scraps")
            }
        }

        val baseQuery = prompt?.suggestedSearchQuery
        if (baseQuery != null && selectedSupplies.isEmpty()) {
            return baseQuery
        }

        val distinctTerms = supplyTerms.distinct().joinToString(" ")
        return if (prompt != null) {
            "${prompt.title.lowercase()} $distinctTerms tutorial".replace("  ", " ").trim()
        } else {
            "easy $craftPart project ideas $distinctTerms".replace("  ", " ").trim()
        }
    }
}
