package com.example.model

/**
 * Standard ABO and Rh blood types with medical compatibility mappings
 * for Whole Blood / Packed Red Blood Cells (RBC).
 */
enum class BloodGroup(val display: String, val bengaliDisplay: String) {
    A_POSITIVE("A+", "এ পজিটিভ"),
    A_NEGATIVE("A-", "এ নেগেটিভ"),
    B_POSITIVE("B+", "বি পজিটিভ"),
    B_NEGATIVE("B-", "বি নেগেটিভ"),
    AB_POSITIVE("AB+", "এবি পজিটিভ"),
    AB_NEGATIVE("AB-", "এবি নেগেটিভ"),
    O_POSITIVE("O+", "ও পজিটিভ"),
    O_NEGATIVE("O-", "ও নেগেটিভ");

    /**
     * Determines which blood groups this blood group can donate to.
     */
    fun canDonateTo(): List<BloodGroup> {
        return when (this) {
            A_POSITIVE -> listOf(A_POSITIVE, AB_POSITIVE)
            A_NEGATIVE -> listOf(A_POSITIVE, A_NEGATIVE, AB_POSITIVE, AB_NEGATIVE)
            B_POSITIVE -> listOf(B_POSITIVE, AB_POSITIVE)
            B_NEGATIVE -> listOf(B_POSITIVE, B_NEGATIVE, AB_POSITIVE, AB_NEGATIVE)
            AB_POSITIVE -> listOf(AB_POSITIVE)
            AB_NEGATIVE -> listOf(AB_POSITIVE, AB_NEGATIVE)
            O_POSITIVE -> listOf(O_POSITIVE, A_POSITIVE, B_POSITIVE, AB_POSITIVE)
            O_NEGATIVE -> values().toList() // Universal red cell donor
        }
    }

    /**
     * Determines which blood groups this blood group can receive from.
     */
    fun canReceiveFrom(): List<BloodGroup> {
        return when (this) {
            A_POSITIVE -> listOf(A_POSITIVE, A_NEGATIVE, O_POSITIVE, O_NEGATIVE)
            A_NEGATIVE -> listOf(A_NEGATIVE, O_NEGATIVE)
            B_POSITIVE -> listOf(B_POSITIVE, B_NEGATIVE, O_POSITIVE, O_NEGATIVE)
            B_NEGATIVE -> listOf(B_NEGATIVE, O_NEGATIVE)
            AB_POSITIVE -> values().toList() // Universal red cell recipient
            AB_NEGATIVE -> listOf(AB_NEGATIVE, A_NEGATIVE, B_NEGATIVE, O_NEGATIVE)
            O_POSITIVE -> listOf(O_POSITIVE, O_NEGATIVE)
            O_NEGATIVE -> listOf(O_NEGATIVE)
        }
    }

    companion object {
        fun fromString(value: String?): BloodGroup? {
            if (value == null) return null
            val normalized = value.trim().uppercase().replace(" ", "_")
            return entries.firstOrNull {
                it.name == normalized || it.display.equals(value.trim(), ignoreCase = true)
            }
        }
    }
}
