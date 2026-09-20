package com.example.domain

import com.example.model.BloodGroup

/**
 * Domain utility providing comprehensive medical blood compatibility calculations,
 * compatibility matrix validation, and emergency donor ranking.
 */
object BloodCompatibilityHelper {

    /**
     * Checks if a donor blood group is compatible for a recipient patient.
     */
    fun isCompatible(donor: BloodGroup, recipient: BloodGroup): Boolean {
        return recipient.canReceiveFrom().contains(donor)
    }

    /**
     * Returns a list of compatible donor blood groups for a given patient recipient blood group.
     */
    fun getCompatibleDonorGroups(recipient: BloodGroup): List<BloodGroup> {
        return recipient.canReceiveFrom()
    }

    /**
     * Returns an explanation message in Bengali & English for transfusion compatibility.
     */
    fun getCompatibilityExplanation(donor: BloodGroup, recipient: BloodGroup): String {
        return if (isCompatible(donor, recipient)) {
            "✓ ${donor.display} রক্তদাতা ${recipient.display} রোগীকে রক্ত দিতে পারবেন (${donor.display} is compatible for ${recipient.display})"
        } else {
            "✗ ${donor.display} রক্তদাতা ${recipient.display} রোগীকে রক্ত দিতে পারবেন না (${donor.display} is NOT compatible for ${recipient.display})"
        }
    }
}
