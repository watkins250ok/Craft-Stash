package com.example.domain.measurement

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

enum class DimensionType {
    MASS,
    LENGTH,
    VOLUME,
    COUNT,
    AREA,
    GENERIC
}

object UnitConverter {
    private val massFactorsToGrams = mapOf(
        "g" to 1.0,
        "gram" to 1.0,
        "grams" to 1.0,
        "kg" to 1000.0,
        "kilogram" to 1000.0,
        "kilograms" to 1000.0,
        "oz" to 28.349523125,
        "ounce" to 28.349523125,
        "ounces" to 28.349523125,
        "lb" to 453.59237,
        "pound" to 453.59237,
        "pounds" to 453.59237
    )

    private val lengthFactorsToMeters = mapOf(
        "m" to 1.0,
        "meter" to 1.0,
        "meters" to 1.0,
        "cm" to 0.01,
        "centimeter" to 0.01,
        "centimeters" to 0.01,
        "mm" to 0.001,
        "millimeter" to 0.001,
        "yd" to 0.9144,
        "yard" to 0.9144,
        "yards" to 0.9144,
        "ft" to 0.3048,
        "foot" to 0.3048,
        "feet" to 0.3048,
        "in" to 0.0254,
        "inch" to 0.0254,
        "inches" to 0.0254
    )

    private val volumeFactorsToMl = mapOf(
        "ml" to 1.0,
        "milliliter" to 1.0,
        "milliliters" to 1.0,
        "l" to 1000.0,
        "liter" to 1000.0,
        "liters" to 1000.0,
        "fl oz" to 29.5735,
        "fluid ounce" to 29.5735,
        "fluid ounces" to 29.5735,
        "cup" to 236.588,
        "cups" to 236.588,
        "pint" to 473.176,
        "pints" to 473.176
    )

    private val countUnits = setOf(
        "piece", "pieces", "item", "items", "skein", "skeins", "ball", "balls",
        "spool", "spools", "bottle", "bottles", "sheet", "sheets", "set", "sets",
        "pair", "pairs", "count"
    )

    fun getDimensionType(unit: String?): DimensionType {
        if (unit == null) return DimensionType.GENERIC
        val normalized = unit.trim().lowercase()
        return when {
            massFactorsToGrams.containsKey(normalized) -> DimensionType.MASS
            lengthFactorsToMeters.containsKey(normalized) -> DimensionType.LENGTH
            volumeFactorsToMl.containsKey(normalized) -> DimensionType.VOLUME
            countUnits.contains(normalized) -> DimensionType.COUNT
            else -> DimensionType.GENERIC
        }
    }

    fun areUnitsCompatible(unitA: String?, unitB: String?): Boolean {
        if (unitA.isNullOrBlank() || unitB.isNullOrBlank()) return false
        val normA = unitA.trim().lowercase()
        val normB = unitB.trim().lowercase()
        if (normA == normB) return true
        val typeA = getDimensionType(normA)
        val typeB = getDimensionType(normB)
        if (typeA == DimensionType.GENERIC || typeB == DimensionType.GENERIC) {
            return normA == normB
        }
        return typeA == typeB
    }

    /**
     * Converts a value from sourceUnit to targetUnit.
     * Returns null if units are incompatible or either unit is undefined.
     */
    fun convert(value: Double, sourceUnit: String?, targetUnit: String?): Double? {
        if (sourceUnit.isNullOrBlank() || targetUnit.isNullOrBlank()) return null
        val normSrc = sourceUnit.trim().lowercase()
        val normTarget = targetUnit.trim().lowercase()
        if (normSrc == normTarget) return value

        val typeSrc = getDimensionType(normSrc)
        val typeTarget = getDimensionType(normTarget)
        if (typeSrc != typeTarget || typeSrc == DimensionType.GENERIC) {
            return null
        }

        return when (typeSrc) {
            DimensionType.MASS -> {
                val toGrams = massFactorsToGrams[normSrc] ?: return null
                val fromGrams = massFactorsToGrams[normTarget] ?: return null
                val grams = value * toGrams
                grams / fromGrams
            }
            DimensionType.LENGTH -> {
                val toMeters = lengthFactorsToMeters[normSrc] ?: return null
                val fromMeters = lengthFactorsToMeters[normTarget] ?: return null
                val meters = value * toMeters
                meters / fromMeters
            }
            DimensionType.VOLUME -> {
                val toMl = volumeFactorsToMl[normSrc] ?: return null
                val fromMl = volumeFactorsToMl[normTarget] ?: return null
                val ml = value * toMl
                ml / fromMl
            }
            DimensionType.COUNT -> {
                if (normSrc == normTarget) value else null
            }
            else -> null
        }
    }

    fun formatQuantity(value: Double?): String {
        if (value == null) return ""
        val df = DecimalFormat("#,##0.##")
        return df.format(value)
    }

    /**
     * Formats a display string representing a quantity and unit honestly.
     */
    fun formatDisplayQuantity(value: Double?, unit: String?, mode: com.example.domain.model.QuantityMode): String {
        return when (mode) {
            com.example.domain.model.QuantityMode.PRESENCE_ONLY -> "On hand (untracked)"
            com.example.domain.model.QuantityMode.APPROXIMATE -> {
                if (value == null) "~Some" else "~${formatQuantity(value)} ${unit.orEmpty()}".trim()
            }
            com.example.domain.model.QuantityMode.EXACT -> {
                if (value == null) "Unknown quantity" else "${formatQuantity(value)} ${unit.orEmpty()}".trim()
            }
        }
    }

    /**
     * Checks if a physical fabric/paper piece of dimensions (availW x availL)
     * can satisfy required dimensions (reqW x reqL) in either orientation.
     */
    fun canPieceSatisfyDimensions(
        availW: Double?,
        availL: Double?,
        reqW: Double?,
        reqL: Double?
    ): Boolean {
        if (reqW == null || reqL == null) return true
        if (availW == null || availL == null) return false
        val fitsNormal = (availW >= reqW && availL >= reqL)
        val fitsRotated = (availW >= reqL && availL >= reqW)
        return fitsNormal || fitsRotated
    }
}
